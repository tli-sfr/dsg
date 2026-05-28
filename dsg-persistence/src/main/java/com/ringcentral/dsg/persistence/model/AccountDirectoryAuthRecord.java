package com.ringcentral.dsg.persistence.model;

public record AccountDirectoryAuthRecord(
        long id,
        String accountId,
        int directoryTypeId,
        String directoryTypeName,
        String directoryGroupId,
        String etmSubscriberId,
        Long oauthConfigId,
        boolean active) {
}
