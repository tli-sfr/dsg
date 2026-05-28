package com.ringcentral.dsg.persistence.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DirectorySyncTimeRepository {

    private final JdbcTemplate jdbcTemplate;

    public DirectorySyncTimeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(
            String accountId,
            int directoryTypeId,
            int jobTypeId,
            int directionId,
            int frequencyId,
            String cronExpression) {
        jdbcTemplate.update("""
                INSERT INTO directory_sync_time
                    (account_id, directory_type_id, job_type_id, direction_id, frequency_id, cron_expression)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    job_type_id = VALUES(job_type_id),
                    direction_id = VALUES(direction_id),
                    frequency_id = VALUES(frequency_id),
                    cron_expression = VALUES(cron_expression)
                """,
                accountId,
                directoryTypeId,
                jobTypeId,
                directionId,
                frequencyId,
                cronExpression);
    }
}
