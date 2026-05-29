package com.ringcentral.dsg.mapping;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.rules.DirectoryAttributePathResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps directory user fields to RC extension attributes using account or default attribute mappings.
 */
public final class AttributeMappingApplier {

    private AttributeMappingApplier() {}

    public static DirectoryUser apply(DirectoryUser user, List<AttributeMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return user;
        }
        Map<String, String> enriched = new LinkedHashMap<>(user.attributes());
        for (AttributeMapping mapping : mappings) {
            String value = DirectoryAttributePathResolver.resolve(user, mapping.directoryAttributePath());
            if (value != null && !value.isBlank()) {
                enriched.put(mapping.rcAttributeName(), value);
            }
        }
        String email = enriched.getOrDefault("email", user.email());
        return new DirectoryUser(user.externalId(), email, Map.copyOf(enriched));
    }
}
