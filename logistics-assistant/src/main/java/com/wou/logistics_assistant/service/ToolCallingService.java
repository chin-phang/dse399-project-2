package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.chat.ToolResult;
import java.util.List;
import java.util.Map;

/**
 * An explicit, non-LLM invocation path over the same tool logic — used by tests and manual
 * exercise of a tool without a live model.
 *
 * Not on the LLM's runtime path. When the model calls a tool, Spring AI's auto-registered
 * {@code ToolCallingAdvisor} invokes the {@code @Tool} method on {@code LogisticsTools} straight
 * off the {@code ChatClient}, bypassing this service entirely.
 */
public interface ToolCallingService {

	String resolveTool(String prompt);

	ToolResult executeTool(String tool, Map<String, Object> arguments);

	List<String> availableTools();
}
