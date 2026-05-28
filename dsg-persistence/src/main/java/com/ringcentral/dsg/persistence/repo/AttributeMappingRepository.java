package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.AttributeMappingView;
import java.util.List;
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

    public List<AttributeMappingView> listAccountMappings(String accountId, int directionId) {
        return listAccountMappings(accountId, directionId, null);
    }

    public List<AttributeMappingView> listAccountMappings(
            String accountId, int directionId, Integer directoryTypeId) {
        String sql = """
                SELECT COALESCE(am.directory_attribute_path, da.attribute_path) AS attribute_path,
                       da.attribute_name,
                       ra.attribute_name AS rc_attribute_name,
                       am.id AS display_sequence
                FROM attribute_mapping am
                JOIN directory_attribute da ON da.id = am.directory_attribute_id
                JOIN rc_attribute ra ON ra.id = am.rc_attribute_id
                WHERE am.account_id = ? AND am.direction_id = ?
                """;
        if (directoryTypeId != null) {
            sql += " AND da.directory_type_id = ?";
            return jdbcTemplate.query(
                    sql + " ORDER BY am.id",
                    (rs, rowNum) -> new AttributeMappingView(
                            rs.getString("attribute_path"),
                            rs.getString("attribute_name"),
                            rs.getString("rc_attribute_name"),
                            rs.getInt("display_sequence")),
                    accountId,
                    directionId,
                    directoryTypeId);
        }
        return jdbcTemplate.query(
                sql + " ORDER BY am.id",
                (rs, rowNum) -> new AttributeMappingView(
                        rs.getString("attribute_path"),
                        rs.getString("attribute_name"),
                        rs.getString("rc_attribute_name"),
                        rs.getInt("display_sequence")),
                accountId,
                directionId);
    }

    public int countBasicMappings(String accountId) {
        return countBasicMappings(accountId, null);
    }

    public int countBasicMappings(String accountId, Integer directoryTypeId) {
        if (directoryTypeId == null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM attribute_mapping WHERE account_id = ?",
                    Integer.class,
                    accountId);
            return count != null ? count : 0;
        }
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM attribute_mapping am
                        JOIN directory_attribute da ON da.id = am.directory_attribute_id
                        WHERE am.account_id = ? AND da.directory_type_id = ?
                        """,
                Integer.class,
                accountId,
                directoryTypeId);
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
