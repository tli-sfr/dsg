package com.ringcentral.dsg.api.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import com.ringcentral.dsg.api.support.InMemoryMessageQueuePort;
import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import com.ringcentral.dsg.worker.listener.SyncWorkerConsumer;
import com.ringcentral.dsg.worker.service.JobRetrievalService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Validates that two Okta users are persisted as job details, published to the queue, and both consumed by the
 * sync worker in one poll cycle — without calling RingCentral APIs.
 */
@SpringBootTest(
        properties = {
            "dsg.messaging.listener.enabled=true",
            "spring.task.scheduling.enabled=false",
            "dsg.directory.stub=false"
        })
@Import(DualUserSyncPipelineIT.QueueTestConfig.class)
class DualUserSyncPipelineIT extends AbstractApiIntegrationTest {

    private static final String ACCOUNT_ID = "acct-dual-okta";
    private static final String GROUP_ID = "00g23m3xtmelnUvqL1d8";
    private static final String USER_1 = "00u16ux1qhu3swZ5x1d8";
    private static final String USER_2 = "00u1gr9kuopuehtYz1d8";

    @TestConfiguration
    static class QueueTestConfig {
        @Bean
        @Primary
        InMemoryMessageQueuePort inMemoryMessageQueuePort() {
            return new InMemoryMessageQueuePort();
        }
    }

    @Autowired
    private InMemoryMessageQueuePort messageQueuePort;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AccountDirectoryAuthRepository authRepository;

    @Autowired
    private ProvisioningRuleRepository provisioningRuleRepository;

    @Autowired
    private JobRetrievalService jobRetrievalService;

    @Autowired
    private SyncWorkerConsumer syncWorkerConsumer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DirectoryPort directoryPort;

    @MockBean
    private RcProvisioningPort rcProvisioningPort;

    private long ringExRuleId;

    @BeforeEach
    void seedAccountAndMocks() {
        authRepository.upsert(ACCOUNT_ID, 2, null);
        authRepository.update(ACCOUNT_ID, GROUP_ID, "IT Admins", true);
        ringExRuleId = provisioningRuleRepository.upsertRule(ACCOUNT_ID, "All Okta Users", 1, "{\"match\":\"ALL\"}");
        provisioningRuleRepository.insertLicenseAssignment(ringExRuleId, 1, "RingEX");

        when(directoryPort.listGroupMembers(eq(ACCOUNT_ID), eq(GROUP_ID))).thenReturn(twoOktaUsers());
        when(rcProvisioningPort.provisionUser(eq(ACCOUNT_ID), any(DirectoryUser.class), any()))
                .thenAnswer(invocation -> {
                    DirectoryUser user = invocation.getArgument(1, DirectoryUser.class);
                    return new ProvisioningResult(
                            "test-mailbox-" + user.externalId(), true, "test provision (no RC API)");
                });
    }

    @Test
    void retrievalEnqueuesTwoJobDetailsAndSyncWorkerProcessesBothWithoutRcApi() {
        long jobId = jobRepository.createJob(ACCOUNT_ID, 1, 2, 1);

        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId), ACCOUNT_ID, "FULL"));

        Integer detailRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_detail WHERE job_id = ?",
                Integer.class,
                jobId);
        assertEquals(2, detailRows, "expected two job_detail rows for two Okta users");

        Set<String> externalIdsInDb = jdbcTemplate.query(
                        "SELECT external_id FROM job_detail WHERE job_id = ? ORDER BY id",
                        (rs, rowNum) -> rs.getString("external_id"),
                        jobId)
                .stream()
                .collect(Collectors.toSet());
        assertEquals(Set.of(USER_1, USER_2), externalIdsInDb);

        assertEquals(2, messageQueuePort.pendingJobDetailCount(), "both users should be on the job-detail queue");

        syncWorkerConsumer.pollJobDetailQueue();

        assertEquals(0, messageQueuePort.pendingJobDetailCount(), "queue should be drained");
        assertEquals(0, messageQueuePort.inFlightJobDetailCount(), "all messages should be acknowledged");

        ArgumentCaptor<DirectoryUser> userCaptor = ArgumentCaptor.forClass(DirectoryUser.class);
        verify(rcProvisioningPort, times(2)).provisionUser(eq(ACCOUNT_ID), userCaptor.capture(), any());
        Set<String> provisionedExternalIds = userCaptor.getAllValues().stream()
                .map(DirectoryUser::externalId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(USER_1, USER_2), provisionedExternalIds);

        Integer succeeded = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        WHERE jd.job_id = ? AND s.state = 'SUCCEEDED'
                        """,
                Integer.class,
                jobId);
        assertEquals(2, succeeded);

        String jobState = jdbcTemplate.queryForObject(
                "SELECT s.state FROM job j JOIN job_state s ON s.id = j.state_id WHERE j.id = ?",
                String.class,
                jobId);
        assertEquals("COMPLETED", jobState);

        List<String> comments = jdbcTemplate.queryForList(
                "SELECT comment FROM job_detail WHERE job_id = ? ORDER BY id", String.class, jobId);
        assertEquals(2, comments.size());
        assertTrue(comments.stream().allMatch(c -> c != null && c.contains("test provision (no RC API)")));
    }

    private static List<DirectoryUser> twoOktaUsers() {
        return List.of(
                new DirectoryUser(
                        USER_1,
                        "tony.li+testorg@ringcentral.com",
                        Map.of(
                                "profile.email", "tony.li+testorg@ringcentral.com",
                                "profile.mobilePhone", "16504443333",
                                "profile.firstName", "rc",
                                "profile.login", "tony.li@ringcentral.com",
                                "profile.lastName", "test")),
                new DirectoryUser(
                        USER_2,
                        "rc20.user1@testorg.com",
                        Map.of(
                                "profile.lastName", "user1a",
                                "profile.state", "CA",
                                "profile.city", "San mateo",
                                "profile.countryCode", "US",
                                "profile.primaryPhone", "+16507810271#76717",
                                "profile.email", "rc20.user1@testorg.com",
                                "profile.streetAddress", "123",
                                "profile.firstName", "rc20a",
                                "profile.login", "rc20.user1@englab.local",
                                "profile.displayName", "rc20 user1")));
    }
}
