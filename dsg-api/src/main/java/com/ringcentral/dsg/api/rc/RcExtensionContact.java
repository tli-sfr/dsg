package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcExtensionContact(
        String firstName,
        String lastName,
        String email,
        String department,
        String jobTitle) {
}
