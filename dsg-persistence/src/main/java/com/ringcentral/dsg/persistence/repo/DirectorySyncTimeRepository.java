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

    public void recordLatestJobCompletion(
            String accountId, int directoryTypeId, long jobId, String terminalState) {
        jdbcTemplate.update(
                """
                        UPDATE directory_sync_time dst
                        JOIN job j ON j.id = ?
                        JOIN job_state s ON s.state = ?
                        SET dst.latest_job_id = j.id,
                            dst.latest_job_start_time = j.created_on,
                            dst.latest_job_end_time = j.updated_on,
                            dst.latest_job_state = s.id
                        WHERE dst.account_id = ? AND dst.directory_type_id = ?
                        """,
                jobId,
                terminalState,
                accountId,
                directoryTypeId);
    }
}
