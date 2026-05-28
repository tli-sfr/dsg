package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.model.AdminApiModels.CreateJobRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.ErrorResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.JobReportResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.JobResponse;
import com.ringcentral.dsg.api.service.AdminApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dsg/v1/{accountId}")
public class JobController {

    private final AdminApiService adminApiService;

    public JobController(AdminApiService adminApiService) {
        this.adminApiService = adminApiService;
    }

    @PostMapping("/jobs")
    public ResponseEntity<?> createJob(@PathVariable String accountId, @RequestBody CreateJobRequest request) {
        return adminApiService.createJob(accountId, request)
                .<ResponseEntity<?>>map(job -> ResponseEntity.status(HttpStatus.ACCEPTED).body(job))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("JOB_ALREADY_RUNNING", "Another job is already running for this account")));
    }

    @GetMapping("/jobs/{jobId}/report")
    public ResponseEntity<JobReportResponse> getJobReport(@PathVariable String accountId, @PathVariable String jobId) {
        return ResponseEntity.ok(adminApiService.getJobReport(jobId));
    }
}
