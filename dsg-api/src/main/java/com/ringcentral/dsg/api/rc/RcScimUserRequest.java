package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcScimUserRequest(
        List<String> schemas,
        String userName,
        boolean active,
        RcScimName name,
        List<RcScimEmail> emails) {

    public static RcScimUserRequest fromDirectoryUser(String email, String firstName, String lastName) {
        return new RcScimUserRequest(
                List.of("urn:ietf:params:scim:schemas:core:2.0:User"),
                email,
                true,
                new RcScimName(firstName, lastName),
                List.of(new RcScimEmail("work", email)));
    }
}
