package com.ringcentral.dsg.persistence.model;

public record DirectorySyncUserHashRecord(
        String accountId,
        int directoryTypeId,
        String externalId,
        String externalUserHash,
        String mailboxId,
        String rcUserHash) {}
