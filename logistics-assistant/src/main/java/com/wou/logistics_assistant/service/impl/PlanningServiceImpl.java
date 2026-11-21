package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.route.CreateRouteRequest;
import com.wou.logistics_assistant.dto.route.PlanRouteRequest;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.exception.ValidationException;
import com.wou.logistics_assistant.service.JobService;
import com.wou.logistics_assistant.service.PlanningService;
import com.wou.logistics_assistant.service.RouteService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nearest-neighbour route ordering.
 *
 * Deliberately an in-memory calculation over coordinates already loaded through {@link JobService}:
 * it keeps this service free of repositories (so route persistence stays owned by
 * {@link RouteService}) and makes the heuristic swappable for a real solver later without touching
 * any business service. Distance here is Haversine — numerically equivalent to what PostGIS
 * computes for the reporting statistic, but without a database round trip per candidate pair.
 */
@Service
@Transactional
public class PlanningServiceImpl implements PlanningService {

	public static final double EARTH_RADIUS_METRES = 6_371_008.8;

	private final RouteService routeService;
	private final JobService jobService;

	public PlanningServiceImpl(RouteService routeService, JobService jobService) {
		this.routeService = routeService;
		this.jobService = jobService;
	}

	@Override
	public RouteDto generateOptimisedRoute(PlanRouteRequest request) {
		List<JobDto> jobs = jobService.getJobsByIds(request.jobIds());
		if (jobs.isEmpty()) {
			throw new ValidationException("At least one job is required to plan a route");
		}
		List<Long> alreadyPlanned = jobs.stream()
			.filter(job -> job.routeId() != null)
			.map(JobDto::jobId)
			.toList();
		if (!alreadyPlanned.isEmpty()) {
			String ids = alreadyPlanned.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
			throw new ValidationException("Jobs already assigned to a route: " + ids);
		}

		String routeName = request.routeName() == null || request.routeName().isBlank()
			? defaultRouteName()
			: request.routeName();
		RouteDto route = routeService.createRoute(new CreateRouteRequest(routeName, request.vehicleId()));
		for (JobDto job : jobs) {
			routeService.addJob(route.routeId(), job.jobId());
		}
		return routeService.applyOrdering(route.routeId(), order(jobs).stream().map(JobDto::jobId).toList());
	}

	@Override
	public RouteDto optimiseRoute(Long routeId) {
		RouteDto route = routeService.getRoute(routeId);
		if (route.jobs().isEmpty()) {
			throw new ValidationException("Route " + routeId + " has no jobs to optimise");
		}
		return routeService.applyOrdering(routeId, order(route.jobs()).stream().map(JobDto::jobId).toList());
	}

	/**
	 * Greedy nearest-neighbour walk. Jobs with no coordinates cannot be ordered spatially, so
	 * they are appended after the geocoded ones (highest priority first) rather than defaulting
	 * to (0,0), which would drag the route into the Gulf of Guinea.
	 */
	public List<JobDto> order(List<JobDto> jobs) {
		List<JobDto> locatable = new ArrayList<>();
		List<JobDto> unlocatable = new ArrayList<>();
		for (JobDto job : jobs) {
			if (job.latitude() != null && job.longitude() != null) {
				locatable.add(job);
			} else {
				unlocatable.add(job);
			}
		}
		unlocatable.sort(Comparator.comparingInt((JobDto job) -> job.priority().ordinal()).reversed());
		if (locatable.size() <= 1) {
			List<JobDto> ordered = new ArrayList<>(locatable);
			ordered.addAll(unlocatable);
			return ordered;
		}

		List<JobDto> remaining = new ArrayList<>(locatable);
		JobDto current = remaining.stream()
			.max(Comparator.comparingInt(job -> job.priority().ordinal()))
			.orElse(remaining.getFirst());
		remaining.remove(current);

		List<JobDto> ordered = new ArrayList<>();
		ordered.add(current);
		while (!remaining.isEmpty()) {
			JobDto cursor = current;
			JobDto next = remaining.stream()
				.min(Comparator.comparingDouble(candidate -> haversineMetres(cursor, candidate)))
				.orElse(null);
			if (next == null) {
				break;
			}
			remaining.remove(next);
			ordered.add(next);
			current = next;
		}
		ordered.addAll(unlocatable);
		return ordered;
	}

	private double haversineMetres(JobDto a, JobDto b) {
		double lat1 = Math.toRadians(a.latitude());
		double lat2 = Math.toRadians(b.latitude());
		double dLat = lat2 - lat1;
		double dLon = Math.toRadians(b.longitude() - a.longitude());
		double sinLat = Math.sin(dLat / 2);
		double sinLon = Math.sin(dLon / 2);
		double h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
		return 2 * EARTH_RADIUS_METRES * Math.asin(Math.min(1.0, Math.sqrt(h)));
	}

	private String defaultRouteName() {
		return "Route " + LocalDate.now();
	}
}
