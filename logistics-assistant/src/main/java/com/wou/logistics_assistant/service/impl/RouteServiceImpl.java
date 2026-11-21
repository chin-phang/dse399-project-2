package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.route.CreateRouteRequest;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.dto.route.UpdateRouteRequest;
import com.wou.logistics_assistant.entity.Job;
import com.wou.logistics_assistant.entity.Route;
import com.wou.logistics_assistant.entity.Vehicle;
import com.wou.logistics_assistant.exception.ResourceNotFoundException;
import com.wou.logistics_assistant.exception.ValidationException;
import com.wou.logistics_assistant.mapper.Mappers;
import com.wou.logistics_assistant.repository.JobRepository;
import com.wou.logistics_assistant.repository.RouteRepository;
import com.wou.logistics_assistant.repository.VehicleRepository;
import com.wou.logistics_assistant.service.RouteService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RouteServiceImpl implements RouteService {

	private final RouteRepository routeRepository;
	private final VehicleRepository vehicleRepository;
	private final JobRepository jobRepository;

	public RouteServiceImpl(
		RouteRepository routeRepository,
		VehicleRepository vehicleRepository,
		JobRepository jobRepository
	) {
		this.routeRepository = routeRepository;
		this.vehicleRepository = vehicleRepository;
		this.jobRepository = jobRepository;
	}

	@Override
	public RouteDto createRoute(CreateRouteRequest request) {
		Route route = new Route(request.routeName());
		if (request.vehicleId() != null) {
			route.assignVehicle(findVehicle(request.vehicleId()));
		}
		return Mappers.toDto(routeRepository.save(route));
	}

	@Override
	public RouteDto updateRoute(Long routeId, UpdateRouteRequest request) {
		Route route = find(routeId);
		if (request.routeName() != null) {
			route.setRouteName(request.routeName());
		}
		if (request.routeStatus() != null) {
			route.updateStatus(request.routeStatus());
		}
		if (request.vehicleId() != null) {
			route.assignVehicle(findVehicle(request.vehicleId()));
		}
		return Mappers.toDto(routeRepository.save(route));
	}

	@Override
	public void deleteRoute(Long routeId) {
		Route route = find(routeId);
		// Detach jobs rather than cascading the delete — a job outlives the route it was planned
		// onto, and returns to the unassigned pool.
		for (Job job : new ArrayList<>(route.getJobs())) {
			job.removeFromRoute();
		}
		routeRepository.delete(route);
	}

	@Override
	public RouteDto assignVehicle(Long routeId, Long vehicleId) {
		Route route = find(routeId);
		Vehicle vehicle = findVehicle(vehicleId);
		if (!vehicle.validateCapacity()) {
			throw new ValidationException("Vehicle " + vehicleId + " has no usable capacity");
		}
		route.assignVehicle(vehicle);
		return Mappers.toDto(routeRepository.save(route));
	}

	@Override
	public RouteDto addJob(Long routeId, Long jobId) {
		Route route = find(routeId);
		Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
		route.addJob(job);
		return Mappers.toDto(routeRepository.save(route));
	}

	@Override
	public RouteDto removeJob(Long routeId, Long jobId) {
		Route route = find(routeId);
		Job job = route.getJobs().stream()
			.filter(candidate -> jobId.equals(candidate.getJobId()))
			.findFirst()
			.orElseThrow(() -> new ResourceNotFoundException("Job on route " + routeId, jobId));
		route.removeJob(job);
		return Mappers.toDto(routeRepository.save(route));
	}

	@Override
	@Transactional(readOnly = true)
	public RouteDto getRoute(Long routeId) {
		return Mappers.toDto(find(routeId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<RouteDto> getRoutes() {
		return routeRepository.findAll().stream().map(Mappers::toDto).toList();
	}

	@Override
	public RouteDto applyOrdering(Long routeId, List<Long> orderedJobIds) {
		Route route = find(routeId);
		Map<Long, Job> onRoute = route.getJobs().stream()
			.filter(job -> job.getJobId() != null)
			.collect(Collectors.toMap(Job::getJobId, Function.identity()));
		Set<Long> unknown = new HashSet<>(orderedJobIds);
		unknown.removeAll(onRoute.keySet());
		if (!unknown.isEmpty()) {
			String ids = unknown.stream().map(String::valueOf).collect(Collectors.joining(", "));
			throw new ValidationException("Jobs not on route " + routeId + ": " + ids);
		}
		List<Job> ordered = new ArrayList<>();
		for (Long jobId : orderedJobIds) {
			Job job = onRoute.get(jobId);
			if (job != null) {
				ordered.add(job);
			}
		}
		route.applyOrdering(ordered);
		return Mappers.toDto(routeRepository.save(route));
	}

	private Route find(Long routeId) {
		return routeRepository.findById(routeId).orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
	}

	private Vehicle findVehicle(Long vehicleId) {
		return vehicleRepository.findById(vehicleId).orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
	}
}
