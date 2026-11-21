package com.wou.logistics_assistant.service.impl;

import com.wou.logistics_assistant.dto.job.CreateJobRequest;
import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.job.UpdateJobRequest;
import com.wou.logistics_assistant.entity.Job;
import com.wou.logistics_assistant.entity.JobStatus;
import com.wou.logistics_assistant.exception.ResourceNotFoundException;
import com.wou.logistics_assistant.exception.ValidationException;
import com.wou.logistics_assistant.mapper.Mappers;
import com.wou.logistics_assistant.repository.JobRepository;
import com.wou.logistics_assistant.service.JobService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobServiceImpl implements JobService {

	private final JobRepository jobRepository;

	public JobServiceImpl(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	@Override
	public JobDto createJob(CreateJobRequest request) {
		Job job = new Job(
			request.jobName(),
			request.deliveryLocation(),
			request.latitude(),
			request.longitude(),
			request.priorityOrDefault(),
			request.statusOrDefault()
		);
		if (!job.validate()) {
			throw new ValidationException("Job details are invalid");
		}
		// Saved unassigned: jobs exist before any route does.
		return Mappers.toDto(jobRepository.save(job));
	}

	@Override
	public JobDto updateJob(Long jobId, UpdateJobRequest request) {
		Job job = find(jobId);
		if (request.jobName() != null) {
			job.setJobName(request.jobName());
		}
		if (request.deliveryLocation() != null) {
			job.setDeliveryLocation(request.deliveryLocation());
		}
		if (request.latitude() != null) {
			job.setLatitude(request.latitude());
		}
		if (request.longitude() != null) {
			job.setLongitude(request.longitude());
		}
		if (request.priority() != null) {
			job.setPriority(request.priority());
		}
		if (request.status() != null) {
			job.updateStatus(request.status());
		}
		if (!job.validate()) {
			throw new ValidationException("Job details are invalid");
		}
		// location is DB-generated from the scalar pair, so it refreshes on flush.
		return Mappers.toDto(jobRepository.save(job));
	}

	@Override
	public void deleteJob(Long jobId) {
		Job job = find(jobId);
		job.removeFromRoute();
		jobRepository.delete(job);
	}

	@Override
	@Transactional(readOnly = true)
	public JobDto getJob(Long jobId) {
		return Mappers.toDto(find(jobId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobDto> getAllJobs() {
		return jobRepository.findAll().stream().map(Mappers::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobDto> getJobsByStatus(JobStatus status) {
		return jobRepository.findByStatus(status).stream().map(Mappers::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobDto> getJobsByIds(List<Long> jobIds) {
		List<Job> found = jobRepository.findAllById(jobIds);
		Set<Long> foundIds = found.stream().map(Job::getJobId).collect(Collectors.toSet());
		Set<Long> missing = new HashSet<>(jobIds);
		missing.removeAll(foundIds);
		if (!missing.isEmpty()) {
			String ids = missing.stream().map(String::valueOf).collect(Collectors.joining(", "));
			throw new ResourceNotFoundException("Job", ids);
		}
		// Preserve the caller's ordering; findAllById makes no promise about it.
		Map<Long, Job> byId = found.stream().collect(Collectors.toMap(Job::getJobId, Function.identity()));
		List<JobDto> ordered = new ArrayList<>();
		for (Long id : jobIds) {
			Job job = byId.get(id);
			if (job != null) {
				ordered.add(Mappers.toDto(job));
			}
		}
		return ordered;
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobDto> getUnassignedJobs() {
		return jobRepository.findByRouteIsNull().stream().map(Mappers::toDto).toList();
	}

	private Job find(Long jobId) {
		return jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
	}
}
