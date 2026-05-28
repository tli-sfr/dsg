package com.ringcentral.dsg.worker.rules;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.persistence.model.RuleBasedAttributeMappingRecord;
import com.ringcentral.dsg.rules.DirectoryAttributePathResolver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RuleBasedMappingApplier {

    private RuleBasedMappingApplier() {}

    /**
     * Applies rule-based RC attribute assignments when directory values match mapping rows.
     * Matched values are stored under {@code rc.<attributeName>} keys for the provisioning stub.
     */
    public static DirectoryUser apply(DirectoryUser user, List<RuleBasedAttributeMappingRecord> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return user;
        }
        Map<String, String> enriched = new HashMap<>(user.attributes());
        for (RuleBasedAttributeMappingRecord mapping : mappings) {
            String actual = DirectoryAttributePathResolver.resolve(user, mapping.directoryAttributePath());
            if (mapping.directoryAttributeValue() != null
                    && mapping.directoryAttributeValue().equals(actual)) {
                enriched.put("rc." + mapping.rcAttributeName(), mapping.rcObjectId());
            }
        }
        return new DirectoryUser(user.externalId(), user.email(), Map.copyOf(enriched));
    }
}
