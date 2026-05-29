package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RcOAuthService {

    private final RcOAuthProperties properties;
    private final RcOAuthClient oauthClient;
    private final AccountDirectoryAuthRepository authRepository;
    private final SecretEncryptionService encryptionService;
    private final RcAccessTokenService accessTokenService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, String> pendingStates = new ConcurrentHashMap<>();

    public RcOAuthService(
            RcOAuthProperties properties,
            RcOAuthClient oauthClient,
            AccountDirectoryAuthRepository authRepository,
            SecretEncryptionService encryptionService,
            RcAccessTokenService accessTokenService) {
        this.properties = properties;
        this.oauthClient = oauthClient;
        this.authRepository = authRepository;
        this.encryptionService = encryptionService;
        this.accessTokenService = accessTokenService;
    }

    public Map<String, String> buildAuthorizeUrl(String accountId) {
        requireConfigured();
        String state = createState(accountId);
        String authorizeUrl = UriComponentsBuilder
                .fromHttpUrl(properties.authorizeEndpoint())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return Map.of("authorizeUrl", authorizeUrl, "state", state);
    }

    public void exchangeAuthorizationCode(String accountId, String code, String state) {
        requireConfigured();
        validateState(accountId, state);
        RcTokenResponse tokenResponse = oauthClient.exchangeAuthorizationCode(code, properties.getRedirectUri());
        storeRefreshToken(accountId, tokenResponse);
        accessTokenService.cacheAuthorizationResponse(accountId, tokenResponse);
    }

    /**
     * Whether the server has a refresh token that can produce a valid access token right now.
     */
    public boolean hasValidRefreshToken(String accountId) {
        if (!authRepository.hasRcRefreshToken(accountId)) {
            return false;
        }
        return accessTokenService.obtainAccessToken(accountId).isPresent();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public void logout(String accountId) {
        authRepository.clearRcRefreshToken(accountId);
    }

    private void storeRefreshToken(String accountId, RcTokenResponse tokenResponse) {
        if (tokenResponse.refreshToken() == null || tokenResponse.refreshToken().isBlank()) {
            throw new IllegalStateException("RingCentral did not return a refresh token");
        }
        String encrypted = encryptionService.encrypt(tokenResponse.refreshToken());
        authRepository.updateRcRefreshToken(accountId, encrypted);
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "RingCentral OAuth is not configured. Set dsg.rc.client-id and dsg.rc.client-secret "
                            + "(see application-local.yml.example).");
        }
    }

    private String createState(String accountId) {
        byte[] random = new byte[16];
        secureRandom.nextBytes(random);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String state = accountId + ":" + nonce;
        pendingStates.put(state, accountId);
        return state;
    }

    private void validateState(String accountId, String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("Missing OAuth state parameter");
        }
        String decoded = URLDecoder.decode(state, StandardCharsets.UTF_8);
        int colon = decoded.indexOf(':');
        if (colon <= 0) {
            throw new IllegalArgumentException("Invalid OAuth state format");
        }
        String stateAccountId = decoded.substring(0, colon);
        if (!stateAccountId.equals(accountId)) {
            throw new IllegalArgumentException("OAuth state account mismatch");
        }
        pendingStates.remove(decoded);
    }
}
