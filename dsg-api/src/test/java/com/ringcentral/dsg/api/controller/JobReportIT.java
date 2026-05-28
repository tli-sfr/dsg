package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import com.ringcentral.dsg.messaging.JobDetailMessage;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.JobRepository;
import com.ringcentral.dsg.persistence.repo.ProvisioningRuleRepository;
import com.ringcentral.dsg.worker.service.JobRetrievalService;
import com.ringcentral.dsg.worker.service.SyncWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dsg.messaging.listener.enabled=false")
@AutoConfigureMockMvc
class JobReportIT extends AbstractApiIntegrationTest {

    private static final String ACCOUNT = "acct-report";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountDirectoryAuthRepository authRepository;

    @Autowired
    private ProvisioningRuleRepository provisioningRuleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobRetrievalService jobRetrievalService;

    @Autowired
    private SyncWorkerService syncWorkerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedAccount() {
        jdbcTemplate.update(
                "UPDATE directory_sync_time SET latest_job_id = NULL WHERE account_id = ?", ACCOUNT);
        jdbcTemplate.update(
                "DELETE jd FROM job_detail jd JOIN job j ON j.id = jd.job_id WHERE j.account_id = ?",
                ACCOUNT);
        jdbcTemplate.update("DELETE FROM job WHERE account_id = ?", ACCOUNT);
        authRepository.upsert(ACCOUNT, 2, null);
        authRepository.update(ACCOUNT, "sales-group", "Sales", true);
        provisioningRuleRepository.upsertRule(ACCOUNT, "All Users", 1, "{\"match\":\"ALL\"}");
        jdbcTemplate.update(
                """
                        INSERT INTO directory_sync_time
                            (account_id, directory_type_id, job_type_id, direction_id, frequency_id, cron_expression)
                        VALUES (?, 2, 1, 1, 1, '0 0 2 * * ?')
                        ON DUPLICATE KEY UPDATE cron_expression = VALUES(cron_expression)
                        """,
                ACCOUNT);
    }

    @Test
    void returnsFullReportHistoryAndLatestAfterJobCompletes() throws Exception {
        long jobId = jobRepository.createJob(ACCOUNT, 1, 2, 1);
        jobRetrievalService.processJobMessage(new JobMessage(Long.toString(jobId), ACCOUNT, "FULL"));

        var detailIds = jdbcTemplate.queryForList(
                "SELECT id FROM job_detail WHERE job_id = ? ORDER BY id", Long.class, jobId);
        for (Long detailId : detailIds) {
            String externalId = jdbcTemplate.queryForObject(
                    "SELECT external_id FROM job_detail WHERE id = ?", String.class, detailId);
            syncWorkerService.processJobDetailMessage(new JobDetailMessage(
                    Long.toString(detailId),
                    Long.toString(jobId),
                    ACCOUNT,
                    externalId,
                    "CREATE",
                    null,
                    externalId + "@example.com",
                    java.util.Map.of("department", "Sales")));
        }

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/jobs/" + jobId + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(Long.toString(jobId)))
                .andExpect(jsonPath("$.jobType").value("FULL"))
                .andExpect(jsonPath("$.syncDirection").value("DIR_TO_RC"))
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.failures", hasSize(0)));

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/jobs/latest/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(Long.toString(jobId)))
                .andExpect(jsonPath("$.state").value("COMPLETED"));

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs", hasSize(1)))
                .andExpect(jsonPath("$.jobs[0].jobId").value(Long.toString(jobId)))
                .andExpect(jsonPath("$.jobs[0].successCount").value(2));

        Integer latestJobId = jdbcTemplate.queryForObject(
                "SELECT latest_job_id FROM directory_sync_time WHERE account_id = ?",
                Integer.class,
                ACCOUNT);
        org.junit.jupiter.api.Assertions.assertEquals((int) jobId, latestJobId);
    }

    @Test
    void latestReportPathDoesNotMatchNumericJobIdRoute() throws Exception {
        long jobId = jobRepository.createJob(ACCOUNT, 1, 2, 1);
        jobRepository.updateJobState(jobId, "COMPLETED");

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/jobs/latest/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(Long.toString(jobId)));
    }

    @Test
    void returnsNotFoundForWrongAccountOrUnknownJob() throws Exception {
        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/jobs/99999/report"))
                .andExpect(status().isNotFound());

        long jobId = jobRepository.createJob(ACCOUNT, 1, 2, 1);
        mockMvc.perform(get("/dsg/v1/other-acct/jobs/" + jobId + "/report"))
                .andExpect(status().isNotFound());
    }

    @Test
    void includesFailedUsersInReport() throws Exception {
        long jobId = jobRepository.createJob(ACCOUNT, 1, 2, 1);
        jdbcTemplate.update(
                """
                        INSERT INTO job_detail (job_id, external_id, state_id, operation_id, comment)
                        VALUES (?, 'failed-user', 9, 1, 'Insufficient inventory')
                        """,
                jobId);
        jobRepository.updateJobState(jobId, "COMPLETED");

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/jobs/" + jobId + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.failures", hasSize(1)))
                .andExpect(jsonPath("$.failures[0].externalId").value("failed-user"))
                .andExpect(jsonPath("$.failures[0].operation").value("CREATE"))
                .andExpect(jsonPath("$.failures[0].comment").value("Insufficient inventory"));
    }
}
