package com.wou.logistics_assistant.dto.vehicle;

import com.wou.logistics_assistant.entity.VehicleStatus;

public record VehicleDto(
	Long vehicleId,
	String licensePlate,
	String type,
	double capacity,
	VehicleStatus status
) {
}
