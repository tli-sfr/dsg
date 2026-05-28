package com.ringcentral.dsg.messaging;

/**
 * Wrapper for a consumed queue message including SQS receipt handle for ack/delete.
 */
public record ReceivedMessage<T>(
    T payload,
    String receiptHandle,
    String messageId
) {}
