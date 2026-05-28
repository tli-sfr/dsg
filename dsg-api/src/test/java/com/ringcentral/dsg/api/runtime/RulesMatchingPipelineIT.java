package com.ringcentral.dsg.api.runtime;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import com.ringcentral.dsg.worker.service.JobRetrievalService;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "dsg.messaging.listener.enabled=false")
class RulesMatchingPipelineIT extends AbstractApiIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AccountDirectoryAuthRepository authRepository;

    @Autowired
    private ProvisioningRuleRepository provisioningRuleRepository;

    @Autowired
    private JobRetrievalService jobRetrievalService;

    @Autowired
    private SyncWorkerService syncWorkerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private MessageQueuePort messageQueuePort;

    @BeforeEach
    void seedAccount() {
        authRepository.upsert("acct-rules", 2, null);
        authRepository.update("acct-rules", "sales-group", "Sales", true);
    }

    @Test
    void createsJobDetailOnlyForUsersMatchingFirstRuleByPriority() {
        long salesRuleId = provisioningRuleRepository.upsertRule(
                "acct-rules",
                "Sales",
                1,
                """
                        {"match":"ALL","criteria":[{"attribute":"user.department","operator":"EQ","value":"Sales"}]}
                        """);
        provisioningRuleRepository.insertLicenseAssignment(salesRuleId, 1, "VideoPro");
        provisioningRuleRepository.upsertRule(
                "acct-rules",
                "Engineering",
                2,
                """
                        {"match":"ALL","criteria":[{"attribute":"user.department","operator":"EQ","value":"Engineering"}]}
                        """);

        long jobId = jobRepository.createJob("acct-rules", 1, 2, 1);
        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId), "acct-rules", "FULL"));

        ArgumentCaptor<JobDetailMessage> captor = ArgumentCaptor.forClass(JobDetailMessage.class);
        verify(messageQueuePort, times(1)).publishJobDetail(captor.capture());

        JobDetailMessage detail = captor.getValue();
        assertEquals("acct-rules-user-1", detail.externalId());
        assertEquals(Long.toString(salesRuleId), detail.ruleId());
        assertEquals("Sales", detail.attributes().get("department"));

        Long storedRuleId = jdbcTemplate.queryForObject(
                "SELECT rule_id FROM job_detail WHERE job_id = ?",
                Long.class,
                jobId);
        assertEquals(salesRuleId, storedRuleId);
    }

    @Test
    void appliesRuleBasedAttributeMappingsOnCreate() {
        long ruleId = provisioningRuleRepository.upsertRule(
                "acct-rules",
                "Sales",
                1,
                """
                        {"match":"ALL","criteria":[{"attribute":"user.department","operator":"EQ","value":"Sales"}]}
                        """);
        provisioningRuleRepository.insertLicenseAssignment(ruleId, 1, "RingEX");
        provisioningRuleRepository.insertRuleBasedAttributeMapping(
                "acct-rules", ruleId, "user.department", "Sales", 1, "SalesManager");

        long jobId = jobRepository.createJob("acct-rules", 1, 2, 1);
        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId), "acct-rules", "FULL"));

        ArgumentCaptor<JobDetailMessage> captor = ArgumentCaptor.forClass(JobDetailMessage.class);
        verify(messageQueuePort).publishJobDetail(captor.capture());
        syncWorkerService.processJobDetailMessage(captor.getValue());

        String comment = jdbcTemplate.queryForObject(
                "SELECT comment FROM job_detail WHERE job_id = ?",
                String.class,
                jobId);
        assertNotNull(comment);
        assertTrue(comment.contains("rule-based attribute"));
    }

    @Test
    void skipsUsersWithNoMatchingRule() {
        provisioningRuleRepository.upsertRule(
                "acct-rules",
                "Finance only",
                1,
                """
                        {"match":"ALL","criteria":[{"attribute":"user.department","operator":"EQ","value":"Finance"}]}
                        """);

        long jobId = jobRepository.createJob("acct-rules", 1, 2, 1);
        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId), "acct-rules", "FULL"));

        Integer detailCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_detail WHERE job_id = ?",
                Integer.class,
                jobId);
        assertEquals(0, detailCount);
    }
}
