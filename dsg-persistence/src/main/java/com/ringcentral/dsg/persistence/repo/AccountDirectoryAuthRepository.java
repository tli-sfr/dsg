package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.AccountDirectoryAuthRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountDirectoryAuthRepository {

    private static final RowMapper<AccountDirectoryAuthRecord> ROW_MAPPER = (rs, rowNum) -> new AccountDirectoryAuthRecord(
            rs.getLong("id"),
            rs.getString("account_id"),
            rs.getInt("directory_type_id"),
            rs.getString("directory_type"),
            rs.getString("directory_group_id"),
            rs.getString("directory_group_name"),
            rs.getString("etm_subscriber_id"),
            rs.getObject("oauth_config_id") != null ? rs.getLong("oauth_config_id") : null,
            rs.getString("rc_refresh_token"),
            rs.getInt("active") == 1);

    private final JdbcTemplate jdbcTemplate;

    public AccountDirectoryAuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(String accountId, int directoryTypeId, String etmSubscriberId) {
        jdbcTemplate.update("""
                INSERT INTO account_directory_auth (account_id, directory_type_id, etm_subscriber_id, active)
                VALUES (?, ?, ?, 0)
                ON DUPLICATE KEY UPDATE
                    directory_type_id = VALUES(directory_type_id),
                    etm_subscriber_id = VALUES(etm_subscriber_id)
                """, accountId, directoryTypeId, etmSubscriberId);
    }

    public void update(String accountId, String directoryGroupId, String directoryGroupName, Boolean active) {
        if (directoryGroupId != null && directoryGroupName != null && active != null) {
            jdbcTemplate.update("""
                    UPDATE account_directory_auth
                    SET directory_group_id = ?, directory_group_name = ?, active = ?
                    WHERE account_id = ?
                    """, directoryGroupId, directoryGroupName, active ? 1 : 0, accountId);
            return;
        }
        if (directoryGroupId != null && directoryGroupName != null) {
            jdbcTemplate.update("""
                    UPDATE account_directory_auth
                    SET directory_group_id = ?, directory_group_name = ?
                    WHERE account_id = ?
                    """, directoryGroupId, directoryGroupName, accountId);
            return;
        }
        if (directoryGroupId != null && active != null) {
            jdbcTemplate.update("""
                    UPDATE account_directory_auth
                    SET directory_group_id = ?, active = ?
                    WHERE account_id = ?
                    """, directoryGroupId, active ? 1 : 0, accountId);
            return;
        }
        if (directoryGroupId != null) {
            jdbcTemplate.update("""
                    UPDATE account_directory_auth
                    SET directory_group_id = ?
                    WHERE account_id = ?
                    """, directoryGroupId, accountId);
            return;
        }
        if (active != null) {
            jdbcTemplate.update("""
                    UPDATE account_directory_auth
                    SET active = ?
                    WHERE account_id = ?
                    """, active ? 1 : 0, accountId);
        }
    }

    public void linkOAuthConfig(String accountId, long oauthConfigId) {
        jdbcTemplate.update("""
                UPDATE account_directory_auth
                SET oauth_config_id = ?
                WHERE account_id = ?
                """, oauthConfigId, accountId);
    }

    public Optional<AccountDirectoryAuthRecord> findByAccountId(String accountId) {
        String sql = """
                SELECT a.id, a.account_id, a.directory_type_id, t.directory_type,
                       a.directory_group_id, a.directory_group_name, a.etm_subscriber_id, a.oauth_config_id,
                       a.rc_refresh_token, a.active
                FROM account_directory_auth a
                JOIN directory_type t ON t.id = a.directory_type_id
                WHERE a.account_id = ?
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void updateRcRefreshToken(String accountId, String encryptedRefreshToken) {
        int updated = jdbcTemplate.update("""
                UPDATE account_directory_auth
                SET rc_refresh_token = ?
                WHERE account_id = ?
                """, encryptedRefreshToken, accountId);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO account_directory_auth (account_id, directory_type_id, rc_refresh_token, active)
                    VALUES (?, 1, ?, 0)
                    """, accountId, encryptedRefreshToken);
        }
    }

    public Optional<String> findRcRefreshToken(String accountId) {
        String sql = """
                SELECT rc_refresh_token
                FROM account_directory_auth
                WHERE account_id = ? AND rc_refresh_token IS NOT NULL
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, String.class, accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public boolean hasRcRefreshToken(String accountId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_directory_auth WHERE account_id = ? AND rc_refresh_token IS NOT NULL",
                Integer.class,
                accountId);
        return count != null && count > 0;
    }

    public void migrateRcRefreshToken(String fromAccountId, String toAccountId) {
        if (fromAccountId.equals(toAccountId)) {
            return;
        }
        findRcRefreshToken(fromAccountId).ifPresent(token -> updateRcRefreshToken(toAccountId, token));
    }
}
