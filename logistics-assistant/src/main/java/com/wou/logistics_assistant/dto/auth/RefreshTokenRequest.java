package com.wou.logistics_assistant.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank(message = "refreshToken is required") String refreshToken) {
}
