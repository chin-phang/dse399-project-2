package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.vehicle.CreateVehicleRequest;
import com.wou.logistics_assistant.dto.vehicle.UpdateVehicleRequest;
import com.wou.logistics_assistant.dto.vehicle.VehicleDto;
import com.wou.logistics_assistant.entity.Vehicle;
import com.wou.logistics_assistant.entity.VehicleStatus;
import com.wou.logistics_assistant.exception.ConflictException;
import com.wou.logistics_assistant.exception.ResourceNotFoundException;
import com.wou.logistics_assistant.exception.ValidationException;
import com.wou.logistics_assistant.mapper.Mappers;
import com.wou.logistics_assistant.repository.VehicleRepository;
import com.wou.logistics_assistant.service.VehicleService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

	private final VehicleRepository vehicleRepository;

	public VehicleServiceImpl(VehicleRepository vehicleRepository) {
		this.vehicleRepository = vehicleRepository;
	}

	@Override
	public VehicleDto registerVehicle(CreateVehicleRequest request) {
		if (vehicleRepository.existsByLicensePlate(request.licensePlate())) {
			throw new ConflictException("A vehicle with licence plate " + request.licensePlate() + " already exists");
		}
		Vehicle vehicle = new Vehicle(
			request.licensePlate(),
			request.type(),
			request.capacity(),
			request.statusOrDefault()
		);
		if (!vehicle.validateCapacity()) {
			throw new ValidationException("capacity must be greater than zero");
		}
		return Mappers.toDto(vehicleRepository.save(vehicle));
	}

	@Override
	public VehicleDto updateVehicle(Long vehicleId, UpdateVehicleRequest request) {
		Vehicle vehicle = find(vehicleId);
		if (request.licensePlate() != null) {
			String plate = request.licensePlate();
			if (!plate.equals(vehicle.getLicensePlate()) && vehicleRepository.existsByLicensePlate(plate)) {
				throw new ConflictException("A vehicle with licence plate " + plate + " already exists");
			}
			vehicle.setLicensePlate(plate);
		}
		if (request.type() != null) {
			vehicle.setType(request.type());
		}
		if (request.capacity() != null) {
			vehicle.setCapacity(request.capacity());
		}
		if (request.status() != null) {
			vehicle.updateAvailability(request.status());
		}
		if (!vehicle.validateCapacity()) {
			throw new ValidationException("capacity must be greater than zero");
		}
		return Mappers.toDto(vehicleRepository.save(vehicle));
	}

	@Override
	public void deleteVehicle(Long vehicleId) {
		Vehicle vehicle = find(vehicleId);
		if (!vehicle.getRoutes().isEmpty()) {
			throw new ConflictException(
				"Vehicle " + vehicleId + " is assigned to " + vehicle.getRoutes().size() + " route(s)"
			);
		}
		vehicleRepository.delete(vehicle);
	}

	@Override
	@Transactional(readOnly = true)
	public VehicleDto getVehicle(Long vehicleId) {
		return Mappers.toDto(find(vehicleId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<VehicleDto> getVehicles() {
		return vehicleRepository.findAll().stream().map(Mappers::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<VehicleDto> getVehiclesByStatus(VehicleStatus status) {
		return vehicleRepository.findByStatus(status).stream().map(Mappers::toDto).toList();
	}

	private Vehicle find(Long vehicleId) {
		return vehicleRepository.findById(vehicleId).orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
	}
}
