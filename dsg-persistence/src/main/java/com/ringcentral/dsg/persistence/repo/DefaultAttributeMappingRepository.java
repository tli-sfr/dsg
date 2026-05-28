package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.AttributeMappingView;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DefaultAttributeMappingRepository {

    private final JdbcTemplate jdbcTemplate;

    public DefaultAttributeMappingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AttributeMappingView> listDefaults(int directoryTypeId, int directionId) {
        return jdbcTemplate.query(
                """
                        SELECT da.attribute_path,
                               da.attribute_name,
                               ra.attribute_name,
                               dam.display_sequence
                        FROM default_attribute_mapping dam
                        JOIN directory_attribute da ON da.id = dam.directory_attribute_id
                        JOIN rc_attribute ra ON ra.id = dam.rc_attribute_id
                        WHERE dam.directory_type_id = ? AND dam.direction_id = ?
                        ORDER BY dam.display_sequence
                        """,
                (rs, rowNum) -> new AttributeMappingView(
                        rs.getString("attribute_path"),
                        rs.getString("attribute_name"),
                        rs.getString("attribute_name"),
                        rs.getInt("display_sequence")),
                directoryTypeId,
                directionId);
    }
}
