package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
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
class ConfigurationReadIT extends AbstractApiIntegrationTest {

    private static final String ACCOUNT = "acct-read";

    @Autowired
    private MockMvc mockMvc;

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
    void listsRulesAndManagesDeprovisioning() throws Exception {
        mockMvc.perform(post("/dsg/v1/" + ACCOUNT + "/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleName": "Sales",
                                  "priority": 1,
                                  "selectionExpression": {
                                    "match": "ALL",
                                    "criteria": [
                                      { "attribute": "user.department", "operator": "EQ", "value": "Sales" }
                                    ]
                                  },
                                  "licenseAssignments": [
                                    { "licenseType": "PRIMARY_LICENSE", "licenseId": "RingEX" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules.length()").value(1))
                .andExpect(jsonPath("$.rules[0].ruleName").value("Sales"));

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/deprovisioning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deprovisioningType").value("FULL_DELETE"));

        mockMvc.perform(put("/dsg/v1/" + ACCOUNT + "/deprovisioning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "deprovisioningType": "DISABLE_ONLY" }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dsg/v1/" + ACCOUNT + "/deprovisioning"))
                .andExpect(jsonPath("$.deprovisioningType").value("DISABLE_ONLY"));
    }
}
