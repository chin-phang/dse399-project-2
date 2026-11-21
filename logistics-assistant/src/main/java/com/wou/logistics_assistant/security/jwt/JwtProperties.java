package com.wou.logistics_assistant.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private String secret;
	private Duration accessTokenTtl = Duration.ofHours(1);
	private Duration refreshTokenTtl = Duration.ofDays(7);

	public JwtProperties() {
	}

	public JwtProperties(String secret) {
		this.secret = secret;
	}

	public JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
		this.secret = secret;
		if (accessTokenTtl != null) {
			this.accessTokenTtl = accessTokenTtl;
		}
		if (refreshTokenTtl != null) {
			this.refreshTokenTtl = refreshTokenTtl;
		}
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Duration getAccessTokenTtl() {
		return accessTokenTtl;
	}

	public void setAccessTokenTtl(Duration accessTokenTtl) {
		this.accessTokenTtl = accessTokenTtl;
	}

	public Duration getRefreshTokenTtl() {
		return refreshTokenTtl;
	}

	public void setRefreshTokenTtl(Duration refreshTokenTtl) {
		this.refreshTokenTtl = refreshTokenTtl;
	}
}
