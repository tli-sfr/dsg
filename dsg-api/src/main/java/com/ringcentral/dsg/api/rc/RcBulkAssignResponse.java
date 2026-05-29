package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RcBulkAssignResponse(@JsonProperty("items") List<RcBulkAssignResultItem> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RcBulkAssignResultItem(
            Boolean successful, @JsonProperty("extension") RcBulkAssignExtensionResult extension) {
    }

    public String firstExtensionId() {
        if (items == null || items.isEmpty()) {
            return null;
        }
        RcBulkAssignResultItem item = items.get(0);
        if (item == null || item.extension() == null) {
            return null;
        }
        return item.extension().resolveExtensionId();
    }

    public Boolean firstItemSuccessfulFlag() {
        if (items == null || items.isEmpty()) {
            return null;
        }
        RcBulkAssignResultItem item = items.get(0);
        return item != null ? item.successful() : null;
    }

    /** True when RC returned an extension id, or explicitly marked the item successful. */
    public boolean isProvisionSuccess() {
        String extensionId = firstExtensionId();
        if (extensionId != null) {
            return true;
        }
        return Boolean.TRUE.equals(firstItemSuccessfulFlag());
    }
}
