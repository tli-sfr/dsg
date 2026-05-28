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
        Map<String, String> attributes) {

    public JobDetailMessage(
            String jobDetailId, String jobId, String accountId, String externalId, String operation) {
        this(jobDetailId, jobId, accountId, externalId, operation, null, null, Map.of());
    }
}
