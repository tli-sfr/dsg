package com.ringcentral.dsg.persistence.service;

import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import org.springframework.stereotype.Component;

/**
 * Active IDP type for an account: IDP OAuth config overrides stale {@code account_directory_auth.directory_type_id}.
 */
@Component
public class EffectiveDirectoryTypeResolver {

    private final AccountDirectoryOauthRepository oauthRepository;
    private final AccountDirectoryAuthRepository authRepository;

    public EffectiveDirectoryTypeResolver(
            AccountDirectoryOauthRepository oauthRepository,
            AccountDirectoryAuthRepository authRepository) {
        this.oauthRepository = oauthRepository;
        this.authRepository = authRepository;
    }

    public int resolveDirectoryTypeId(String accountId) {
        return oauthRepository.findByAccountId(accountId)
                .map(record -> record.directoryTypeId())
                .or(() -> authRepository.findByAccountId(accountId)
                        .map(record -> record.directoryTypeId()))
                .orElseThrow(() -> new IllegalStateException(
                        "Directory is not configured for account: " + accountId));
    }

    public String resolveDirectoryTypeName(String accountId) {
        return oauthRepository.findByAccountId(accountId)
                .map(record -> record.directoryTypeName())
                .or(() -> authRepository.findByAccountId(accountId)
                        .map(record -> record.directoryTypeName()))
                .orElse("Unknown");
    }
}
