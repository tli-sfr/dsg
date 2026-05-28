package com.ringcentral.dsg.api.controller;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.api.rc.RcApiClient;
import com.ringcentral.dsg.api.rc.RcExtensionResponse;
import com.ringcentral.dsg.api.rc.RcOAuthClient;
import com.ringcentral.dsg.api.rc.RcOAuthService;
import com.ringcentral.dsg.api.rc.RcTokenResponse;
import com.ringcentral.dsg.api.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RcOAuthControllerIT extends AbstractApiIntegrationTest {

    @DynamicPropertySource
    static void rcOAuthProperties(DynamicPropertyRegistry registry) {
        registry.add("dsg.rc.client-id", () -> "test-rc-client-id");
        registry.add("dsg.rc.client-secret", () -> "test-rc-client-secret");
        registry.add("dsg.rc.redirect-uri", () -> "http://localhost:5173/oauth/callback");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SecretEncryptionService encryptionService;

    @Autowired
    private RcOAuthService rcOAuthService;

    @MockBean
    private RcOAuthClient rcOAuthClient;

    @MockBean
    private RcApiClient rcApiClient;

    @BeforeEach
    void cleanAuthRows() {
        jdbcTemplate.update("DELETE FROM account_directory_auth WHERE account_id IN (?, ?)", "acct-rc", "123456789");
    }

    @Test
    void returnsConfiguredStatusAndAuthorizeUrl() throws Exception {
        mockMvc.perform(get("/dsg/v1/acct-rc/rc/oauth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.connected").value(false));

        mockMvc.perform(get("/dsg/v1/acct-rc/rc/oauth/authorize-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizeUrl").exists())
                .andExpect(jsonPath("$.state").exists());
    }

    @Test
    void exchangesCodeAndPersistsEncryptedRefreshToken() throws Exception {
        when(rcOAuthClient.exchangeAuthorizationCode(eq("auth-code-123"), anyString()))
                .thenReturn(new RcTokenResponse(
                        "access-token",
                        "refresh-token-plain",
                        3600L,
                        604800L,
                        "bearer",
                        "ReadAccounts"));

        String state = rcOAuthService.buildAuthorizeUrl("acct-rc").get("state");

        mockMvc.perform(post("/dsg/v1/acct-rc/rc/oauth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "auth-code-123",
                                  "state": "%s"
                                }
                                """.formatted(state)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));

        String stored = jdbcTemplate.queryForObject(
                "SELECT rc_refresh_token FROM account_directory_auth WHERE account_id = ?",
                String.class,
                "acct-rc");
        assertNotEquals("refresh-token-plain", stored);
        assertEquals("refresh-token-plain", encryptionService.decrypt(stored));

        mockMvc.perform(get("/dsg/v1/acct-rc/rc/oauth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    void returnsRcAccountIdFromReadExtension() throws Exception {
        when(rcOAuthClient.refreshAccessToken(anyString()))
                .thenReturn(new RcTokenResponse(
                        "access-token",
                        "refresh-token-plain",
                        3600L,
                        604800L,
                        "bearer",
                        "ReadAccounts"));
        when(rcApiClient.readCurrentExtension("access-token"))
                .thenReturn(new RcExtensionResponse(
                        "https://platform.example/restapi/v1.0/account/123456789/extension/987654321",
                        987654321L,
                        "101",
                        new RcExtensionResponse.Contact("Jane", "Admin")));

        jdbcTemplate.update("""
                INSERT INTO account_directory_auth (account_id, directory_type_id, rc_refresh_token, active)
                VALUES (?, 1, ?, 0)
                """, "acct-rc", encryptionService.encrypt("refresh-token-plain"));

        mockMvc.perform(get("/dsg/v1/acct-rc/rc/oauth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rcAccountId").value("123456789"))
                .andExpect(jsonPath("$.extensionId").value(987654321))
                .andExpect(jsonPath("$.extensionNumber").value("101"))
                .andExpect(jsonPath("$.extensionName").value("Jane Admin"));

        Integer migratedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_directory_auth WHERE account_id = ? AND rc_refresh_token IS NOT NULL",
                Integer.class,
                "123456789");
        assertEquals(1, migratedCount);
    }
}
