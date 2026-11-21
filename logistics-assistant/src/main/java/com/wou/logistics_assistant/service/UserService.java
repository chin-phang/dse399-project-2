package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.auth.RegisterRequest;
import com.wou.logistics_assistant.entity.User;

public interface UserService {

	User create(RegisterRequest request);

	User findByUsername(String username);

	boolean existsByUsername(String username);
}
