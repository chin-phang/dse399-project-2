package com.wou.logistics_assistant.dto.route;

import com.wou.logistics_assistant.entity.RouteStatus;
import jakarta.validation.constraints.Size;

public record UpdateRouteRequest(
	@Size(max = 100) String routeName,
	RouteStatus routeStatus,
	Long vehicleId
) {
}
