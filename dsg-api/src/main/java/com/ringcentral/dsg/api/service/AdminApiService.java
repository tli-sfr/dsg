package com.ringcentral.dsg.api.service;

import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.CreateJobRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryConfigRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryUpdateRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.JobFailure;
import com.ringcentral.dsg.api.model.AdminApiModels.JobReportResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.JobResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.SchedulerRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminApiService {

    private final DirectoryConfigService directoryConfigService;
    private final Map<String, SchedulerRequest> schedulerByAccount = new ConcurrentHashMap<>();
    private final Map<String, AttributeMappingRequest> mappingsByAccount = new ConcurrentHashMap<>();
    private final Map<String, ProvisioningRuleRequest> ruleByAccount = new ConcurrentHashMap<>();
    private final Map<String, JobRecord> activeJobByAccount = new ConcurrentHashMap<>();
    private final Map<String, JobReportResponse> reportByJobId = new ConcurrentHashMap<>();

    public AdminApiService(DirectoryConfigService directoryConfigService) {
        this.directoryConfigService = directoryConfigService;
    }

    public void createDirectory(String accountId, DirectoryConfigRequest request) {
        directoryConfigService.createDirectory(accountId, request);
    }

    public void updateDirectory(String accountId, DirectoryUpdateRequest request) {
        directoryConfigService.updateDirectory(accountId, request);
    }

    public DirectoryResponse getDirectory(String accountId) {
        return directoryConfigService.getDirectory(accountId);
    }

    public void putOAuth(String accountId, DirectoryOAuthRequest request) {
        directoryConfigService.putOAuth(accountId, request);
    }

    public DirectoryOAuthResponse getOAuth(String accountId) {
        return directoryConfigService.getOAuth(accountId);
    }

    public boolean testOAuth(String accountId) {
        return directoryConfigService.testOAuth(accountId);
    }

    public void configureScheduler(String accountId, SchedulerRequest request) {
        schedulerByAccount.put(accountId, request);
    }

    public void saveAttributeMapping(String accountId, AttributeMappingRequest request) {
        mappingsByAccount.put(accountId, request);
    }

    public void saveRule(String accountId, ProvisioningRuleRequest request) {
        ruleByAccount.put(accountId, request);
    }

    public Optional<JobResponse> createJob(String accountId, CreateJobRequest request) {
        JobRecord current = activeJobByAccount.get(accountId);
        if (current != null && !current.terminal()) {
            return Optional.empty();
        }
        String jobId = UUID.randomUUID().toString();
        activeJobByAccount.put(accountId, new JobRecord(jobId, false));
        reportByJobId.put(jobId, new JobReportResponse(jobId, 0, 0, List.of()));
        return Optional.of(new JobResponse(jobId, "PENDING"));
    }

    public JobReportResponse getJobReport(String jobId) {
        return reportByJobId.getOrDefault(jobId, new JobReportResponse(jobId, 0, 0, List.of(new JobFailure(null, "Report not found"))));
    }

    private record JobRecord(String jobId, boolean terminal) {
    }
}
