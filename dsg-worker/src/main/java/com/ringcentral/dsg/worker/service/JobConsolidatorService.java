package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.persistence.model.JobContext;
import com.ringcentral.dsg.persistence.repo.DirectorySyncTimeRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobConsolidatorService {

    private static final Logger log = LoggerFactory.getLogger(JobConsolidatorService.class);

    private final JobRepository jobRepository;
    private final DirectorySyncTimeRepository directorySyncTimeRepository;

    public JobConsolidatorService(
            JobRepository jobRepository, DirectorySyncTimeRepository directorySyncTimeRepository) {
        this.jobRepository = jobRepository;
        this.directorySyncTimeRepository = directorySyncTimeRepository;
    }

    public void consolidateIfComplete(long jobId) {
        if (!jobRepository.hasNonTerminalJobDetails(jobId)) {
            jobRepository.updateJobState(jobId, "COMPLETED");
            log.info("Job {} marked COMPLETED", jobId);
            recordSyncHistory(jobId);
        }
    }

    private void recordSyncHistory(long jobId) {
        Optional<JobContext> context = jobRepository.findJobContext(jobId);
        if (context.isEmpty()) {
            return;
        }
        JobContext job = context.get();
        directorySyncTimeRepository.recordLatestJobCompletion(
                job.accountId(), job.directoryTypeId(), jobId, job.jobState());
    }
}
