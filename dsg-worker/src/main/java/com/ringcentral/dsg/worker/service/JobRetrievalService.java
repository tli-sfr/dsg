package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.mapping.DirectorySyncTrace;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.model.DirectorySyncUserHashRecord;
import com.ringcentral.dsg.persistence.model.JobContext;
import com.ringcentral.dsg.persistence.model.ProvisioningRuleRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.DirectorySyncUserHashRepository;
import com.ringcentral.dsg.persistence.repo.JobDetailRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import com.ringcentral.dsg.persistence.tx.AfterCommitRunner;
import com.ringcentral.dsg.rules.ProvisioningRuleMatch;
import com.ringcentral.dsg.rules.ProvisioningRuleSelector;
import com.ringcentral.dsg.worker.mapping.AccountAttributeMappingResolver;
import com.ringcentral.dsg.worker.sync.SyncOperation;
import com.ringcentral.dsg.worker.sync.SyncOperationPlan;
import com.ringcentral.dsg.worker.sync.SyncOperationPlanner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final ProvisioningRuleRepository provisioningRuleRepository;
    private final DirectoryPort directoryPort;
    private final MessageQueuePort messageQueuePort;
    private final AccountAttributeMappingResolver attributeMappingResolver;
    private final SyncOperationPlanner syncOperationPlanner;
    private final JobConsolidatorService consolidatorService;
    private final DirectorySyncUserHashRepository userHashRepository;

    public JobRetrievalService(
            JobRepository jobRepository,
            JobDetailRepository jobDetailRepository,
            AccountDirectoryAuthRepository authRepository,
            ProvisioningRuleRepository provisioningRuleRepository,
            DirectoryPort directoryPort,
            MessageQueuePort messageQueuePort,
            AccountAttributeMappingResolver attributeMappingResolver,
            SyncOperationPlanner syncOperationPlanner,
            JobConsolidatorService consolidatorService,
            DirectorySyncUserHashRepository userHashRepository) {
        this.jobRepository = jobRepository;
        this.jobDetailRepository = jobDetailRepository;
        this.authRepository = authRepository;
        this.provisioningRuleRepository = provisioningRuleRepository;
        this.directoryPort = directoryPort;
        this.messageQueuePort = messageQueuePort;
        this.attributeMappingResolver = attributeMappingResolver;
        this.syncOperationPlanner = syncOperationPlanner;
        this.consolidatorService = consolidatorService;
        this.userHashRepository = userHashRepository;
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
        List<DirectoryUser> members = directoryPort.listGroupMembers(message.accountId(), groupId);
        log.info("[DSG sync:retrieve] account={} groupId={} directoryUsers={}", message.accountId(), groupId, members.size());
        for (DirectoryUser member : members) {
            DirectorySyncTrace.logDirectoryUser("retrieve", message.accountId(), member);
        }
        List<ProvisioningRuleMatch> rules = provisioningRuleRepository.listByAccountOrderByPriority(message.accountId())
                .stream()
                .map(this::toMatch)
                .toList();

        var mappings = attributeMappingResolver.listForAccount(message.accountId());
        int detailCount = 0;
        int unchangedCount = 0;
        List<JobDetailMessage> detailMessages = new ArrayList<>();
        for (DirectoryUser user : members) {
            Optional<ProvisioningRuleMatch> matchedRule = ProvisioningRuleSelector.selectFirstMatch(rules, user);
            if (matchedRule.isEmpty()) {
                log.info("Skipping user {} — no provisioning rule matched", user.externalId());
                continue;
            }
            long ruleId = matchedRule.get().ruleId();
            SyncOperationPlan plan = syncOperationPlanner.plan(
                    message.accountId(), auth.directoryTypeId(), user, mappings);

            if (plan.operation() == SyncOperation.UNCHANGED) {
                unchangedCount++;
                continue;
            }

            long detailId;
            String operation;
            if (plan.operation() == SyncOperation.CREATE) {
                detailId = jobDetailRepository.insertPendingCreate(jobId, user.externalId(), ruleId);
                operation = "CREATE";
            } else {
                detailId = jobDetailRepository.insertPendingUpdate(jobId, user.externalId(), plan.mailboxId());
                operation = "UPDATE";
            }

            JobDetailMessage detailMessage = new JobDetailMessage(
                    Long.toString(detailId),
                    message.jobId(),
                    message.accountId(),
                    user.externalId(),
                    operation,
                    plan.operation() == SyncOperation.CREATE ? Long.toString(ruleId) : null,
                    plan.mappedUser().email(),
                    user.attributes(),
                    plan.mailboxId());
            detailMessages.add(detailMessage);
            log.info(
                    "Job {} enqueued job detail {} operation={} externalId={} email={}",
                    jobId,
                    detailId,
                    operation,
                    user.externalId(),
                    plan.mappedUser().email());
            detailCount++;
        }

        if ("FULL".equals(job.jobType())) {
            detailCount += enqueueRemovedFromDirectoryUsers(
                    jobId, message, auth.directoryTypeId(), members, detailMessages);
        }

        jobRepository.updateJobState(jobId, "READY");
        log.info(
                "Job {} prepared {} job details ({} unchanged) from {} directory users",
                jobId,
                detailCount,
                unchangedCount,
                members.size());

        if (!detailMessages.isEmpty()) {
            AfterCommitRunner.run(() -> {
                for (JobDetailMessage detailMessage : detailMessages) {
                    messageQueuePort.publishJobDetail(detailMessage);
                }
            });
        } else {
            consolidatorService.consolidateIfComplete(jobId);
        }
    }

    private int enqueueRemovedFromDirectoryUsers(
            long jobId,
            JobMessage message,
            int directoryTypeId,
            List<DirectoryUser> pulledUsers,
            List<JobDetailMessage> detailMessages) {
        Set<String> pulledExternalIds = new HashSet<>();
        for (DirectoryUser user : pulledUsers) {
            pulledExternalIds.add(user.externalId());
        }

        List<DirectorySyncUserHashRecord> syncedUsers = userHashRepository.listByAccount(message.accountId(), directoryTypeId);
        List<DirectorySyncUserHashRecord> removedUsers = syncedUsers.stream()
                .filter(hash -> !pulledExternalIds.contains(hash.externalId()))
                .toList();

        log.info(
                "[DSG sync:removed-from-directory] account={} hashRows={} pulledUsers={} removedFromDirectory={}",
                message.accountId(),
                syncedUsers.size(),
                pulledExternalIds.size(),
                removedUsers.size());

        int deleteCount = 0;
        for (DirectorySyncUserHashRecord removed : removedUsers) {
            log.info(
                    "[DSG sync:removed-from-directory] account={} externalId={} mailboxId={} externalUserHash={}",
                    message.accountId(),
                    removed.externalId(),
                    removed.mailboxId(),
                    removed.externalUserHash());

            if (removed.mailboxId() == null || removed.mailboxId().isBlank()) {
                log.warn(
                        "[DSG sync:removed-from-directory] account={} externalId={} skipped DELETE — no mailbox_id in hash",
                        message.accountId(),
                        removed.externalId());
                continue;
            }

            long detailId = jobDetailRepository.insertPendingDelete(
                    jobId, removed.externalId(), removed.mailboxId());
            JobDetailMessage detailMessage = new JobDetailMessage(
                    Long.toString(detailId),
                    message.jobId(),
                    message.accountId(),
                    removed.externalId(),
                    "DELETE",
                    null,
                    null,
                    Map.of(),
                    removed.mailboxId());
            detailMessages.add(detailMessage);
            log.info(
                    "Job {} enqueued job detail {} operation=DELETE externalId={} mailboxId={}",
                    jobId,
                    detailId,
                    removed.externalId(),
                    removed.mailboxId());
            deleteCount++;
        }
        return deleteCount;
    }

    private ProvisioningRuleMatch toMatch(ProvisioningRuleRecord record) {
        return new ProvisioningRuleMatch(
                record.id(), record.ruleName(), record.priority(), record.selectionExpressionJson());
    }
}
