package com.wou.logistics_assistant.entity;

/** Lifecycle of a delivery job. */
public enum JobStatus {
	PENDING,
	ASSIGNED,
	IN_TRANSIT,
	DELIVERED,
	CANCELLED
}
