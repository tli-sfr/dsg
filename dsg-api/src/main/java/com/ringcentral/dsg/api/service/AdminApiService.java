package com.ringcentral.dsg.api.service;

import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingConfigResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DeprovisioningRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DeprovisioningResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleDetailResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleListResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.CreateJobRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryConfigRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryUpdateRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.JobHistoryResponse;
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

    public com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthConfigResponse getOAuthConfig(String accountId) {
        return directoryConfigService.getOAuthConfig(accountId);
    }

    public java.util.Map<String, String> getDirectoryAuthorizeUrl(String accountId) {
        return directoryConfigService.getDirectoryAuthorizeUrl(accountId);
    }

    public com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthConnectResponse exchangeDirectoryOAuthToken(
            String accountId,
            com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthTokenRequest request) {
        return directoryConfigService.exchangeDirectoryOAuthToken(accountId, request);
    }

    public void disconnectDirectoryOAuth(String accountId) {
        directoryConfigService.disconnectDirectoryOAuth(accountId);
    }

    public com.ringcentral.dsg.api.model.AdminApiModels.DirectoryGroupsResponse listDirectoryGroups(
            String accountId,
            String search) {
        return directoryConfigService.listDirectoryGroups(accountId, search);
    }

    public void configureScheduler(String accountId, SchedulerRequest request) {
        configurationService.configureScheduler(accountId, request);
    }

    public AttributeMappingConfigResponse getAttributeMappingConfig(String accountId) {
        return configurationService.getAttributeMappingConfig(accountId);
    }

    public void saveAttributeMapping(String accountId, AttributeMappingRequest request) {
        configurationService.saveAttributeMapping(accountId, request);
    }

    public void saveRule(String accountId, ProvisioningRuleRequest request) {
        configurationService.saveRule(accountId, request);
    }

    public void updateRule(String accountId, long ruleId, ProvisioningRuleRequest request) {
        configurationService.updateRule(accountId, ruleId, request);
    }

    public ProvisioningRuleDetailResponse getRule(String accountId, long ruleId) {
        return configurationService.getRule(accountId, ruleId);
    }

    public ProvisioningRuleListResponse listRules(String accountId) {
        return configurationService.listRules(accountId);
    }

    public DeprovisioningResponse getDeprovisioning(String accountId) {
        return configurationService.getDeprovisioning(accountId);
    }

    public void saveDeprovisioning(String accountId, DeprovisioningRequest request) {
        configurationService.saveDeprovisioning(accountId, request.deprovisioningType());
    }

    public Optional<JobResponse> createJob(String accountId, CreateJobRequest request) {
        return jobManagerService.createJob(accountId, request);
    }

    public Optional<JobReportResponse> getJobReport(String accountId, String jobId) {
        return jobManagerService.getJobReport(accountId, jobId);
    }

    public Optional<JobReportResponse> getLatestJobReport(String accountId) {
        return jobManagerService.getLatestJobReport(accountId);
    }

    public JobHistoryResponse listJobs(String accountId, Integer limit) {
        return jobManagerService.listJobs(accountId, limit);
    }
}
