package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;
import com.ringcentral.dsg.persistence.repo.JobDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncWorkerService {

    private static final Logger log = LoggerFactory.getLogger(SyncWorkerService.class);

    private final JobDetailRepository jobDetailRepository;
    private final RcProvisioningPort rcProvisioningPort;
    private final JobConsolidatorService consolidatorService;

    public SyncWorkerService(
            JobDetailRepository jobDetailRepository,
            RcProvisioningPort rcProvisioningPort,
            JobConsolidatorService consolidatorService) {
        this.jobDetailRepository = jobDetailRepository;
        this.rcProvisioningPort = rcProvisioningPort;
        this.consolidatorService = consolidatorService;
    }

    @Transactional
    public void processJobDetailMessage(JobDetailMessage message) {
        long jobDetailId = Long.parseLong(message.jobDetailId());
        jobDetailRepository.updateState(jobDetailId, "IN_SYNC", null, null);

        DirectoryUser user = new DirectoryUser(message.externalId(), message.externalId() + "@example.com", java.util.Map.of());
        ProvisioningResult result = rcProvisioningPort.createExtension(message.accountId(), user);

        if (result.success()) {
            jobDetailRepository.updateState(jobDetailId, "SUCCEEDED", result.mailboxId(), result.message());
            log.info("Job detail {} succeeded mailboxId={}", jobDetailId, result.mailboxId());
        } else {
            jobDetailRepository.updateState(jobDetailId, "FAILED", null, result.message());
            log.warn("Job detail {} failed: {}", jobDetailId, result.message());
        }

        jobDetailRepository.findJobIdForDetail(jobDetailId)
                .ifPresent(consolidatorService::consolidateIfComplete);
    }
}
