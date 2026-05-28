package com.ringcentral.dsg.provisioning;

import com.ringcentral.dsg.directory.DirectoryUser;

/**
 * RingCentral provisioning port (ADR-007). Phase 1: createExtension stubbed; PLA paths deferred.
 */
public interface RcProvisioningPort {

    ProvisioningResult createExtension(String accountId, DirectoryUser directoryUser);
}
