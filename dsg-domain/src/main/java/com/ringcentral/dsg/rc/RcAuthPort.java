package com.ringcentral.dsg.rc;

import java.util.Optional;

/**
 * RingCentral platform OAuth access tokens for sync / provisioning API calls.
 */
public interface RcAuthPort {

    Optional<String> obtainAccessToken(String accountId);
}
