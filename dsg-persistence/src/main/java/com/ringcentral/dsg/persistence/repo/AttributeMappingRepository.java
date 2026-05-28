package com.ringcentral.dsg.persistence.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AttributeMappingRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttributeMappingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replaceForAccount(String accountId) {
        jdbcTemplate.update("DELETE FROM attribute_mapping WHERE account_id = ?", accountId);
        jdbcTemplate.update("DELETE FROM custom_attribute_mapping WHERE account_id = ?", accountId);
    }

    public void insertBasicMapping(
            String accountId,
            int rcAttributeId,
            int directionId,
            int directoryAttributeId,
            String directoryAttributePath) {
        jdbcTemplate.update("""
                INSERT INTO attribute_mapping
                    (account_id, rc_attribute_id, direction_id, directory_attribute_id, directory_attribute_path)
                VALUES (?, ?, ?, ?, ?)
                """,
                accountId,
                rcAttributeId,
                directionId,
                directoryAttributeId,
                directoryAttributePath);
    }

    public void insertCustomMapping(
            String accountId,
            String directoryAttributePath,
            String rcCustomAttributeName) {
        jdbcTemplate.update("""
                INSERT INTO custom_attribute_mapping
                    (account_id, directory_attribute_path, rc_custom_attribute_name)
                VALUES (?, ?, ?)
                """,
                accountId,
                directoryAttributePath,
                rcCustomAttributeName);
    }

    public int countBasicMappings(String accountId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attribute_mapping WHERE account_id = ?",
                Integer.class,
                accountId);
        return count != null ? count : 0;
    }

    public int countCustomMappings(String accountId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM custom_attribute_mapping WHERE account_id = ?",
                Integer.class,
                accountId);
        return count != null ? count : 0;
    }
}
