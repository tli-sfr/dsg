package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DirectoryPersistenceIT extends AbstractApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SecretEncryptionService encryptionService;

    @Test
    void persistsDirectoryAndOAuthWithEncryptedSecret() throws Exception {
        mockMvc.perform(post("/dsg/v1/acct-db/directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "directoryType": "Okta",
                                  "etmSubscriberId": "etm-999"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/dsg/v1/acct-db/directory/oauth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "directoryType": "Okta",
                                  "authFlow": "CLIENT_CREDENTIALS",
                                  "clientId": "cid-okta-1",
                                  "clientSecret": "super-secret-value"
                                }
                                """))
                .andExpect(status().isOk());

        String storedSecret = jdbcTemplate.queryForObject(
                "SELECT client_secret_enc FROM account_directory_oauth WHERE account_id = ?",
                String.class,
                "acct-db");
        assertNotEquals("super-secret-value", storedSecret);
        assertEquals("super-secret-value", encryptionService.decrypt(storedSecret));

        Integer authCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_directory_auth WHERE account_id = ? AND oauth_config_id IS NOT NULL",
                Integer.class,
                "acct-db");
        assertEquals(1, authCount);

        mockMvc.perform(get("/dsg/v1/acct-db/directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directoryType").value("Okta"))
                .andExpect(jsonPath("$.connected").value(false));

        mockMvc.perform(get("/dsg/v1/acct-db/directory/oauth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callbackUrl").exists())
                .andExpect(jsonPath("$.connected").value(false));

        mockMvc.perform(post("/dsg/v1/acct-db/directory/oauth/test"))
                .andExpect(status().isBadRequest());

        String responseBody = mockMvc.perform(get("/dsg/v1/acct-db/directory/oauth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("ci****-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(!responseBody.contains("super-secret-value"));
    }
}
