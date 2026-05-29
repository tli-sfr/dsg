package com.ringcentral.dsg.worker.sync;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.mapping.AttributeMapping;
import com.ringcentral.dsg.mapping.AttributeMappingApplier;
import com.ringcentral.dsg.mapping.DirectorySyncHashCalculator;
import com.ringcentral.dsg.persistence.model.DirectorySyncUserHashRecord;
import com.ringcentral.dsg.persistence.repo.DirectorySyncUserHashRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SyncOperationPlanner {

    private static final Logger log = LoggerFactory.getLogger(SyncOperationPlanner.class);

    private final DirectorySyncUserHashRepository userHashRepository;

    public SyncOperationPlanner(DirectorySyncUserHashRepository userHashRepository) {
        this.userHashRepository = userHashRepository;
    }

    public SyncOperationPlan plan(
            String accountId,
            int directoryTypeId,
            DirectoryUser directoryUser,
            List<AttributeMapping> mappings) {
        DirectoryUser mapped = AttributeMappingApplier.apply(directoryUser, mappings);
        String hash = DirectorySyncHashCalculator.compute(mapped, mappings);
        Optional<DirectorySyncUserHashRecord> existing =
                userHashRepository.find(accountId, directoryTypeId, directoryUser.externalId());

        if (existing.isEmpty()) {
            log.info(
                    "[DSG sync:plan] account={} externalId={} operation=CREATE (no prior hash)",
                    accountId,
                    directoryUser.externalId());
            return new SyncOperationPlan(SyncOperation.CREATE, mapped, null, hash);
        }

        DirectorySyncUserHashRecord prior = existing.get();
        if (hash.equals(prior.externalUserHash())) {
            log.info(
                    "[DSG sync:plan] account={} externalId={} operation=UNCHANGED mailboxId={}",
                    accountId,
                    directoryUser.externalId(),
                    prior.mailboxId());
            return new SyncOperationPlan(SyncOperation.UNCHANGED, mapped, prior.mailboxId(), hash);
        }

        log.info(
                "[DSG sync:plan] account={} externalId={} operation=UPDATE mailboxId={} hashChanged=true",
                accountId,
                directoryUser.externalId(),
                prior.mailboxId());
        return new SyncOperationPlan(SyncOperation.UPDATE, mapped, prior.mailboxId(), hash);
    }
}
