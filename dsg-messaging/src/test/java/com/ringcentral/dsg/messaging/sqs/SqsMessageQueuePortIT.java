package com.ringcentral.dsg.messaging.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import com.ringcentral.dsg.messaging.config.MessagingProperties;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * SPIKE-4: publish/consume against ElasticMQ using AWS SDK v2 endpoint override.
 */
@Testcontainers
class SqsMessageQueuePortIT {

  @Container
  @SuppressWarnings("resource")
  static GenericContainer<?> elasticmq =
      new GenericContainer<>("softwaremill/elasticmq-native:latest")
          .withExposedPorts(9324)
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("elasticmq-test.conf"), "/opt/elasticmq.conf")
          .withCommand("-Dconfig.file=/opt/elasticmq.conf");

  private SqsMessageQueuePort messageQueuePort;

  @BeforeEach
  void setUp() {
    String endpoint = "http://" + elasticmq.getHost() + ":" + elasticmq.getMappedPort(9324);
    MessagingProperties properties =
        new MessagingProperties(
            endpoint,
            "us-east-1",
            "x",
            "x",
            "dsg-job-queue",
            "dsg-job-detail-queue",
            1,
            1);
    messageQueuePort = new SqsMessageQueuePort(properties, new ObjectMapper());
  }

  @AfterEach
  void tearDown() {
    if (messageQueuePort != null) {
      messageQueuePort.close();
    }
  }

  @Test
  void publishesAndConsumesJobMessage() {
    JobMessage sent = new JobMessage("job-1", "acct-100", "FULL");
    messageQueuePort.publishJob(sent);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              Optional<ReceivedMessage<JobMessage>> received =
                  messageQueuePort.receiveJob(Duration.ofSeconds(2));
              assertThat(received).isPresent();
              assertThat(received.get().payload()).isEqualTo(sent);
              messageQueuePort.acknowledgeJob(received.get());
            });
  }

  @Test
  void publishesAndConsumesJobDetailMessage() {
    JobDetailMessage sent =
        new JobDetailMessage("detail-1", "job-1", "acct-100", "ext-user-9", "CREATE");
    messageQueuePort.publishJobDetail(sent);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              Optional<ReceivedMessage<JobDetailMessage>> received =
                  messageQueuePort.receiveJobDetail(Duration.ofSeconds(2));
              assertThat(received).isPresent();
              assertThat(received.get().payload()).isEqualTo(sent);
              messageQueuePort.acknowledgeJobDetail(received.get());
            });
  }
}
