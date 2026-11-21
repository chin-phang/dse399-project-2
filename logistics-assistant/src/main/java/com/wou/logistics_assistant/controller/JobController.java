package com.wou.logistics_assistant.controller;

import com.wou.logistics_assistant.dto.job.CreateJobRequest;
import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.job.UpdateJobRequest;
import com.wou.logistics_assistant.entity.JobStatus;
import com.wou.logistics_assistant.service.JobService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC04 — delivery job management. */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private final JobService jobService;

	public JobController(JobService jobService) {
		this.jobService = jobService;
	}

	@GetMapping
	public List<JobDto> list(
		@RequestParam(required = false) JobStatus status,
		@RequestParam(required = false) Boolean unassigned
	) {
		if (status != null) {
			return jobService.getJobsByStatus(status);
		}
		if (Boolean.TRUE.equals(unassigned)) {
			return jobService.getUnassignedJobs();
		}
		return jobService.getAllJobs();
	}

	@GetMapping("/{id}")
	public JobDto get(@PathVariable Long id) {
		return jobService.getJob(id);
	}

	@PostMapping
	public ResponseEntity<JobDto> create(@Valid @RequestBody CreateJobRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
	}

	@PutMapping("/{id}")
	public JobDto update(@PathVariable Long id, @Valid @RequestBody UpdateJobRequest request) {
		return jobService.updateJob(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		jobService.deleteJob(id);
		return ResponseEntity.noContent().build();
	}
}
