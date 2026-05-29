package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.rc.RcAuthPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches short-lived RingCentral access tokens and refreshes them safely for background sync jobs.
 * Uses per-account locking so parallel job workers do not invalidate rotated refresh tokens.
 */
@Service
public class RcAccessTokenService implements RcAuthPort {

    private static final Logger log = LoggerFactory.getLogger(RcAccessTokenService.class);
    private static final long EXPIRY_BUFFER_SECONDS = 120;

    private final AccountDirectoryAuthRepository authRepository;
    private final SecretEncryptionService encryptionService;
    private final RcOAuthClient oauthClient;
    private final Map<String, CachedAccessToken> accessTokenCache = new ConcurrentHashMap<>();
    private final Map<String, Object> refreshLocks = new ConcurrentHashMap<>();

    public RcAccessTokenService(
            AccountDirectoryAuthRepository authRepository,
            SecretEncryptionService encryptionService,
            RcOAuthClient oauthClient) {
        this.authRepository = authRepository;
        this.encryptionService = encryptionService;
        this.oauthClient = oauthClient;
    }

    /**
     * Seeds the in-memory access-token cache after a successful OAuth code exchange.
     */
    public void cacheAuthorizationResponse(String accountId, RcTokenResponse response) {
        if (response.accessToken() == null || response.accessToken().isBlank()) {
            return;
        }
        long expiresInSeconds = response.expiresIn() != null && response.expiresIn() > 0
                ? response.expiresIn()
                : 3600;
        Instant expiresAt = Instant.now().plusSeconds(Math.max(60, expiresInSeconds - EXPIRY_BUFFER_SECONDS));
        accessTokenCache.put(accountId, new CachedAccessToken(response.accessToken(), expiresAt));
    }

    @Override
    public Optional<String> obtainAccessToken(String accountId) {
        CachedAccessToken cached = accessTokenCache.get(accountId);
        if (cached != null && cached.isValid()) {
            return Optional.of(cached.accessToken());
        }

        Object lock = refreshLocks.computeIfAbsent(accountId, ignored -> new Object());
        synchronized (lock) {
            cached = accessTokenCache.get(accountId);
            if (cached != null && cached.isValid()) {
                return Optional.of(cached.accessToken());
            }

            Optional<String> refreshTokenEnc = authRepository.findRcRefreshToken(accountId);
            if (refreshTokenEnc.isEmpty()) {
                return Optional.empty();
            }

            try {
                String accessToken = refreshAndCache(accountId, encryptionService.decrypt(refreshTokenEnc.get()));
                return Optional.of(accessToken);
            } catch (RcRefreshTokenInvalidException ex) {
                log.warn("RingCentral refresh token invalid for account {} — clearing stored credentials", accountId);
                authRepository.clearRcRefreshToken(accountId);
                accessTokenCache.remove(accountId);
                return Optional.empty();
            }
        }
    }

    private String refreshAndCache(String accountId, String refreshToken) {
        RcTokenResponse response;
        try {
            response = oauthClient.refreshAccessToken(refreshToken);
        } catch (IllegalStateException ex) {
            if (isInvalidGrant(ex)) {
                throw new RcRefreshTokenInvalidException(ex);
            }
            throw ex;
        }

        if (response.refreshToken() != null && !response.refreshToken().isBlank()) {
            authRepository.updateRcRefreshToken(accountId, encryptionService.encrypt(response.refreshToken()));
        }
        if (response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("RingCentral token refresh did not return an access token");
        }

        long expiresInSeconds = response.expiresIn() != null && response.expiresIn() > 0
                ? response.expiresIn()
                : 3600;
        Instant expiresAt = Instant.now().plusSeconds(Math.max(60, expiresInSeconds - EXPIRY_BUFFER_SECONDS));
        accessTokenCache.put(accountId, new CachedAccessToken(response.accessToken(), expiresAt));
        return response.accessToken();
    }

    private static boolean isInvalidGrant(Throwable ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("invalid_grant")) {
            return true;
        }
        Throwable cause = ex.getCause();
        if (cause instanceof HttpStatusCodeException httpEx) {
            String body = httpEx.getResponseBodyAsString();
            return body != null && body.contains("invalid_grant");
        }
        return false;
    }

    private record CachedAccessToken(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private static final class RcRefreshTokenInvalidException extends RuntimeException {
        RcRefreshTokenInvalidException(Throwable cause) {
            super(cause);
        }
    }
}
