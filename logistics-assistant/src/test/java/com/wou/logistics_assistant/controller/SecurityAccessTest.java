package com.wou.logistics_assistant.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wou.logistics_assistant.security.config.CorsConfiguration;
import com.wou.logistics_assistant.security.config.CorsProperties;
import com.wou.logistics_assistant.security.config.SecurityConfiguration;
import com.wou.logistics_assistant.security.jwt.JwtAccessDeniedHandler;
import com.wou.logistics_assistant.security.jwt.JwtAuthenticationEntryPoint;
import com.wou.logistics_assistant.security.jwt.JwtAuthenticationFilter;
import com.wou.logistics_assistant.security.jwt.JwtProperties;
import com.wou.logistics_assistant.security.jwt.JwtService;
import com.wou.logistics_assistant.service.JobService;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * TC-NFR-SEC-001 — business endpoints are closed by default.
 *
 * The real security configuration and JWT filter are loaded here, so this exercises the actual
 * rule set rather than a stand-in.
 */
@WebMvcTest(controllers = JobController.class)
@Import({
	SecurityConfiguration.class,
	JwtAuthenticationFilter.class,
	JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class,
	SecurityAccessTest.TestBeans.class
})
class SecurityAccessTest {

	@TestConfiguration
	@EnableWebSecurity
	static class TestBeans {

		@Bean
		CorsConfigurationSource corsConfigurationSource() {
			return new CorsConfiguration(new CorsProperties(List.of("http://localhost:5173"))).corsConfigurationSource();
		}

		@Bean
		JwtService jwtService() {
			String secret = Base64.getEncoder().encodeToString("a-test-signing-secret-of-at-least-32-bytes".getBytes());
			return new JwtService(new JwtProperties(secret));
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private JobService jobService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	void anUnauthenticatedRequestToABusinessEndpointIsRejectedWith401() throws Exception {
		mockMvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
	}

	@Test
	void aGarbageBearerTokenIsStill401Not500() throws Exception {
		mockMvc.perform(get("/api/jobs").header("Authorization", "Bearer not-a-real-token"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void aRefreshTokenDoesNotBuyAccessToBusinessEndpoints() throws Exception {
		User planner = planner();
		given(userDetailsService.loadUserByUsername("planner")).willReturn(planner);
		String refresh = jwtService.generateRefreshToken(planner);

		mockMvc.perform(get("/api/jobs").header("Authorization", "Bearer " + refresh))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void aValidAccessTokenIsAccepted() throws Exception {
		User planner = planner();
		given(userDetailsService.loadUserByUsername("planner")).willReturn(planner);
		given(jobService.getAllJobs()).willReturn(List.of());
		String token = jwtService.generateAccessToken(planner);

		mockMvc.perform(get("/api/jobs").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
	}

	private User planner() {
		return (User) User.withUsername("planner")
			.password("x")
			.authorities(List.of(new SimpleGrantedAuthority("ROLE_ROUTE_PLANNER")))
			.build();
	}
}
