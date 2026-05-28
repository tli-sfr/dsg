package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import com.ringcentral.dsg.messaging.JobMessage;
import com.ringcentral.dsg.messaging.MessageQueuePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIT extends AbstractApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageQueuePort messageQueuePort;

    @BeforeEach
    void configureDirectoryForJobAccounts() throws Exception {
        for (String accountId : new String[] {"acct-1", "acct-3"}) {
            mockMvc.perform(post("/dsg/v1/" + accountId + "/directory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "directoryType": "Okta" }
                                    """))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void createsJobThenReturnsConflictForConcurrentRequest() throws Exception {
        String request = """
                {
                  "jobType": "FULL",
                  "externalUserIds": []
                }
                """;

        mockMvc.perform(post("/dsg/v1/acct-1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("PENDING"));

        mockMvc.perform(post("/dsg/v1/acct-1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_ALREADY_RUNNING"));

        verify(messageQueuePort, times(1)).publishJob(org.mockito.ArgumentMatchers.any(JobMessage.class));
    }

    @Test
    void rejectsUnknownJobType() throws Exception {
        String badRequest = """
                {
                  "jobType": "UNKNOWN",
                  "externalUserIds": []
                }
                """;

        mockMvc.perform(post("/dsg/v1/acct-3/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
