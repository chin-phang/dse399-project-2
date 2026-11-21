package com.wou.logistics_assistant.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An AI-generated delivery route — the aggregate root for route management.
 *
 * Derived statistics live on {@link RouteSummary}, never here: this record stays operational.
 */
@Entity
@Table(name = "route")
public class Route {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "route_id")
	private Long routeId;

	@NotBlank
	@Column(name = "route_name", nullable = false, length = 100)
	private String routeName;

	@Enumerated(EnumType.STRING)
	@Column(name = "route_status", nullable = false, length = 30)
	private RouteStatus routeStatus = RouteStatus.PLANNED;

	@Column(name = "generated_time", nullable = false)
	private LocalDateTime generatedTime = LocalDateTime.now();

	/** Nullable: createRoute and assignVehicle are separate operations. */
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "vehicle_id")
	private Vehicle vehicle;

	@OneToMany(mappedBy = "route", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@OrderBy("sequenceOrder ASC, jobId ASC")
	private List<Job> jobs = new ArrayList<>();

	@OneToOne(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
	private RouteSummary summary;

	protected Route() {
	}

	public Route(String routeName) {
		this.routeName = routeName;
		this.routeStatus = RouteStatus.PLANNED;
		this.generatedTime = LocalDateTime.now();
	}

	public void assignVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
		if (vehicle.getRoutes().stream().noneMatch(route -> route == this)) {
			vehicle.getRoutes().add(this);
		}
	}

	public void addJob(Job job) {
		job.assignToRoute(this);
	}

	public void removeJob(Job job) {
		job.removeFromRoute();
	}

	/**
	 * Applies an ordering produced by the planner. The heuristic itself lives in
	 * {@code PlanningService} — the entity only records the result.
	 */
	public void applyOrdering(List<Job> ordered) {
		for (int index = 0; index < ordered.size(); index++) {
			ordered.get(index).setSequenceOrder(index);
		}
		jobs.sort(Comparator.comparingInt(Route::sequenceKey));
	}

	public void updateStatus(RouteStatus status) {
		this.routeStatus = status;
	}

	/** Stops in planned visiting order — what the distance calculation walks. */
	public List<Job> orderedJobs() {
		return jobs.stream().sorted(Comparator.comparingInt(Route::sequenceKey)).toList();
	}

	private static int sequenceKey(Job job) {
		return job.getSequenceOrder() == null ? Integer.MAX_VALUE : job.getSequenceOrder();
	}

	public Long getRouteId() {
		return routeId;
	}

	public String getRouteName() {
		return routeName;
	}

	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}

	public RouteStatus getRouteStatus() {
		return routeStatus;
	}

	public void setRouteStatus(RouteStatus routeStatus) {
		this.routeStatus = routeStatus;
	}

	public LocalDateTime getGeneratedTime() {
		return generatedTime;
	}

	public void setGeneratedTime(LocalDateTime generatedTime) {
		this.generatedTime = generatedTime;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public List<Job> getJobs() {
		return jobs;
	}

	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}

	public RouteSummary getSummary() {
		return summary;
	}

	public void setSummary(RouteSummary summary) {
		this.summary = summary;
	}
}
