package com.ringcentral.dsg.persistence.repo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public LookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Integer> findSyncDirectionId(String apiDirection) {
        if (apiDirection == null) {
            return Optional.empty();
        }
        return switch (apiDirection) {
            case "DIR_TO_RC", "BIDIRECTIONAL" -> Optional.of(1);
            case "RC_TO_DIR" -> Optional.of(2);
            default -> Optional.empty();
        };
    }

    public Optional<Integer> findJobTypeId(String jobType) {
        return findIdByColumn("job_type", "type", jobType);
    }

    public Optional<Integer> findLicenseTypeId(String licenseType) {
        return findIdByColumn("license_type", "type", licenseType);
    }

    public Optional<Integer> findAreaCodeRuleTypeId(String areaCodeRuleType) {
        return findIdByColumn("dl_area_code_type", "area_code_rule_type", areaCodeRuleType);
    }

    public Optional<Integer> findTemplateTypeId(String templateType) {
        return findIdByColumn("template_type", "type", templateType);
    }

    public Optional<Integer> findDeviceTypeId(String deviceType) {
        String normalized = switch (deviceType) {
            case "RingCentral App" -> "RINGCENTRAL_APP";
            case "Inventory phone" -> "INVENTORY_PHONE";
            default -> deviceType;
        };
        return findIdByColumn("device_type", "type", normalized);
    }

    public Optional<Integer> findRuleBasedAttributeId(String attributeName) {
        return findIdByColumn("rc_rule_based_attribute", "attribute_name", attributeName);
    }

    public Optional<Integer> findDeprovisioningTypeId(String type) {
        return findIdByColumn("deprovisioning_type", "type", type);
    }

    public int defaultJobFrequencyId() {
        return 1;
    }

    private Optional<Integer> findIdByColumn(String table, String column, String value) {
        try {
            Integer id = jdbcTemplate.queryForObject(
                    "SELECT id FROM " + table + " WHERE " + column + " = ?",
                    Integer.class,
                    value);
            return Optional.ofNullable(id);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
