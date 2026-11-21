package com.wou.logistics_assistant.dto.chat;

import java.util.List;

public record ChatResponse(String content, String sessionId, List<ToolResult> toolCalls, String error) {

	public ChatResponse {
		if (toolCalls == null) {
			toolCalls = List.of();
		}
	}
}
