package com.wou.logistics_assistant.dto.chat;

import java.util.Map;

/** The outcome of one tool invocation, as surfaced to the UI's tool-calling panel. */
public record ToolResult(String toolName, Map<String, Object> arguments, String result, String error) {

	public ToolResult {
		if (arguments == null) {
			arguments = Map.of();
		}
	}

	public ToolResult(String toolName, Map<String, Object> arguments, String result) {
		this(toolName, arguments, result, null);
	}
}
