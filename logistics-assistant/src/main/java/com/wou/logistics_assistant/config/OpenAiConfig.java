package com.wou.logistics_assistant.config;

import com.wou.logistics_assistant.tool.LogisticsTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

@Configuration
public class OpenAiConfig {

	/**
	 * Window capped at 20 messages: long-context requests bill at a higher tier, so an unbounded
	 * window has a direct cost consequence.
	 *
	 * Backed by the JDBC repository (not an in-memory map) so conversation memory survives
	 * restarts and any instance can serve any session.
	 */
	public static final int MAX_MEMORY_MESSAGES = 20;

	public static final String SYSTEM_PROMPT = """
		You are the route planning assistant for a logistics operator.

		You help Route Planners manage delivery jobs and vehicles, and produce route planning
		recommendations. Use the provided tools to read and change real data rather than
		guessing or inventing records. When a tool returns results, summarise them for the
		planner in plain language.

		Guidelines:
		- Never invent job IDs, vehicle IDs or route IDs. Look them up with a tool first.
		- A newly created job has no coordinates unless the planner supplies them. Say so
		  rather than assuming a location.
		- When recommending a route, explain briefly why the stops are in that order.
		- Distances are in metres, durations in seconds, fuel in litres. Convert to friendlier
		  units when you present them.
		- If a tool fails, tell the planner plainly what went wrong and what they could try.
		""";

	@Bean
	public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(chatMemoryRepository)
			.maxMessages(MAX_MEMORY_MESSAGES)
			.build();
	}

	/**
	 * Tools are registered here with {@code defaultTools}. Spring AI auto-registers the
	 * {@code ToolCallingAdvisor} that drives the call/execute/respond loop, so there is no
	 * hand-written tool loop anywhere in this codebase.
	 */
	@Bean
	public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, LogisticsTools logisticsTools) {
		return builder
			.defaultSystem(SYSTEM_PROMPT)
			.defaultTools(logisticsTools)
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
			.build();
	}

	/**
	 * HTTP timeouts live here, in code: Spring AI exposes no configuration property for them, and
	 * {@code spring.ai.openai.chat.options} holds model parameters only.
	 *
	 * Both clients are configured because Spring AI calls through {@code RestClient} and streams
	 * through {@code WebClient} — setting one leaves the other unbounded, and the UI only ever
	 * exercises the streaming path.
	 *
	 * These are deliberately far above the 10s NFR: that budget is time-to-first-token and is
	 * enforced client-side, so a long but healthy answer must not be killed by the transport.
	 */
	@Bean
	public RestClientCustomizer openAiRestClientCustomizer(AiProperties properties) {
		return builder -> {
			SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
			requestFactory.setConnectTimeout(properties.getConnectTimeout());
			requestFactory.setReadTimeout(properties.getReadTimeout());
			builder.requestFactory(requestFactory);
		};
	}

	@Bean
	public WebClientCustomizer openAiWebClientCustomizer(AiProperties properties) {
		return builder -> builder.clientConnector(
			new ReactorClientHttpConnector(HttpClient.create().responseTimeout(properties.getReadTimeout()))
		);
	}
}
