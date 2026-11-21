package com.wou.logistics_assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.wou.logistics_assistant.service.impl.ConversationMemoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * TC-FR03-001 — conversation memory keying.
 *
 * Chat memory is a shared, durable Postgres table, so the key decides who can read whose
 * conversation. These assertions are a security boundary, not a naming preference.
 */
class ConversationMemoryServiceTest {

	private final ConversationMemoryService service = new ConversationMemoryServiceImpl(mock(ChatMemory.class));

	@Test
	void namespacesTheClientSessionIdWithTheUser() {
		assertThat(service.conversationId("planner", "tab-1")).isEqualTo("planner:tab-1");
	}

	@Test
	void twoUsersSendingTheSameSessionIdGetDifferentConversations() {
		String alice = service.conversationId("alice", "tab-1");
		String bob = service.conversationId("bob", "tab-1");

		assertThat(alice).isNotEqualTo(bob);
	}

	@Test
	void oneUserKeepsSeparateThreadsPerSessionId() {
		assertThat(service.conversationId("alice", "tab-1")).isNotEqualTo(service.conversationId("alice", "tab-2"));
	}

	@Test
	void fallsBackToADefaultSessionWhenTheClientSendsNone() {
		assertThat(service.conversationId("alice", null)).isEqualTo("alice:default");
		assertThat(service.conversationId("alice", "  ")).isEqualTo("alice:default");
	}

	@Test
	void aClientCannotEscapeItsNamespaceBySupplyingAnotherUsersPrefix() {
		String forged = service.conversationId("bob", "alice:tab-1");

		assertThat(forged).startsWith("bob:");
		assertThat(forged).isNotEqualTo(service.conversationId("alice", "tab-1"));
	}
}
