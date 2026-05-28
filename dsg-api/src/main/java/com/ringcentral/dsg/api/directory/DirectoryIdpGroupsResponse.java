package com.ringcentral.dsg.api.directory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AzureGroupsResponse(List<AzureGroupItem> value) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AzureGroupItem(String id, String displayName) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record OktaGroupItem(String id, String type, OktaGroupProfile profile) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record OktaGroupProfile(String name) {
}
