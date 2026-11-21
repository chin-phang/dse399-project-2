package com.wou.logistics_assistant.dto.vehicle;

import com.wou.logistics_assistant.entity.VehicleStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateVehicleRequest(
	@Size(max = 50) String licensePlate,
	@Size(max = 50) String type,
	@Positive(message = "capacity must be greater than zero") Double capacity,
	VehicleStatus status
) {
}
