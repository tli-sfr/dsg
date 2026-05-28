package com.ringcentral.dsg.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DirectoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndRetrievesDirectoryConfiguration() throws Exception {
        mockMvc.perform(post("/dsg/v1/acct-2/directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "directoryType": "Okta",
                                  "etmSubscriberId": "etm-123"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/dsg/v1/acct-2/directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "directoryGroupId": "group-sales",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dsg/v1/acct-2/directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directoryType").value("Okta"))
                .andExpect(jsonPath("$.directoryGroupId").value("group-sales"))
                .andExpect(jsonPath("$.active").value(true));
    }
}
