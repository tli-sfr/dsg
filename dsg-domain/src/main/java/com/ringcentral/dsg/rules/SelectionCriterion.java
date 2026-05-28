package com.ringcentral.dsg.rules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SelectionCriterion(String attribute, String operator, String value) {}
