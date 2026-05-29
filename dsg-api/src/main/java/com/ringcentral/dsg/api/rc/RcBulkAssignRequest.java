package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.directory.DirectoryUser;
import java.util.List;

public record RcBulkAssignRequest(List<RcBulkAssignItem> items) {

    public static RcBulkAssignRequest forDirectoryUser(DirectoryUser user) {
        RcBulkAssignContact contact = RcMappedContactBuilder.buildContact(user);
        String directoryRef = requireExternalId(user);
        return new RcBulkAssignRequest(List.of(new RcBulkAssignItem(new RcBulkAssignExtension(
                contact, List.of(new RcDirectoryReference("CustomerDirectoryId", directoryRef))))));
    }

    private static String requireExternalId(DirectoryUser user) {
        if (user.externalId() == null || user.externalId().isBlank()) {
            throw new IllegalStateException(
                    "Directory user externalId is required for CustomerDirectoryId reference");
        }
        return user.externalId();
    }
}
