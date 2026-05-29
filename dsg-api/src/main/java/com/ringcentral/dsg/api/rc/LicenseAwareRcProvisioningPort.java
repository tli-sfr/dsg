package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.mapping.DirectorySyncTrace;
import com.ringcentral.dsg.provisioning.ProductLicense;
import com.ringcentral.dsg.provisioning.ProvisioningResult;
import com.ringcentral.dsg.provisioning.RcProvisioningPort;
import com.ringcentral.dsg.rc.RcAuthPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Routes provisioning by primary license:
 * Video Pro / Video Pro+ → Extensions createExtension;
 * RingEX → Extensions bulk-assign with CustomerDirectoryId reference;
 * Updates → Extensions updateExtension.
 */
@Component
@Primary
public class LicenseAwareRcProvisioningPort implements RcProvisioningPort {

    private final RcAuthPort rcAuthPort;
    private final RcProvisioningClient provisioningClient;
    private final RcOAuthService rcOAuthService;

    public LicenseAwareRcProvisioningPort(
            RcAuthPort rcAuthPort,
            RcProvisioningClient provisioningClient,
            RcOAuthService rcOAuthService) {
        this.rcAuthPort = rcAuthPort;
        this.provisioningClient = provisioningClient;
        this.rcOAuthService = rcOAuthService;
    }

    @Override
    public ProvisioningResult provisionUser(
            String accountId, DirectoryUser directoryUser, String primaryLicenseId) {
        ProductLicense license = ProductLicense.fromLicenseId(primaryLicenseId);
        if (!rcOAuthService.isConfigured()) {
            return stubProvision(directoryUser, license);
        }
        if (!rcOAuthService.hasValidRefreshToken(accountId)) {
            return rcNotConnectedFailure();
        }
        return rcAuthPort
                .obtainAccessToken(accountId)
                .map(token -> provisionWithToken(accountId, token, directoryUser, license))
                .orElseGet(this::rcReauthRequiredFailure);
    }

    @Override
    public ProvisioningResult updateExtension(
            String accountId, String rcUserId, DirectoryUser directoryUser) {
        if (!rcOAuthService.isConfigured()) {
            return new ProvisioningResult(
                    rcUserId,
                    true,
                    "Stub updateExtension (User-Settings/updateExtension) for " + directoryUser.email());
        }
        if (!rcOAuthService.hasValidRefreshToken(accountId)) {
            return rcNotConnectedFailure();
        }
        return rcAuthPort
                .obtainAccessToken(accountId)
                .map(token -> updateWithToken(token, rcUserId, directoryUser))
                .orElseGet(this::rcReauthRequiredFailure);
    }

    private static ProvisioningResult rcNotConnectedFailure() {
        return new ProvisioningResult(
                null,
                false,
                "RingCentral is not connected. Sign in to RingCentral from the dashboard.");
    }

    private ProvisioningResult rcReauthRequiredFailure() {
        return new ProvisioningResult(
                null,
                false,
                "RingCentral session expired. Sign in to RingCentral again, then retry the sync.");
    }

    private ProvisioningResult provisionWithToken(
            String accountId, String accessToken, DirectoryUser user, ProductLicense license) {
        try {
            if (license.usesExtensionCreateApi()) {
                RcExtensionCreateRequest request = buildCreateRequest(user, license);
                DirectorySyncTrace.logRcProvisionPayload(accountId, "CREATE_EXTENSION", rcPayload(request));
                RcExtensionResponse created = provisioningClient.createExtension(accessToken, request);
                String extensionId = created != null && created.extensionId() != null
                        ? String.valueOf(created.extensionId())
                        : "ext-" + user.externalId();
                return new ProvisioningResult(
                        extensionId,
                        true,
                        "Created via Extensions/createExtension (" + license.label() + ")");
            }
            RcBulkAssignRequest bulkRequest = RcBulkAssignRequest.forDirectoryUser(user);
            DirectorySyncTrace.logRcProvisionPayload(
                    accountId, "BULK_ASSIGN_EXTENSION", rcPayload(bulkRequest));
            RcBulkAssignHttpResult httpResult = provisioningClient.bulkAssignExtensions(accessToken, bulkRequest);
            RcBulkAssignResponse response = httpResult.response();
            DirectorySyncTrace.logRcProvisionResponse(
                    accountId,
                    "BULK_ASSIGN_EXTENSION",
                    httpResult.rawBody(),
                    response != null ? response.firstExtensionId() : null,
                    response != null ? response.firstItemSuccessfulFlag() : null);
            if (response == null || !response.isProvisionSuccess()) {
                return new ProvisioningResult(
                        null,
                        false,
                        "bulk-assign failed or returned no extension id (see response log above)");
            }
            String extensionId = response.firstExtensionId();
            if (extensionId == null) {
                return new ProvisioningResult(
                        null,
                        false,
                        "bulk-assign marked successful but returned no extension id (see response log above)");
            }
            return new ProvisioningResult(
                    extensionId,
                    true,
                    "Provisioned via Extensions/bulk-assign (CustomerDirectoryId="
                            + user.externalId()
                            + ") for RingEX");
        } catch (IllegalStateException ex) {
            return new ProvisioningResult(null, false, ex.getMessage());
        }
    }

