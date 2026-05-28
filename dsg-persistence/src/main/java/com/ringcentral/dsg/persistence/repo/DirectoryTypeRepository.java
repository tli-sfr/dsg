package com.ringcentral.dsg.persistence.repo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DirectoryTypeRepository {

    private final JdbcTemplate jdbcTemplate;

    public DirectoryTypeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Integer> findIdByName(String directoryTypeName) {
        try {
            Integer id = jdbcTemplate.queryForObject(
                    "SELECT id FROM directory_type WHERE directory_type = ?",
                    Integer.class,
                    directoryTypeName);
            return Optional.ofNullable(id);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
