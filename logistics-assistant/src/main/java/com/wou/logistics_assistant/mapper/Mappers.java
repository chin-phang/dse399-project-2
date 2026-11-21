package com.wou.logistics_assistant.mapper;

import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.dto.route.RouteSummaryDto;
import com.wou.logistics_assistant.dto.vehicle.VehicleDto;
import com.wou.logistics_assistant.entity.Job;
import com.wou.logistics_assistant.entity.Route;
import com.wou.logistics_assistant.entity.RouteSummary;
import com.wou.logistics_assistant.entity.Vehicle;
import java.util.List;

/**
 * Manual entity to DTO mapping. Entities never cross the service boundary, so every read path
 * ends here.
 */
public final class Mappers {

	private Mappers() {
	}

	public static VehicleDto toDto(Vehicle vehicle) {
		return new VehicleDto(
			requireId(vehicle.getVehicleId(), "Vehicle has not been persisted"),
			vehicle.getLicensePlate(),
			vehicle.getType(),
			vehicle.getCapacity(),
			vehicle.getStatus()
		);
	}

	public static JobDto toDto(Job job) {
		Long routeId = job.getRoute() == null ? null : job.getRoute().getRouteId();
		return new JobDto(
			requireId(job.getJobId(), "Job has not been persisted"),
			routeId,
			job.getJobName(),
			job.getDeliveryLocation(),
			job.getLatitude(),
			job.getLongitude(),
			job.getSequenceOrder(),
			job.getPriority(),
			job.getStatus()
		);
	}

	public static RouteDto toDto(Route route) {
		List<JobDto> jobs = route.orderedJobs().stream().map(Mappers::toDto).toList();
		Long vehicleId = route.getVehicle() == null ? null : route.getVehicle().getVehicleId();
		return new RouteDto(
			requireId(route.getRouteId(), "Route has not been persisted"),
			route.getRouteName(),
			route.getRouteStatus(),
			route.getGeneratedTime(),
			vehicleId,
			jobs
		);
	}

	public static RouteSummaryDto toDto(RouteSummary summary) {
		return new RouteSummaryDto(
			requireId(summary.getSummaryId(), "RouteSummary has not been persisted"),
			requireId(summary.getRoute().getRouteId(), "Route has not been persisted"),
			summary.getTotalDistance(),
			summary.getEstimatedDuration(),
			summary.getTotalJobs(),
			summary.getFuelConsumption(),
			summary.getGeneratedTime()
		);
	}

	private static long requireId(Long id, String message) {
		if (id == null) {
			throw new IllegalStateException(message);
		}
		return id;
	}
}
