package com.ringcentral.dsg.worker.stub;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;

public class StubRcProvisioningPort implements RcProvisioningPort {

    @Override
    public ProvisioningResult createExtension(String accountId, DirectoryUser directoryUser) {
        String mailboxId = "mbx-" + directoryUser.externalId();
        long ruleBasedCount = directoryUser.attributes().keySet().stream()
                .filter(key -> key.startsWith("rc."))
                .count();
        String message = ruleBasedCount > 0
                ? "Stub extension created with " + ruleBasedCount + " rule-based attribute(s)"
                : "Stub extension created";
        return new ProvisioningResult(mailboxId, true, message);
    }
}
