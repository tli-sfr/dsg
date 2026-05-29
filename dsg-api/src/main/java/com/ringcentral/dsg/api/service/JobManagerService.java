package com.ringcentral.dsg.api.service;

import com.ringcentral.dsg.api.model.AdminApiModels.CreateJobRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.JobFailure;
import com.ringcentral.dsg.api.model.AdminApiModels.JobHistoryResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.JobReportResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.JobResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.JobSummaryResponse;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.model.JobReportData;
import com.ringcentral.dsg.persistence.model.JobSummaryData;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.persistence.repo.LookupRepository;
import com.ringcentral.dsg.persistence.tx.AfterCommitRunner;
import com.ringcentral.dsg.worker.service.JobConsolidatorService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobManagerService {

    private static final Logger log = LoggerFactory.getLogger(JobManagerService.class);

    private static final int DEFAULT_SYNC_DIRECTION_ID = 1;
    private static final int DEFAULT_JOB_HISTORY_LIMIT = 20;

    private final JobRepository jobRepository;
    private final AccountDirectoryAuthRepository authRepository;
    private final LookupRepository lookupRepository;
    private final MessageQueuePort messageQueuePort;
    private final JobConsolidatorService consolidatorService;

    public JobManagerService(
            JobRepository jobRepository,
            AccountDirectoryAuthRepository authRepository,
            LookupRepository lookupRepository,
            MessageQueuePort messageQueuePort,
            JobConsolidatorService consolidatorService) {
        this.jobRepository = jobRepository;
        this.authRepository = authRepository;
        this.lookupRepository = lookupRepository;
        this.messageQueuePort = messageQueuePort;
        this.consolidatorService = consolidatorService;
    }

    @Transactional
    public Optional<JobResponse> createJob(String accountId, CreateJobRequest request) {
        consolidatorService.reconcileAccountJobs(accountId);
        if (jobRepository.hasActiveJob(accountId)) {
            log.warn("Rejected new {} job for account {} — another job is still active", request.jobType(), accountId);
            return Optional.empty();
        }

        AccountDirectoryAuthRecord auth = authRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Directory is not configured for account: " + accountId));

        int jobTypeId = lookupRepository.findJobTypeId(request.jobType().name())
                .orElseThrow(() -> new IllegalArgumentException("Unknown job type: " + request.jobType()));

        long jobId = jobRepository.createJob(
                accountId,
                jobTypeId,
                auth.directoryTypeId(),
                DEFAULT_SYNC_DIRECTION_ID);

        String jobIdValue = Long.toString(jobId);
        JobMessage jobMessage = new JobMessage(jobIdValue, accountId, request.jobType().name());
        AfterCommitRunner.run(() -> messageQueuePort.publishJob(jobMessage));

        return Optional.of(new JobResponse(jobIdValue, "PENDING"));
    }

    public Optional<JobReportResponse> getJobReport(String accountId, String jobIdValue) {
        long jobId = Long.parseLong(jobIdValue);
        return jobRepository.findJobReportForAccount(jobId, accountId).map(this::toReportResponse);
    }

    public Optional<JobReportResponse> getLatestJobReport(String accountId) {
        return jobRepository.findLatestJobIdForAccount(accountId)
                .flatMap(jobId -> jobRepository.findJobReportForAccount(jobId, accountId))
                .map(this::toReportResponse);
    }

    public JobHistoryResponse listJobs(String accountId, Integer limit) {
        int effectiveLimit = limit != null && limit > 0 ? Math.min(limit, 100) : DEFAULT_JOB_HISTORY_LIMIT;
        List<JobSummaryResponse> jobs = jobRepository.listJobsForAccount(accountId, effectiveLimit).stream()
                .map(this::toSummaryResponse)
                .toList();
        return new JobHistoryResponse(jobs);
    }

    private JobReportResponse toReportResponse(JobReportData data) {
        return new JobReportResponse(
                Long.toString(data.jobId()),
                data.jobType(),
                data.syncDirection(),
                data.state(),
                data.startedAt(),
                data.completedAt(),
                data.successCount(),
                data.failedCount(),
                data.failures().stream()
                        .map(row -> new JobFailure(row.externalId(), row.operation(), row.comment()))
                        .toList());
    }

    private JobSummaryResponse toSummaryResponse(JobSummaryData data) {
        return new JobSummaryResponse(
                Long.toString(data.jobId()),
                data.jobType(),
                data.syncDirection(),
                data.state(),
                data.startedAt(),
                data.completedAt(),
                data.successCount(),
                data.failedCount());
    }
}
