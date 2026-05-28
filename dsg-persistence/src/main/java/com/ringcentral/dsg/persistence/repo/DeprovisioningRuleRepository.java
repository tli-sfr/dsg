package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.DeprovisioningRuleRecord;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DeprovisioningRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public DeprovisioningRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<DeprovisioningRuleRecord> findByAccountId(String accountId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                            SELECT dt.type
                            FROM deprovisioning_rule dr
                            JOIN deprovisioning_type dt ON dt.id = dr.deprovisioning_type_id
                            WHERE dr.account_id = ?
                            """,
                    (rs, rowNum) -> new DeprovisioningRuleRecord(rs.getString("type")),
                    accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void upsert(String accountId, int deprovisioningTypeId) {
        jdbcTemplate.update(
                """
                        INSERT INTO deprovisioning_rule (account_id, deprovisioning_type_id)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE deprovisioning_type_id = VALUES(deprovisioning_type_id)
                        """,
                accountId,
                deprovisioningTypeId);
    }
}
