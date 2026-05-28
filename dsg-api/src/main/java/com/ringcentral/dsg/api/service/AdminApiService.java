package com.ringcentral.dsg.api.service;

import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.CreateJobRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryConfigRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryUpdateRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.JobReportResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.JobResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.SchedulerRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminApiService {

    private final DirectoryConfigService directoryConfigService;
    private final ConfigurationService configurationService;
    private final JobManagerService jobManagerService;

    public AdminApiService(
            DirectoryConfigService directoryConfigService,
            ConfigurationService configurationService,
            JobManagerService jobManagerService) {
        this.directoryConfigService = directoryConfigService;
        this.configurationService = configurationService;
        this.jobManagerService = jobManagerService;
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
        configurationService.configureScheduler(accountId, request);
    }

    public void saveAttributeMapping(String accountId, AttributeMappingRequest request) {
        configurationService.saveAttributeMapping(accountId, request);
    }

    public void saveRule(String accountId, ProvisioningRuleRequest request) {
        configurationService.saveRule(accountId, request);
    }

    public Optional<JobResponse> createJob(String accountId, CreateJobRequest request) {
        return jobManagerService.createJob(accountId, request);
    }

    public JobReportResponse getJobReport(String jobId) {
        return jobManagerService.getJobReport(jobId);
    }
}
