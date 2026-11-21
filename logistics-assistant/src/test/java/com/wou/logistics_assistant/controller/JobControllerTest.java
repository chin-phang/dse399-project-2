package com.wou.logistics_assistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wou.logistics_assistant.dto.job.CreateJobRequest;
import com.wou.logistics_assistant.dto.job.JobDto;
import com.wou.logistics_assistant.entity.JobPriority;
import com.wou.logistics_assistant.entity.JobStatus;
import com.wou.logistics_assistant.exception.ResourceNotFoundException;
import com.wou.logistics_assistant.security.jwt.JwtService;
import com.wou.logistics_assistant.service.JobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * TC-FR04-001 — job management over HTTP. Security is excluded here so these assertions are about
 * routing, validation and serialisation; authentication has its own test.
 */
@WebMvcTest(
	controllers = JobController.class,
	excludeAutoConfiguration = {SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class}
)
class JobControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JobService jobService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	private final JobDto sampleJob = new JobDto(
		1L,
		null,
		"Deliver pallets",
		"Klang",
		3.0,
		101.4,
		null,
		JobPriority.HIGH,
		JobStatus.PENDING
	);

	@Test
	void listsJobs() throws Exception {
		given(jobService.getAllJobs()).willReturn(List.of(sampleJob));

		mockMvc.perform(get("/api/jobs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].jobName").value("Deliver pallets"))
			.andExpect(jsonPath("$[0].routeId").doesNotExist());
	}

	@Test
	void createsAJobAndReturns201() throws Exception {
		given(jobService.createJob(any())).willReturn(sampleJob);

		mockMvc.perform(post("/api/jobs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CreateJobRequest("Deliver pallets", "Klang"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.jobId").value(1));
	}

	@Test
	void rejectsAJobWithABlankNameAs400() throws Exception {
		mockMvc.perform(post("/api/jobs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"jobName\":\"\",\"deliveryLocation\":\"Klang\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAnOutOfRangeLatitudeAs400() throws Exception {
		mockMvc.perform(post("/api/jobs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"jobName\":\"J\",\"deliveryLocation\":\"K\",\"latitude\":95.0}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void aMissingJobBecomes404ThroughTheProblemDetailHandler() throws Exception {
		given(jobService.getJob(99L)).willThrow(new ResourceNotFoundException("Job", 99));

		mockMvc.perform(get("/api/jobs/99"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Not Found"));
	}
}
