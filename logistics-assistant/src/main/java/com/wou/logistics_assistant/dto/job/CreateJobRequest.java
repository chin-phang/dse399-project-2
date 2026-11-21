package com.wou.logistics_assistant.dto.job;

import com.wou.logistics_assistant.entity.JobPriority;
import com.wou.logistics_assistant.entity.JobStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
	@NotBlank(message = "jobName is required") @Size(max = 150) String jobName,
	@NotBlank(message = "deliveryLocation is required") @Size(max = 255) String deliveryLocation,
	@DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
	@DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
	JobPriority priority,
	JobStatus status
) {

	public CreateJobRequest(String jobName, String deliveryLocation) {
		this(jobName, deliveryLocation, null, null, null, null);
	}

	public JobPriority priorityOrDefault() {
		return priority == null ? JobPriority.MEDIUM : priority;
	}

	public JobStatus statusOrDefault() {
		return status == null ? JobStatus.PENDING : status;
	}
}
