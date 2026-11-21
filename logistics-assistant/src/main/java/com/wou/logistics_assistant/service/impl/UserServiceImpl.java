package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.auth.RegisterRequest;
import com.wou.logistics_assistant.entity.User;
import com.wou.logistics_assistant.exception.ConflictException;
import com.wou.logistics_assistant.exception.ResourceNotFoundException;
import com.wou.logistics_assistant.repository.UserRepository;
import com.wou.logistics_assistant.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public User create(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new ConflictException("Username " + request.username() + " is already taken");
		}
		return userRepository.save(
			new User(request.username(), passwordEncoder.encode(request.password()), request.roleOrDefault())
		);
	}

	@Override
	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", username));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}
}
