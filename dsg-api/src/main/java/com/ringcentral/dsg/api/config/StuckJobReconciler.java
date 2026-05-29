package com.ringcentral.dsg.api.config;

import com.ringcentral.dsg.worker.service.JobConsolidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** On startup, close jobs left in READY when all job details already finished. */
@Component
public class StuckJobReconciler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StuckJobReconciler.class);

    private final JobConsolidatorService consolidatorService;

    public StuckJobReconciler(JobConsolidatorService consolidatorService) {
        this.consolidatorService = consolidatorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        consolidatorService.reconcileStuckJobs();
        log.info("Stuck job reconciliation complete");
    }
}
