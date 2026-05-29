package com.ringcentral.dsg.worker.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ringcentral.dsg.api.support.InMemoryMessageQueuePort;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.worker.service.PendingJobDetailRecoveryService;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import org.junit.jupiter.api.Test;

/** Fast unit test: sync worker drain loop processes every message published to the in-memory queue. */
class SyncWorkerConsumerBatchTest {

    @Test
    void pollJobDetailQueueProcessesAllPublishedMessages() {
        InMemoryMessageQueuePort queue = new InMemoryMessageQueuePort();
        SyncWorkerService syncWorkerService = mock(SyncWorkerService.class);
        PendingJobDetailRecoveryService recoveryService = mock(PendingJobDetailRecoveryService.class);
        SyncWorkerConsumer consumer = new SyncWorkerConsumer(queue, syncWorkerService, recoveryService);

        queue.publishJobDetail(new JobDetailMessage(
                "37", "14", "623256020", "00u16ux1qhu3swZ5x1d8", "CREATE", "1", "a@test.com", java.util.Map.of()));
        queue.publishJobDetail(new JobDetailMessage(
                "38", "14", "623256020", "00u1gr9kuopuehtYz1d8", "CREATE", "1", "b@test.com", java.util.Map.of()));

        consumer.pollJobDetailQueue();

        verify(syncWorkerService, times(2)).processJobDetailMessage(any());
    }
}
