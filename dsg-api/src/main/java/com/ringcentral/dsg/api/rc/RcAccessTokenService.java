package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.rc.RcAuthPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Obtains short-lived RingCentral access tokens from the stored refresh token during sync jobs.
 */
@Service
public class RcAccessTokenService implements RcAuthPort {

    private final AccountDirectoryAuthRepository authRepository;
    private final SecretEncryptionService encryptionService;
    private final RcOAuthClient oauthClient;

    public RcAccessTokenService(
            AccountDirectoryAuthRepository authRepository,
            SecretEncryptionService encryptionService,
            RcOAuthClient oauthClient) {
        this.authRepository = authRepository;
        this.encryptionService = encryptionService;
        this.oauthClient = oauthClient;
    }

    @Override
    public Optional<String> obtainAccessToken(String accountId) {
        return authRepository.findRcRefreshToken(accountId)
                .map(encryptionService::decrypt)
                .map(refreshToken -> refreshAccessToken(accountId, refreshToken));
    }

    private String refreshAccessToken(String accountId, String refreshToken) {
        RcTokenResponse response = oauthClient.refreshAccessToken(refreshToken);
        if (response.refreshToken() != null && !response.refreshToken().isBlank()
                && !response.refreshToken().equals(refreshToken)) {
            authRepository.updateRcRefreshToken(
                    accountId, encryptionService.encrypt(response.refreshToken()));
        }
        if (response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("RingCentral token refresh did not return an access token");
        }
        return response.accessToken();
    }
}
