package com.ringcentral.dsg.persistence.repo;

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
        Optional<AccountDirectoryOauthRecord> existing = findByAccountId(accountId);
        if (existing.isPresent()) {
            jdbcTemplate.update("""
                    UPDATE account_directory_oauth
                    SET directory_type_id = ?, auth_flow = ?, client_id = ?, client_secret_enc = ?
                    WHERE account_id = ?
                    """, directoryTypeId, authFlow, clientId, clientSecretEnc, accountId);
            return existing.get().id();
        }

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO account_directory_oauth
                        (account_id, directory_type_id, auth_flow, client_id, client_secret_enc)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, accountId);
            ps.setInt(2, directoryTypeId);
            ps.setString(3, authFlow);
            ps.setString(4, clientId);
            ps.setString(5, clientSecretEnc);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to insert account_directory_oauth row");
        }
        return key.longValue();
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

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
