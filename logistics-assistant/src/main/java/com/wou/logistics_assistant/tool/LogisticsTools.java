package com.wou.logistics_assistant.tool;

import com.wou.logistics_assistant.dto.chat.ToolResult;
import com.wou.logistics_assistant.dto.job.CreateJobRequest;
import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.route.PlanRouteRequest;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.dto.route.RouteSummaryDto;
import com.wou.logistics_assistant.dto.vehicle.VehicleDto;
import com.wou.logistics_assistant.entity.JobPriority;
import com.wou.logistics_assistant.entity.JobStatus;
import com.wou.logistics_assistant.entity.VehicleStatus;
import com.wou.logistics_assistant.service.JobService;
import com.wou.logistics_assistant.service.PlanningService;
import com.wou.logistics_assistant.service.RouteSummaryService;
import com.wou.logistics_assistant.service.VehicleService;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

/**
 * The backend functions GPT can invoke — this is FR02/UC02.
 *
 * Methods stay thin: they call business services and never reimplement logic. They are registered
 * on the {@code ChatClient} via {@code defaultTools}, and Spring AI's auto-registered
 * {@code ToolCallingAdvisor} drives the call/execute/respond loop, so {@code ToolCallingService}
 * is not on this path.
 *
 * Every method takes a trailing {@link ToolContext}. That is the only safe way to reach
 * per-request state from here: this component is a singleton, and tool execution during
 * {@code ChatClient.stream()} runs on Reactor threads, so a field would leak across concurrent
 * sessions and a {@code ThreadLocal} would be empty. The context carries the tool-call event sink
 * (which {@code chatStream} merges into the SSE response) and the caller's identity — never read
 * {@code SecurityContextHolder} here, it is empty on those threads.
 */
@Component
public class LogisticsTools {

	/** ToolContext key for the per-request tool-call event sink. */
	public static final String TOOL_EVENT_SINK = "toolEventSink";

	/** ToolContext key for the authenticated username, captured on the request thread. */
	public static final String PRINCIPAL = "principal";

	private static final Logger log = LoggerFactory.getLogger(LogisticsTools.class);

	private final JobService jobService;
	private final VehicleService vehicleService;
	private final PlanningService planningService;
	private final RouteSummaryService routeSummaryService;

	public LogisticsTools(
		JobService jobService,
		VehicleService vehicleService,
		PlanningService planningService,
		RouteSummaryService routeSummaryService
	) {
		this.jobService = jobService;
		this.vehicleService = vehicleService;
		this.planningService = planningService;
		this.routeSummaryService = routeSummaryService;
	}

	@Tool(
		description = "List delivery vehicles in the fleet, optionally filtered by status "
			+ "(AVAILABLE, IN_USE, MAINTENANCE, OUT_OF_SERVICE)"
	)
	public List<VehicleDto> listAvailableVehicles(
		@ToolParam(required = false, description = "vehicle status filter; defaults to AVAILABLE") String status,
		ToolContext toolContext
	) {
		return record(toolContext, "listAvailableVehicles", args("status", status), () -> {
			VehicleStatus filter = status == null
				? VehicleStatus.AVAILABLE
				: parseEnum(VehicleStatus.class, status, "status");
			return vehicleService.getVehiclesByStatus(filter);
		});
	}

