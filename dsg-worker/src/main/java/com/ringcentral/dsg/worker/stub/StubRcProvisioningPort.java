package com.ringcentral.dsg.worker.stub;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;

public class StubRcProvisioningPort implements RcProvisioningPort {

    @Override
    public ProvisioningResult createExtension(String accountId, DirectoryUser directoryUser) {
        String mailboxId = "mbx-" + directoryUser.externalId();
        return new ProvisioningResult(mailboxId, true, "Stub extension created");
    }
}
