package com.ringcentral.dsg.persistence.model;

import java.time.Instant;

public record AccountDirectoryOauthCredentialsRecord(
        long id,
        String accountId,
        int directoryTypeId,
        String directoryTypeName,
        String authFlow,
        String clientId,
        String clientSecretEnc,
        String azureTenantId,
        String oktaDomain,
        String scopes,
        String refreshTokenEnc,
        Instant accessTokenExpiresAt) {
}
