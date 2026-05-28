package com.ringcentral.dsg.api.service;

import com.ringcentral.dsg.api.directory.DirectoryIdpOAuthService;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryConfigRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthConnectResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthConfigResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthTokenRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryGroupsResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryUpdateRequest;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import com.ringcentral.dsg.persistence.repo.DirectoryTypeRepository;
import com.ringcentral.dsg.persistence.service.EffectiveDirectoryTypeResolver;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DirectoryConfigService {

    private final DirectoryTypeRepository directoryTypeRepository;
    private final AccountDirectoryAuthRepository authRepository;
    private final AccountDirectoryOauthRepository oauthRepository;
    private final DirectoryIdpOAuthService directoryIdpOAuthService;
    private final EffectiveDirectoryTypeResolver effectiveDirectoryTypeResolver;

    public DirectoryConfigService(
            DirectoryTypeRepository directoryTypeRepository,
            AccountDirectoryAuthRepository authRepository,
            AccountDirectoryOauthRepository oauthRepository,
            DirectoryIdpOAuthService directoryIdpOAuthService,
            EffectiveDirectoryTypeResolver effectiveDirectoryTypeResolver) {
        this.directoryTypeRepository = directoryTypeRepository;
        this.authRepository = authRepository;
        this.oauthRepository = oauthRepository;
        this.directoryIdpOAuthService = directoryIdpOAuthService;
        this.effectiveDirectoryTypeResolver = effectiveDirectoryTypeResolver;
    }

    public void createDirectory(String accountId, DirectoryConfigRequest request) {
        int directoryTypeId = resolveDirectoryTypeId(request.directoryType().name());
        authRepository.upsert(accountId, directoryTypeId, request.etmSubscriberId());
    }

    public void updateDirectory(String accountId, DirectoryUpdateRequest request) {
        authRepository.update(
                accountId,
                request.directoryGroupId(),
                request.directoryGroupName(),
                request.active());
    }

    public DirectoryResponse getDirectory(String accountId) {
        return authRepository.findByAccountId(accountId)
                .map(record -> new DirectoryResponse(
                        effectiveDirectoryTypeResolver.resolveDirectoryTypeName(accountId),
                        record.directoryGroupId(),
                        record.directoryGroupName(),
                        record.active(),
                        oauthRepository.hasRefreshToken(accountId)))
                .orElse(new DirectoryResponse("Unknown", null, null, false, false));
    }

    public void putOAuth(String accountId, DirectoryOAuthRequest request) {
        directoryIdpOAuthService.saveCredentials(accountId, request);
    }

    public DirectoryOAuthResponse getOAuth(String accountId) {
        return oauthRepository.findByAccountId(accountId)
                .map(record -> new DirectoryOAuthResponse(
                        record.directoryTypeName(),
                        maskClientId(record.clientId()),
                        record.accessTokenExpiresAt() != null
                                ? record.accessTokenExpiresAt()
                                : Instant.now().plusSeconds(3600)))
                .orElse(new DirectoryOAuthResponse(null, null, null));
    }

    public boolean testOAuth(String accountId) {
        return directoryIdpOAuthService.isConnected(accountId);
    }

    public DirectoryOAuthConfigResponse getOAuthConfig(String accountId) {
        return directoryIdpOAuthService.getConfig(accountId);
    }

    public java.util.Map<String, String> getDirectoryAuthorizeUrl(String accountId) {
        return directoryIdpOAuthService.buildAuthorizeUrl(accountId);
    }

    public DirectoryOAuthConnectResponse exchangeDirectoryOAuthToken(String accountId, DirectoryOAuthTokenRequest request) {
        return directoryIdpOAuthService.exchangeAuthorizationCode(accountId, request);
    }

    public void disconnectDirectoryOAuth(String accountId) {
        directoryIdpOAuthService.disconnect(accountId);
    }

    public DirectoryGroupsResponse listDirectoryGroups(String accountId, String search) {
        return new DirectoryGroupsResponse(directoryIdpOAuthService.searchGroups(accountId, search));
    }

    private int resolveDirectoryTypeId(String directoryTypeName) {
        return directoryTypeRepository.findIdByName(directoryTypeName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown directory type: " + directoryTypeName));
    }

    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 4) {
            return "****";
        }
        return clientId.substring(0, 2) + "****" + clientId.substring(clientId.length() - 2);
    }
}
