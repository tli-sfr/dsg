package com.ringcentral.dsg.provisioning;

public record ProvisioningResult(String mailboxId, boolean success, String message) {
}
