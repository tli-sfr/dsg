package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.persistence.model.JobContext;
import com.ringcentral.dsg.persistence.repo.DirectorySyncTimeRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import java.util.List;
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
            String detailStates = jobRepository.summarizeJobDetailStates(jobId);
            jobRepository.updateJobState(jobId, "COMPLETED");
            log.info("Job {} marked COMPLETED (job detail states: {})", jobId, detailStates);
            recordSyncHistory(jobId);
        }
    }

    /** Close jobs stuck in READY/IN_PREP when all job details are already terminal. */
    public void reconcileAccountJobs(String accountId) {
        for (long jobId : jobRepository.findNonTerminalJobIdsForAccount(accountId)) {
            consolidateIfComplete(jobId);
        }
    }

    public void reconcileStuckJobs() {
        for (long jobId : jobRepository.findJobIdsInStates(List.of("READY", "IN_PREP", "IN_SYNC"))) {
            consolidateIfComplete(jobId);
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
