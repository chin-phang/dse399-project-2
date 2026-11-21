package com.wou.logistics_assistant.service;

import com.wou.logistics_assistant.dto.job.CreateJobRequest;
import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.dto.job.UpdateJobRequest;
import com.wou.logistics_assistant.entity.JobStatus;
import java.util.List;

/** UC04 — delivery job management. */
public interface JobService {

	JobDto createJob(CreateJobRequest request);

	JobDto updateJob(Long jobId, UpdateJobRequest request);

	void deleteJob(Long jobId);

	JobDto getJob(Long jobId);

	List<JobDto> getAllJobs();

	List<JobDto> getJobsByStatus(JobStatus status);

	List<JobDto> getJobsByIds(List<Long> jobIds);

	List<JobDto> getUnassignedJobs();
}
