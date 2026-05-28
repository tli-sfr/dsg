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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminApiService {

    private final Map<String, DirectoryRecord> directoryByAccount = new ConcurrentHashMap<>();
    private final Map<String, DirectoryOAuthRequest> oauthByAccount = new ConcurrentHashMap<>();
    private final Map<String, SchedulerRequest> schedulerByAccount = new ConcurrentHashMap<>();
    private final Map<String, AttributeMappingRequest> mappingsByAccount = new ConcurrentHashMap<>();
    private final Map<String, ProvisioningRuleRequest> ruleByAccount = new ConcurrentHashMap<>();
    private final Map<String, JobRecord> activeJobByAccount = new ConcurrentHashMap<>();
    private final Map<String, JobReportResponse> reportByJobId = new ConcurrentHashMap<>();

    public void createDirectory(String accountId, DirectoryConfigRequest request) {
        directoryByAccount.put(accountId, new DirectoryRecord(request.directoryType().name(), null, false));
    }

    public void updateDirectory(String accountId, DirectoryUpdateRequest request) {
        DirectoryRecord current = directoryByAccount.getOrDefault(accountId, new DirectoryRecord("Unknown", null, false));
        boolean active = request.active() != null ? request.active() : current.active();
        String groupId = request.directoryGroupId() != null ? request.directoryGroupId() : current.directoryGroupId();
        directoryByAccount.put(accountId, new DirectoryRecord(current.directoryType(), groupId, active));
    }

    public DirectoryResponse getDirectory(String accountId) {
        DirectoryRecord record = directoryByAccount.getOrDefault(accountId, new DirectoryRecord("Unknown", null, false));
        return new DirectoryResponse(record.directoryType(), record.directoryGroupId(), record.active(), oauthByAccount.containsKey(accountId));
    }

    public void putOAuth(String accountId, DirectoryOAuthRequest request) {
        oauthByAccount.put(accountId, request);
    }

    public DirectoryOAuthResponse getOAuth(String accountId) {
        DirectoryOAuthRequest request = oauthByAccount.get(accountId);
        if (request == null) {
            return new DirectoryOAuthResponse(null, null, null);
        }
        return new DirectoryOAuthResponse(request.directoryType().name(), maskClientId(request.clientId()), Instant.now().plusSeconds(3600));
    }

    public boolean testOAuth(String accountId) {
        return oauthByAccount.containsKey(accountId);
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

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 4) {
            return "****";
        }
        return clientId.substring(0, 2) + "****" + clientId.substring(clientId.length() - 2);
    }

    private record DirectoryRecord(String directoryType, String directoryGroupId, boolean active) {
    }

    private record JobRecord(String jobId, boolean terminal) {
    }
}
