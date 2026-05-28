package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigurationPersistenceIT extends AbstractApiIntegrationTest {

    private static final String ACCOUNT = "acct-config";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void persistsSchedulerMappingsAndRule() throws Exception {
        mockMvc.perform(post("/dsg/v1/" + ACCOUNT + "/scheduler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incrementalEnabled": true,
                                  "cronExpression": "0 0 2 * * ?",
                                  "syncDirection": "DIR_TO_RC"
                                }
                                """))
                .andExpect(status().isOk());

        Integer syncRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM directory_sync_time WHERE account_id = ? AND job_type_id = 2",
                Integer.class,
                ACCOUNT);
        assertEquals(1, syncRows);

        mockMvc.perform(post("/dsg/v1/" + ACCOUNT + "/attribute-mapping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "basicMappings": [
                                    {
                                      "syncDirection": "DIR_TO_RC",
                                      "directoryAttribute": "profile.email",
                                      "rcAttribute": "email"
                                    }
                                  ],
                                  "customMappings": [
                                    {
                                      "syncDirection": "DIR_TO_RC",
                                      "directoryAttribute": "profile.costCenter",
                                      "rcAttribute": "CostCenter"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attribute_mapping WHERE account_id = ?",
                Integer.class,
                ACCOUNT));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM custom_attribute_mapping WHERE account_id = ?",
                Integer.class,
                ACCOUNT));

        mockMvc.perform(post("/dsg/v1/" + ACCOUNT + "/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleName": "Sales Team Provisioning",
                                  "priority": 1,
                                  "selectionExpression": {
                                    "match": "ALL",
                                    "criteria": [
                                      { "attribute": "user.department", "operator": "EQ", "value": "Sales" }
                                    ]
                                  },
                                  "licenseAssignments": [
                                    { "licenseType": "PRIMARY_LICENSE", "licenseId": "RingEX" }
                                  ],
                                  "areaCodeAssignment": {
                                    "areaCodeRuleType": "SPECIFIED_AREA_CODE",
                                    "areaCodeList": ["+1-650"]
                                  },
                                  "deviceAssignments": [
                                    { "deviceType": "RingCentral App" }
                                  ],
                                  "templateAssignments": [
                                    { "templateType": "CALL_HANDLING", "templateId": "sales-queue-routing" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());

        Long ruleId = jdbcTemplate.queryForObject(
                "SELECT id FROM provisioning_assignment_rule WHERE account_id = ? AND priority = 1",
                Long.class,
                ACCOUNT);
        assertTrue(ruleId > 0);

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM license_assignment WHERE rule_id = ?",
                Integer.class,
                ruleId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dl_area_code_assignment WHERE rule_id = ?",
                Integer.class,
                ruleId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device_assignment WHERE rule_id = ?",
                Integer.class,
                ruleId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM template_assignment WHERE rule_id = ?",
                Integer.class,
                ruleId));
    }
}
