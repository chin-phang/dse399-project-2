package com.wou.logistics_assistant.dto.vehicle;

import com.wou.logistics_assistant.entity.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(
	@NotBlank(message = "licensePlate is required") @Size(max = 50) String licensePlate,
	@NotBlank(message = "type is required") @Size(max = 50) String type,
	@Positive(message = "capacity must be greater than zero") double capacity,
	VehicleStatus status
) {

	public VehicleStatus statusOrDefault() {
		return status == null ? VehicleStatus.AVAILABLE : status;
	}
}
