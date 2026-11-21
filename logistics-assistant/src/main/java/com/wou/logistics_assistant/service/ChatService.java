package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.chat.ChatRequest;
import com.wou.logistics_assistant.dto.chat.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/** FR01/UC01 — the conversational assistant. */
public interface ChatService {

	/** Blocking façade, used by tests and Postman. Not exposed over HTTP. */
	ChatResponse chat(ChatRequest request);

	/**
	 * What the real frontend calls: {@code ChatClient.stream()} output encoded as AI SDK UI
	 * Message Stream Protocol v1 frames, with tool-call events merged in.
	 */
	Flux<ServerSentEvent<String>> chatStream(ChatRequest request);
}
