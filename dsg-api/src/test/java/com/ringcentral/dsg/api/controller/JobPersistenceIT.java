package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobPersistenceIT extends AbstractApiIntegrationTest {

    private static final String ACCOUNT = "acct-job";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private MessageQueuePort messageQueuePort;

    @BeforeEach
    void configureDirectory() throws Exception {
        mockMvc.perform(post("/dsg/v1/" + ACCOUNT + "/directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "directoryType": "Okta" }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void persistsPendingJobAndPublishesQueueMessage() throws Exception {
        String response = mockMvc.perform(post("/dsg/v1/" + ACCOUNT + "/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobType": "INCREMENTAL",
                                  "externalUserIds": []
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = response.replaceAll(".*\"jobId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        Integer pendingJobs = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM job j
                        JOIN job_state s ON s.id = j.state_id
                        WHERE j.account_id = ? AND j.id = ? AND s.state = 'PENDING'
                        """,
                Integer.class,
                ACCOUNT,
                Long.parseLong(jobId));
        assertEquals(1, pendingJobs);

        ArgumentCaptor<JobMessage> messageCaptor = ArgumentCaptor.forClass(JobMessage.class);
        verify(messageQueuePort).publishJob(messageCaptor.capture());
        assertEquals(jobId, messageCaptor.getValue().jobId());
        assertEquals(ACCOUNT, messageCaptor.getValue().accountId());
        assertEquals("INCREMENTAL", messageCaptor.getValue().jobType());

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/jobs/" + jobId + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.successCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0));
    }
}
