package com.ringcentral.dsg.api.directory;

import com.ringcentral.dsg.directory.DirectoryPort;
import com.ringcentral.dsg.directory.DirectoryUser;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads group members from the Okta Management API using the account's IDP OAuth token.
 * Registered as {@link DirectoryPort} via {@link com.ringcentral.dsg.api.config.DirectoryPortConfiguration}.
 */
public class OktaDirectoryPort implements DirectoryPort {

    private static final Logger log = LoggerFactory.getLogger(OktaDirectoryPort.class);

    private final AccountDirectoryOauthRepository oauthRepository;
    private final DirectoryIdpAccessTokenService accessTokenService;
    private final DirectoryIdpOAuthClient idpOAuthClient;

    public OktaDirectoryPort(
            AccountDirectoryOauthRepository oauthRepository,
            DirectoryIdpAccessTokenService accessTokenService,
            DirectoryIdpOAuthClient idpOAuthClient) {
        this.oauthRepository = oauthRepository;
        this.accessTokenService = accessTokenService;
        this.idpOAuthClient = idpOAuthClient;
    }

    @Override
    public List<DirectoryUser> listGroupMembers(String accountId, String directoryGroupId) {
        if (directoryGroupId == null || directoryGroupId.isBlank()) {
            throw new IllegalStateException("Directory sync group is not configured for account " + accountId);
        }
        var creds = oauthRepository.findCredentialsByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("IDP OAuth config not found for account " + accountId));
        if (!"Okta".equals(creds.directoryTypeName())) {
            throw new IllegalStateException(
                    "Okta directory adapter cannot load users for directory type " + creds.directoryTypeName());
        }
        String accessToken = accessTokenService.requireAccessToken(accountId);
        List<OktaUserItem> oktaUsers = idpOAuthClient.listOktaGroupUsers(
                creds.oktaDomain(), accessToken, directoryGroupId);

        log.info(
                "[DSG Okta] account={} groupId={} fetched {} users from Okta",
                accountId,
                directoryGroupId,
                oktaUsers.size());

        return oktaUsers.stream()
                .map(item -> {
                    DirectoryUser user = OktaDirectoryUserMapper.toDirectoryUser(item);
                    log.info(
                            "[DSG Okta user] account={} externalId={} email={} status={} profileAttributes={}",
                            accountId,
                            user.externalId(),
                            user.email(),
                            item.status(),
                            user.attributes());
                    return user;
                })
                .toList();
    }
}
