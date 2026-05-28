package com.ringcentral.dsg.persistence.model;

public record ProvisioningRuleRecord(long id, String ruleName, int priority, String selectionExpressionJson) {}
