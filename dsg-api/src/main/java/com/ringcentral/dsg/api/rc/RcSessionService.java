package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthSessionResponse;
import com.ringcentral.dsg.api.service.AccountScopeMigrationService;
import com.ringcentral.dsg.rc.RcAuthPort;
import org.springframework.stereotype.Service;

@Service
public class RcSessionService {

    private final RcAuthPort rcAuthPort;
    private final RcApiClient rcApiClient;
    private final AccountScopeMigrationService accountScopeMigrationService;

    public RcSessionService(
            RcAuthPort rcAuthPort,
            RcApiClient rcApiClient,
            AccountScopeMigrationService accountScopeMigrationService) {
        this.rcAuthPort = rcAuthPort;
        this.rcApiClient = rcApiClient;
        this.accountScopeMigrationService = accountScopeMigrationService;
    }

    public RcOAuthSessionResponse getSession(String lookupAccountId) {
        String accessToken = rcAuthPort.obtainAccessToken(lookupAccountId)
                .orElseThrow(() -> new IllegalStateException("RingCentral is not connected for this account"));

        RcExtensionResponse extension = rcApiClient.readCurrentExtension(accessToken);
        if (extension == null) {
            throw new IllegalStateException("RingCentral readExtension returned an empty response");
        }

        String rcAccountId = extension.parseAccountId();
        accountScopeMigrationService.migrateIfNeeded(lookupAccountId, rcAccountId);
        accountScopeMigrationService.migrateKnownPlaceholders(rcAccountId);

        return new RcOAuthSessionResponse(
                rcAccountId,
                extension.extensionId(),
                extension.extensionNumber(),
                extension.displayName());
    }
}
