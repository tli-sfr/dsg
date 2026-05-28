package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RcExtensionResponse(
        String uri,
        @JsonProperty("id") Long extensionId,
        @JsonProperty("extensionNumber") String extensionNumber,
        Contact contact) {

    private static final Pattern ACCOUNT_ID_IN_URI =
            Pattern.compile("/account/(\\d+)/extension/");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName) {
    }

    public String parseAccountId() {
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("RingCentral extension response missing uri");
        }
        Matcher matcher = ACCOUNT_ID_IN_URI.matcher(uri);
        if (!matcher.find()) {
            throw new IllegalStateException("Cannot parse account ID from extension uri: " + uri);
        }
        return matcher.group(1);
    }

    public String displayName() {
        if (contact == null) {
            return null;
        }
        String first = contact.firstName() != null ? contact.firstName().trim() : "";
        String last = contact.lastName() != null ? contact.lastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? null : combined;
    }
}
