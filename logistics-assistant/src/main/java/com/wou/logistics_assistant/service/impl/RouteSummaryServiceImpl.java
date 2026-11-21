package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.route.RouteSummaryDto;
import com.wou.logistics_assistant.entity.Route;
import com.wou.logistics_assistant.entity.RouteSummary;
import com.wou.logistics_assistant.exception.ResourceNotFoundException;
import com.wou.logistics_assistant.mapper.Mappers;
import com.wou.logistics_assistant.repository.RouteRepository;
import com.wou.logistics_assistant.repository.RouteSummaryRepository;
import com.wou.logistics_assistant.service.RouteSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Distance is computed here, not on the entity. The calculation is a PostGIS query, and an entity
 * issuing queries would break the layering rule — which is also why this service injects
 * {@link RouteRepository} alongside its own.
 */
@Service
@Transactional
public class RouteSummaryServiceImpl implements RouteSummaryService {

	private final RouteSummaryRepository routeSummaryRepository;
	private final RouteRepository routeRepository;

	public RouteSummaryServiceImpl(RouteSummaryRepository routeSummaryRepository, RouteRepository routeRepository) {
		this.routeSummaryRepository = routeSummaryRepository;
		this.routeRepository = routeRepository;
	}

	@Override
	public RouteSummaryDto generateSummary(Long routeId) {
		Route route = findRoute(routeId);
		// ST_Distance over the route's ordered stops, skipping any without coordinates.
		double distanceMetres = routeRepository.calculateTotalDistanceMetres(routeId);

		RouteSummary summary = routeSummaryRepository.findByRouteRouteId(routeId).orElseGet(RouteSummary::new);
		if (summary.getRoute() == null) {
			summary.setRoute(route);
		}
		summary.applyStatistics(distanceMetres, route.getJobs().size());
		return Mappers.toDto(routeSummaryRepository.save(summary));
	}

	@Override
	@Transactional(readOnly = true)
	public RouteSummaryDto getSummary(Long routeId) {
		return routeSummaryRepository
			.findByRouteRouteId(routeId)
			.map(Mappers::toDto)
			.orElseThrow(() -> new ResourceNotFoundException("RouteSummary for route", routeId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<RouteSummaryDto> getAllSummaries() {
		return routeSummaryRepository.findAll().stream().map(Mappers::toDto).toList();
	}

	private Route findRoute(Long routeId) {
		return routeRepository.findById(routeId).orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
	}
}
