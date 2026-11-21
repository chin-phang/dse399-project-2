package com.wou.logistics_assistant.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.wou.logistics_assistant.dto.chat.ToolResult;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * The wire format is the contract with {@code useChat}. These assertions pin the exact part types
 * of the AI SDK UI Message Stream Protocol v1 — getting one wrong makes the client silently drop
 * the message.
 */
class AiSdkStreamEncoderTest {

	private final AiSdkStreamEncoder encoder = new AiSdkStreamEncoder(new ObjectMapper());

	@Test
	void emitsTheProtocolPartTypes() {
		assertThat(encoder.start("m1").data()).contains("\"type\":\"start\"", "\"messageId\":\"m1\"");
		assertThat(encoder.textStart("t1").data()).contains("\"type\":\"text-start\"", "\"id\":\"t1\"");
		assertThat(encoder.textDelta("t1", "Hello").data()).contains("\"type\":\"text-delta\"", "\"delta\":\"Hello\"");
		assertThat(encoder.textEnd("t1").data()).contains("\"type\":\"text-end\"");
		assertThat(encoder.finish().data()).contains("\"type\":\"finish\"");
	}

	@Test
	void terminatesWithTheLiteralDoneSentinel() {
		assertThat(encoder.done().data()).isEqualTo("[DONE]");
	}

	@Test
	void toolCallsBecomeACustomDataPart() {
		String frame = encoder.toolCall(
			new ToolResult("listAvailableVehicles", Map.of("status", "AVAILABLE"), "3 result(s)")
		).data();

		assertThat(frame).contains("\"type\":\"data-toolCall\"");
		assertThat(frame).contains("listAvailableVehicles");
		assertThat(frame).contains("3 result(s)");
	}

	@Test
	void errorsAreFramesCarryingErrorText() {
		assertThat(encoder.error("provider timed out").data()).contains("\"type\":\"error\"", "\"errorText\":\"provider timed out\"");
	}

	@Test
	void escapesPayloadsThatWouldOtherwiseBreakTheFrame() {
		String frame = encoder.textDelta("t1", "line\"one\"\nline two").data();

		assertThat(frame).contains("\\\"one\\\"");
		assertThat(frame).contains("\\n");
		assertThat(frame.lines()).hasSize(1);
	}
}
