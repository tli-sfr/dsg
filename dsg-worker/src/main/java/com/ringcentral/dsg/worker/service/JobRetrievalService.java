package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.model.JobContext;
import com.ringcentral.dsg.persistence.model.ProvisioningRuleRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobDetailRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import com.ringcentral.dsg.rules.ProvisioningRuleMatch;
import com.ringcentral.dsg.rules.ProvisioningRuleSelector;
import java.util.List;
import java.util.Optional;
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

    public JobRetrievalService(
            JobRepository jobRepository,
            JobDetailRepository jobDetailRepository,
            AccountDirectoryAuthRepository authRepository,
            ProvisioningRuleRepository provisioningRuleRepository,
            DirectoryPort directoryPort,
            MessageQueuePort messageQueuePort) {
        this.jobRepository = jobRepository;
        this.jobDetailRepository = jobDetailRepository;
        this.authRepository = authRepository;
        this.provisioningRuleRepository = provisioningRuleRepository;
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
        List<ProvisioningRuleMatch> rules = provisioningRuleRepository.listByAccountOrderByPriority(message.accountId())
                .stream()
                .map(this::toMatch)
                .toList();

        int detailCount = 0;
        for (DirectoryUser user : members) {
            Optional<ProvisioningRuleMatch> matchedRule = ProvisioningRuleSelector.selectFirstMatch(rules, user);
            if (matchedRule.isEmpty()) {
                log.debug("Skipping user {} — no provisioning rule matched", user.externalId());
                continue;
            }
            long ruleId = matchedRule.get().ruleId();
            long detailId = jobDetailRepository.insertPendingCreate(jobId, user.externalId(), ruleId);
            messageQueuePort.publishJobDetail(new JobDetailMessage(
                    Long.toString(detailId),
                    message.jobId(),
                    message.accountId(),
                    user.externalId(),
                    "CREATE",
                    Long.toString(ruleId),
                    user.email(),
                    user.attributes()));
            detailCount++;
        }

        jobRepository.updateJobState(jobId, "READY");
        log.info("Job {} prepared {} job details from {} directory users", jobId, detailCount, members.size());
    }

    private ProvisioningRuleMatch toMatch(ProvisioningRuleRecord record) {
        return new ProvisioningRuleMatch(
                record.id(), record.ruleName(), record.priority(), record.selectionExpressionJson());
    }
}
