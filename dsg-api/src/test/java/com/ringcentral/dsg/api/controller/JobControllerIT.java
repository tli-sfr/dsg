package com.ringcentral.dsg.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIT {

    @Autowired
    private MockMvc mockMvc;

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
