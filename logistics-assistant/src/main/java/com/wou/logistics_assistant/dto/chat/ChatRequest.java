package com.wou.logistics_assistant.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
	@NotBlank(message = "message is required") @Size(max = 4000) String message,
	/**
	 * Opaque per-tab/thread id from the client. It carries no security meaning: the server
	 * namespaces it with the authenticated user id before using it as the chat-memory key, so a
	 * client cannot read another user's conversation by guessing a session id.
	 */
	@Size(max = 100) String sessionId
) {
}
