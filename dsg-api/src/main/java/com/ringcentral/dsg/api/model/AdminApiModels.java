package com.ringcentral.dsg.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AdminApiModels {

    private AdminApiModels() {
    }

    public enum DirectoryType {
        Azure,
        Okta,
        Google,
        OneLogin
    }

    public enum JobType {
        FULL,
        INCREMENTAL,
        ON_DEMAND
    }

    public record DirectoryConfigRequest(@NotNull DirectoryType directoryType, String etmSubscriberId) {
    }

    public record DirectoryUpdateRequest(String directoryGroupId, Boolean active) {
    }

    public record DirectoryResponse(String directoryType, String directoryGroupId, boolean active, boolean connected) {
    }

    public record DirectoryOAuthRequest(
            @NotNull DirectoryType directoryType,
            @NotBlank String authFlow,
            @NotBlank String clientId,
            @NotBlank String clientSecret) {
    }

    public record DirectoryOAuthResponse(String directoryType, String clientId, Instant tokenExpiresAt) {
    }

    public record SchedulerRequest(Boolean incrementalEnabled, String cronExpression, String syncDirection) {
    }

    public record AttributeMappingRow(
            @NotBlank String syncDirection,
            @NotBlank String directoryAttribute,
            @NotBlank String rcAttribute) {
    }

    public record AttributeMappingRequest(
            @NotNull List<@Valid AttributeMappingRow> basicMappings,
            @NotNull List<@Valid AttributeMappingRow> customMappings) {
    }

    public record ProvisioningRuleSummary(
            String ruleId,
            String ruleName,
            int priority,
            Map<String, Object> selectionExpression) {
    }

    public record ProvisioningRuleListResponse(List<ProvisioningRuleSummary> rules) {
    }

    public record DeprovisioningRequest(@NotBlank String deprovisioningType) {
    }

    public record DeprovisioningResponse(String deprovisioningType) {
    }

    public record ProvisioningRuleRequest(
            @NotBlank String ruleName,
            @NotNull @Min(1) @Max(1000) Integer priority,
            Map<String, Object> selectionExpression,
            List<Map<String, Object>> licenseAssignments,
            List<Map<String, Object>> ruleBasedAttributeMappings,
            Map<String, Object> areaCodeAssignment,
            List<Map<String, Object>> deviceAssignments,
            List<Map<String, Object>> templateAssignments) {
    }

    public record CreateJobRequest(@NotNull JobType jobType, @NotNull List<String> externalUserIds) {
    }

    public record JobResponse(String jobId, String state) {
    }

    public record JobFailure(String externalId, String operation, String comment) {
    }

    public record JobReportResponse(
            String jobId,
            String jobType,
            String syncDirection,
            String state,
            Instant startedAt,
            Instant completedAt,
            int successCount,
            int failedCount,
            List<JobFailure> failures) {
    }

    public record JobSummaryResponse(
            String jobId,
            String jobType,
            String syncDirection,
            String state,
            Instant startedAt,
            Instant completedAt,
            int successCount,
            int failedCount) {
    }

    public record JobHistoryResponse(List<JobSummaryResponse> jobs) {
    }

    public record ErrorResponse(String code, String message) {
    }
}
