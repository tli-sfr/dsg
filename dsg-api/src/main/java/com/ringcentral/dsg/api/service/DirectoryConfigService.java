package com.ringcentral.dsg.api.service;

import com.ringcentral.dsg.api.crypto.SecretEncryptionService;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryConfigRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthRequest;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryOAuthResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryResponse;
import com.ringcentral.dsg.api.model.AdminApiModels.DirectoryUpdateRequest;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import com.ringcentral.dsg.persistence.repo.DirectoryTypeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DirectoryConfigService {

    private final DirectoryTypeRepository directoryTypeRepository;
    private final AccountDirectoryAuthRepository authRepository;
    private final AccountDirectoryOauthRepository oauthRepository;
    private final SecretEncryptionService encryptionService;

    public DirectoryConfigService(
            DirectoryTypeRepository directoryTypeRepository,
            AccountDirectoryAuthRepository authRepository,
            AccountDirectoryOauthRepository oauthRepository,
            SecretEncryptionService encryptionService) {
        this.directoryTypeRepository = directoryTypeRepository;
        this.authRepository = authRepository;
        this.oauthRepository = oauthRepository;
        this.encryptionService = encryptionService;
    }

    public void createDirectory(String accountId, DirectoryConfigRequest request) {
        int directoryTypeId = resolveDirectoryTypeId(request.directoryType().name());
        authRepository.upsert(accountId, directoryTypeId, request.etmSubscriberId());
    }

    public void updateDirectory(String accountId, DirectoryUpdateRequest request) {
        authRepository.update(accountId, request.directoryGroupId(), request.active());
    }

    public DirectoryResponse getDirectory(String accountId) {
        return authRepository.findByAccountId(accountId)
                .map(record -> new DirectoryResponse(
                        record.directoryTypeName(),
                        record.directoryGroupId(),
                        record.active(),
                        record.oauthConfigId() != null || oauthRepository.hasCredentials(accountId)))
                .orElse(new DirectoryResponse("Unknown", null, false, false));
    }

    public void putOAuth(String accountId, DirectoryOAuthRequest request) {
        int directoryTypeId = resolveDirectoryTypeId(request.directoryType().name());
        String encryptedSecret = encryptionService.encrypt(request.clientSecret());
        long oauthId = oauthRepository.upsert(
                accountId,
                directoryTypeId,
                request.authFlow(),
                request.clientId(),
                encryptedSecret);
        if (authRepository.findByAccountId(accountId).isEmpty()) {
            authRepository.upsert(accountId, directoryTypeId, null);
        }
        authRepository.linkOAuthConfig(accountId, oauthId);
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
        return oauthRepository.hasCredentials(accountId);
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
