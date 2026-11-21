package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.auth.LoginRequest;
import com.wou.logistics_assistant.dto.auth.LoginResponse;
import com.wou.logistics_assistant.dto.auth.RefreshTokenRequest;
import com.wou.logistics_assistant.dto.auth.RegisterRequest;
import com.wou.logistics_assistant.entity.User;
import com.wou.logistics_assistant.exception.ValidationException;
import com.wou.logistics_assistant.security.jwt.JwtService;
import com.wou.logistics_assistant.service.AuthService;
import com.wou.logistics_assistant.service.UserService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates the login/register use case: authenticate, load the user, mint the token. */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final UserService userService;
	private final JwtService jwtService;

	public AuthServiceImpl(
		AuthenticationManager authenticationManager,
		UserDetailsService userDetailsService,
		UserService userService,
		JwtService jwtService
	) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@Override
	public LoginResponse login(LoginRequest request) {
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);
		UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
		return issueTokens(userDetails);
	}

	@Override
	public void register(RegisterRequest request) {
		userService.create(request);
	}

	@Override
	public LoginResponse refresh(RefreshTokenRequest request) {
		String username;
		try {
			username = jwtService.extractUsername(request.refreshToken());
		} catch (JwtException ex) {
			throw new ValidationException("Refresh token is invalid or expired");
		}
		if (!jwtService.isRefreshToken(request.refreshToken())) {
			throw new ValidationException("Supplied token is not a refresh token");
		}
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
			throw new ValidationException("Refresh token is invalid or expired");
		}
		return issueTokens(userDetails);
	}

	private LoginResponse issueTokens(UserDetails userDetails) {
		User user = userService.findByUsername(userDetails.getUsername());
		return new LoginResponse(
			jwtService.generateAccessToken(userDetails),
			jwtService.generateRefreshToken(userDetails),
			jwtService.accessTokenTtlSeconds(),
			user.getUsername(),
			user.getRole()
		);
	}
}
