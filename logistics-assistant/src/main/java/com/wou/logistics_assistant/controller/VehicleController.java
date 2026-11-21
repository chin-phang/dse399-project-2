package com.wou.logistics_assistant.controller;

import com.wou.logistics_assistant.dto.vehicle.CreateVehicleRequest;
import com.wou.logistics_assistant.dto.vehicle.UpdateVehicleRequest;
import com.wou.logistics_assistant.dto.vehicle.VehicleDto;
import com.wou.logistics_assistant.entity.VehicleStatus;
import com.wou.logistics_assistant.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC05 — vehicle management. Depends on the interface, never the impl. */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

	private final VehicleService vehicleService;

	public VehicleController(VehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

	@GetMapping
	public List<VehicleDto> list(@RequestParam(required = false) VehicleStatus status) {
		if (status != null) {
			return vehicleService.getVehiclesByStatus(status);
		}
		return vehicleService.getVehicles();
	}

	@GetMapping("/{id}")
	public VehicleDto get(@PathVariable Long id) {
		return vehicleService.getVehicle(id);
	}

	@PostMapping
	public ResponseEntity<VehicleDto> create(@Valid @RequestBody CreateVehicleRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.registerVehicle(request));
	}

	@PutMapping("/{id}")
	public VehicleDto update(@PathVariable Long id, @Valid @RequestBody UpdateVehicleRequest request) {
		return vehicleService.updateVehicle(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		vehicleService.deleteVehicle(id);
		return ResponseEntity.noContent().build();
	}
}
