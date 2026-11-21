package com.wou.logistics_assistant.entity;

/**
 * Application roles. Spring Security authorities are derived as {@code ROLE_<name>}.
 */
public enum Role {
	ROUTE_PLANNER,
	OPERATIONS_MANAGER,
	ADMIN
}
