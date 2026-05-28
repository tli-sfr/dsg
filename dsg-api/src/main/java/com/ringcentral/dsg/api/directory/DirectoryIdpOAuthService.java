package com.ringcentral.dsg.api.directory;

import com.ringcentral.dsg.api.config.AppProperties;
import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthConfigResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthTokenRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryGroupItem;
import com.ringcentral.dsg.persistence.model.AccountDirectoryOauthCredentialsRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import com.ringcentral.dsg.persistence.repo.DirectoryTypeRepository;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DirectoryIdpOAuthService {

    private final AppProperties appProperties;
    private final DirectoryIdpOAuthClient idpOAuthClient;
    private final AccountDirectoryOauthRepository oauthRepository;
    private final AccountDirectoryAuthRepository authRepository;
    private final DirectoryTypeRepository directoryTypeRepository;
    private final SecretEncryptionService encryptionService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, String> pendingStates = new ConcurrentHashMap<>();

    public DirectoryIdpOAuthService(
            AppProperties appProperties,
            DirectoryIdpOAuthClient idpOAuthClient,
            AccountDirectoryOauthRepository oauthRepository,
            AccountDirectoryAuthRepository authRepository,
            DirectoryTypeRepository directoryTypeRepository,
            SecretEncryptionService encryptionService) {
        this.appProperties = appProperties;
        this.idpOAuthClient = idpOAuthClient;
        this.oauthRepository = oauthRepository;
        this.authRepository = authRepository;
        this.directoryTypeRepository = directoryTypeRepository;
        this.encryptionService = encryptionService;
    }

    public DirectoryOAuthConfigResponse getConfig(String accountId) {
        return oauthRepository.findCredentialsByAccountId(accountId)
                .map(creds -> new DirectoryOAuthConfigResponse(
                        creds.directoryTypeName(),
                        creds.authFlow(),
                        maskClientId(creds.clientId()),
                        creds.azureTenantId(),
                        creds.oktaDomain(),
                        appProperties.directoryOAuthCallbackUrl(),
                        creds.refreshTokenEnc() != null,
                        creds.accessTokenExpiresAt()))
                .orElse(new DirectoryOAuthConfigResponse(
                        null, null, null, null, null,
                        appProperties.directoryOAuthCallbackUrl(),
                        false, null));
    }

    public void saveCredentials(String accountId, DirectoryOAuthRequest request) {
        int directoryTypeId = resolveDirectoryTypeId(request.directoryType().name());
        String encryptedSecret = encryptionService.encrypt(request.clientSecret());
        long oauthId = oauthRepository.upsertOAuthConfig(
                accountId,
                directoryTypeId,
                request.authFlow(),
                request.clientId(),
                encryptedSecret,
                request.azureTenantId(),
                request.oktaDomain(),
                defaultScopes(request));
        if (authRepository.findByAccountId(accountId).isEmpty()) {
            authRepository.upsert(accountId, directoryTypeId, null);
        }
        authRepository.linkOAuthConfig(accountId, oauthId);
    }

    public Map<String, String> buildAuthorizeUrl(String accountId) {
        AccountDirectoryOauthCredentialsRecord creds = oauthRepository.findCredentialsByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("Save IDP credentials before connecting"));
        String redirectUri = appProperties.directoryOAuthCallbackUrl();
        String state = createState(accountId, creds.directoryTypeName());
        String authorizeUrl = switch (creds.directoryTypeName()) {
            case "Azure" -> {
                requireField(creds.azureTenantId(), "Azure tenant ID");
                yield idpOAuthClient.buildAzureAuthorizeUrl(
                        creds.azureTenantId(), creds.clientId(), redirectUri, creds.scopes(), state);
            }
            case "Okta" -> {
                requireField(creds.oktaDomain(), "Okta domain");
                yield idpOAuthClient.buildOktaAuthorizeUrl(
                        creds.oktaDomain(), creds.clientId(), redirectUri, creds.scopes(), state);
            }
            default -> throw new IllegalArgumentException(
                    "3-legged OAuth connect is not supported for " + creds.directoryTypeName() + " yet");
        };
        return Map.of("authorizeUrl", authorizeUrl, "state", state);
    }

    public void exchangeAuthorizationCode(String accountId, DirectoryOAuthTokenRequest request) {
        validateState(accountId, request.state());
        AccountDirectoryOauthCredentialsRecord creds = oauthRepository.findCredentialsByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("IDP OAuth config not found"));
        String redirectUri = appProperties.directoryOAuthCallbackUrl();
        String clientSecret = encryptionService.decrypt(creds.clientSecretEnc());
        DirectoryIdpTokenResponse tokenResponse = switch (creds.directoryTypeName()) {
            case "Azure" -> idpOAuthClient.exchangeAzureCode(
                    creds.azureTenantId(), creds.clientId(), clientSecret, redirectUri, request.code());
            case "Okta" -> idpOAuthClient.exchangeOktaCode(
                    creds.oktaDomain(), creds.clientId(), clientSecret, redirectUri, request.code());
            default -> throw new IllegalArgumentException("Unsupported directory type for token exchange");
        };
        storeTokens(accountId, tokenResponse);
    }

    public void disconnect(String accountId) {
        oauthRepository.clearOAuthTokens(accountId);
    }

    public boolean isConnected(String accountId) {
        return oauthRepository.hasRefreshToken(accountId);
    }

    public List<DirectoryGroupItem> listGroups(String accountId) {
        if (!isConnected(accountId)) {
            throw new IllegalStateException("Connect to the directory before selecting a group");
        }
        String directoryType = oauthRepository.findCredentialsByAccountId(accountId)
                .map(AccountDirectoryOauthCredentialsRecord::directoryTypeName)
                .orElse("Unknown");
        return switch (directoryType) {
            case "Azure" -> List.of(
                    new DirectoryGroupItem("all-users", "All Users"),
                    new DirectoryGroupItem("engineering", "Engineering"),
                    new DirectoryGroupItem("sales", "Sales"));
            case "Okta" -> List.of(
                    new DirectoryGroupItem("everyone", "Everyone"),
                    new DirectoryGroupItem("it-admins", "IT Admins"),
                    new DirectoryGroupItem("field-team", "Field Team"));
            default -> List.of(new DirectoryGroupItem("default", "Default group"));
        };
    }

    private void storeTokens(String accountId, DirectoryIdpTokenResponse tokenResponse) {
        if (tokenResponse.refreshToken() == null || tokenResponse.refreshToken().isBlank()) {
            throw new IllegalStateException("Directory provider did not return a refresh token");
        }
        Instant expiresAt = tokenResponse.expiresIn() != null
                ? Instant.now().plusSeconds(tokenResponse.expiresIn())
                : Instant.now().plusSeconds(3600);
        oauthRepository.updateOAuthTokens(
                accountId,
                encryptionService.encrypt(tokenResponse.refreshToken()),
                encryptionService.encrypt(tokenResponse.accessToken()),
                expiresAt);
    }

    private String createState(String accountId, String directoryType) {
        byte[] random = new byte[16];
        secureRandom.nextBytes(random);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String state = accountId + ":" + directoryType + ":" + nonce;
        pendingStates.put(state, accountId);
        return state;
    }

    private void validateState(String accountId, String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("Missing OAuth state parameter");
        }
        String decoded = URLDecoder.decode(state, StandardCharsets.UTF_8);
        int firstColon = decoded.indexOf(':');
        int lastColon = decoded.lastIndexOf(':');
        if (firstColon <= 0 || lastColon <= firstColon) {
            throw new IllegalArgumentException("Invalid OAuth state format");
        }
        String stateAccountId = decoded.substring(0, firstColon);
        if (!stateAccountId.equals(accountId)) {
            throw new IllegalArgumentException("OAuth state account mismatch");
        }
        pendingStates.remove(decoded);
    }

    private int resolveDirectoryTypeId(String directoryTypeName) {
        return directoryTypeRepository.findIdByName(directoryTypeName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown directory type: " + directoryTypeName));
    }

    private static String defaultScopes(DirectoryOAuthRequest request) {
        if (request.scopes() != null && !request.scopes().isBlank()) {
            return request.scopes();
        }
        return switch (request.directoryType().name()) {
            case "Azure" -> "offline_access openid https://graph.microsoft.com/Group.Read.All https://graph.microsoft.com/User.Read.All";
            case "Okta" -> "openid offline_access okta.groups.read okta.users.read";
            default -> "openid offline_access";
        };
    }

    private static void requireField(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    private static String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 4) {
            return "****";
        }
        return clientId.substring(0, 2) + "****" + clientId.substring(clientId.length() - 2);
    }
}
