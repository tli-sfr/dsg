package com.ringcentral.dsg.messaging.listener;

import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SPIKE-4 POC only — do not enable alongside real workers ({@code JobRetrievalConsumer},
 * {@code SyncWorkerConsumer}). This listener acks messages without processing them.
 *
 * <p>Not registered by default (requires {@code dsg.messaging.listener.poc-only=true} and component
 * scan of {@code com.ringcentral.dsg.messaging.listener}).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "dsg.messaging.listener", name = "poc-only", havingValue = "true")
public class JobQueuePollingListener {

  private static final Logger log = LoggerFactory.getLogger(JobQueuePollingListener.class);

  private final MessageQueuePort messageQueuePort;

  public JobQueuePollingListener(MessageQueuePort messageQueuePort) {
    this.messageQueuePort = messageQueuePort;
  }

  @Scheduled(fixedDelayString = "${dsg.messaging.listener.poll-interval-ms:1000}")
  public void pollJobQueue() {
    Optional<ReceivedMessage<JobMessage>> received =
        messageQueuePort.receiveJob(Duration.ofSeconds(1));
    received.ifPresent(
        msg -> {
          log.info("Received job message jobId={} accountId={}", msg.payload().jobId(), msg.payload().accountId());
          messageQueuePort.acknowledgeJob(msg);
        });
  }

  @Scheduled(fixedDelayString = "${dsg.messaging.listener.poll-interval-ms:1000}")
  public void pollJobDetailQueue() {
    Optional<ReceivedMessage<JobDetailMessage>> received =
        messageQueuePort.receiveJobDetail(Duration.ofSeconds(1));
    received.ifPresent(
        msg -> {
          log.info(
              "Received job detail jobDetailId={} externalId={}",
              msg.payload().jobDetailId(),
              msg.payload().externalId());
          messageQueuePort.acknowledgeJobDetail(msg);
        });
  }
}
