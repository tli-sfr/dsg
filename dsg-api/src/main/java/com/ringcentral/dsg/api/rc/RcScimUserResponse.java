package com.ringcentral.dsg.api.rc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RcScimUserResponse(String id, String userName, RcScimName name) {
}
