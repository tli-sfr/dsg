package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.mapping.AttributeMapping;
import com.ringcentral.dsg.mapping.AttributeMappingApplier;
import com.ringcentral.dsg.mapping.DirectorySyncHashCalculator;
import com.ringcentral.dsg.mapping.DirectorySyncTrace;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.DirectorySyncUserHashRepository;
import com.ringcentral.dsg.persistence.repo.JobDetailRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import com.ringcentral.dsg.worker.mapping.AccountAttributeMappingResolver;
import com.ringcentral.dsg.worker.rules.RuleBasedMappingApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncWorkerService {

    private static final Logger log = LoggerFactory.getLogger(SyncWorkerService.class);

    private final JobDetailRepository jobDetailRepository;
    private final ProvisioningRuleRepository provisioningRuleRepository;
    private final AccountDirectoryAuthRepository authRepository;
    private final AccountAttributeMappingResolver attributeMappingResolver;
    private final DirectorySyncUserHashRepository userHashRepository;
    private final RcProvisioningPort rcProvisioningPort;
    private final JobConsolidatorService consolidatorService;

    public SyncWorkerService(
            JobDetailRepository jobDetailRepository,
            ProvisioningRuleRepository provisioningRuleRepository,
            AccountDirectoryAuthRepository authRepository,
            AccountAttributeMappingResolver attributeMappingResolver,
            DirectorySyncUserHashRepository userHashRepository,
            RcProvisioningPort rcProvisioningPort,
            JobConsolidatorService consolidatorService) {
        this.jobDetailRepository = jobDetailRepository;
        this.provisioningRuleRepository = provisioningRuleRepository;
        this.authRepository = authRepository;
        this.attributeMappingResolver = attributeMappingResolver;
        this.userHashRepository = userHashRepository;
        this.rcProvisioningPort = rcProvisioningPort;
        this.consolidatorService = consolidatorService;
    }

    @Transactional
    public void processJobDetailMessage(JobDetailMessage message) {
        long jobDetailId = Long.parseLong(message.jobDetailId());
        jobDetailRepository.updateState(jobDetailId, "IN_SYNC", null, null);

        DirectoryUser user = new DirectoryUser(
                message.externalId(),
                message.email(),
                message.attributes() != null ? message.attributes() : java.util.Map.of());
        DirectorySyncTrace.logDirectoryUser("worker-input", message.accountId(), user);

        AccountDirectoryAuthRecord auth = authRepository.findByAccountId(message.accountId())
                .orElseThrow(() -> new IllegalStateException(
                        "Directory auth missing for account " + message.accountId()));
        java.util.List<AttributeMapping> mappings = attributeMappingResolver.listForAccount(message.accountId());
        DirectoryUser mapped = AttributeMappingApplier.apply(user, mappings);
        DirectorySyncTrace.logMappingResolution(message.accountId(), user, mappings, mapped);

        if (mapped.email() == null || mapped.email().isBlank()) {
            ProvisioningResult missingEmail = new ProvisioningResult(
                    null,
                    false,
                    "Required RC attribute 'email' could not be resolved from directory attribute mappings");
            jobDetailRepository.updateState(jobDetailId, "FAILED", null, missingEmail.message());
            jobDetailRepository.findJobIdForDetail(jobDetailId)
                    .ifPresent(consolidatorService::consolidateIfComplete);
            return;
        }

        DirectoryUser provisionUser = mapped;
        if ("CREATE".equals(message.operation()) && message.ruleId() != null) {
            long ruleId = Long.parseLong(message.ruleId());
            provisionUser = RuleBasedMappingApplier.apply(
                    mapped, provisioningRuleRepository.listRuleBasedMappings(ruleId));
        }

        ProvisioningResult result = switch (message.operation()) {
            case "CREATE" -> provisionCreate(message, provisionUser);
            case "UPDATE" -> updateExisting(message, mapped);
            default -> {
                log.warn("Skipping job detail {} — unsupported operation {}", jobDetailId, message.operation());
                yield new ProvisioningResult(null, false, "Unsupported operation: " + message.operation());
            }
        };

        if (result.success()) {
            String externalHash = DirectorySyncHashCalculator.compute(mapped, mappings);
            userHashRepository.upsertAfterProvision(
                    message.accountId(),
                    auth.directoryTypeId(),
                    message.externalId(),
                    externalHash,
                    result.mailboxId());
            log.info(
                    "[DSG sync:hash] account={} externalId={} mailboxId={} hashStored=true",
                    message.accountId(),
                    message.externalId(),
                    result.mailboxId());
            jobDetailRepository.updateState(jobDetailId, "SUCCEEDED", result.mailboxId(), result.message());
            log.info("Job detail {} succeeded mailboxId={}", jobDetailId, result.mailboxId());
        } else {
            jobDetailRepository.updateState(jobDetailId, "FAILED", null, result.message());
            log.warn("Job detail {} failed: {}", jobDetailId, result.message());
        }

        jobDetailRepository.findJobIdForDetail(jobDetailId)
                .ifPresent(consolidatorService::consolidateIfComplete);
    }

    private ProvisioningResult provisionCreate(JobDetailMessage message, DirectoryUser user) {
        String primaryLicenseId = null;
        if (message.ruleId() != null) {
            primaryLicenseId = provisioningRuleRepository.findPrimaryLicenseId(Long.parseLong(message.ruleId()));
        }
        return rcProvisioningPort.provisionUser(message.accountId(), user, primaryLicenseId);
    }

    private ProvisioningResult updateExisting(JobDetailMessage message, DirectoryUser user) {
        String rcExtensionId = message.mailboxId();
        if (rcExtensionId == null || rcExtensionId.isBlank()) {
            return new ProvisioningResult(
                    null,
                    false,
                    "UPDATE requires mailbox_id from directory_sync_user_hash; re-run full sync to recreate");
        }
        return rcProvisioningPort.updateExtension(message.accountId(), rcExtensionId, user);
    }
}
