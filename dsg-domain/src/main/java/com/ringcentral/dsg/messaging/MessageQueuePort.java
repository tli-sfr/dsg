package com.ringcentral.dsg.messaging;

import java.time.Duration;
import java.util.Optional;

/**
 * Queue abstraction for DSQ (ElasticMQ local, AWS SQS production).
 *
 * @see docs/adr/002-queue-abstraction.md
 */
public interface MessageQueuePort {

  void publishJob(JobMessage message);

  void publishJobDetail(JobDetailMessage message);

  Optional<ReceivedMessage<JobMessage>> receiveJob(Duration waitTime);

  Optional<ReceivedMessage<JobDetailMessage>> receiveJobDetail(Duration waitTime);

  void acknowledgeJob(ReceivedMessage<JobMessage> message);

  void acknowledgeJobDetail(ReceivedMessage<JobDetailMessage> message);
}
