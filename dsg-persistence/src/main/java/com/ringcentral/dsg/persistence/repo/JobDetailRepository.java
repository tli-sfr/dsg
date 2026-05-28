package com.ringcentral.dsg.persistence.repo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class JobDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertPendingCreate(long jobId, String externalId) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO job_detail (job_id, external_id, state_id, operation_id)
                            VALUES (?, ?, 1, 1)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, jobId);
            ps.setString(2, externalId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateState(long jobDetailId, String stateName, String mailboxId, String comment) {
        jdbcTemplate.update(
                """
                        UPDATE job_detail jd
                        JOIN job_state s ON s.state = ?
                        SET jd.state_id = s.id,
                            jd.mailbox_id = COALESCE(?, jd.mailbox_id),
                            jd.comment = COALESCE(?, jd.comment)
                        WHERE jd.id = ?
                        """,
                stateName,
                mailboxId,
                comment,
                jobDetailId);
    }

    public Optional<Long> findJobIdForDetail(long jobDetailId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT job_id FROM job_detail WHERE id = ?",
                    Long.class,
                    jobDetailId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
