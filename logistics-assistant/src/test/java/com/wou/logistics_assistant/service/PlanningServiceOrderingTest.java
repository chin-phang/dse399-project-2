package com.wou.logistics_assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.entity.JobPriority;
import com.wou.logistics_assistant.entity.JobStatus;
import com.wou.logistics_assistant.service.impl.PlanningServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * TC-FR06-001 — AI route planning recommendation (nearest-neighbour ordering).
 *
 * The heuristic is a pure function over coordinates, so it is tested without a database or any
 * Spring context at all.
 */
class PlanningServiceOrderingTest {

	private final PlanningServiceImpl planning = new PlanningServiceImpl(mock(RouteService.class), mock(JobService.class));

	private JobDto job(long id, Double lat, Double lon, JobPriority priority) {
		return new JobDto(id, null, "Job " + id, "Location " + id, lat, lon, null, priority, JobStatus.PENDING);
	}

	private JobDto job(long id, Double lat, Double lon) {
		return job(id, lat, lon, JobPriority.MEDIUM);
	}

	@Test
	void visitsTheNearestNextStopRatherThanInputOrder() {
		JobDto start = job(1, 3.1390, 101.6869, JobPriority.HIGH);
		JobDto far = job(2, 5.4141, 100.3288);
		JobDto near = job(3, 3.0738, 101.5183);

		List<JobDto> ordered = planning.order(List.of(start, far, near));

		assertThat(ordered.stream().map(JobDto::jobId).toList()).containsExactly(1L, 3L, 2L);
	}

	@Test
	void startsFromTheHighestPriorityStop() {
		JobDto low = job(1, 3.1390, 101.6869, JobPriority.LOW);
		JobDto high = job(2, 3.1490, 101.6969, JobPriority.HIGH);

		List<JobDto> ordered = planning.order(List.of(low, high));

		assertThat(ordered.getFirst().jobId()).isEqualTo(2L);
	}

	@Test
	void jobsWithoutCoordinatesSortLastInsteadOfDefaultingToZeroZero() {
		JobDto located = job(1, 3.1390, 101.6869);
		JobDto unlocated = job(2, null, null, JobPriority.HIGH);
		JobDto alsoUnlocated = job(3, null, null, JobPriority.LOW);

		List<JobDto> ordered = planning.order(List.of(unlocated, located, alsoUnlocated));

		assertThat(ordered.stream().map(JobDto::jobId).toList()).containsExactly(1L, 2L, 3L);
	}

	@Test
	void handlesAnEmptyAndASingleJobList() {
		assertThat(planning.order(List.of())).isEmpty();
		assertThat(planning.order(List.of(job(1, 1.0, 2.0)))).hasSize(1);
	}

	@Test
	void keepsEveryJobExactlyOnce() {
		List<JobDto> jobs = new ArrayList<>();
		for (long id = 1; id <= 8; id++) {
			jobs.add(job(id, 3.0 + id * 0.01, 101.0 + id * 0.02));
		}
		List<JobDto> shuffled = new ArrayList<>(jobs);
		Collections.shuffle(shuffled);

		List<JobDto> ordered = planning.order(shuffled);

		assertThat(ordered).hasSize(8);
		assertThat(ordered.stream().map(JobDto::jobId).toList())
			.containsExactlyInAnyOrderElementsOf(jobs.stream().map(JobDto::jobId).toList());
	}
}
