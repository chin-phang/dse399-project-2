package com.wou.logistics_assistant.dto.auth;

import com.wou.logistics_assistant.entity.Role;

public record LoginResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresInSeconds,
	String username,
	Role role
) {

	public LoginResponse(
		String accessToken,
		String refreshToken,
		long expiresInSeconds,
		String username,
		Role role
	) {
		this(accessToken, refreshToken, "Bearer", expiresInSeconds, username, role);
	}
}
