package com.ringcentral.dsg.persistence.model;

public record JobContext(
        long jobId,
        String accountId,
        int directoryTypeId,
        String jobType,
        String jobState) {
}
