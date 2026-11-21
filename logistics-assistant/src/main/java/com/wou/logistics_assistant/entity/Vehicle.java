package com.wou.logistics_assistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;

/** A fleet vehicle available for route assignment. */
@Entity
@Table(name = "vehicle")
public class Vehicle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "vehicle_id")
	private Long vehicleId;

	@NotBlank
	@Column(name = "license_plate", nullable = false, unique = true, length = 50)
	private String licensePlate;

	@NotBlank
	@Column(name = "type", nullable = false, length = 50)
	private String type;

	@Positive
	@Column(name = "capacity", nullable = false)
	private double capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private VehicleStatus status = VehicleStatus.AVAILABLE;

	@OneToMany(mappedBy = "vehicle")
	private List<Route> routes = new ArrayList<>();

	protected Vehicle() {
	}

	public Vehicle(String licensePlate, String type, double capacity) {
		this(licensePlate, type, capacity, VehicleStatus.AVAILABLE);
	}

	public Vehicle(String licensePlate, String type, double capacity, VehicleStatus status) {
		this.licensePlate = licensePlate;
		this.type = type;
		this.capacity = capacity;
		this.status = status == null ? VehicleStatus.AVAILABLE : status;
	}

	/** Attaches a route to this vehicle, keeping both sides of the association in step. */
	public void assignRoute(Route route) {
		if (routes.stream().noneMatch(existing -> existing == route)) {
			routes.add(route);
		}
		if (route.getVehicle() != this) {
			route.setVehicle(this);
		}
	}

	public void updateAvailability(VehicleStatus newStatus) {
		this.status = newStatus;
	}

	/** A vehicle with no positive capacity cannot carry anything, so it cannot be planned. */
	public boolean validateCapacity() {
		return capacity > 0.0;
	}

	public boolean isAvailable() {
		return status == VehicleStatus.AVAILABLE;
	}

	public Long getVehicleId() {
		return vehicleId;
	}

	public String getLicensePlate() {
		return licensePlate;
	}

	public void setLicensePlate(String licensePlate) {
		this.licensePlate = licensePlate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public VehicleStatus getStatus() {
		return status;
	}

	public void setStatus(VehicleStatus status) {
		this.status = status;
	}

	public List<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(List<Route> routes) {
		this.routes = routes;
	}
}