	@Tool(description = "Create a new delivery job with a job name, delivery location and priority")
	public JobDto createDeliveryJob(
		@ToolParam(description = "short name for the delivery job") String jobName,
		@ToolParam(description = "delivery destination as an address string") String deliveryLocation,
		@ToolParam(required = false, description = "priority: LOW, MEDIUM or HIGH") String priority,
		@ToolParam(required = false, description = "latitude in decimal degrees, if known") Double latitude,
		@ToolParam(required = false, description = "longitude in decimal degrees, if known") Double longitude,
		ToolContext toolContext
	) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		arguments.put("jobName", jobName);
		arguments.put("deliveryLocation", deliveryLocation);
		arguments.put("priority", priority);
		arguments.put("latitude", latitude);
		arguments.put("longitude", longitude);
		return record(toolContext, "createDeliveryJob", arguments, () -> jobService.createJob(
			new CreateJobRequest(
				jobName,
				deliveryLocation,
				latitude,
				longitude,
				priority == null ? JobPriority.MEDIUM : parseEnum(JobPriority.class, priority, "priority"),
				null
			)
		));
	}

	@Tool(
		description = "List delivery jobs, optionally filtered by status "
			+ "(PENDING, ASSIGNED, IN_TRANSIT, DELIVERED, CANCELLED)"
	)
	public List<JobDto> getDeliveryJobs(
		@ToolParam(required = false, description = "job status filter; omit for all jobs") String status,
		ToolContext toolContext
	) {
		return record(toolContext, "getDeliveryJobs", args("status", status), () -> {
			if (status == null) {
				return jobService.getAllJobs();
			}
			return jobService.getJobsByStatus(parseEnum(JobStatus.class, status, "status"));
		});
	}

	@Tool(description = "Generate an AI-assisted route recommendation covering a list of delivery job IDs")
	public RouteDto generateRouteRecommendation(
		@ToolParam(description = "IDs of the delivery jobs to include on the route") List<Long> jobIds,
		@ToolParam(required = false, description = "name for the generated route") String routeName,
		@ToolParam(required = false, description = "ID of the vehicle to assign") Long vehicleId,
		ToolContext toolContext
	) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		arguments.put("jobIds", jobIds);
		arguments.put("routeName", routeName);
		arguments.put("vehicleId", vehicleId);
		return record(toolContext, "generateRouteRecommendation", arguments, () -> planningService.generateOptimisedRoute(
			new PlanRouteRequest(jobIds, routeName, vehicleId)
		));
	}

	@Tool(description = "Get the summary statistics (distance, duration, stop count, fuel) for a generated route")
	public RouteSummaryDto getRouteSummary(
		@ToolParam(description = "route ID") Long routeId,
		ToolContext toolContext
	) {
		return record(toolContext, "getRouteSummary", args("routeId", routeId), () -> routeSummaryService.generateSummary(routeId));
	}

	/**
	 * Runs the tool body and publishes what happened onto the per-request sink so the UI's
	 * tool-calling panel has real data. Failures are published too and then rethrown, so the
	 * model still sees the error and can recover in its next turn.
	 */
	private <T> T record(ToolContext toolContext, String toolName, Map<String, Object> arguments, ToolBody<T> body) {
		T result;
		try {
			result = body.get();
		} catch (RuntimeException ex) {
			log.warn("Tool {} failed: {}", toolName, ex.getMessage());
			String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
			publish(toolContext, new ToolResult(toolName, arguments, null, error));
			throw ex;
		}
		publish(toolContext, new ToolResult(toolName, arguments, summarise(result), null));
		return result;
	}

	@SuppressWarnings("unchecked")
	private void publish(ToolContext toolContext, ToolResult event) {
		Object sink = toolContext.getContext().get(TOOL_EVENT_SINK);
		if (sink instanceof Sinks.Many<?> many) {
			((Sinks.Many<ToolResult>) many).tryEmitNext(event);
		}
	}

	/** A short, human-readable trace for the panel — not the full payload. */
	private String summarise(Object value) {
		if (value == null) {
			return "no result";
		}
		if (value instanceof Collection<?> collection) {
			return collection.size() + " result(s)";
		}
		String text = value.toString();
		return text.length() <= 500 ? text : text.substring(0, 500);
	}

	private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String field) {
		String trimmed = raw.trim();
		for (E value : type.getEnumConstants()) {
			if (value.name().equalsIgnoreCase(trimmed)) {
				return value;
			}
		}
		String expected = Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
		throw new IllegalArgumentException("Unknown " + field + " '" + raw + "'. Expected one of: " + expected);
	}

	private static Map<String, Object> args(String key, Object value) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		arguments.put(key, value);
		return arguments;
	}

	@FunctionalInterface
	private interface ToolBody<T> {
		T get();
	}
}
