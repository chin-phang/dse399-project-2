package com.wou.logistics_assistant.repository;

import com.wou.logistics_assistant.entity.Job;
import com.wou.logistics_assistant.entity.JobStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

	/**
	 * The property path is {@code route.routeId}, so {@code findByRouteId} would not resolve
	 * (Route's identifier property is {@code routeId}, not {@code id}).
	 */
	List<Job> findByRouteRouteIdOrderBySequenceOrderAscJobIdAsc(Long routeId);

	List<Job> findByStatus(JobStatus status);

	/** Unassigned jobs — the pool the planner draws from. */
	List<Job> findByRouteIsNull();
}
