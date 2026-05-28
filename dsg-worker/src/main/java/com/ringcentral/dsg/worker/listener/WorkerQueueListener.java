package com.ringcentral.dsg.worker.listener;

import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import com.ringcentral.dsg.worker.service.JobRetrievalService;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "dsg.messaging.listener", name = "enabled", havingValue = "true")
public class WorkerQueueListener {

    private static final Logger log = LoggerFactory.getLogger(WorkerQueueListener.class);

    private final MessageQueuePort messageQueuePort;
    private final JobRetrievalService jobRetrievalService;
    private final SyncWorkerService syncWorkerService;

    public WorkerQueueListener(
            MessageQueuePort messageQueuePort,
            JobRetrievalService jobRetrievalService,
            SyncWorkerService syncWorkerService) {
        this.messageQueuePort = messageQueuePort;
        this.jobRetrievalService = jobRetrievalService;
        this.syncWorkerService = syncWorkerService;
    }

    @Scheduled(fixedDelayString = "${dsg.messaging.listener.poll-interval-ms:1000}")
    public void pollJobQueue() {
        Optional<ReceivedMessage<JobMessage>> received = messageQueuePort.receiveJob(Duration.ofSeconds(1));
        received.ifPresent(msg -> {
            try {
                jobRetrievalService.processJobMessage(msg.payload());
                messageQueuePort.acknowledgeJob(msg);
            } catch (RuntimeException ex) {
                log.error("Failed to process job message jobId={}", msg.payload().jobId(), ex);
            }
        });
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
