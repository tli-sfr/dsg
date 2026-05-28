package com.ringcentral.dsg.persistence.model;

import java.util.List;

public record JobReportData(
        long jobId,
        int successCount,
        int failedCount,
        List<JobFailureRow> failures) {

    public record JobFailureRow(String externalId, String comment) {
    }
}
