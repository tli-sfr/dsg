package com.ringcentral.dsg.provisioning;

import com.ringcentral.dsg.directory.DirectoryUser;

/**
 * RingCentral provisioning port (ADR-007).
 *
 * @see <a href="https://developers.ringcentral.com/api-reference/Extensions/createExtension">createExtension</a>
 * @see <a href="https://developers.ringcentral.com/api-reference/Extensions/bulkAssignExtensions">bulkAssignExtensions</a>
 * @see <a href="https://developers.ringcentral.com/api-reference/User-Settings/updateExtension">updateExtension</a>
 */
public interface RcProvisioningPort {

    /**
     * Type 1 provision — route by primary license on the matched rule.
     */
    ProvisioningResult provisionUser(String accountId, DirectoryUser directoryUser, String primaryLicenseId);

    /**
     * Type 2 directory → RC sync — always uses Extensions update API.
     */
    ProvisioningResult updateExtension(String accountId, String rcUserId, DirectoryUser directoryUser);

    /** @deprecated use {@link #provisionUser} */
    default ProvisioningResult createExtension(String accountId, DirectoryUser directoryUser) {
        return provisionUser(accountId, directoryUser, ProductLicense.RING_EX.licenseId());
    }
}
