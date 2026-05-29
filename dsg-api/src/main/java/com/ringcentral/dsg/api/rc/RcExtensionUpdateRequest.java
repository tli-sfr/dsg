package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcExtensionUpdateRequest(RcExtensionContact contact, String status) {
}
