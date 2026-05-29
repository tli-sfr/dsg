package com.ringcentral.dsg.mapping;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step-by-step console logging for directory → attribute mapping → RC provisioning.
 */
public final class DirectorySyncTrace {

    private static final Logger log = LoggerFactory.getLogger(DirectorySyncTrace.class);

    private DirectorySyncTrace() {}

    public static void logDirectoryUser(String step, String accountId, DirectoryUser user) {
        log.info(
                "[DSG sync:{}] account={} externalId={} email={} directoryAttributes={}",
                step,
                accountId,
                user.externalId(),
                user.email(),
                user.attributes());
    }

    public static void logMappingConfig(String accountId, List<AttributeMapping> mappings) {
        String lines = mappings.stream()
                .map(m -> m.directoryAttributePath() + " -> " + m.rcAttributeName())
                .collect(Collectors.joining(", "));
        log.info("[DSG sync:mappings] account={} attributeMappings=[{}]", accountId, lines);
    }

    public static void logMappingResolution(
            String accountId, DirectoryUser source, List<AttributeMapping> mappings, DirectoryUser mapped) {
        logMappingConfig(accountId, mappings);
        for (AttributeMapping mapping : mappings) {
            String directoryValue = com.ringcentral.dsg.rules.DirectoryAttributePathResolver.resolve(
                    source, mapping.directoryAttributePath());
            String rcValue = mapped.attributes().get(mapping.rcAttributeName());
            log.info(
                    "[DSG sync:map] account={} {} -> {} | directoryValue={} | rcValue={}",
                    accountId,
                    mapping.directoryAttributePath(),
                    mapping.rcAttributeName(),
                    directoryValue,
                    rcValue);
        }
        log.info(
                "[DSG sync:mapped] account={} externalId={} rcEmail={} rcAttributes={}",
                accountId,
                mapped.externalId(),
                mapped.email(),
                rcProvisioningFields(mapped.attributes()));
    }

    public static void logRcProvisionPayload(String accountId, String operation, Map<String, String> rcFields) {
        log.info(
                "[DSG sync:rc-api] account={} operation={} payload={}",
                accountId,
                operation,
                rcFields);
    }

    public static void logRcProvisionResponse(
            String accountId, String operation, String rawBody, String extensionId, Boolean successful) {
        log.info(
                "[DSG sync:rc-api] account={} operation={} response={}",
                accountId,
                operation,
                rawBody);
        log.info(
                "[DSG sync:rc-api] account={} operation={} parsed successful={} extensionId={}",
                accountId,
                operation,
                successful,
                extensionId);
    }

    private static Map<String, String> rcProvisioningFields(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("rc.") && !e.getKey().startsWith("profile."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }
}