    private ProvisioningResult updateWithToken(
            String accessToken, String rcUserId, DirectoryUser user) {
        try {
            provisioningClient.updateExtension(
                    accessToken, rcUserId, buildUpdateRequest(user));
            return new ProvisioningResult(
                    rcUserId, true, "Updated via User-Settings/updateExtension");
        } catch (IllegalStateException ex) {
            return new ProvisioningResult(rcUserId, false, ex.getMessage());
        }
    }

    private static RcExtensionCreateRequest buildCreateRequest(DirectoryUser user, ProductLicense license) {
        RcBulkAssignContact contact = RcMappedContactBuilder.buildContact(user);
        return RcExtensionCreateRequest.fromDirectoryUser(
                contact.firstName(), contact.lastName(), contact.email(), contact.department());
    }

    private static RcExtensionUpdateRequest buildUpdateRequest(DirectoryUser user) {
        return new RcExtensionUpdateRequest(
                new RcExtensionContact(
                        rcAttribute(user, "firstName"),
                        rcAttribute(user, "lastName"),
                        user.email(),
                        rcAttribute(user, "department"),
                        rcAttribute(user, "jobTitle")),
                null);
    }

    private static String rcAttribute(DirectoryUser user, String rcAttributeName) {
        String value = user.attributes().get(rcAttributeName);
        return value != null && !value.isBlank() ? value : null;
    }

    private static java.util.Map<String, String> rcPayload(RcExtensionCreateRequest request) {
        RcExtensionContact contact = request.contact();
        java.util.Map<String, String> payload = new java.util.LinkedHashMap<>();
        if (contact != null) {
            payload.put("firstName", contact.firstName());
            payload.put("lastName", contact.lastName());
            payload.put("email", contact.email());
            payload.put("department", contact.department());
        }
        return payload;
    }

    private static java.util.Map<String, String> rcPayload(RcBulkAssignRequest request) {
        java.util.Map<String, String> payload = new java.util.LinkedHashMap<>();
        if (request.items() != null && !request.items().isEmpty()) {
            RcBulkAssignItem item = request.items().get(0);
            if (item.extension() != null && item.extension().contact() != null) {
                RcBulkAssignContact contact = item.extension().contact();
                payload.put("firstName", contact.firstName());
                payload.put("lastName", contact.lastName());
                payload.put("email", contact.email());
                payload.put("department", contact.department());
                payload.put("mobilePhone", contact.mobilePhone());
                if (contact.businessAddress() != null) {
                    RcBusinessAddress address = contact.businessAddress();
                    payload.put("street", address.street());
                    payload.put("city", address.city());
                    payload.put("state", address.state());
                    payload.put("zip", address.zip());
                    payload.put("country", address.country());
                }
            }
            if (item.extension() != null
                    && item.extension().references() != null
                    && !item.extension().references().isEmpty()) {
                payload.put("CustomerDirectoryId", item.extension().references().get(0).ref());
            }
        }
        return payload;
    }

    private static ProvisioningResult stubProvision(DirectoryUser user, ProductLicense license) {
        String mailboxId = license.usesBulkAssignApi()
                ? "bulk-" + user.externalId()
                : "ext-" + user.externalId();
        String api = license.usesExtensionCreateApi()
                ? "Extensions/createExtension"
                : "Extensions/bulk-assign";
        return new ProvisioningResult(
                mailboxId,
                true,
                "Stub " + api + " for " + license.label() + " (RC not connected)");
    }
}
