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
 * RingEX → SCIM Users (create, then scimGetUser2);
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
            RcScimUserRequest scimRequest = buildScimRequest(user);
            DirectorySyncTrace.logRcProvisionPayload(accountId, "CREATE_SCIM_USER", rcPayload(scimRequest));
            RcScimUserResponse created = provisioningClient.createScimUser(accessToken, scimRequest);
            if (created == null || created.id() == null) {
                return new ProvisioningResult(null, false, "SCIM create user returned no id");
            }
            RcScimUserResponse verified = provisioningClient.getScimUser(accessToken, created.id());
            String mailboxId = verified != null && verified.id() != null ? verified.id() : created.id();
            return new ProvisioningResult(
                    mailboxId,
                    true,
                    "Provisioned via SCIM (create + scimGetUser2) for RingEX");
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
        String firstName = requireRcAttribute(user, "firstName");
        String lastName = requireRcAttribute(user, "lastName");
        String department = rcAttribute(user, "department");
        return RcExtensionCreateRequest.fromDirectoryUser(firstName, lastName, user.email(), department);
    }

    private static RcExtensionUpdateRequest buildUpdateRequest(DirectoryUser user) {
        String firstName = rcAttribute(user, "firstName");
        String lastName = rcAttribute(user, "lastName");
        String department = rcAttribute(user, "department");
        String jobTitle = rcAttribute(user, "jobTitle");
        return new RcExtensionUpdateRequest(
                new RcExtensionContact(firstName, lastName, user.email(), department, jobTitle),
                null);
    }

    private static RcScimUserRequest buildScimRequest(DirectoryUser user) {
        String firstName = requireRcAttribute(user, "firstName");
        String lastName = requireRcAttribute(user, "lastName");
        return RcScimUserRequest.fromDirectoryUser(user.email(), firstName, lastName);
    }

    private static String requireRcAttribute(DirectoryUser user, String rcAttributeName) {
        String value = rcAttribute(user, rcAttributeName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required RC attribute '" + rcAttributeName + "' is missing after attribute mapping");
        }
        return value;
    }

    /** Values populated by {@link com.ringcentral.dsg.mapping.AttributeMappingApplier} in the worker. */
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

    private static java.util.Map<String, String> rcPayload(RcScimUserRequest request) {
        java.util.Map<String, String> payload = new java.util.LinkedHashMap<>();
        payload.put("userName", request.userName());
        if (request.name() != null) {
            payload.put("firstName", request.name().givenName());
            payload.put("lastName", request.name().familyName());
        }
        if (request.emails() != null && !request.emails().isEmpty()) {
            payload.put("email", request.emails().get(0).value());
        }
        return payload;
    }

    private static ProvisioningResult stubProvision(DirectoryUser user, ProductLicense license) {
        String mailboxId = license.usesScimApi()
                ? "scim-" + user.externalId()
                : "ext-" + user.externalId();
        String api = license.usesExtensionCreateApi()
                ? "Extensions/createExtension"
                : "SCIM Users (create + scimGetUser2)";
        return new ProvisioningResult(
                mailboxId,
                true,
                "Stub " + api + " for " + license.label() + " (RC not connected)");
    }
}
