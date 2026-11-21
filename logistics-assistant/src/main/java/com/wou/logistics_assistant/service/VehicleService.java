package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.vehicle.CreateVehicleRequest;
import com.wou.logistics_assistant.dto.vehicle.UpdateVehicleRequest;
import com.wou.logistics_assistant.dto.vehicle.VehicleDto;
import com.wou.logistics_assistant.entity.VehicleStatus;
import java.util.List;

/** UC05 — fleet management. */
public interface VehicleService {

	VehicleDto registerVehicle(CreateVehicleRequest request);

	VehicleDto updateVehicle(Long vehicleId, UpdateVehicleRequest request);

	void deleteVehicle(Long vehicleId);

	VehicleDto getVehicle(Long vehicleId);

	List<VehicleDto> getVehicles();

	List<VehicleDto> getVehiclesByStatus(VehicleStatus status);
}
