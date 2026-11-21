package com.wou.logistics_assistant.dto.job;

import com.wou.logistics_assistant.entity.JobPriority;
import com.wou.logistics_assistant.entity.JobStatus;

public record JobDto(
	Long jobId,
	Long routeId,
	String jobName,
	String deliveryLocation,
	Double latitude,
	Double longitude,
	Integer sequenceOrder,
	JobPriority priority,
	JobStatus status
) {
}
