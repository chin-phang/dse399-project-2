package com.wou.logistics_assistant.service;

import java.util.List;
import org.springframework.ai.chat.messages.Message;

/**
 * FR03/UC03 — a thin façade over Spring AI's {@code ChatMemory}.
 *
 * The real backing store is the JDBC repository, so memory survives restarts and any instance
 * can serve any session.
 */
public interface ConversationMemoryService {

	/**
	 * Builds the chat-memory key. Always {@code "<userId>:<sessionId>"} — never the raw client
	 * session id, which would let one user read another's conversation out of a shared table.
	 */
	String conversationId(String username, String sessionId);

	List<Message> getConversation(String conversationId);

	void clearConversation(String conversationId);
}
