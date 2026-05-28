package com.ringcentral.dsg.persistence.model;

import java.time.Instant;

public record JobSummaryData(
        long jobId,
        String jobType,
        String syncDirection,
        String state,
        Instant startedAt,
        Instant completedAt,
        int successCount,
        int failedCount) {}
