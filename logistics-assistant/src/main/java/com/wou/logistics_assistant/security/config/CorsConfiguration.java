package com.wou.logistics_assistant.security.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfiguration {

	private final CorsProperties properties;

	public CorsConfiguration(CorsProperties properties) {
		this.properties = properties;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
		config.setAllowedOrigins(properties.getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		// useChat reads this header to select the UI Message Stream Protocol, and custom
		// response headers are not readable cross-origin unless explicitly exposed.
		config.setExposedHeaders(List.of("x-vercel-ai-ui-message-stream", "Content-Disposition"));
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
