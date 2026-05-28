package com.ringcentral.dsg.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Account-level job envelope published to the job queue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobMessage(
    String jobId,
    String accountId,
    String jobType
) {}
