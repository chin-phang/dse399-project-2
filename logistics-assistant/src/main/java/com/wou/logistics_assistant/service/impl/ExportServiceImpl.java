package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.dto.route.RouteSummaryDto;
import com.wou.logistics_assistant.service.ExportService;
import com.wou.logistics_assistant.service.RouteService;
import com.wou.logistics_assistant.service.RouteSummaryService;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

	private final RouteSummaryService routeSummaryService;
	private final RouteService routeService;

	public ExportServiceImpl(RouteSummaryService routeSummaryService, RouteService routeService) {
		this.routeSummaryService = routeSummaryService;
		this.routeService = routeService;
	}

	@Override
	public byte[] exportCSV(Long routeId) {
		RouteDto route = routeService.getRoute(routeId);
		RouteSummaryDto summary = routeSummaryService.getSummary(routeId);

		StringBuilder rows = new StringBuilder();
		rows.append("Route Report\n");
		rows.append("Route ID,Route Name,Status,Generated,Vehicle ID\n");
		rows.append(csv(
			String.valueOf(route.routeId()),
			route.routeName(),
			route.routeStatus().name(),
			route.generatedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
			route.vehicleId() == null ? "" : route.vehicleId().toString()
		)).append('\n');
		rows.append('\n');
		rows.append("Total Distance (m),Estimated Duration (s),Total Jobs,Fuel Consumption (L)\n");
		rows.append(csv(
			summary.totalDistance() == null ? "" : summary.totalDistance().toString(),
			summary.estimatedDuration() == null ? "" : summary.estimatedDuration().toString(),
			summary.totalJobs() == null ? "" : summary.totalJobs().toString(),
			summary.fuelConsumption() == null ? "" : summary.fuelConsumption().toString()
		)).append('\n');
		rows.append('\n');
		rows.append("Stop,Job ID,Job Name,Delivery Location,Latitude,Longitude,Priority,Status\n");
		List<JobDto> jobs = route.jobs();
		for (int index = 0; index < jobs.size(); index++) {
			JobDto job = jobs.get(index);
			rows.append(csv(
				String.valueOf(index + 1),
				job.jobId().toString(),
				job.jobName(),
				job.deliveryLocation(),
				job.latitude() == null ? "" : job.latitude().toString(),
				job.longitude() == null ? "" : job.longitude().toString(),
				job.priority().name(),
				job.status().name()
			)).append('\n');
		}
		return rows.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public byte[] exportReport(Long routeId) {
		return exportCSV(routeId);
	}

	private String csv(String... values) {
		StringBuilder row = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				row.append(',');
			}
			row.append(escape(values[i]));
		}
		return row.toString();
	}

	/** RFC 4180: quote when the value contains a comma, quote or newline; double inner quotes. */
	private String escape(String value) {
		boolean needsQuotes = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == ',' || c == '"' || c == '\n' || c == '\r') {
				needsQuotes = true;
				break;
			}
		}
		if (!needsQuotes) {
			return value;
		}
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}
