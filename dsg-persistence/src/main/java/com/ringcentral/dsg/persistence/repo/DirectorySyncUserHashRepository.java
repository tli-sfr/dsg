package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.DirectorySyncUserHashRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DirectorySyncUserHashRepository {

    private final JdbcTemplate jdbcTemplate;

    public DirectorySyncUserHashRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DirectorySyncUserHashRecord> listByAccount(String accountId, int directoryTypeId) {
        return jdbcTemplate.query(
                """
                        SELECT account_id, directory_type_id, external_id,
                               external_user_hash, mailbox_id, rc_user_hash
                        FROM directory_sync_user_hash
                        WHERE account_id = ? AND directory_type_id = ?
                        ORDER BY external_id
                        """,
                (rs, rowNum) -> new DirectorySyncUserHashRecord(
                        rs.getString("account_id"),
                        rs.getInt("directory_type_id"),
                        rs.getString("external_id"),
                        rs.getString("external_user_hash"),
                        rs.getString("mailbox_id"),
                        rs.getString("rc_user_hash")),
                accountId,
                directoryTypeId);
    }

    public Optional<DirectorySyncUserHashRecord> find(String accountId, int directoryTypeId, String externalId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                            SELECT account_id, directory_type_id, external_id,
                                   external_user_hash, mailbox_id, rc_user_hash
                            FROM directory_sync_user_hash
                            WHERE account_id = ? AND directory_type_id = ? AND external_id = ?
                            """,
                    (rs, rowNum) -> new DirectorySyncUserHashRecord(
                            rs.getString("account_id"),
                            rs.getInt("directory_type_id"),
                            rs.getString("external_id"),
                            rs.getString("external_user_hash"),
                            rs.getString("mailbox_id"),
                            rs.getString("rc_user_hash")),
                    accountId,
                    directoryTypeId,
                    externalId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void delete(String accountId, int directoryTypeId, String externalId) {
        jdbcTemplate.update(
                """
                        DELETE FROM directory_sync_user_hash
                        WHERE account_id = ? AND directory_type_id = ? AND external_id = ?
                        """,
                accountId,
                directoryTypeId,
                externalId);
    }

    public void upsertAfterProvision(
            String accountId,
            int directoryTypeId,
            String externalId,
            String externalUserHash,
            String mailboxId) {
        jdbcTemplate.update(
                """
                        INSERT INTO directory_sync_user_hash
                            (account_id, directory_type_id, external_id, external_user_hash, mailbox_id)
                        VALUES (?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            external_user_hash = VALUES(external_user_hash),
                            mailbox_id = VALUES(mailbox_id),
                            updated_on = CURRENT_TIMESTAMP
                        """,
                accountId,
                directoryTypeId,
                externalId,
                externalUserHash,
                mailboxId);
    }
}
