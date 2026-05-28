package com.ringcentral.dsg.persistence.model;

import java.time.Instant;

public record AccountDirectoryOauthRecord(
        long id,
        String accountId,
        int directoryTypeId,
        String directoryTypeName,
        String authFlow,
        String clientId,
        Instant accessTokenExpiresAt) {
}
