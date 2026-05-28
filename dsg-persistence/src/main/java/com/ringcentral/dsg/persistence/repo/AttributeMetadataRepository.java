package com.ringcentral.dsg.persistence.repo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class AttributeMetadataRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttributeMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int findOrCreateRcAttributeId(String attributeName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM rc_attribute WHERE attribute_name = ?",
                    Integer.class,
                    attributeName);
        } catch (EmptyResultDataAccessException ex) {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO rc_attribute (attribute_name, attribute_path, display_name) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, attributeName);
                ps.setString(2, attributeName);
                ps.setString(3, attributeName);
                return ps;
            }, keyHolder);
            return keyHolder.getKey().intValue();
        }
    }

    public int findOrCreateDirectoryAttributeId(int directoryTypeId, String attributePath) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT id FROM directory_attribute
                            WHERE directory_type_id = ? AND attribute_path = ?
                            """,
                    Integer.class,
                    directoryTypeId,
                    attributePath);
        } catch (EmptyResultDataAccessException ex) {
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        """
                                INSERT INTO directory_attribute
                                    (directory_type_id, attribute_name, attribute_path, description)
                                VALUES (?, ?, ?, ?)
                                """,
                        Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, directoryTypeId);
                ps.setString(2, attributePath);
                ps.setString(3, attributePath);
                ps.setString(4, "Auto-created by Admin API");
                return ps;
            }, keyHolder);
            return keyHolder.getKey().intValue();
        }
    }
}
