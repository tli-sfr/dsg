package com.ringcentral.dsg.worker.listener;

import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import java.time.Duration;
import java.util.Optional;
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

    public SyncWorkerConsumer(MessageQueuePort messageQueuePort, SyncWorkerService syncWorkerService) {
        this.messageQueuePort = messageQueuePort;
        this.syncWorkerService = syncWorkerService;
    }

    @Scheduled(fixedDelayString = "${dsg.messaging.listener.poll-interval-ms:1000}")
    public void pollJobDetailQueue() {
        Optional<ReceivedMessage<JobDetailMessage>> received =
                messageQueuePort.receiveJobDetail(Duration.ofSeconds(1));
        received.ifPresent(msg -> {
            try {
                syncWorkerService.processJobDetailMessage(msg.payload());
                messageQueuePort.acknowledgeJobDetail(msg);
            } catch (RuntimeException ex) {
                log.error("Failed to process job detail jobDetailId={}", msg.payload().jobDetailId(), ex);
            }
        });
    }
}
