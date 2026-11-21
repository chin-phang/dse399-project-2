package com.wou.logistics_assistant.entity;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

/**
 * An individual delivery job. Jobs exist independently of routes — they are created unassigned
 * (via the Jobs module or the AI {@code createDeliveryJob} tool) and only later planned onto a route.
 */
@Entity
@Table(name = "job")
public class Job {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "job_id")
	private Long jobId;

	@NotBlank
	@Column(name = "job_name", nullable = false, length = 150)
	private String jobName;

	@NotBlank
	@Column(name = "delivery_location", nullable = false, length = 255)
	private String deliveryLocation;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Enumerated(EnumType.STRING)
	@Column(name = "priority", nullable = false, length = 20)
	private JobPriority priority = JobPriority.MEDIUM;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private JobStatus status = JobStatus.PENDING;

	/** Nullable: a job is unassigned until a route is planned for it. */
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "route_id")
	private Route route;

	/** Position within its route, set by the planner. Null while the job is unassigned. */
	@Column(name = "sequence_order")
	private Integer sequenceOrder;

	/**
	 * DB-generated from latitude/longitude — Postgres computes it, Hibernate only reads it.
	 * Never write to this; always write the scalar pair.
	 */
	@JdbcTypeCode(SqlTypes.GEOGRAPHY)
	@Column(name = "location", insertable = false, updatable = false)
	private Point location;

	protected Job() {
	}

	public Job(String jobName, String deliveryLocation) {
		this(jobName, deliveryLocation, null, null, JobPriority.MEDIUM, JobStatus.PENDING);
	}

	public Job(String jobName, String deliveryLocation, Double latitude, Double longitude) {
		this(jobName, deliveryLocation, latitude, longitude, JobPriority.MEDIUM, JobStatus.PENDING);
	}

	public Job(
		String jobName,
		String deliveryLocation,
		Double latitude,
		Double longitude,
		JobPriority priority,
		JobStatus status
	) {
		this.jobName = jobName;
		this.deliveryLocation = deliveryLocation;
		this.latitude = latitude;
		this.longitude = longitude;
		this.priority = priority == null ? JobPriority.MEDIUM : priority;
		this.status = status == null ? JobStatus.PENDING : status;
	}

	/** Attaches this job to a route, keeping both sides of the association in step. */
	public void assignToRoute(Route route) {
		if (this.route != null) {
			this.route.getJobs().removeIf(job -> job == this);
		}
		this.route = route;
		if (route.getJobs().stream().noneMatch(job -> job == this)) {
			route.getJobs().add(this);
		}
		if (status == JobStatus.PENDING) {
			status = JobStatus.ASSIGNED;
		}
	}

	public void removeFromRoute() {
		if (route != null) {
			route.getJobs().removeIf(job -> job == this);
		}
		route = null;
		sequenceOrder = null;
		if (status == JobStatus.ASSIGNED) {
			status = JobStatus.PENDING;
		}
	}

	public void updateStatus(JobStatus newStatus) {
		this.status = newStatus;
	}

	/** True when the job carries usable coordinates, i.e. the planner can order it spatially. */
	public boolean hasCoordinates() {
		return latitude != null && longitude != null;
	}

	public boolean validate() {
		return jobName != null
			&& !jobName.isBlank()
			&& deliveryLocation != null
			&& !deliveryLocation.isBlank()
			&& (latitude == null || (latitude >= -90.0 && latitude <= 90.0))
			&& (longitude == null || (longitude >= -180.0 && longitude <= 180.0));
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getDeliveryLocation() {
		return deliveryLocation;
	}

	public void setDeliveryLocation(String deliveryLocation) {
		this.deliveryLocation = deliveryLocation;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public JobPriority getPriority() {
		return priority;
	}

	public void setPriority(JobPriority priority) {
		this.priority = priority;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public Integer getSequenceOrder() {
		return sequenceOrder;
	}

	public void setSequenceOrder(Integer sequenceOrder) {
		this.sequenceOrder = sequenceOrder;
	}

	public Point getLocation() {
		return location;
	}
}
