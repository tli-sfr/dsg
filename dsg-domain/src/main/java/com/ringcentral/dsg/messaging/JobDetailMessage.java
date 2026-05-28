package com.ringcentral.dsg.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Per-user work unit published to the job-detail queue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobDetailMessage(
    String jobDetailId,
    String jobId,
    String accountId,
    String externalId,
    String operation
) {}
