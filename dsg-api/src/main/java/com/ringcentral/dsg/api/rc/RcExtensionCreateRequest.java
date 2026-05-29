package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcExtensionCreateRequest(
        String type,
        String status,
        RcExtensionContact contact) {

    public static RcExtensionCreateRequest fromDirectoryUser(
            String firstName, String lastName, String email, String department) {
        return new RcExtensionCreateRequest(
                "User",
                "NotActivated",
                new RcExtensionContact(firstName, lastName, email, department, null));
    }
}
