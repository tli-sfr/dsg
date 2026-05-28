package com.ringcentral.dsg.api.directory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record OktaUserItem(String id, String status, Map<String, Object> profile) {
}
