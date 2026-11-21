package com.wou.logistics_assistant.dto.route;

import java.time.LocalDateTime;

public record RouteSummaryDto(
	Long summaryId,
	Long routeId,
	Double totalDistance,
	Long estimatedDuration,
	Integer totalJobs,
	Double fuelConsumption,
	LocalDateTime generatedTime
) {
}
