package com.ringcentral.dsg.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Queue client settings. Local profile points at ElasticMQ; prod uses native SQS (no endpoint override).
 */
@ConfigurationProperties(prefix = "dsg.messaging")
public record MessagingProperties(
    String endpoint,
    String region,
    String accessKeyId,
    String secretAccessKey,
    String jobQueueName,
    String jobDetailQueueName,
    int maxNumberOfMessages,
    int waitTimeSeconds
) {
  public MessagingProperties {
    if (region == null || region.isBlank()) {
      region = "us-east-1";
    }
    if (accessKeyId == null || accessKeyId.isBlank()) {
      accessKeyId = "x";
    }
    if (secretAccessKey == null || secretAccessKey.isBlank()) {
      secretAccessKey = "x";
    }
    if (jobQueueName == null || jobQueueName.isBlank()) {
      jobQueueName = "dsg-job-queue";
    }
    if (jobDetailQueueName == null || jobDetailQueueName.isBlank()) {
      jobDetailQueueName = "dsg-job-detail-queue";
    }
    if (maxNumberOfMessages <= 0) {
      maxNumberOfMessages = 1;
    }
  }

  public boolean isLocalEndpoint() {
    return endpoint != null && !endpoint.isBlank();
  }
}
