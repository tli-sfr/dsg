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
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
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
        "SQS client ready (local={} endpoint={} jobQueue={} detailQueue={})",
        properties.isLocalEndpoint(),
        properties.endpoint(),
        properties.jobQueueName(),
        properties.jobDetailQueueName());
  }

  @Override
  public void publishJob(JobMessage message) {
    send(properties.jobQueueName(), message);
  }

  @Override
  public void publishJobDetail(JobDetailMessage message) {
    send(properties.jobDetailQueueName(), message);
  }

  @Override
  public Optional<ReceivedMessage<JobMessage>> receiveJob(Duration waitTime) {
    return receive(properties.jobQueueName(), JobMessage.class, waitTime);
  }

  @Override
  public Optional<ReceivedMessage<JobDetailMessage>> receiveJobDetail(Duration waitTime) {
    return receive(properties.jobDetailQueueName(), JobDetailMessage.class, waitTime);
  }

  @Override
  public void acknowledgeJob(ReceivedMessage<JobMessage> message) {
    delete(properties.jobQueueName(), message.receiptHandle());
  }

  @Override
  public void acknowledgeJobDetail(ReceivedMessage<JobDetailMessage> message) {
    delete(properties.jobDetailQueueName(), message.receiptHandle());
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
    int waitSeconds = (int) Math.min(Math.max(waitTime.getSeconds(), 0), 20);
    ReceiveMessageRequest request =
        ReceiveMessageRequest.builder()
            .queueUrl(queueUrl(queueName))
            .maxNumberOfMessages(properties.maxNumberOfMessages())
            .waitTimeSeconds(waitSeconds > 0 ? waitSeconds : properties.waitTimeSeconds())
            .build();

    var response = sqsClient.receiveMessage(request);
    if (response.messages().isEmpty()) {
      return Optional.empty();
    }
    Message raw = response.messages().get(0);
    try {
      T payload = objectMapper.readValue(raw.body(), type);
      return Optional.of(new ReceivedMessage<>(payload, raw.receiptHandle(), raw.messageId()));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize queue message", e);
    }
  }

  private void delete(String queueName, String receiptHandle) {
    sqsClient.deleteMessage(
        DeleteMessageRequest.builder()
            .queueUrl(queueUrl(queueName))
            .receiptHandle(receiptHandle)
            .build());
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
