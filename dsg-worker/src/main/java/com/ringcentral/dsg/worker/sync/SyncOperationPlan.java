package com.ringcentral.dsg.worker.sync;

import com.ringcentral.dsg.directory.DirectoryUser;

public record SyncOperationPlan(
        SyncOperation operation, DirectoryUser mappedUser, String mailboxId, String externalUserHash) {}
