package com.wou.logistics_assistant.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Creates and validates tokens. That is the whole job — it never authenticates a user, touches
 * the database, or calls a repository. Deliberately concrete: single implementation,
 * infrastructure rather than a use case, so there is no interface to mock.
 */
@Service
public class JwtService {

	public static final String CLAIM_TOKEN_TYPE = "typ";
	public static final String CLAIM_AUTHORITIES = "authorities";
	public static final String TOKEN_TYPE_ACCESS = "access";
	public static final String TOKEN_TYPE_REFRESH = "refresh";

	private final JwtProperties properties;
	private volatile SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
	}

	public String generateAccessToken(UserDetails user) {
		return buildToken(user, properties.getAccessTokenTtl(), TOKEN_TYPE_ACCESS);
	}

	public String generateRefreshToken(UserDetails user) {
		return buildToken(user, properties.getRefreshTokenTtl(), TOKEN_TYPE_REFRESH);
	}

	public String extractUsername(String token) {
		return claims(token).getSubject();
	}

	public boolean isRefreshToken(String token) {
		return TOKEN_TYPE_REFRESH.equals(claims(token).get(CLAIM_TOKEN_TYPE));
	}

	public boolean isTokenValid(String token, UserDetails user) {
		try {
			Claims parsed = claims(token);
			return parsed.getSubject().equals(user.getUsername()) && parsed.getExpiration().after(new Date());
		} catch (JwtException ex) {
			return false;
		}
	}

	public boolean isExpired(String token) {
		try {
			return extractExpiration(token).before(new Date());
		} catch (JwtException ex) {
			return true;
		}
	}

	public Date extractExpiration(String token) {
		return claims(token).getExpiration();
	}

	public long accessTokenTtlSeconds() {
		return properties.getAccessTokenTtl().toSeconds();
	}

	private String buildToken(UserDetails user, Duration ttl, String tokenType) {
		Date now = new Date();
		return Jwts.builder()
			.subject(user.getUsername())
			.claim(CLAIM_TOKEN_TYPE, tokenType)
			.claim(CLAIM_AUTHORITIES, user.getAuthorities().stream().map(authority -> authority.getAuthority()).toList())
			.issuedAt(now)
			.expiration(new Date(now.getTime() + ttl.toMillis()))
			.signWith(key())
			.compact();
	}

	private Claims claims(String token) {
		return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey key() {
		SecretKey current = key;
		if (current == null) {
			synchronized (this) {
				current = key;
				if (current == null) {
					current = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
					key = current;
				}
			}
		}
		return current;
	}
}
