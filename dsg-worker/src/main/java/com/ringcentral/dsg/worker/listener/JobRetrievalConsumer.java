package com.ringcentral.dsg.worker.listener;

import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import com.ringcentral.dsg.worker.service.JobRetrievalService;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Detail DB publisher (retrieval): consumes account-level {@link JobMessage} from the job queue.
 * Runs on the shared Spring scheduler thread pool inside the single DSG backend process.
 */
@Component
@ConditionalOnProperty(prefix = "dsg.messaging.listener", name = "enabled", havingValue = "true")
public class JobRetrievalConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobRetrievalConsumer.class);

    private final MessageQueuePort messageQueuePort;
    private final JobRetrievalService jobRetrievalService;

    public JobRetrievalConsumer(MessageQueuePort messageQueuePort, JobRetrievalService jobRetrievalService) {
        this.messageQueuePort = messageQueuePort;
        this.jobRetrievalService = jobRetrievalService;
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
}
