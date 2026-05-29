package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcBulkAssignExtension(
        RcBulkAssignContact contact, List<RcDirectoryReference> references) {
}
