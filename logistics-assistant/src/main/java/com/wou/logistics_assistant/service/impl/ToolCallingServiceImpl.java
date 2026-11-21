package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.chat.ToolResult;
import com.wou.logistics_assistant.service.ToolCallingService;
import com.wou.logistics_assistant.tool.LogisticsTools;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * Explicit invocation path over the same {@code @Tool} methods, for tests and manual exercise
 * without a live model. The LLM never comes through here — it reaches {@code LogisticsTools} via
 * the auto-registered {@code ToolCallingAdvisor}.
 */
@Service
public class ToolCallingServiceImpl implements ToolCallingService {

	private final LogisticsTools logisticsTools;
	private final Map<String, Method> tools = new LinkedHashMap<>();

	public ToolCallingServiceImpl(LogisticsTools logisticsTools) {
		this.logisticsTools = logisticsTools;
		for (Method method : LogisticsTools.class.getDeclaredMethods()) {
			if (method.getAnnotation(Tool.class) != null) {
				method.setAccessible(true);
				tools.put(method.getName(), method);
			}
		}
	}

	@Override
	public List<String> availableTools() {
		return tools.keySet().stream().sorted().toList();
	}

	/** Crude keyword match — enough to route a prompt to a tool in tests without a model. */
	@Override
	public String resolveTool(String prompt) {
		String normalised = prompt.toLowerCase(Locale.ROOT);
		for (String name : tools.keySet()) {
			String[] parts = name.split("(?=[A-Z])");
			boolean matches = Arrays.stream(parts).allMatch(part -> normalised.contains(part.toLowerCase(Locale.ROOT)));
			if (matches) {
				return name;
			}
		}
		return null;
	}

	@Override
	public ToolResult executeTool(String tool, Map<String, Object> arguments) {
		Map<String, Object> args = arguments == null ? Map.of() : arguments;
		Method function = tools.get(tool);
		if (function == null) {
			return new ToolResult(tool, args, null, "Unknown tool: " + tool);
		}
		try {
			Object[] values = new Object[function.getParameterCount()];
			Parameter[] parameters = function.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				if (ToolContext.class.isAssignableFrom(parameter.getType())) {
					values[i] = new ToolContext(Map.of());
				} else if (args.containsKey(parameter.getName())) {
					values[i] = args.get(parameter.getName());
				} else {
					values[i] = null;
				}
			}
			Object result = function.invoke(logisticsTools, values);
			return new ToolResult(tool, args, result == null ? null : result.toString(), null);
		} catch (InvocationTargetException ex) {
			Throwable cause = ex.getCause() == null ? ex : ex.getCause();
			String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
			return new ToolResult(tool, args, null, message);
		} catch (Exception ex) {
			String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
			return new ToolResult(tool, args, null, message);
		}
	}
}
