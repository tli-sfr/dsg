package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.AttributeCatalogEntry;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AttributeCatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttributeCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AttributeCatalogEntry> listDirectoryAttributes(int directoryTypeId) {
        return jdbcTemplate.query(
                """
                        SELECT attribute_name, attribute_path, COALESCE(description, attribute_name)
                        FROM directory_attribute
                        WHERE directory_type_id = ?
                        ORDER BY attribute_name
                        """,
                (rs, rowNum) -> new AttributeCatalogEntry(
                        rs.getString("attribute_name"),
                        rs.getString("attribute_path"),
                        rs.getString(3)),
                directoryTypeId);
    }

    public List<AttributeCatalogEntry> listRcAttributes() {
        return jdbcTemplate.query(
                """
                        SELECT attribute_name, attribute_path, COALESCE(display_name, attribute_name)
                        FROM rc_attribute
                        ORDER BY attribute_name
                        """,
                (rs, rowNum) -> new AttributeCatalogEntry(
                        rs.getString("attribute_name"),
                        rs.getString("attribute_path"),
                        rs.getString(3)));
    }
}
