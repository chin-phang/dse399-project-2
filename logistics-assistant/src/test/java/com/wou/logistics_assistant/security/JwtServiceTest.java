package com.wou.logistics_assistant.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.wou.logistics_assistant.security.jwt.JwtProperties;
import com.wou.logistics_assistant.security.jwt.JwtService;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/** Part of TC-NFR-SEC-001 — token creation and validation in isolation. */
class JwtServiceTest {

	private final String secret = Base64.getEncoder().encodeToString(
		"a-test-signing-secret-of-at-least-32-bytes".getBytes()
	);

	private final JwtService jwtService = new JwtService(
		new JwtProperties(secret, Duration.ofMinutes(30), Duration.ofDays(1))
	);

	private final UserDetails user = User.withUsername("planner")
		.password("irrelevant")
		.authorities(List.of(new SimpleGrantedAuthority("ROLE_ROUTE_PLANNER")))
		.build();

	@Test
	void roundTripsTheSubjectAndValidatesAgainstTheSameUser() {
		String token = jwtService.generateAccessToken(user);

		assertThat(jwtService.extractUsername(token)).isEqualTo("planner");
		assertThat(jwtService.isTokenValid(token, user)).isTrue();
		assertThat(jwtService.isExpired(token)).isFalse();
	}

	@Test
	void rejectsATokenIssuedForADifferentUser() {
		String token = jwtService.generateAccessToken(user);
		UserDetails other = User.withUsername("someone-else").password("x").authorities(List.of()).build();

		assertThat(jwtService.isTokenValid(token, other)).isFalse();
	}

	@Test
	void rejectsATokenSignedWithADifferentKey() {
		String otherSecret = Base64.getEncoder().encodeToString("a-completely-different-secret-key-32b".getBytes());
		String foreign = new JwtService(new JwtProperties(otherSecret)).generateAccessToken(user);

		assertThat(jwtService.isTokenValid(foreign, user)).isFalse();
	}

	@Test
	void distinguishesRefreshTokensFromAccessTokens() {
		assertThat(jwtService.isRefreshToken(jwtService.generateRefreshToken(user))).isTrue();
		assertThat(jwtService.isRefreshToken(jwtService.generateAccessToken(user))).isFalse();
	}

	@Test
	void treatsAnExpiredTokenAsInvalidRatherThanThrowing() {
		JwtService shortLived = new JwtService(new JwtProperties(secret, Duration.ofMillis(-1), Duration.ofDays(1)));
		String token = shortLived.generateAccessToken(user);

		assertThat(jwtService.isExpired(token)).isTrue();
		assertThat(jwtService.isTokenValid(token, user)).isFalse();
	}
}
