package com.ringcentral.dsg.rules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SelectionExpression(String match, List<SelectionCriterion> criteria) {}
