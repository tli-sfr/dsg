package com.ringcentral.dsg.messaging.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import com.ringcentral.dsg.messaging.config.MessagingProperties;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * SQS-compatible implementation (ElasticMQ local, AWS SQS production).
 */
public class SqsMessageQueuePort implements MessageQueuePort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(SqsMessageQueuePort.class);

  private final SqsClient sqsClient;
  private final ObjectMapper objectMapper;
  private final MessagingProperties properties;
  private final Map<String, String> queueUrlByName = new ConcurrentHashMap<>();

  public SqsMessageQueuePort(MessagingProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.sqsClient = buildClient(properties);
    log.info(
        "SQS client ready (local={} endpoint={} jobQueue={} detailQueue={} maxBatch={})",
        properties.isLocalEndpoint(),
        properties.endpoint(),
        properties.jobQueueName(),
        properties.jobDetailQueueName(),
        properties.maxNumberOfMessages());
  }

  @Override
  public void publishJob(JobMessage message) {
    send(properties.jobQueueName(), message);
  }

  @Override
  public void publishJobDetail(JobDetailMessage message) {
    send(properties.jobDetailQueueName(), message);
    log.info(
        "[DSG queue:publish-detail] jobDetailId={} jobId={} externalId={} email={}",
        message.jobDetailId(),
        message.jobId(),
        message.externalId(),
        message.email());
    logDetailQueueStats("after-publish");
  }

  @Override
  public Optional<ReceivedMessage<JobMessage>> receiveJob(Duration waitTime) {
    return receive(properties.jobQueueName(), JobMessage.class, waitTime);
  }

  @Override
  public Optional<ReceivedMessage<JobDetailMessage>> receiveJobDetail(Duration waitTime) {
    return receiveAll(properties.jobDetailQueueName(), JobDetailMessage.class, waitTime).stream()
        .findFirst();
  }

  @Override
  public List<ReceivedMessage<JobDetailMessage>> receiveJobDetails(Duration waitTime) {
    return receiveAll(properties.jobDetailQueueName(), JobDetailMessage.class, waitTime);
  }

  @Override
  public void acknowledgeJob(ReceivedMessage<JobMessage> message) {
    delete(properties.jobQueueName(), message.receiptHandle());
  }

  @Override
  public void acknowledgeJobDetail(ReceivedMessage<JobDetailMessage> message) {
    JobDetailMessage payload = message.payload();
    delete(properties.jobDetailQueueName(), message.receiptHandle());
    log.info(
        "[DSG queue:ack-detail] jobDetailId={} externalId={} email={}",
        payload.jobDetailId(),
        payload.externalId(),
        payload.email());
    logDetailQueueStats("after-ack");
  }

  @Override
  public void close() {
    sqsClient.close();
  }

  private void send(String queueName, Object payload) {
    try {
      String body = objectMapper.writeValueAsString(payload);
      sqsClient.sendMessage(
          SendMessageRequest.builder().queueUrl(queueUrl(queueName)).messageBody(body).build());
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize queue message", e);
    }
  }

  private <T> Optional<ReceivedMessage<T>> receive(
      String queueName, Class<T> type, Duration waitTime) {
    return receiveAll(queueName, type, waitTime).stream().findFirst();
  }

  private <T> List<ReceivedMessage<T>> receiveAll(
      String queueName, Class<T> type, Duration waitTime) {
    int waitSeconds = resolveWaitSeconds(waitTime);
    ReceiveMessageRequest request =
        ReceiveMessageRequest.builder()
            .queueUrl(queueUrl(queueName))
            .maxNumberOfMessages(properties.maxNumberOfMessages())
            .waitTimeSeconds(waitSeconds)
            .build();

    var response = sqsClient.receiveMessage(request);
    if (response.messages().isEmpty()) {
      if (properties.jobDetailQueueName().equals(queueName)) {
        logDetailQueueStats("receive-empty");
      }
      return List.of();
    }
    List<ReceivedMessage<T>> received = new ArrayList<>(response.messages().size());
    for (Message raw : response.messages()) {
      try {
        T payload = objectMapper.readValue(raw.body(), type);
        received.add(new ReceivedMessage<>(payload, raw.receiptHandle(), raw.messageId()));
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Failed to deserialize queue message", e);
      }
    }
    if (properties.jobDetailQueueName().equals(queueName) && type == JobDetailMessage.class) {
      String ids = received.stream()
          .map(msg -> ((JobDetailMessage) msg.payload()).jobDetailId())
          .reduce((a, b) -> a + "," + b)
          .orElse("");
      log.info(
          "[DSG queue:receive-detail] waitSeconds={} requestedMax={} received={} jobDetailIds=[{}]",
          waitSeconds,
          properties.maxNumberOfMessages(),
          received.size(),
          ids);
      logDetailQueueStats("after-receive");
    }
    return received;
  }

  private int resolveWaitSeconds(Duration waitTime) {
    if (waitTime == null) {
      return properties.waitTimeSeconds();
    }
    if (waitTime.isZero() || waitTime.isNegative()) {
      return 0;
    }
    long seconds = waitTime.getSeconds();
    if (seconds <= 0 && waitTime.toMillis() > 0) {
      return 1;
    }
    return (int) Math.min(seconds, 20);
  }

  private void delete(String queueName, String receiptHandle) {
    sqsClient.deleteMessage(
        DeleteMessageRequest.builder()
            .queueUrl(queueUrl(queueName))
            .receiptHandle(receiptHandle)
            .build());
  }

  private void logDetailQueueStats(String event) {
    try {
      var attributes = sqsClient.getQueueAttributes(
              GetQueueAttributesRequest.builder()
                  .queueUrl(queueUrl(properties.jobDetailQueueName()))
                  .attributeNames(
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)
                  .build())
          .attributes();
      log.info(
          "[DSG queue:stats-detail] event={} visible={} inFlight={}",
          event,
          attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES),
          attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE));
    } catch (RuntimeException ex) {
      log.debug("Failed to read detail queue stats for event={}", event, ex);
    }
  }

  private String queueUrl(String queueName) {
    return queueUrlByName.computeIfAbsent(
        queueName,
        name ->
            sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build())
                .queueUrl());
  }

  private static SqsClient buildClient(MessagingProperties properties) {
    SqsClientBuilder builder =
        SqsClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        properties.accessKeyId(), properties.secretAccessKey())));

    if (properties.isLocalEndpoint()) {
      builder.endpointOverride(URI.create(properties.endpoint()));
    }
    return builder.build();
  }
}
