package com.ringcentral.dsg.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.AttributeMappingRow;
import com.ringcentral.dsg.api.model.AdminApiModels.ProvisioningRuleRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.SchedulerRequest;
import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.AttributeMappingRepository;
import com.ringcentral.dsg.persistence.repo.AttributeMetadataRepository;
import com.ringcentral.dsg.persistence.repo.DirectorySyncTimeRepository;
import com.ringcentral.dsg.persistence.repo.LookupRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ConfigurationService {

    private final AccountDirectoryAuthRepository authRepository;
    private final DirectorySyncTimeRepository syncTimeRepository;
    private final AttributeMappingRepository attributeMappingRepository;
    private final AttributeMetadataRepository attributeMetadataRepository;
    private final ProvisioningRuleRepository provisioningRuleRepository;
    private final LookupRepository lookupRepository;
    private final ObjectMapper objectMapper;

    public ConfigurationService(
            AccountDirectoryAuthRepository authRepository,
            DirectorySyncTimeRepository syncTimeRepository,
            AttributeMappingRepository attributeMappingRepository,
            AttributeMetadataRepository attributeMetadataRepository,
            ProvisioningRuleRepository provisioningRuleRepository,
            LookupRepository lookupRepository,
            ObjectMapper objectMapper) {
        this.authRepository = authRepository;
        this.syncTimeRepository = syncTimeRepository;
        this.attributeMappingRepository = attributeMappingRepository;
        this.attributeMetadataRepository = attributeMetadataRepository;
        this.provisioningRuleRepository = provisioningRuleRepository;
        this.lookupRepository = lookupRepository;
        this.objectMapper = objectMapper;
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
