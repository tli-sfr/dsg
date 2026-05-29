package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RcBulkAssignExtensionResult(
        @JsonProperty("id") Object id,
        String uri,
        String url) {

    private static final Pattern EXTENSION_ID_IN_PATH =
            Pattern.compile("/extension/(\\d+)(?:/|$)");

    public String resolveExtensionId() {
        if (id != null) {
            String text = id.toString().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return parseIdFromPath(uri != null ? uri : url);
    }

    private static String parseIdFromPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Matcher matcher = EXTENSION_ID_IN_PATH.matcher(path);
        return matcher.find() ? matcher.group(1) : null;
    }
}
