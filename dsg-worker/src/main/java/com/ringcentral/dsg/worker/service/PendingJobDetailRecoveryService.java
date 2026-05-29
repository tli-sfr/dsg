package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.model.PendingJobDetailRow;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobDetailRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Republishes job-detail queue messages when DB rows are still PENDING on a READY job
 * (e.g. another process acked the SQS message without provisioning).
 */
@Service
public class PendingJobDetailRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PendingJobDetailRecoveryService.class);

    private final JobDetailRepository jobDetailRepository;
    private final AccountDirectoryAuthRepository authRepository;
    private final DirectoryPort directoryPort;
    private final MessageQueuePort messageQueuePort;

    public PendingJobDetailRecoveryService(
            JobDetailRepository jobDetailRepository,
            AccountDirectoryAuthRepository authRepository,
            DirectoryPort directoryPort,
            MessageQueuePort messageQueuePort) {
        this.jobDetailRepository = jobDetailRepository;
        this.authRepository = authRepository;
        this.directoryPort = directoryPort;
        this.messageQueuePort = messageQueuePort;
    }

    public int republishOrphanedPendingDetails() {
        List<PendingJobDetailRow> pending = jobDetailRepository.listPendingDetailsForReadyJobs();
        if (pending.isEmpty()) {
            return 0;
        }
        log.warn(
                "[DSG sync:recovery] found {} PENDING job detail(s) on READY jobs — republishing to queue",
                pending.size());
        int republished = 0;
        for (PendingJobDetailRow row : pending) {
            if (republish(row)) {
                republished++;
            }
        }
        if (republished > 0) {
            log.info("[DSG sync:recovery] republished {} job detail message(s)", republished);
        }
        return republished;
    }

    private boolean republish(PendingJobDetailRow row) {
        Optional<DirectoryUser> user = findDirectoryUser(row.accountId(), row.externalId());
        if (user.isEmpty()) {
            log.warn(
                    "[DSG sync:recovery] jobDetailId={} jobId={} externalId={} not found in directory group — skip",
                    row.jobDetailId(),
                    row.jobId(),
                    row.externalId());
            return false;
        }
        DirectoryUser directoryUser = user.get();
        String operation = row.operation() != null ? row.operation() : "CREATE";
        messageQueuePort.publishJobDetail(new JobDetailMessage(
                Long.toString(row.jobDetailId()),
                Long.toString(row.jobId()),
                row.accountId(),
                row.externalId(),
                operation,
                row.ruleId() != null ? Long.toString(row.ruleId()) : null,
                directoryUser.email(),
                directoryUser.attributes(),
                row.mailboxId()));
        log.info(
                "[DSG sync:recovery] republished jobDetailId={} jobId={} externalId={} email={}",
                row.jobDetailId(),
                row.jobId(),
                row.externalId(),
                directoryUser.email());
        return true;
    }

    private Optional<DirectoryUser> findDirectoryUser(String accountId, String externalId) {
        AccountDirectoryAuthRecord auth = authRepository.findByAccountId(accountId).orElse(null);
        if (auth == null) {
            log.warn("[DSG sync:recovery] directory auth missing for account {}", accountId);
            return Optional.empty();
        }
        String groupId = auth.directoryGroupId() != null ? auth.directoryGroupId() : "default-group";
        return directoryPort.listGroupMembers(accountId, groupId).stream()
                .filter(user -> externalId.equals(user.externalId()))
                .findFirst();
    }
}
