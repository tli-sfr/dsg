package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.ProvisioningRuleRecord;
import com.ringcentral.dsg.persistence.model.RuleBasedAttributeMappingRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ProvisioningRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProvisioningRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long upsertRule(String accountId, String ruleName, int priority, String selectionExpressionJson) {
        Optional<Long> existingId = findRuleIdByAccountAndPriority(accountId, priority);
        if (existingId.isPresent()) {
            long ruleId = existingId.get();
            deleteAssignments(ruleId);
            jdbcTemplate.update("""
                    UPDATE provisioning_assignment_rule
                    SET rule_name = ?, selection_expression = ?
                    WHERE id = ?
                    """, ruleName, selectionExpressionJson, ruleId);
            return ruleId;
        }

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO provisioning_assignment_rule
                                (rule_name, account_id, priority, selection_expression)
                            VALUES (?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ruleName);
            ps.setString(2, accountId);
            ps.setInt(3, priority);
            ps.setString(4, selectionExpressionJson);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void insertLicenseAssignment(long ruleId, int licenseTypeId, String licenseId) {
        jdbcTemplate.update("""
                INSERT INTO license_assignment (rule_id, license_type_id, license_id)
                VALUES (?, ?, ?)
                """, ruleId, licenseTypeId, licenseId);
    }

    public void insertAreaCodeAssignment(long ruleId, int areaCodeRuleTypeId, String areaCodeListJson) {
        jdbcTemplate.update("""
                INSERT INTO dl_area_code_assignment (rule_id, area_code_rule_type_id, area_code_list)
                VALUES (?, ?, ?)
                """, ruleId, areaCodeRuleTypeId, areaCodeListJson);
    }

    public void insertTemplateAssignment(long ruleId, int templateTypeId, String templateId) {
        jdbcTemplate.update("""
                INSERT INTO template_assignment (rule_id, template_type_id, template_id)
                VALUES (?, ?, ?)
                """, ruleId, templateTypeId, templateId);
    }

    public void insertDeviceAssignment(long ruleId, int deviceTypeId, String deviceId) {
        jdbcTemplate.update("""
                INSERT INTO device_assignment (rule_id, device_type_id, device_id)
                VALUES (?, ?, ?)
                """, ruleId, deviceTypeId, deviceId);
    }

    public void insertRuleBasedAttributeMapping(
            String accountId,
            long ruleId,
            String directoryAttributePath,
            String directoryAttributeValue,
            int rcRuleBasedAttributeId,
            String rcObjectId) {
        jdbcTemplate.update("""
                INSERT INTO rule_based_attribute_mapping
                    (account_id, rule_id, directory_attribute_path, directory_attribute_value,
                     rc_rule_based_attribute_id, rc_object_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                accountId,
                ruleId,
                directoryAttributePath,
                directoryAttributeValue,
                rcRuleBasedAttributeId,
                rcObjectId);
    }

    public int countLicenseAssignments(long ruleId) {
        return count("license_assignment", ruleId);
    }

    public List<ProvisioningRuleRecord> listByAccountOrderByPriority(String accountId) {
        return jdbcTemplate.query(
                """
                        SELECT id, rule_name, priority, selection_expression
                        FROM provisioning_assignment_rule
                        WHERE account_id = ?
                        ORDER BY priority ASC
                        """,
                (rs, rowNum) -> new ProvisioningRuleRecord(
                        rs.getLong("id"),
                        rs.getString("rule_name"),
                        rs.getInt("priority"),
                        rs.getString("selection_expression")),
                accountId);
    }

    public List<RuleBasedAttributeMappingRecord> listRuleBasedMappings(long ruleId) {
        return jdbcTemplate.query(
                """
                        SELECT m.directory_attribute_path,
                               m.directory_attribute_value,
                               a.attribute_name,
                               m.rc_object_id
                        FROM rule_based_attribute_mapping m
                        JOIN rc_rule_based_attribute a ON a.id = m.rc_rule_based_attribute_id
                        WHERE m.rule_id = ?
                        """,
                (rs, rowNum) -> new RuleBasedAttributeMappingRecord(
                        rs.getString("directory_attribute_path"),
                        rs.getString("directory_attribute_value"),
                        rs.getString("attribute_name"),
                        rs.getString("rc_object_id")),
                ruleId);
    }

    private Optional<Long> findRuleIdByAccountAndPriority(String accountId, int priority) {
        try {
            Long id = jdbcTemplate.queryForObject(
                    """
                            SELECT id FROM provisioning_assignment_rule
                            WHERE account_id = ? AND priority = ?
                            """,
                    Long.class,
                    accountId,
                    priority);
            return Optional.ofNullable(id);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private void deleteAssignments(long ruleId) {
        jdbcTemplate.update("DELETE FROM license_assignment WHERE rule_id = ?", ruleId);
        jdbcTemplate.update("DELETE FROM dl_area_code_assignment WHERE rule_id = ?", ruleId);
        jdbcTemplate.update("DELETE FROM template_assignment WHERE rule_id = ?", ruleId);
        jdbcTemplate.update("DELETE FROM device_assignment WHERE rule_id = ?", ruleId);
        jdbcTemplate.update("DELETE FROM rule_based_attribute_mapping WHERE rule_id = ?", ruleId);
    }

    private int count(String table, long ruleId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE rule_id = ?",
                Integer.class,
                ruleId);
        return count != null ? count : 0;
    }
}
