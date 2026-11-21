package com.wou.logistics_assistant.controller;

import com.wou.logistics_assistant.dto.chat.ChatRequest;
import com.wou.logistics_assistant.service.ChatService;
import com.wou.logistics_assistant.util.AiSdkStreamEncoder;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * FR01/FR02/FR03 — the conversational endpoint.
 *
 * Streaming only: there is no {@code /api/chat/sync} route. {@code ChatService.chat()} stays
 * reachable as a service-level façade for tests and Postman, not over HTTP.
 *
 * Spring MVC adapts the {@code Flux} return value to SSE through {@code ReactiveTypeHandler} on
 * the servlet stack — reactor-core is on the classpath transitively via Spring AI, and no WebFlux
 * server is involved.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<Flux<ServerSentEvent<String>>> chat(@Valid @RequestBody ChatRequest request) {
		return ResponseEntity.ok()
			// useChat checks for this header to select the UI Message Stream Protocol. It is
			// also listed in Access-Control-Expose-Headers, or the browser could not read it.
			.header(AiSdkStreamEncoder.STREAM_HEADER, AiSdkStreamEncoder.STREAM_HEADER_VALUE)
			.contentType(MediaType.TEXT_EVENT_STREAM)
			.body(chatService.chatStream(request));
	}
}
