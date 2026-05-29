package com.ringcentral.dsg.worker.listener;

import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import com.ringcentral.dsg.worker.service.PendingJobDetailRecoveryService;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sync worker: consumes per-user {@link JobDetailMessage} from the job-detail queue.
 * Runs on the shared Spring scheduler thread pool inside the single DSG backend process.
 */
@Component
@ConditionalOnProperty(prefix = "dsg.messaging.listener", name = "enabled", havingValue = "true")
public class SyncWorkerConsumer {

    private static final Logger log = LoggerFactory.getLogger(SyncWorkerConsumer.class);

    private final MessageQueuePort messageQueuePort;
    private final SyncWorkerService syncWorkerService;
    private final PendingJobDetailRecoveryService pendingJobDetailRecoveryService;

    public SyncWorkerConsumer(
            MessageQueuePort messageQueuePort,
            SyncWorkerService syncWorkerService,
            PendingJobDetailRecoveryService pendingJobDetailRecoveryService) {
        this.messageQueuePort = messageQueuePort;
        this.syncWorkerService = syncWorkerService;
        this.pendingJobDetailRecoveryService = pendingJobDetailRecoveryService;
    }

    @PostConstruct
    void logConsumerIdentity() {
        log.info(
                "Sync worker consumer started (pid={}, only one backend should consume dsg-job-detail-queue)",
                ProcessHandle.current().pid());
    }

    @Scheduled(fixedDelayString = "${dsg.messaging.listener.poll-interval-ms:1000}")
    public synchronized void pollJobDetailQueue() {
        int processed = 0;
        int receiveAttempt = 0;
        List<ReceivedMessage<JobDetailMessage>> batch;
        do {
            receiveAttempt++;
            Duration wait = receiveAttempt == 1 ? Duration.ofSeconds(1) : Duration.ofMillis(200);
            batch = messageQueuePort.receiveJobDetails(wait);
            if (!batch.isEmpty()) {
                log.info("[DSG sync:worker-poll] receiveAttempt={} batchSize={}", receiveAttempt, batch.size());
                processed += processBatch(batch);
            }
        } while (!batch.isEmpty() && receiveAttempt < 10);

        int recovered = pendingJobDetailRecoveryService.republishOrphanedPendingDetails();
        if (processed > 0 || recovered > 0) {
            log.info(
                    "Sync worker poll cycle processed {} job detail(s) in {} receive attempt(s), recovered={}",
                    processed,
                    receiveAttempt,
                    recovered);
        }
    }

    private int processBatch(List<ReceivedMessage<JobDetailMessage>> messages) {
        int processed = 0;
        for (ReceivedMessage<JobDetailMessage> msg : messages) {
            JobDetailMessage payload = msg.payload();
            log.info(
                    "Processing job detail jobDetailId={} externalId={} email={}",
                    payload.jobDetailId(),
                    payload.externalId(),
                    payload.email());
            try {
                syncWorkerService.processJobDetailMessage(payload);
                messageQueuePort.acknowledgeJobDetail(msg);
                processed++;
            } catch (RuntimeException ex) {
                log.error(
                        "Failed to process job detail jobDetailId={} externalId={}",
                        payload.jobDetailId(),
                        payload.externalId(),
                        ex);
            }
        }
        return processed;
    }
}
