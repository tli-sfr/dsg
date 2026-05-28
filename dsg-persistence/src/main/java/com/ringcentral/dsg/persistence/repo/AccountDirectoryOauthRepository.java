package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.AccountDirectoryOauthCredentialsRecord;
import com.ringcentral.dsg.persistence.model.AccountDirectoryOauthRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class AccountDirectoryOauthRepository {

    private static final RowMapper<AccountDirectoryOauthRecord> ROW_MAPPER = (rs, rowNum) -> new AccountDirectoryOauthRecord(
            rs.getLong("id"),
            rs.getString("account_id"),
            rs.getInt("directory_type_id"),
            rs.getString("directory_type"),
            rs.getString("auth_flow"),
            rs.getString("client_id"),
            toInstant(rs.getTimestamp("access_token_expires_at")));

    private static final RowMapper<AccountDirectoryOauthCredentialsRecord> CREDENTIALS_MAPPER = (rs, rowNum) ->
            new AccountDirectoryOauthCredentialsRecord(
                    rs.getLong("id"),
                    rs.getString("account_id"),
                    rs.getInt("directory_type_id"),
                    rs.getString("directory_type"),
                    rs.getString("auth_flow"),
                    rs.getString("client_id"),
                    rs.getString("client_secret_enc"),
                    rs.getString("azure_tenant_id"),
                    rs.getString("okta_domain"),
                    rs.getString("scopes"),
                    rs.getString("refresh_token_enc"),
                    toInstant(rs.getTimestamp("access_token_expires_at")));

    private final JdbcTemplate jdbcTemplate;

    public AccountDirectoryOauthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long upsert(
            String accountId,
            int directoryTypeId,
            String authFlow,
            String clientId,
            String clientSecretEnc) {
        return upsertOAuthConfig(accountId, directoryTypeId, authFlow, clientId, clientSecretEnc, null, null, null);
    }

    public long upsertOAuthConfig(
            String accountId,
            int directoryTypeId,
            String authFlow,
            String clientId,
            String clientSecretEnc,
            String azureTenantId,
            String oktaDomain,
            String scopes) {
        Optional<AccountDirectoryOauthRecord> existing = findByAccountId(accountId);
        if (existing.isPresent()) {
            jdbcTemplate.update("""
                    UPDATE account_directory_oauth
                    SET directory_type_id = ?, auth_flow = ?, client_id = ?, client_secret_enc = ?,
                        azure_tenant_id = ?, okta_domain = ?, scopes = ?
                    WHERE account_id = ?
                    """, directoryTypeId, authFlow, clientId, clientSecretEnc,
                    azureTenantId, oktaDomain, scopes, accountId);
            return existing.get().id();
        }

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO account_directory_oauth
                        (account_id, directory_type_id, auth_flow, client_id, client_secret_enc,
                         azure_tenant_id, okta_domain, scopes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, accountId);
            ps.setInt(2, directoryTypeId);
            ps.setString(3, authFlow);
            ps.setString(4, clientId);
            ps.setString(5, clientSecretEnc);
            ps.setString(6, azureTenantId);
            ps.setString(7, oktaDomain);
            ps.setString(8, scopes);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to insert account_directory_oauth row");
        }
        return key.longValue();
    }

    public void updateOAuthTokens(
            String accountId,
            String refreshTokenEnc,
            String accessTokenEnc,
            Instant accessTokenExpiresAt) {
        jdbcTemplate.update("""
                UPDATE account_directory_oauth
                SET refresh_token_enc = ?, access_token_enc = ?, access_token_expires_at = ?
                WHERE account_id = ?
                """, refreshTokenEnc, accessTokenEnc, Timestamp.from(accessTokenExpiresAt), accountId);
    }

    public void clearOAuthTokens(String accountId) {
        jdbcTemplate.update("""
                UPDATE account_directory_oauth
                SET refresh_token_enc = NULL, access_token_enc = NULL, access_token_expires_at = NULL
                WHERE account_id = ?
                """, accountId);
    }

    public Optional<AccountDirectoryOauthRecord> findByAccountId(String accountId) {
        String sql = """
                SELECT o.id, o.account_id, o.directory_type_id, t.directory_type,
                       o.auth_flow, o.client_id, o.access_token_expires_at
                FROM account_directory_oauth o
                JOIN directory_type t ON t.id = o.directory_type_id
                WHERE o.account_id = ?
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<AccountDirectoryOauthCredentialsRecord> findCredentialsByAccountId(String accountId) {
        String sql = """
                SELECT o.id, o.account_id, o.directory_type_id, t.directory_type,
                       o.auth_flow, o.client_id, o.client_secret_enc, o.azure_tenant_id,
                       o.okta_domain, o.scopes, o.refresh_token_enc, o.access_token_expires_at
                FROM account_directory_oauth o
                JOIN directory_type t ON t.id = o.directory_type_id
                WHERE o.account_id = ?
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, CREDENTIALS_MAPPER, accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public boolean hasCredentials(String accountId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM account_directory_oauth
                WHERE account_id = ?
                  AND client_secret_enc IS NOT NULL
                  AND client_secret_enc <> ''
                """, Integer.class, accountId);
        return count != null && count > 0;
    }

    public boolean hasRefreshToken(String accountId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM account_directory_oauth
                WHERE account_id = ?
                  AND refresh_token_enc IS NOT NULL
                  AND refresh_token_enc <> ''
                """, Integer.class, accountId);
        return count != null && count > 0;
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
