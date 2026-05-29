package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RcAccessTokenServiceTest {

    @Mock
    private AccountDirectoryAuthRepository authRepository;

    @Mock
    private SecretEncryptionService encryptionService;

    @Mock
    private RcOAuthClient oauthClient;

    private RcAccessTokenService service;

    @BeforeEach
    void setUp() {
        service = new RcAccessTokenService(authRepository, encryptionService, oauthClient);
    }

    @Test
    void cachesAccessTokenAndReusesUntilExpiry() {
        when(authRepository.findRcRefreshToken("acct-1")).thenReturn(Optional.of("enc-refresh"));
        when(encryptionService.decrypt("enc-refresh")).thenReturn("refresh-1");
        when(oauthClient.refreshAccessToken("refresh-1"))
                .thenReturn(new RcTokenResponse("access-1", "refresh-2", 3600L, null, "Bearer", null));

        assertEquals("access-1", service.obtainAccessToken("acct-1").orElseThrow());
        assertEquals("access-1", service.obtainAccessToken("acct-1").orElseThrow());

        verify(oauthClient, times(1)).refreshAccessToken("refresh-1");
        verify(authRepository).updateRcRefreshToken(eq("acct-1"), any());
    }

    @Test
    void clearsStoredRefreshTokenOnInvalidGrant() {
        when(authRepository.findRcRefreshToken("acct-1")).thenReturn(Optional.of("enc-refresh"));
        when(encryptionService.decrypt("enc-refresh")).thenReturn("stale-refresh");
        when(oauthClient.refreshAccessToken("stale-refresh"))
                .thenThrow(new IllegalStateException(
                        "RingCentral token request failed (400): invalid_grant — refresh token expired",
                        httpBadRequest("{\"error\":\"invalid_grant\"}")));

        assertTrue(service.obtainAccessToken("acct-1").isEmpty());
        verify(authRepository).clearRcRefreshToken("acct-1");
    }

    @Test
    void seedsCacheFromAuthorizationResponse() {
        service.cacheAuthorizationResponse(
                "acct-1", new RcTokenResponse("access-from-code", "refresh-1", 3600L, null, "Bearer", null));

        assertEquals("access-from-code", service.obtainAccessToken("acct-1").orElseThrow());
        verify(oauthClient, never()).refreshAccessToken(any());
    }

    private static HttpClientErrorException httpBadRequest(String body) {
        return HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                org.springframework.http.HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }
}
