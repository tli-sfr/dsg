package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcBusinessAddress(String street, String city, String zip, String state, String country) {
}
