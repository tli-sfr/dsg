package com.ringcentral.dsg.api.rc;

import com.ringcentral.dsg.api.model.AdminApiModels.RcOAuthSessionResponse;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.rc.RcAuthPort;
import org.springframework.stereotype.Service;

@Service
public class RcSessionService {

    private final RcAuthPort rcAuthPort;
    private final RcApiClient rcApiClient;
    private final AccountDirectoryAuthRepository authRepository;

    public RcSessionService(
            RcAuthPort rcAuthPort,
            RcApiClient rcApiClient,
            AccountDirectoryAuthRepository authRepository) {
        this.rcAuthPort = rcAuthPort;
        this.rcApiClient = rcApiClient;
        this.authRepository = authRepository;
    }

    public RcOAuthSessionResponse getSession(String lookupAccountId) {
        String accessToken = rcAuthPort.obtainAccessToken(lookupAccountId)
                .orElseThrow(() -> new IllegalStateException("RingCentral is not connected for this account"));

        RcExtensionResponse extension = rcApiClient.readCurrentExtension(accessToken);
        if (extension == null) {
            throw new IllegalStateException("RingCentral readExtension returned an empty response");
        }

        String rcAccountId = extension.parseAccountId();
        if (!lookupAccountId.equals(rcAccountId)) {
            authRepository.migrateRcRefreshToken(lookupAccountId, rcAccountId);
        }

        return new RcOAuthSessionResponse(
                rcAccountId,
                extension.extensionId(),
                extension.extensionNumber(),
                extension.displayName());
    }
}
