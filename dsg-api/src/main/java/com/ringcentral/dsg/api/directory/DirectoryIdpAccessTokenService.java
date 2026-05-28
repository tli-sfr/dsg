package com.ringcentral.dsg.api.directory;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.persistence.model.AccountDirectoryOauthCredentialsRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Returns a valid directory IDP access token, refreshing with the stored refresh token when needed.
 */
@Service
public class DirectoryIdpAccessTokenService {

    private static final long EXPIRY_BUFFER_SECONDS = 60;

    private final AccountDirectoryOauthRepository oauthRepository;
    private final DirectoryIdpOAuthClient idpOAuthClient;
    private final SecretEncryptionService encryptionService;
    private final Map<String, Object> refreshLocks = new ConcurrentHashMap<>();

    public DirectoryIdpAccessTokenService(
            AccountDirectoryOauthRepository oauthRepository,
            DirectoryIdpOAuthClient idpOAuthClient,
            SecretEncryptionService encryptionService) {
        this.oauthRepository = oauthRepository;
        this.idpOAuthClient = idpOAuthClient;
        this.encryptionService = encryptionService;
    }

    public String requireAccessToken(String accountId) {
        AccountDirectoryOauthCredentialsRecord creds = oauthRepository.findCredentialsByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("IDP OAuth config not found"));
        if (creds.refreshTokenEnc() == null || creds.refreshTokenEnc().isBlank()) {
            throw new IllegalStateException("Connect to the directory before using directory APIs");
        }

        if (hasUsableAccessToken(creds)) {
            return encryptionService.decrypt(creds.accessTokenEnc());
        }

        Object lock = refreshLocks.computeIfAbsent(accountId, ignored -> new Object());
        synchronized (lock) {
            AccountDirectoryOauthCredentialsRecord latest = oauthRepository.findCredentialsByAccountId(accountId)
                    .orElseThrow(() -> new IllegalStateException("IDP OAuth config not found"));
            if (hasUsableAccessToken(latest)) {
                return encryptionService.decrypt(latest.accessTokenEnc());
            }
            return refreshAndStore(accountId, latest);
        }
    }

    private boolean hasUsableAccessToken(AccountDirectoryOauthCredentialsRecord creds) {
        if (creds.accessTokenEnc() == null || creds.accessTokenEnc().isBlank()) {
            return false;
        }
        Instant expiresAt = creds.accessTokenExpiresAt();
        if (expiresAt == null) {
            return true;
        }
        return expiresAt.isAfter(Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS));
    }

    private String refreshAndStore(String accountId, AccountDirectoryOauthCredentialsRecord creds) {
        String refreshToken = encryptionService.decrypt(creds.refreshTokenEnc());
        String clientSecret = encryptionService.decrypt(creds.clientSecretEnc());
        DirectoryIdpTokenResponse tokenResponse;
        try {
            tokenResponse = switch (creds.directoryTypeName()) {
                case "Okta" -> idpOAuthClient.refreshOktaToken(
                        creds.oktaDomain(), creds.clientId(), clientSecret, refreshToken);
                case "Azure" -> idpOAuthClient.refreshAzureToken(
                        creds.azureTenantId(), creds.clientId(), clientSecret, refreshToken);
                default -> throw new IllegalArgumentException(
                        "Token refresh is not supported for " + creds.directoryTypeName());
            };
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
                oauthRepository.clearOAuthTokens(accountId);
                throw new IllegalStateException(
                        "Directory session expired — reconnect to the identity provider", ex);
            }
            throw new IllegalStateException("Directory token refresh failed: " + ex.getMessage(), ex);
        }

        if (tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new IllegalStateException("Directory token refresh did not return an access token");
        }
        String refreshToStore = tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().isBlank()
                ? tokenResponse.refreshToken()
                : refreshToken;
        Instant expiresAt = tokenResponse.expiresIn() != null
                ? Instant.now().plusSeconds(tokenResponse.expiresIn())
                : Instant.now().plusSeconds(3600);
        oauthRepository.updateOAuthTokens(
                accountId,
                encryptionService.encrypt(refreshToStore),
                encryptionService.encrypt(tokenResponse.accessToken()),
                expiresAt);
        return tokenResponse.accessToken();
    }
}
