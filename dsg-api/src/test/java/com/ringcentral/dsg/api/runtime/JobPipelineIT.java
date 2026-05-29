package com.ringcentral.dsg.api.runtime;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;
import com.ringcentral.dsg.worker.service.JobRetrievalService;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

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

    @Autowired
    private ProvisioningRuleRepository provisioningRuleRepository;

    @MockBean
    private MessageQueuePort messageQueuePort;

    @MockBean
    private RcProvisioningPort rcProvisioningPort;

    private long allUsersRuleId;

    @BeforeEach
    void seedAccountAndJob() {
        jdbcTemplate.update(
                "DELETE FROM job_detail WHERE job_id IN (SELECT id FROM job WHERE account_id = ?)",
                "acct-worker");
        jdbcTemplate.update("DELETE FROM job WHERE account_id = ?", "acct-worker");
        jdbcTemplate.update("DELETE FROM directory_sync_user_hash WHERE account_id = ?", "acct-worker");

        authRepository.upsert("acct-worker", 2, null);
        authRepository.update("acct-worker", "sales-group", "Sales", true);
        allUsersRuleId = provisioningRuleRepository.upsertRule("acct-worker", "All Users", 1, "{\"match\":\"ALL\"}");
        provisioningRuleRepository.insertLicenseAssignment(allUsersRuleId, 1, "RingEX");

        when(rcProvisioningPort.provisionUser(eq("acct-worker"), any(DirectoryUser.class), any()))
                .thenAnswer(invocation -> {
                    DirectoryUser user = invocation.getArgument(1, DirectoryUser.class);
                    return new ProvisioningResult(
                            "test-mailbox-" + user.externalId(), true, "test provision (no RC API)");
                });
        when(rcProvisioningPort.updateExtension(eq("acct-worker"), any(), any(DirectoryUser.class)))
                .thenAnswer(invocation -> {
                    String extensionId = invocation.getArgument(1, String.class);
                    return new ProvisioningResult(extensionId, true, "test update (no RC API)");
                });
        when(rcProvisioningPort.deleteExtension(eq("acct-worker"), eq("mailbox-former-user")))
                .thenReturn(new ProvisioningResult(
                        "mailbox-former-user", true, "test delete (no RC API)"));
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
                    "CREATE",
                    Long.toString(allUsersRuleId),
                    externalId + "@dsg-sync.dev",
                    stubAttributes(externalId)));
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

        Integer hashRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM directory_sync_user_hash WHERE account_id = ?",
                Integer.class,
                "acct-worker");
        assertEquals(2, hashRows);

        clearInvocations(messageQueuePort);
        long jobId2 = jobRepository.createJob("acct-worker", 1, 2, 1);
        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId2), "acct-worker", "FULL"));
        verify(messageQueuePort, never()).publishJobDetail(any(JobDetailMessage.class));

        String job2State = jdbcTemplate.queryForObject(
                "SELECT s.state FROM job j JOIN job_state s ON s.id = j.state_id WHERE j.id = ?",
                String.class,
                jobId2);
        assertEquals("COMPLETED", job2State);
    }

    @Test
    void fullSyncDeletesRcExtensionWhenHashUserNotInCurrentDirectoryPull() {
        long jobId = jobRepository.createJob("acct-worker", 1, 2, 1);
        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId), "acct-worker", "FULL"));

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
                    "CREATE",
                    Long.toString(allUsersRuleId),
                    externalId + "@dsg-sync.dev",
                    stubAttributes(externalId)));
        }

        jdbcTemplate.update(
                """
                        INSERT INTO directory_sync_user_hash
                            (account_id, directory_type_id, external_id, external_user_hash, mailbox_id)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                "acct-worker",
                2,
                "acct-worker-former-user",
                "former-hash",
                "mailbox-former-user");

        clearInvocations(messageQueuePort);
        long jobId2 = jobRepository.createJob("acct-worker", 1, 2, 1);
        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId2), "acct-worker", "FULL"));

        ArgumentCaptor<JobDetailMessage> captor = ArgumentCaptor.forClass(JobDetailMessage.class);
        verify(messageQueuePort, times(1)).publishJobDetail(captor.capture());
        JobDetailMessage deleteMessage = captor.getValue();
        assertEquals("DELETE", deleteMessage.operation());
        assertEquals("mailbox-former-user", deleteMessage.mailboxId());
        assertEquals("acct-worker-former-user", deleteMessage.externalId());

        syncWorkerService.processJobDetailMessage(deleteMessage);
        verify(rcProvisioningPort).deleteExtension("acct-worker", "mailbox-former-user");

        Integer orphanHashRows = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM directory_sync_user_hash
                        WHERE account_id = ? AND external_id = ?
                        """,
                Integer.class,
                "acct-worker",
                "acct-worker-former-user");
        assertEquals(0, orphanHashRows);

        String deleteState = jdbcTemplate.queryForObject(
                """
                        SELECT s.state FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        WHERE jd.job_id = ? AND jd.external_id = ?
                        """,
                String.class,
                jobId2,
                "acct-worker-former-user");
        assertEquals("SUCCEEDED", deleteState);
    }

    private static Map<String, String> stubAttributes(String externalId) {
        if (externalId.endsWith("user-1")) {
            return Map.of(
                    "profile.firstName", "Alex",
                    "profile.lastName", "Morgan",
                    "profile.email", externalId + "@dsg-sync.dev",
                    "department", "Sales");
        }
        return Map.of(
                "profile.firstName", "Jordan",
                "profile.lastName", "Kim",
                "profile.email", externalId + "@dsg-sync.dev",
                "department", "Engineering");
    }
}
