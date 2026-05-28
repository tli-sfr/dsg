package com.ringcentral.dsg.api.directory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryIdpUserProfile(
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName,
        @JsonProperty("givenName") String givenNameCamel,
        @JsonProperty("surname") String surname) {

    public String firstName() {
        if (givenName != null && !givenName.isBlank()) {
            return givenName.trim();
        }
        if (givenNameCamel != null && !givenNameCamel.isBlank()) {
            return givenNameCamel.trim();
        }
        return null;
    }

    public String lastName() {
        if (familyName != null && !familyName.isBlank()) {
            return familyName.trim();
        }
        if (surname != null && !surname.isBlank()) {
            return surname.trim();
        }
        return null;
    }
}
