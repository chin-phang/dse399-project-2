package com.wou.logistics_assistant.dto.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRouteRequest(
	@NotBlank(message = "routeName is required") @Size(max = 100) String routeName,
	Long vehicleId
) {
}
