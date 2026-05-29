package com.ringcentral.dsg.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Per-user work unit published to the job-detail queue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobDetailMessage(
        String jobDetailId,
        String jobId,
        String accountId,
        String externalId,
        String operation,
        String ruleId,
        String email,
        Map<String, String> attributes,
        /** RC extension id for UPDATE; null for CREATE. */
        String mailboxId) {

    public JobDetailMessage(
            String jobDetailId, String jobId, String accountId, String externalId, String operation) {
        this(jobDetailId, jobId, accountId, externalId, operation, null, null, Map.of(), null);
    }

    public JobDetailMessage(
            String jobDetailId,
            String jobId,
            String accountId,
            String externalId,
            String operation,
            String ruleId,
            String email,
            Map<String, String> attributes) {
        this(jobDetailId, jobId, accountId, externalId, operation, ruleId, email, attributes, null);
    }
}
