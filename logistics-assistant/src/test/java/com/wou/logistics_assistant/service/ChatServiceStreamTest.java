package com.wou.logistics_assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.wou.logistics_assistant.dto.chat.ChatRequest;
import com.wou.logistics_assistant.dto.chat.ToolResult;
import com.wou.logistics_assistant.service.impl.ChatServiceImpl;
import com.wou.logistics_assistant.service.impl.ConversationMemoryServiceImpl;
import com.wou.logistics_assistant.tool.LogisticsTools;
import com.wou.logistics_assistant.util.AiSdkStreamEncoder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-FR01-001 / TC-FR02-001 — the {@code chatStream} contract, and specifically the three
 * composition rules that are invisible until they bite: ordering, sink completion, and error framing.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceStreamTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	private ChatClient.StreamResponseSpec streamSpec;

	@Mock
	private ChatMemory chatMemory;

	private final ArgumentCaptor<Map<String, Object>> toolContextCaptor = ArgumentCaptor.captor();

	private ChatService chatService;

	@BeforeEach
	void setUp() {
		AiSdkStreamEncoder encoder = new AiSdkStreamEncoder(new ObjectMapper());
		ConversationMemoryService memoryService = new ConversationMemoryServiceImpl(chatMemory);
		chatService = new ChatServiceImpl(chatClient, memoryService, encoder);
	}

	private void stubStream(Flux<String> tokens) {
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())).thenReturn(requestSpec);
		when(requestSpec.toolContext(toolContextCaptor.capture())).thenReturn(requestSpec);
		when(requestSpec.stream()).thenReturn(streamSpec);
		when(streamSpec.content()).thenReturn(tokens);
	}

	private List<String> dataLines() {
		return dataLines(new ChatRequest("hello", "tab-1"));
	}

	private List<String> dataLines(ChatRequest request) {
		List<ServerSentEvent<String>> events = chatService.chatStream(request).collectList().block(Duration.ofSeconds(5));
		assertThat(events).isNotNull();
		return events.stream().map(ServerSentEvent::data).toList();
	}

	@Test
	void emitsProtocolFramesInOrderAndTerminatesWithDone() {
		stubStream(Flux.just("Hello", " planner"));

		List<String> frames = dataLines();

		assertThat(frames.getFirst()).contains("\"type\":\"start\"");
		assertThat(frames.get(1)).contains("\"type\":\"text-start\"");
		assertThat(frames.stream().filter(frame -> frame.contains("text-delta")).count()).isEqualTo(2);
		assertThat(frames.get(frames.size() - 3)).contains("\"type\":\"text-end\"");
		assertThat(frames.get(frames.size() - 2)).contains("\"type\":\"finish\"");
		assertThat(frames.getLast()).isEqualTo("[DONE]");
	}

	/**
	 * {@code Flux.merge} stays open while any source is open, so if the tool-event sink is never
	 * completed the response hangs after the answer has already rendered.
	 */
	@Test
	void terminatesEvenWhenAToolEventIsEmittedMidStream() {
		stubStream(Flux.just("Checking", " the fleet").doOnNext(ignored -> {
			@SuppressWarnings("unchecked")
			Sinks.Many<ToolResult> sink = (Sinks.Many<ToolResult>) toolContextCaptor.getValue()
				.get(LogisticsTools.TOOL_EVENT_SINK);
			sink.tryEmitNext(new ToolResult("listAvailableVehicles", Map.of(), "3 result(s)"));
		}));

		StepVerifier.create(chatService.chatStream(new ChatRequest("what vehicles are available?", "tab-1")))
			.expectNextCount(7)
			.thenConsumeWhile(frame -> true)
			.verifyComplete();
	}

	@Test
	void surfacesAToolCallAsADataToolCallFrame() {
		stubStream(Flux.just("ok").doOnNext(ignored -> {
			@SuppressWarnings("unchecked")
			Sinks.Many<ToolResult> sink = (Sinks.Many<ToolResult>) toolContextCaptor.getValue()
				.get(LogisticsTools.TOOL_EVENT_SINK);
			sink.tryEmitNext(new ToolResult("getRouteSummary", Map.of("routeId", 7L), "done"));
		}));

		assertThat(dataLines().stream().anyMatch(frame -> frame.contains("data-toolCall") && frame.contains("getRouteSummary")))
			.isTrue();
	}

	@Test
	void aProviderFailureBecomesAnErrorFrameFollowedByANormalTermination() {
		stubStream(Flux.error(new RuntimeException("401 Incorrect API key provided: sk-abc123")));

		List<String> frames = dataLines();

		assertThat(frames.stream().anyMatch(frame -> frame.contains("\"type\":\"error\""))).isTrue();
		assertThat(frames.getLast()).isEqualTo("[DONE]");
	}

	@Test
	void theErrorFrameDoesNotLeakProviderInternals() {
		stubStream(Flux.error(new RuntimeException("401 Incorrect API key provided: sk-abc123")));

		String errorFrame = dataLines().stream().filter(frame -> frame.contains("\"type\":\"error\"")).findFirst().orElseThrow();

		assertThat(errorFrame).doesNotContain("sk-abc123");
		assertThat(errorFrame).doesNotContain("Incorrect API key");
		assertThat(errorFrame).contains(ChatServiceImpl.AI_UNAVAILABLE_MESSAGE);
	}

	@Test
	void passesTheToolEventSinkAndPrincipalThroughToolContext() {
		stubStream(Flux.just("hi"));

		dataLines();

		Map<String, Object> context = toolContextCaptor.getValue();
		assertThat(context).containsKeys(LogisticsTools.TOOL_EVENT_SINK, LogisticsTools.PRINCIPAL);
		assertThat(context.get(LogisticsTools.TOOL_EVENT_SINK)).isInstanceOf(Sinks.Many.class);
	}
}
