package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.model.JobContext;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobDetailRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(JobRetrievalService.class);

    private final JobRepository jobRepository;
    private final JobDetailRepository jobDetailRepository;
    private final AccountDirectoryAuthRepository authRepository;
    private final DirectoryPort directoryPort;
    private final MessageQueuePort messageQueuePort;

    public JobRetrievalService(
            JobRepository jobRepository,
            JobDetailRepository jobDetailRepository,
            AccountDirectoryAuthRepository authRepository,
            DirectoryPort directoryPort,
            MessageQueuePort messageQueuePort) {
        this.jobRepository = jobRepository;
        this.jobDetailRepository = jobDetailRepository;
        this.authRepository = authRepository;
        this.directoryPort = directoryPort;
        this.messageQueuePort = messageQueuePort;
    }

    @Transactional
    public void processJobMessage(JobMessage message) {
        long jobId = Long.parseLong(message.jobId());
        JobContext job = jobRepository.findJobContext(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

        if (!"PENDING".equals(job.jobState())) {
            log.warn("Skipping job {} in state {}", jobId, job.jobState());
            return;
        }

        jobRepository.updateJobState(jobId, "IN_PREP");

        AccountDirectoryAuthRecord auth = authRepository.findByAccountId(message.accountId())
                .orElseThrow(() -> new IllegalStateException("Directory auth missing for account " + message.accountId()));

        String groupId = auth.directoryGroupId() != null ? auth.directoryGroupId() : "default-group";
        var members = directoryPort.listGroupMembers(message.accountId(), groupId);

        for (DirectoryUser user : members) {
            long detailId = jobDetailRepository.insertPendingCreate(jobId, user.externalId());
            messageQueuePort.publishJobDetail(new JobDetailMessage(
                    Long.toString(detailId),
                    message.jobId(),
                    message.accountId(),
                    user.externalId(),
                    "CREATE"));
        }

        jobRepository.updateJobState(jobId, "READY");
        log.info("Job {} prepared {} job details", jobId, members.size());
    }
}
