package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.chat.ChatRequest;
import com.wou.logistics_assistant.dto.chat.ChatResponse;
import com.wou.logistics_assistant.dto.chat.ToolResult;
import com.wou.logistics_assistant.security.util.SecurityUtils;
import com.wou.logistics_assistant.service.ChatService;
import com.wou.logistics_assistant.service.ConversationMemoryService;
import com.wou.logistics_assistant.tool.LogisticsTools;
import com.wou.logistics_assistant.util.AiSdkStreamEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ChatServiceImpl implements ChatService {

	public static final String ANONYMOUS = "anonymous";

	public static final String AI_UNAVAILABLE_MESSAGE =
		"The AI assistant is temporarily unavailable. Please try again in a moment.";

	private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

	private final ChatClient chatClient;
	private final ConversationMemoryService conversationMemoryService;
	private final AiSdkStreamEncoder encoder;

	public ChatServiceImpl(
		ChatClient chatClient,
		ConversationMemoryService conversationMemoryService,
		AiSdkStreamEncoder encoder
	) {
		this.chatClient = chatClient;
		this.conversationMemoryService = conversationMemoryService;
		this.encoder = encoder;
	}

	@Override
	public ChatResponse chat(ChatRequest request) {
		String username = currentUsername();
		String conversationId = conversationMemoryService.conversationId(username, request.sessionId());
		Sinks.Many<ToolResult> sink = Sinks.many().multicast().onBackpressureBuffer();

		try {
			String content = chatClient
				.prompt()
				.user(request.message())
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
				.toolContext(toolContext(sink, username))
				.call()
				.content();
			sink.tryEmitComplete();
			List<ToolResult> toolCalls = sink.asFlux().collectList().block();
			return new ChatResponse(content == null ? "" : content, conversationId, toolCalls, null);
		} catch (Exception ex) {
			// UC01/UC02 alternative flow: degrade gracefully rather than surfacing a raw failure.
			log.error("Chat request failed for conversation {}", conversationId, ex);
			sink.tryEmitComplete();
			String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
			return new ChatResponse(AI_UNAVAILABLE_MESSAGE, conversationId, List.of(), error);
		}
	}

	/**
	 * Composition matters here, and each rule below is a hang or a malformed message if dropped:
	 *
	 * <ol>
	 * <li>{@code concat} the bookends around a {@code merge} of only the two open-ended sources.
	 * {@code merge} interleaves by arrival, so a {@code start} frame placed inside it can be
	 * overtaken by a fast tool event and {@code useChat} discards the whole message.</li>
	 * <li>Complete the tool sink when the token flux terminates. A merged {@code Flux} stays open
	 * while any source is open, so a forgotten completion hangs the response after the answer has
	 * already rendered.</li>
	 * <li>Convert failures to an {@code error} frame instead of throwing: once the first SSE byte
	 * is flushed the status is committed, and a thrown exception reaches the browser as a
	 * truncated 200 with no error state.</li>
	 * </ol>
	 */
	@Override
	public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
		String username = currentUsername();
		String conversationId = conversationMemoryService.conversationId(username, request.sessionId());
		String messageId = UUID.randomUUID().toString();
		String textId = UUID.randomUUID().toString();
		Sinks.Many<ToolResult> toolEvents = Sinks.many().unicast().onBackpressureBuffer();

		Flux<ServerSentEvent<String>> tokens = chatClient
			.prompt()
			.user(request.message())
			.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
			.toolContext(toolContext(toolEvents, username))
			.stream()
			.content()
			.map(delta -> encoder.textDelta(textId, delta))
			.doFinally(signal -> toolEvents.tryEmitComplete());

		Flux<ServerSentEvent<String>> toolFrames = toolEvents.asFlux().map(encoder::toolCall);

		return Flux.concat(
			Flux.just(encoder.start(messageId), encoder.textStart(textId)),
			Flux.merge(tokens, toolFrames),
			Flux.just(encoder.textEnd(textId), encoder.finish(), encoder.done())
		).onErrorResume(ex -> {
			// The detail stays in the server log; the frame carries a generic message.
			// A provider exception otherwise reaches the browser verbatim — an unauthorised
			// key echoes its own prefix back, along with internal class names.
			log.error("Chat stream failed for conversation {}", conversationId, ex);
			toolEvents.tryEmitComplete();
			return Flux.just(encoder.error(AI_UNAVAILABLE_MESSAGE), encoder.finish(), encoder.done());
		}).doOnCancel(() -> toolEvents.tryEmitComplete());
	}

	/**
	 * Per-request state for {@code @Tool} methods. The principal is read here, on the request
	 * thread — by the time a tool runs, execution has moved to a Reactor thread where the
	 * ThreadLocal-bound security context is empty.
	 */
	private Map<String, Object> toolContext(Sinks.Many<ToolResult> sink, String username) {
		Map<String, Object> context = new HashMap<>();
		context.put(LogisticsTools.TOOL_EVENT_SINK, sink);
		context.put(LogisticsTools.PRINCIPAL, username);
		return context;
	}

	private String currentUsername() {
		String username = SecurityUtils.currentUsername();
		return username == null ? ANONYMOUS : username;
	}
}
