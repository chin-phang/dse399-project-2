package com.wou.logistics_assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.route.RouteDto;
import com.wou.logistics_assistant.dto.route.RouteSummaryDto;
import com.wou.logistics_assistant.entity.JobPriority;
import com.wou.logistics_assistant.entity.JobStatus;
import com.wou.logistics_assistant.entity.RouteStatus;
import com.wou.logistics_assistant.service.impl.ExportServiceImpl;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** TC-FR08-001 — CSV route report export. */
@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

	@Mock
	private RouteService routeService;

	@Mock
	private RouteSummaryService routeSummaryService;

	private JobDto job(long id, String name, String location) {
		return new JobDto(
			id,
			7L,
			name,
			location,
			3.1,
			101.6,
			(int) (id - 1),
			JobPriority.HIGH,
			JobStatus.ASSIGNED
		);
	}

	private void stubRoute(List<JobDto> jobs) {
		when(routeService.getRoute(7L)).thenReturn(new RouteDto(
			7L,
			"Morning run",
			RouteStatus.PLANNED,
			LocalDateTime.of(2026, 8, 30, 9, 0),
			2L,
			jobs
		));
		when(routeSummaryService.getSummary(7L)).thenReturn(new RouteSummaryDto(
			1L,
			7L,
			12_345.6,
			1800L,
			jobs.size(),
			1.48,
			LocalDateTime.of(2026, 8, 30, 9, 5)
		));
	}

	private ExportServiceImpl exportService() {
		return new ExportServiceImpl(routeSummaryService, routeService);
	}

	@Test
	void writesRouteSummaryAndOneRowPerStop() {
		stubRoute(List.of(job(1, "Deliver pallets", "Klang"), job(2, "Collect crates", "Shah Alam")));

		String csv = new String(exportService().exportCSV(7L), StandardCharsets.UTF_8);

		assertThat(csv).contains("Morning run");
		assertThat(csv).contains("12345.6");
		assertThat(csv).contains("Deliver pallets");
		assertThat(csv).contains("Collect crates");
		assertThat(Arrays.stream(csv.split("\n")).filter(line -> line.startsWith("1,") || line.startsWith("2,")).count())
			.isEqualTo(2);
	}

	@Test
	void quotesFieldsContainingCommasSoColumnsDoNotShift() {
		stubRoute(List.of(job(1, "Deliver, then collect", "Jalan Ampang, Kuala Lumpur")));

		String csv = new String(exportService().exportCSV(7L), StandardCharsets.UTF_8);

		assertThat(csv).contains("\"Deliver, then collect\"");
		assertThat(csv).contains("\"Jalan Ampang, Kuala Lumpur\"");
	}

	@Test
	void escapesEmbeddedQuotesByDoublingThem() {
		stubRoute(List.of(job(1, "Deliver \"fragile\" load", "Klang")));

		String csv = new String(exportService().exportCSV(7L), StandardCharsets.UTF_8);

		assertThat(csv).contains("\"Deliver \"\"fragile\"\" load\"");
	}

	@Test
	void exportsARouteWithNoStopsWithoutFailing() {
		stubRoute(List.of());

		String csv = new String(exportService().exportCSV(7L), StandardCharsets.UTF_8);

		assertThat(csv).contains("Morning run");
		assertThat(csv).contains("Stop,Job ID,Job Name");
	}
}
