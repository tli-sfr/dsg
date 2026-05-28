package com.ringcentral.dsg.rules;

public record ProvisioningRuleMatch(long ruleId, String ruleName, int priority, String selectionExpressionJson) {}
