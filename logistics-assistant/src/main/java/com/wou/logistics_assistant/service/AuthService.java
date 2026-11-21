package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.auth.LoginRequest;
import com.wou.logistics_assistant.dto.auth.LoginResponse;
import com.wou.logistics_assistant.dto.auth.RefreshTokenRequest;
import com.wou.logistics_assistant.dto.auth.RegisterRequest;

/** The only place a JWT is generated. */
public interface AuthService {

	LoginResponse login(LoginRequest request);

	void register(RegisterRequest request);

	LoginResponse refresh(RefreshTokenRequest request);
}
