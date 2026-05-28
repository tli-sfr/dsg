package com.ringcentral.dsg.worker.mapping;

import com.ringcentral.dsg.persistence.service.EffectiveDirectoryTypeResolver;
import com.ringcentral.dsg.mapping.AttributeMapping;
import com.ringcentral.dsg.persistence.repo.AttributeMappingRepository;
import com.ringcentral.dsg.persistence.repo.DefaultAttributeMappingRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountAttributeMappingResolver {

    private static final Logger log = LoggerFactory.getLogger(AccountAttributeMappingResolver.class);
    private static final int DIR_TO_RC_DIRECTION_ID = 1;

    private final EffectiveDirectoryTypeResolver effectiveDirectoryTypeResolver;
    private final AttributeMappingRepository attributeMappingRepository;
    private final DefaultAttributeMappingRepository defaultAttributeMappingRepository;

    public AccountAttributeMappingResolver(
            EffectiveDirectoryTypeResolver effectiveDirectoryTypeResolver,
            AttributeMappingRepository attributeMappingRepository,
            DefaultAttributeMappingRepository defaultAttributeMappingRepository) {
        this.effectiveDirectoryTypeResolver = effectiveDirectoryTypeResolver;
        this.attributeMappingRepository = attributeMappingRepository;
        this.defaultAttributeMappingRepository = defaultAttributeMappingRepository;
    }

    public List<AttributeMapping> listForAccount(String accountId) {
        int directoryTypeId = effectiveDirectoryTypeResolver.resolveDirectoryTypeId(accountId);
        String directoryTypeName = effectiveDirectoryTypeResolver.resolveDirectoryTypeName(accountId);
        boolean hasAccountMappings =
                attributeMappingRepository.countBasicMappings(accountId, directoryTypeId) > 0;
        var views = hasAccountMappings
                ? attributeMappingRepository.listAccountMappings(
                        accountId, DIR_TO_RC_DIRECTION_ID, directoryTypeId)
                : defaultAttributeMappingRepository.listDefaults(directoryTypeId, DIR_TO_RC_DIRECTION_ID);

        log.info(
                "[DSG sync:mappings] account={} directoryType={} source={} mappingCount={}",
                accountId,
                directoryTypeName,
                hasAccountMappings ? "account" : "default",
                views.size());

        return views.stream()
                .map(view -> new AttributeMapping(view.directoryAttributePath(), view.rcAttributeName()))
                .toList();
    }
}
