package com.wou.logistics_assistant.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** TC-FR07-001 — derived route statistics. */
class RouteSummaryStatisticsTest {

	@Test
	void derivesDurationFromDistancePlusPerStopServiceTime() {
		long duration = RouteSummary.estimateDurationSeconds(40_000.0, 3);

		assertThat(duration).isEqualTo(3600L + 3 * 300L);
	}

	@Test
	void derivesFuelFromDistance() {
		assertThat(RouteSummary.estimateFuelLitres(100_000.0)).isCloseTo(12.0, within(0.001));
	}

	@Test
	void applyStatisticsRecordsEveryDerivedField() {
		RouteSummary summary = new RouteSummary();

		summary.applyStatistics(20_000.0, 4);

		assertThat(summary.getTotalDistance()).isEqualTo(20_000.0);
		assertThat(summary.getTotalJobs()).isEqualTo(4);
		assertThat(summary.getEstimatedDuration()).isEqualTo(1800L + 4 * 300L);
		assertThat(summary.getFuelConsumption()).isCloseTo(2.4, within(0.001));
	}

	@Test
	void aZeroDistanceRouteStillReportsServiceTime() {
		RouteSummary summary = new RouteSummary();

		summary.applyStatistics(0.0, 2);

		assertThat(summary.getTotalDistance()).isZero();
		assertThat(summary.getEstimatedDuration()).isEqualTo(600L);
	}
}
