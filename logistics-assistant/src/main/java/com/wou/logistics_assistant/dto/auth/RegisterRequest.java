package com.wou.logistics_assistant.dto.auth;

import com.wou.logistics_assistant.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	@NotBlank(message = "username is required") @Size(min = 3, max = 100) String username,
	@NotBlank(message = "password is required")
	@Size(min = 8, max = 72, message = "password must be 8-72 characters")
	String password,
	Role role
) {

	public Role roleOrDefault() {
		return role == null ? Role.ROUTE_PLANNER : role;
	}
}
