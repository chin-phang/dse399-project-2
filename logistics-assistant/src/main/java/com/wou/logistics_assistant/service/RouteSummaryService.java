package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.route.RouteSummaryDto;
import java.util.List;

/** UC07 — route summary reporting. */
public interface RouteSummaryService {

	/**
	 * Computes and stores statistics for a route (distance via PostGIS, duration and fuel
	 * derived from it), replacing any previous summary.
	 */
	RouteSummaryDto generateSummary(Long routeId);

	RouteSummaryDto getSummary(Long routeId);

	List<RouteSummaryDto> getAllSummaries();
}
