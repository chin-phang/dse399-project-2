package com.wou.logistics_assistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Reporting projection of a {@link Route} — one-to-one, so derived statistics stay out of the
 * operational route record.
 *
 * Distance is not calculated here. It comes from a PostGIS {@code ST_Distance} query, and an
 * entity issuing queries would break the layering rule, so {@code RouteSummaryServiceImpl}
 * computes and this record stores. {@link #applyStatistics} is the entity-side half of that split.
 */
@Entity
@Table(name = "route_summary")
public class RouteSummary {

	/** Planning assumptions, deliberately explicit rather than buried as magic numbers. */
	public static final double AVERAGE_SPEED_KMH = 40.0;

	public static final long SERVICE_TIME_PER_STOP_SECONDS = 300L;

	public static final double FUEL_LITRES_PER_100_KM = 12.0;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "summary_id")
	private Long summaryId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false, unique = true)
	private Route route;

	@Column(name = "total_distance")
	private Double totalDistance;

	@Column(name = "estimated_duration")
	private Long estimatedDuration;

	@Column(name = "total_jobs")
	private Integer totalJobs;

	@Column(name = "fuel_consumption")
	private Double fuelConsumption;

	@Column(name = "generated_time", nullable = false)
	private LocalDateTime generatedTime = LocalDateTime.now();

	public RouteSummary() {
	}

	/**
	 * Records a freshly computed statistics set.
	 *
	 * @param distanceMetres total travel distance, from the PostGIS query
	 * @param jobCount number of stops on the route
	 */
	public void applyStatistics(double distanceMetres, int jobCount) {
		this.totalDistance = distanceMetres;
		this.totalJobs = jobCount;
		this.estimatedDuration = estimateDurationSeconds(distanceMetres, jobCount);
		this.fuelConsumption = estimateFuelLitres(distanceMetres);
		this.generatedTime = LocalDateTime.now();
	}

	public static long estimateDurationSeconds(double distanceMetres, int jobCount) {
		double travelSeconds = (distanceMetres / 1000.0) / AVERAGE_SPEED_KMH * 3600.0;
		return (long) travelSeconds + jobCount * SERVICE_TIME_PER_STOP_SECONDS;
	}

	public static double estimateFuelLitres(double distanceMetres) {
		return (distanceMetres / 1000.0) * (FUEL_LITRES_PER_100_KM / 100.0);
	}

	public Long getSummaryId() {
		return summaryId;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public Double getTotalDistance() {
		return totalDistance;
	}

	public Long getEstimatedDuration() {
		return estimatedDuration;
	}

	public Integer getTotalJobs() {
		return totalJobs;
	}

	public Double getFuelConsumption() {
		return fuelConsumption;
	}

	public LocalDateTime getGeneratedTime() {
		return generatedTime;
	}
}
