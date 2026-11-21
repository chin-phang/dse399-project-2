package com.wou.logistics_assistant.controller;

import com.wou.logistics_assistant.dto.route.CreateRouteRequest;
import com.wou.logistics_assistant.dto.route.PlanRouteRequest;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.dto.route.RouteSummaryDto;
import com.wou.logistics_assistant.dto.route.UpdateRouteRequest;
import com.wou.logistics_assistant.service.ExportService;
import com.wou.logistics_assistant.service.PlanningService;
import com.wou.logistics_assistant.service.RouteService;
import com.wou.logistics_assistant.service.RouteSummaryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Routes, plus planning (UC06), summary (UC07) and export (UC08) — one controller. There is no
 * separate RouteSummaryController or ExportController.
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

	private final RouteService routeService;
	private final PlanningService planningService;
	private final RouteSummaryService routeSummaryService;
	private final ExportService exportService;

	public RouteController(
		RouteService routeService,
		PlanningService planningService,
		RouteSummaryService routeSummaryService,
		ExportService exportService
	) {
		this.routeService = routeService;
		this.planningService = planningService;
		this.routeSummaryService = routeSummaryService;
		this.exportService = exportService;
	}

	@GetMapping
	public List<RouteDto> list() {
		return routeService.getRoutes();
	}

	@GetMapping("/{id}")
	public RouteDto get(@PathVariable Long id) {
		return routeService.getRoute(id);
	}

	@PostMapping
	public ResponseEntity<RouteDto> create(@Valid @RequestBody CreateRouteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(request));
	}

	@PutMapping("/{id}")
	public RouteDto update(@PathVariable Long id, @Valid @RequestBody UpdateRouteRequest request) {
		return routeService.updateRoute(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		routeService.deleteRoute(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/vehicle/{vehicleId}")
	public RouteDto assignVehicle(@PathVariable Long id, @PathVariable Long vehicleId) {
		return routeService.assignVehicle(id, vehicleId);
	}

	@PostMapping("/{id}/jobs/{jobId}")
	public RouteDto addJob(@PathVariable Long id, @PathVariable Long jobId) {
		return routeService.addJob(id, jobId);
	}

	@DeleteMapping("/{id}/jobs/{jobId}")
	public RouteDto removeJob(@PathVariable Long id, @PathVariable Long jobId) {
		return routeService.removeJob(id, jobId);
	}

	/** UC06 — AI-assisted route planning recommendation. */
	@PostMapping("/plan")
	public ResponseEntity<RouteDto> plan(@Valid @RequestBody PlanRouteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(planningService.generateOptimisedRoute(request));
	}

	@PostMapping("/{id}/optimise")
	public RouteDto optimise(@PathVariable Long id) {
		return planningService.optimiseRoute(id);
	}

	/** UC07 — route summary. Regenerated on read so the statistics reflect the current stops. */
	@GetMapping("/{id}/summary")
	public RouteSummaryDto summary(@PathVariable Long id) {
		return routeSummaryService.generateSummary(id);
	}

	/** UC08 — CSV route report. */
	@GetMapping("/{id}/export/csv")
	public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
		byte[] body = exportService.exportCSV(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType("text/csv"))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"route-" + id + "-report.csv\"")
			.body(body);
	}
}
