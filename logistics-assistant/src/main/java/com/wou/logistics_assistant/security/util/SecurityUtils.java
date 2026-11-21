package com.wou.logistics_assistant.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the authenticated user off the SecurityContext.
 *
 * Never call this from a {@code @Tool} method or anything reachable from one. The context is
 * ThreadLocal-bound, and tools execute on Reactor threads during {@code ChatClient.stream()},
 * where it is empty — you would get null rather than a failure, and act unauthenticated. Chat
 * code captures the principal on the request thread and passes it through Spring AI's
 * {@code ToolContext}.
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
			return null;
		}
		return authentication.getName();
	}

	public static String requireCurrentUsername() {
		String username = currentUsername();
		if (username == null) {
			throw new IllegalStateException("No authenticated user in the security context");
		}
		return username;
	}
}
