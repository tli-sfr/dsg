package com.ringcentral.dsg.api.support;

import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.messaging.ReceivedMessage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link MessageQueuePort} for pipeline tests. Supports batch receive and tracks pending vs in-flight
 * messages like SQS visibility.
 */
public final class InMemoryMessageQueuePort implements MessageQueuePort {

    private static final int MAX_BATCH = 10;

    private final AtomicLong receiptSequence = new AtomicLong();
    private final Queue<StoredMessage<JobMessage>> jobQueue = new ConcurrentLinkedQueue<>();
    private final Queue<StoredMessage<JobDetailMessage>> detailQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, StoredMessage<JobDetailMessage>> inFlightDetails = new ConcurrentHashMap<>();

    @Override
    public void publishJob(JobMessage message) {
        jobQueue.add(stored(message));
    }

    @Override
    public void publishJobDetail(JobDetailMessage message) {
        detailQueue.add(stored(message));
    }

    @Override
    public Optional<ReceivedMessage<JobMessage>> receiveJob(Duration waitTime) {
        return Optional.ofNullable(jobQueue.poll()).map(this::toReceived);
    }

    @Override
    public Optional<ReceivedMessage<JobDetailMessage>> receiveJobDetail(Duration waitTime) {
        return receiveJobDetails(waitTime).stream().findFirst();
    }

    @Override
    public List<ReceivedMessage<JobDetailMessage>> receiveJobDetails(Duration waitTime) {
        List<ReceivedMessage<JobDetailMessage>> batch = new ArrayList<>();
        while (batch.size() < MAX_BATCH) {
            StoredMessage<JobDetailMessage> next = detailQueue.poll();
            if (next == null) {
                break;
            }
            inFlightDetails.put(next.receiptHandle(), next);
            batch.add(toReceived(next));
        }
        return batch;
    }

    @Override
    public void acknowledgeJob(ReceivedMessage<JobMessage> message) {
        // no-op for tests
    }

    @Override
    public void acknowledgeJobDetail(ReceivedMessage<JobDetailMessage> message) {
        inFlightDetails.remove(message.receiptHandle());
    }

    public int pendingJobDetailCount() {
        return detailQueue.size();
    }

    public int inFlightJobDetailCount() {
        return inFlightDetails.size();
    }

    private <T> StoredMessage<T> stored(T payload) {
        return new StoredMessage<>(payload, "receipt-" + receiptSequence.incrementAndGet());
    }

    private <T> ReceivedMessage<T> toReceived(StoredMessage<T> stored) {
        return new ReceivedMessage<>(stored.payload(), stored.receiptHandle(), stored.receiptHandle());
    }

    private record StoredMessage<T>(T payload, String receiptHandle) {}
}
