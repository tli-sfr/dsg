package com.ringcentral.dsg.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminApiModels {

    private AdminApiModels() {
    }

    public record DirectoryConfigRequest(String directoryType, String etmSubscriberId) {
    }

    public record DirectoryUpdateRequest(String directoryGroupId, Boolean active) {
    }

    public record DirectoryResponse(String directoryType, String directoryGroupId, boolean active, boolean connected) {
    }

    public record DirectoryOAuthRequest(String directoryType, String authFlow, String clientId, String clientSecret) {
    }

    public record DirectoryOAuthResponse(String directoryType, String clientId, Instant tokenExpiresAt) {
    }

    public record SchedulerRequest(Boolean incrementalEnabled, String cronExpression, String syncDirection) {
    }

    public record AttributeMappingRow(String syncDirection, String directoryAttribute, String rcAttribute) {
    }

    public record AttributeMappingRequest(List<AttributeMappingRow> basicMappings, List<AttributeMappingRow> customMappings) {
    }

    public record ProvisioningRuleRequest(
            String ruleName,
            Integer priority,
            Map<String, Object> selectionExpression,
            List<Map<String, Object>> licenseAssignments,
            List<Map<String, Object>> ruleBasedAttributeMappings,
            Map<String, Object> areaCodeAssignment,
            List<Map<String, Object>> deviceAssignments,
            List<Map<String, Object>> templateAssignments) {
    }

    public record CreateJobRequest(String jobType, List<String> externalUserIds) {
    }

    public record JobResponse(String jobId, String state) {
    }

    public record JobFailure(String externalId, String comment) {
    }

    public record JobReportResponse(String jobId, int successCount, int failedCount, List<JobFailure> failures) {
    }

    public record ErrorResponse(String code, String message) {
    }
}
