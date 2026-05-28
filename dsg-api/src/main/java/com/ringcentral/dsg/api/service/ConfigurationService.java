package com.ringcentral.dsg.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeCatalogItem;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingConfigResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingItem;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRow;
import com.ringcentral.dsg.api.model.AdminApiModels.DeprovisioningResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleListResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleSummary;
import com.ringcentral.dsg.api.model.AdminApiModels.SchedulerRequest;
import com.ringcentral.dsg.persistence.repo.DeprovisioningRuleRepository;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.model.AttributeCatalogEntry;
import com.ringcentral.dsg.persistence.model.AttributeMappingView;
import com.ringcentral.dsg.persistence.repo.AttributeCatalogRepository;
import com.ringcentral.dsg.persistence.repo.AttributeMappingRepository;
import com.ringcentral.dsg.persistence.repo.AttributeMetadataRepository;
import com.ringcentral.dsg.persistence.repo.DefaultAttributeMappingRepository;
import com.ringcentral.dsg.persistence.repo.DirectorySyncTimeRepository;
import com.ringcentral.dsg.persistence.repo.LookupRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ConfigurationService {

    private final AccountDirectoryAuthRepository authRepository;
    private final DirectorySyncTimeRepository syncTimeRepository;
    private final AttributeMappingRepository attributeMappingRepository;
    private final DefaultAttributeMappingRepository defaultAttributeMappingRepository;
    private final AttributeCatalogRepository attributeCatalogRepository;
    private final AttributeMetadataRepository attributeMetadataRepository;
    private final ProvisioningRuleRepository provisioningRuleRepository;
    private final DeprovisioningRuleRepository deprovisioningRuleRepository;
    private final LookupRepository lookupRepository;
    private final ObjectMapper objectMapper;

    public ConfigurationService(
            AccountDirectoryAuthRepository authRepository,
            DirectorySyncTimeRepository syncTimeRepository,
            AttributeMappingRepository attributeMappingRepository,
            DefaultAttributeMappingRepository defaultAttributeMappingRepository,
            AttributeCatalogRepository attributeCatalogRepository,
            AttributeMetadataRepository attributeMetadataRepository,
            ProvisioningRuleRepository provisioningRuleRepository,
            DeprovisioningRuleRepository deprovisioningRuleRepository,
            LookupRepository lookupRepository,
            ObjectMapper objectMapper) {
        this.authRepository = authRepository;
        this.syncTimeRepository = syncTimeRepository;
        this.attributeMappingRepository = attributeMappingRepository;
        this.defaultAttributeMappingRepository = defaultAttributeMappingRepository;
        this.attributeCatalogRepository = attributeCatalogRepository;
        this.attributeMetadataRepository = attributeMetadataRepository;
        this.provisioningRuleRepository = provisioningRuleRepository;
        this.deprovisioningRuleRepository = deprovisioningRuleRepository;
        this.lookupRepository = lookupRepository;
        this.objectMapper = objectMapper;
    }

    public ProvisioningRuleListResponse listRules(String accountId) {
        List<ProvisioningRuleSummary> rules = provisioningRuleRepository.listByAccountOrderByPriority(accountId).stream()
                .map(record -> new ProvisioningRuleSummary(
                        Long.toString(record.id()),
                        record.ruleName(),
                        record.priority(),
                        parseSelectionExpression(record.selectionExpressionJson())))
                .toList();
        return new ProvisioningRuleListResponse(rules);
    }

    public DeprovisioningResponse getDeprovisioning(String accountId) {
        return deprovisioningRuleRepository
                .findByAccountId(accountId)
                .map(record -> new DeprovisioningResponse(record.deprovisioningType()))
                .orElse(new DeprovisioningResponse("FULL_DELETE"));
    }

    @Transactional
    public void saveDeprovisioning(String accountId, String deprovisioningType) {
        int typeId = lookupRepository
                .findDeprovisioningTypeId(deprovisioningType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown deprovisioning type: " + deprovisioningType));
        deprovisioningRuleRepository.upsert(accountId, typeId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSelectionExpression(String json) {
        if (json == null || json.isBlank()) {
            return Map.of("match", "ALL");
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            return Map.of("match", "ALL", "raw", json);
        }
    }

    @Transactional
    public void configureScheduler(String accountId, SchedulerRequest request) {
        AccountDirectoryAuthRecord auth = requireDirectoryAuth(accountId);
        int jobTypeId = Boolean.TRUE.equals(request.incrementalEnabled())
                ? lookupRepository.findJobTypeId("INCREMENTAL").orElseThrow()
                : lookupRepository.findJobTypeId("FULL").orElseThrow();
        int directionId = lookupRepository.findSyncDirectionId(request.syncDirection()).orElse(1);
        syncTimeRepository.upsert(
                accountId,
                auth.directoryTypeId(),
                jobTypeId,
                directionId,
                lookupRepository.defaultJobFrequencyId(),
                request.cronExpression());
    }

    public AttributeMappingConfigResponse getAttributeMappingConfig(String accountId) {
        AccountDirectoryAuthRecord auth = requireDirectoryAuth(accountId);
        int directionId = 1;
        boolean configured = attributeMappingRepository.countBasicMappings(accountId) > 0;
        List<AttributeMappingView> views = configured
                ? attributeMappingRepository.listAccountMappings(accountId, directionId)
                : defaultAttributeMappingRepository.listDefaults(auth.directoryTypeId(), directionId);

        List<AttributeMappingItem> mappings = views.stream()
                .map(v -> new AttributeMappingItem(
                        v.directoryAttributePath(),
                        v.directoryAttributeName(),
                        v.rcAttributeName(),
                        v.displaySequence()))
                .toList();

        return new AttributeMappingConfigResponse(
                "DIR_TO_RC",
                configured,
                mappings,
                toCatalogItems(attributeCatalogRepository.listDirectoryAttributes(auth.directoryTypeId())),
                toCatalogItems(attributeCatalogRepository.listRcAttributes()));
    }

    @Transactional
    public void saveAttributeMapping(String accountId, AttributeMappingRequest request) {
        AccountDirectoryAuthRecord auth = requireDirectoryAuth(accountId);
        attributeMappingRepository.replaceForAccount(accountId);

        for (AttributeMappingRow row : request.basicMappings()) {
            int directionId = lookupRepository.findSyncDirectionId(row.syncDirection()).orElse(1);
            int rcAttributeId = attributeMetadataRepository.findOrCreateRcAttributeId(row.rcAttribute());
            int directoryAttributeId = attributeMetadataRepository.findOrCreateDirectoryAttributeId(
                    auth.directoryTypeId(),
                    row.directoryAttribute());
            attributeMappingRepository.insertBasicMapping(
                    accountId,
                    rcAttributeId,
                    directionId,
                    directoryAttributeId,
                    row.directoryAttribute());
        }

        for (AttributeMappingRow row : request.customMappings()) {
            attributeMappingRepository.insertCustomMapping(
                    accountId,
                    row.directoryAttribute(),
                    row.rcAttribute());
        }
    }

    @Transactional
    public void saveRule(String accountId, ProvisioningRuleRequest request) {
        requireDirectoryAuth(accountId);
        String selectionJson = toJson(request.selectionExpression());
        long ruleId = provisioningRuleRepository.upsertRule(
                accountId,
                request.ruleName(),
                request.priority(),
                selectionJson);

        if (request.licenseAssignments() != null) {
            for (Map<String, Object> assignment : request.licenseAssignments()) {
                String licenseType = stringValue(assignment.get("licenseType"));
                String licenseId = stringValue(assignment.get("licenseId"));
                int licenseTypeId = lookupRepository.findLicenseTypeId(licenseType)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown license type: " + licenseType));
                provisioningRuleRepository.insertLicenseAssignment(ruleId, licenseTypeId, licenseId);
            }
        }

        if (request.areaCodeAssignment() != null) {
            String areaCodeRuleType = stringValue(request.areaCodeAssignment().get("areaCodeRuleType"));
            int typeId = lookupRepository.findAreaCodeRuleTypeId(areaCodeRuleType)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown area code rule type: " + areaCodeRuleType));
            provisioningRuleRepository.insertAreaCodeAssignment(ruleId, typeId, toJson(request.areaCodeAssignment().get("areaCodeList")));
        }

        if (request.templateAssignments() != null) {
            for (Map<String, Object> assignment : request.templateAssignments()) {
                String templateType = stringValue(assignment.get("templateType"));
                String templateId = stringValue(assignment.get("templateId"));
                int templateTypeId = lookupRepository.findTemplateTypeId(templateType)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown template type: " + templateType));
                provisioningRuleRepository.insertTemplateAssignment(ruleId, templateTypeId, templateId);
            }
        }

        if (request.deviceAssignments() != null) {
            for (Map<String, Object> assignment : request.deviceAssignments()) {
                String deviceType = stringValue(assignment.get("deviceType"));
                String deviceId = stringValue(assignment.get("deviceId"));
                int deviceTypeId = lookupRepository.findDeviceTypeId(deviceType)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown device type: " + deviceType));
                provisioningRuleRepository.insertDeviceAssignment(ruleId, deviceTypeId, deviceId);
            }
        }

        if (request.ruleBasedAttributeMappings() != null) {
            for (Map<String, Object> mapping : request.ruleBasedAttributeMappings()) {
                String rcAttributeName = stringValue(mapping.get("rcRuleBasedAttribute"));
                if (rcAttributeName == null) {
                    rcAttributeName = stringValue(mapping.get("attributeName"));
                }
                final String resolvedRcAttribute = rcAttributeName;
                int rcAttributeId = lookupRepository.findRuleBasedAttributeId(resolvedRcAttribute)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown rule-based attribute: " + resolvedRcAttribute));
                provisioningRuleRepository.insertRuleBasedAttributeMapping(
                        accountId,
                        ruleId,
                        stringValue(mapping.get("directoryAttributePath")),
                        stringValue(mapping.get("directoryAttributeValue")),
                        rcAttributeId,
                        stringValue(mapping.get("rcObjectId")));
            }
        }
    }

    private AccountDirectoryAuthRecord requireDirectoryAuth(String accountId) {
        return authRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Directory is not configured for account: " + accountId));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }
}
