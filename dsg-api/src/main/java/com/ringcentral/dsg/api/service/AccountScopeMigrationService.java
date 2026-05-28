package com.ringcentral.dsg.api.service;

import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryAuthRepository;
import com.ringcentral.dsg.persistence.repo.AccountDirectoryOauthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Moves directory integration configuration from a placeholder account id (e.g. demo-acct)
 * to the resolved RingCentral account id after login.
 */
@Service
public class AccountScopeMigrationService {

    private static final Logger log = LoggerFactory.getLogger(AccountScopeMigrationService.class);

    private static final String[] ACCOUNT_SCOPED_TABLES = {
            "attribute_mapping",
            "custom_attribute_mapping",
            "provisioning_assignment_rule",
            "deprovisioning_rule",
            "directory_sync_checkpoint",
            "directory_sync_time",
            "directory_sync_user_hash",
    };

    private final JdbcTemplate jdbcTemplate;
    private final AccountDirectoryAuthRepository authRepository;
    private final AccountDirectoryOauthRepository oauthRepository;

    public AccountScopeMigrationService(
            JdbcTemplate jdbcTemplate,
            AccountDirectoryAuthRepository authRepository,
            AccountDirectoryOauthRepository oauthRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.authRepository = authRepository;
        this.oauthRepository = oauthRepository;
    }

    /** Local dev often configures directory integration under {@code demo-acct} before RC login resolves the real id. */
    public void migrateKnownPlaceholders(String toAccountId) {
        migrateIfNeeded("demo-acct", toAccountId);
    }

    @Transactional
    public void migrateIfNeeded(String fromAccountId, String toAccountId) {
        if (fromAccountId == null
                || toAccountId == null
                || fromAccountId.isBlank()
                || fromAccountId.equals(toAccountId)) {
            return;
        }

        log.info("Migrating directory integration data from account {} to {}", fromAccountId, toAccountId);

        migrateDirectoryAuth(fromAccountId, toAccountId);
        migrateDirectoryOauth(fromAccountId, toAccountId);
        migrateAccountScopedRows(fromAccountId, toAccountId);
    }

    private void migrateDirectoryAuth(String fromAccountId, String toAccountId) {
        Optional<AccountDirectoryAuthRecord> from = authRepository.findByAccountId(fromAccountId);
        if (from.isEmpty()) {
            return;
        }
        Optional<AccountDirectoryAuthRecord> to = authRepository.findByAccountId(toAccountId);
        if (to.isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE account_directory_auth SET account_id = ? WHERE account_id = ?",
                    toAccountId,
                    fromAccountId);
            return;
        }
        authRepository.mergeDirectoryConfigInto(toAccountId, from.get());
        jdbcTemplate.update("DELETE FROM account_directory_auth WHERE account_id = ?", fromAccountId);
    }

    private void migrateDirectoryOauth(String fromAccountId, String toAccountId) {
        if (!oauthRepository.findByAccountId(fromAccountId).isPresent()) {
            return;
        }
        if (oauthRepository.findByAccountId(toAccountId).isPresent()) {
            if (!oauthRepository.hasRefreshToken(toAccountId) && oauthRepository.hasRefreshToken(fromAccountId)) {
                oauthRepository.moveRefreshTokens(fromAccountId, toAccountId);
            }
            jdbcTemplate.update("DELETE FROM account_directory_oauth WHERE account_id = ?", fromAccountId);
            return;
        }
        jdbcTemplate.update(
                "UPDATE account_directory_oauth SET account_id = ? WHERE account_id = ?",
                toAccountId,
                fromAccountId);
    }

    private void migrateAccountScopedRows(String fromAccountId, String toAccountId) {
        for (String table : ACCOUNT_SCOPED_TABLES) {
            jdbcTemplate.update(
                    "UPDATE " + table + " SET account_id = ? WHERE account_id = ?",
                    toAccountId,
                    fromAccountId);
        }
    }
}
