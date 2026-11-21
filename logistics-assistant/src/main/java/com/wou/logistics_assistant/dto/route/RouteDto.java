package com.wou.logistics_assistant.dto.route;

import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.entity.RouteStatus;
import java.time.LocalDateTime;
import java.util.List;

public record RouteDto(
	Long routeId,
	String routeName,
	RouteStatus routeStatus,
	LocalDateTime generatedTime,
	Long vehicleId,
	List<JobDto> jobs
) {
}
