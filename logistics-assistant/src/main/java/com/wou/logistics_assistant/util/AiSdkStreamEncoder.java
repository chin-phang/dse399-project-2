package com.wou.logistics_assistant.util;

import com.wou.logistics_assistant.dto.chat.ToolResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Builds the SSE frames for the AI SDK UI Message Stream Protocol v1 (AI SDK 5).
 *
 * This is not the older AI SDK v4 "Data Stream Protocol" — that used an
 * {@code x-vercel-ai-data-stream} header and {@code 0:"…"} frames, and following those docs
 * produces a stream {@code useChat} cannot read.
 *
 * Lives in {@code util/} rather than {@code mapper/} deliberately: this is wire-format encoding,
 * not entity-to-DTO mapping.
 */
@Component
public class AiSdkStreamEncoder {

	/** {@code useChat} checks for this header to select the UI Message Stream Protocol. */
	public static final String STREAM_HEADER = "x-vercel-ai-ui-message-stream";

	public static final String STREAM_HEADER_VALUE = "v1";

	private final ObjectMapper objectMapper;

	public AiSdkStreamEncoder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ServerSentEvent<String> start(String messageId) {
		return frame(mapOf("type", "start", "messageId", messageId));
	}

	public ServerSentEvent<String> textStart(String id) {
		return frame(mapOf("type", "text-start", "id", id));
	}

	public ServerSentEvent<String> textDelta(String id, String delta) {
		return frame(mapOf("type", "text-delta", "id", id, "delta", delta));
	}

	public ServerSentEvent<String> textEnd(String id) {
		return frame(mapOf("type", "text-end", "id", id));
	}

	/**
	 * A custom data part carrying one completed tool call, which the AI Chat module's tool-calling
	 * panel renders. Custom rather than the SDK's native {@code tool-input-*} parts because Spring
	 * AI runs the whole tool loop server-side and only surfaces a call once it has finished.
	 */
	public ServerSentEvent<String> toolCall(ToolResult result) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("toolName", result.toolName());
		data.put("arguments", result.arguments());
		data.put("result", result.result());
		data.put("error", result.error());
		return frame(mapOf("type", "data-toolCall", "data", data));
	}

	/**
	 * Delivered as a frame, never as an HTTP status: by the time this is emitted the response is
	 * already committed, so throwing would reach the browser as a truncated 200 with no error
	 * state. This is the UC01/UC02 AI-unavailable alternative flow.
	 */
	public ServerSentEvent<String> error(String message) {
		return frame(mapOf("type", "error", "errorText", message));
	}

	public ServerSentEvent<String> finish() {
		return frame(mapOf("type", "finish"));
	}

	/** Terminator the protocol requires; {@code useChat} treats the stream as open until it arrives. */
	public ServerSentEvent<String> done() {
		return ServerSentEvent.<String>builder().data("[DONE]").build();
	}

	private ServerSentEvent<String> frame(Map<String, Object> payload) {
		return ServerSentEvent.<String>builder().data(objectMapper.writeValueAsString(payload)).build();
	}

	private static Map<String, Object> mapOf(Object... pairs) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put((String) pairs[i], pairs[i + 1]);
		}
		return map;
	}
}
