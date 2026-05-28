package com.ringcentral.dsg.persistence.model;

public record AttributeMappingView(
        String directoryAttributePath,
        String directoryAttributeName,
        String rcAttributeName,
        int displaySequence) {}
