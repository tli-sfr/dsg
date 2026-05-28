package com.ringcentral.dsg.api.runtime;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.worker.service.JobRetrievalService;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "dsg.messaging.listener.enabled=false")
class JobPipelineIT extends AbstractApiIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AccountDirectoryAuthRepository authRepository;

    @Autowired
    private JobRetrievalService jobRetrievalService;

    @Autowired
    private SyncWorkerService syncWorkerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private MessageQueuePort messageQueuePort;

    @BeforeEach
    void seedAccountAndJob() {
        authRepository.upsert("acct-worker", 2, null);
        authRepository.update("acct-worker", "sales-group", true);
    }

    @Test
    void runsRetrievalSyncAndConsolidationInSingleBackendContext() {
        long jobId = jobRepository.createJob("acct-worker", 1, 2, 1);
        JobMessage jobMessage = new JobMessage(Long.toString(jobId), "acct-worker", "FULL");

        jobRetrievalService.processJobMessage(jobMessage);
        verify(messageQueuePort, times(2)).publishJobDetail(any(JobDetailMessage.class));

        String readyState = jdbcTemplate.queryForObject(
                "SELECT s.state FROM job j JOIN job_state s ON s.id = j.state_id WHERE j.id = ?",
                String.class,
                jobId);
        assertEquals("READY", readyState);

        var detailIds = jdbcTemplate.queryForList(
                "SELECT id FROM job_detail WHERE job_id = ? ORDER BY id",
                Long.class,
                jobId);

        for (Long detailId : detailIds) {
            String externalId = jdbcTemplate.queryForObject(
                    "SELECT external_id FROM job_detail WHERE id = ?",
                    String.class,
                    detailId);
            syncWorkerService.processJobDetailMessage(new JobDetailMessage(
                    Long.toString(detailId),
                    Long.toString(jobId),
                    "acct-worker",
                    externalId,
                    "CREATE"));
        }

        String finalState = jdbcTemplate.queryForObject(
                "SELECT s.state FROM job j JOIN job_state s ON s.id = j.state_id WHERE j.id = ?",
                String.class,
                jobId);
        assertEquals("COMPLETED", finalState);

        Integer succeeded = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        WHERE jd.job_id = ? AND s.state = 'SUCCEEDED'
                        """,
                Integer.class,
                jobId);
        assertEquals(2, succeeded);
    }
}
