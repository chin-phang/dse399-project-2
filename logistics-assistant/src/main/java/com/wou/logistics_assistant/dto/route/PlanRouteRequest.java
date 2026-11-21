package com.wou.logistics_assistant.dto.route;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** UC06 — generate an AI-assisted route recommendation over a chosen set of jobs. */
public record PlanRouteRequest(
	@NotEmpty(message = "jobIds must contain at least one job") List<Long> jobIds,
	@Size(max = 100) String routeName,
	Long vehicleId
) {
}
