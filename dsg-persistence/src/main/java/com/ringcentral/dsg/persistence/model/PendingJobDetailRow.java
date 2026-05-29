package com.ringcentral.dsg.persistence.model;

public record PendingJobDetailRow(
        long jobDetailId,
        long jobId,
        String accountId,
        String externalId,
        Long ruleId,
        String operation,
        String mailboxId) {}
