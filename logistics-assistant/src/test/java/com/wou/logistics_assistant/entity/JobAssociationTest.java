package com.wou.logistics_assistant.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Entity invariants — both sides of an association must stay in step. */
class JobAssociationTest {

	@Test
	void assignToRouteSetsBothSidesAndAdvancesStatus() {
		Route route = new Route("R1");
		Job job = new Job("J1", "Somewhere");

		job.assignToRoute(route);

		assertThat(job.getRoute()).isSameAs(route);
		assertThat(route.getJobs()).containsExactly(job);
		assertThat(job.getStatus()).isEqualTo(JobStatus.ASSIGNED);
	}

	@Test
	void reassigningDetachesFromThePreviousRoute() {
		Route first = new Route("R1");
		Route second = new Route("R2");
		Job job = new Job("J1", "Somewhere");

		job.assignToRoute(first);
		job.assignToRoute(second);

		assertThat(first.getJobs()).isEmpty();
		assertThat(second.getJobs()).containsExactly(job);
	}

	@Test
	void removeFromRouteReturnsTheJobToTheUnassignedPool() {
		Route route = new Route("R1");
		Job job = new Job("J1", "Somewhere");
		job.assignToRoute(route);

		job.removeFromRoute();

		assertThat(job.getRoute()).isNull();
		assertThat(job.getSequenceOrder()).isNull();
		assertThat(route.getJobs()).isEmpty();
		assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
	}

	@Test
	void applyOrderingNumbersTheStopsAndSortsThem() {
		Route route = new Route("R1");
		Job a = new Job("A", "A");
		a.setJobId(1L);
		Job b = new Job("B", "B");
		b.setJobId(2L);
		route.addJob(a);
		route.addJob(b);

		route.applyOrdering(List.of(b, a));

		assertThat(b.getSequenceOrder()).isEqualTo(0);
		assertThat(a.getSequenceOrder()).isEqualTo(1);
		assertThat(route.orderedJobs()).containsExactly(b, a);
	}

	@Test
	void validateRejectsOutOfRangeCoordinates() {
		assertThat(new Job("J", "L", 91.0, null).validate()).isFalse();
		assertThat(new Job("J", "L", null, 181.0).validate()).isFalse();
		assertThat(new Job("J", "L").validate()).isTrue();
	}

	@Test
	void aJobWithNoCoordinatesIsValidButNotLocatable() {
		Job job = new Job("J", "Somewhere with no pin yet");

		assertThat(job.validate()).isTrue();
		assertThat(job.hasCoordinates()).isFalse();
	}
}
