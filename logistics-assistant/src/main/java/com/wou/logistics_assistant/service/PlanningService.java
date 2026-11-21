package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.route.PlanRouteRequest;
import com.wou.logistics_assistant.dto.route.RouteDto;

/**
 * UC06 — AI-assisted route planning.
 *
 * The ordering heuristic is an in-memory nearest-neighbour walk over job coordinates; there is no
 * solver. All persistence and route-lifecycle work is delegated to {@link RouteService} — this
 * service never touches a repository, which is what keeps the optimisation strategy swappable.
 */
public interface PlanningService {

	RouteDto generateOptimisedRoute(PlanRouteRequest request);

	RouteDto optimiseRoute(Long routeId);
}
