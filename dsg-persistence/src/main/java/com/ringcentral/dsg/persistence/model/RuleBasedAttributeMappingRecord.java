package com.ringcentral.dsg.persistence.model;

public record RuleBasedAttributeMappingRecord(
        String directoryAttributePath,
        String directoryAttributeValue,
        String rcAttributeName,
        String rcObjectId) {}
