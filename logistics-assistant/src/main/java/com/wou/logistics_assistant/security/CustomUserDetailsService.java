package com.wou.logistics_assistant.security;

import com.wou.logistics_assistant.entity.User;
import com.wou.logistics_assistant.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a user and maps it to Spring's {@link UserDetails}. No JWT logic lives here — token
 * creation and validation belong to {@code JwtService}. No second interface either: this already
 * implements Spring's own.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		User user = userRepository
			.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
			.password(user.getPasswordHash())
			.authorities(List.of(new SimpleGrantedAuthority(user.authority())))
			.build();
	}
}
