package com.ringcentral.dsg.worker.stub;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.provisioning.ProductLicense;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;

public class StubRcProvisioningPort implements RcProvisioningPort {

    @Override
    public ProvisioningResult provisionUser(
            String accountId, DirectoryUser directoryUser, String primaryLicenseId) {
        ProductLicense license = ProductLicense.fromLicenseId(primaryLicenseId);
        String mailboxId = license.usesScimApi()
                ? "scim-" + directoryUser.externalId()
                : "ext-" + directoryUser.externalId();
        long ruleBasedCount = directoryUser.attributes().keySet().stream()
                .filter(key -> key.startsWith("rc."))
                .count();
        String api = license.usesExtensionCreateApi()
                ? "Extensions/createExtension"
                : "SCIM (create + scimGetUser2)";
        String message = ruleBasedCount > 0
                ? "Stub " + api + " for " + license.label() + " with " + ruleBasedCount + " rule-based attribute(s)"
                : "Stub " + api + " for " + license.label();
        return new ProvisioningResult(mailboxId, true, message);
    }

    @Override
    public ProvisioningResult updateExtension(
            String accountId, String rcUserId, DirectoryUser directoryUser) {
        return new ProvisioningResult(
                rcUserId,
                true,
                "Stub User-Settings/updateExtension for " + directoryUser.email());
    }
}
