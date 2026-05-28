package com.ringcentral.dsg.worker.service;

import com.ringcentral.dsg.persistence.repo.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobConsolidatorService {

    private static final Logger log = LoggerFactory.getLogger(JobConsolidatorService.class);

    private final JobRepository jobRepository;

    public JobConsolidatorService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void consolidateIfComplete(long jobId) {
        if (!jobRepository.hasNonTerminalJobDetails(jobId)) {
            jobRepository.updateJobState(jobId, "COMPLETED");
            log.info("Job {} marked COMPLETED", jobId);
        }
    }
}
