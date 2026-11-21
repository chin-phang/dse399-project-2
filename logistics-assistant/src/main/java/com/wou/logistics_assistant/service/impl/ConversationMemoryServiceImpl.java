package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.service.ConversationMemoryService;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

	public static final String DEFAULT_SESSION = "default";

	private final ChatMemory chatMemory;

	public ConversationMemoryServiceImpl(ChatMemory chatMemory) {
		this.chatMemory = chatMemory;
	}

	/**
	 * Namespaces the client's session id with the authenticated user.
	 *
	 * Chat memory is a shared, durable Postgres table. Keying it on the raw client value would
	 * let any authenticated user read and append to another user's conversation simply by sending
	 * their session id, so the id the client supplies is treated as an opaque per-tab label with
	 * no security meaning.
	 */
	@Override
	public String conversationId(String username, String sessionId) {
		String session = sessionId == null || sessionId.isBlank() ? DEFAULT_SESSION : sessionId;
		return username + ":" + session;
	}

	@Override
	public List<Message> getConversation(String conversationId) {
		return chatMemory.get(conversationId);
	}

	@Override
	public void clearConversation(String conversationId) {
		chatMemory.clear(conversationId);
	}
}
