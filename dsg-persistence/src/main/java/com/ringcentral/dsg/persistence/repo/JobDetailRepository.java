package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.PendingJobDetailRow;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JobDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long insertPendingCreate(long jobId, String externalId, Long ruleId) {
        return insertPending(jobId, externalId, 1, ruleId, null);
    }

    public long insertPendingUpdate(long jobId, String externalId, String mailboxId) {
        return insertPending(jobId, externalId, 2, null, mailboxId);
    }

    private long insertPending(long jobId, String externalId, int operationId, Long ruleId, String mailboxId) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO job_detail (job_id, external_id, state_id, operation_id, rule_id, mailbox_id)
                            VALUES (?, ?, 1, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, jobId);
            ps.setString(2, externalId);
            ps.setInt(3, operationId);
            if (ruleId != null) {
                ps.setLong(4, ruleId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            if (mailboxId != null) {
                ps.setString(5, mailboxId);
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
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

    /** Job details still PENDING while parent job is READY (queue message may have been lost). */
    public List<PendingJobDetailRow> listPendingDetailsForReadyJobs() {
        return jdbcTemplate.query(
                """
                        SELECT jd.id, jd.job_id, j.account_id, jd.external_id, jd.rule_id,
                               jd.mailbox_id, ot.type AS operation
                        FROM job_detail jd
                        JOIN job j ON j.id = jd.job_id
                        JOIN job_state js ON js.id = j.state_id
                        JOIN job_state ds ON ds.id = jd.state_id
                        JOIN operation_type ot ON ot.id = jd.operation_id
                        WHERE js.state = 'READY' AND ds.state = 'PENDING'
                        ORDER BY jd.id
                        """,
                (rs, rowNum) -> new PendingJobDetailRow(
                        rs.getLong("id"),
                        rs.getLong("job_id"),
                        rs.getString("account_id"),
                        rs.getString("external_id"),
                        rs.getObject("rule_id") != null ? rs.getLong("rule_id") : null,
                        rs.getString("operation"),
                        rs.getString("mailbox_id")));
    }
}
