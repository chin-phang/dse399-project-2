package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.route.CreateRouteRequest;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.dto.route.UpdateRouteRequest;
import java.util.List;

/**
 * Owner of the Route aggregate. Every write to a route — including everything PlanningService
 * wants to do — goes through here, so optimisation logic stays swappable without touching
 * persistence.
 */
public interface RouteService {

	RouteDto createRoute(CreateRouteRequest request);

	RouteDto updateRoute(Long routeId, UpdateRouteRequest request);

	void deleteRoute(Long routeId);

	RouteDto assignVehicle(Long routeId, Long vehicleId);

	RouteDto addJob(Long routeId, Long jobId);

	RouteDto removeJob(Long routeId, Long jobId);

	RouteDto getRoute(Long routeId);

	List<RouteDto> getRoutes();

	/**
	 * Persists a visiting order produced by {@link PlanningService}. Added so the planner can
	 * record its result without reaching for a repository.
	 */
	RouteDto applyOrdering(Long routeId, List<Long> orderedJobIds);
}
