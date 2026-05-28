package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.JobReportData;
import com.ringcentral.dsg.persistence.model.JobReportData.JobFailureRow;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JobRepository {

    private static final List<Integer> NON_TERMINAL_STATE_IDS = List.of(1, 2, 3, 4, 7);

    private final JdbcTemplate jdbcTemplate;

    public JobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasActiveJob(String accountId) {
        String placeholders = String.join(",", NON_TERMINAL_STATE_IDS.stream().map(id -> "?").toList());
        Object[] params = new Object[NON_TERMINAL_STATE_IDS.size() + 1];
        params[0] = accountId;
        for (int i = 0; i < NON_TERMINAL_STATE_IDS.size(); i++) {
            params[i + 1] = NON_TERMINAL_STATE_IDS.get(i);
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job WHERE account_id = ? AND state_id IN (" + placeholders + ")",
                Integer.class,
                params);
        return count != null && count > 0;
    }

    public long createJob(String accountId, int jobTypeId, int directoryTypeId, int directionId) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO job (type_id, directory_type_id, direction_id, account_id, state_id)
                            VALUES (?, ?, ?, ?, 1)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, jobTypeId);
            ps.setInt(2, directoryTypeId);
            ps.setInt(3, directionId);
            ps.setString(4, accountId);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<String> findJobStateName(long jobId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                            SELECT s.state
                            FROM job j
                            JOIN job_state s ON s.id = j.state_id
                            WHERE j.id = ?
                            """,
                    String.class,
                    jobId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<JobReportData> findJobReport(long jobId) {
        try {
            jdbcTemplate.queryForObject("SELECT id FROM job WHERE id = ?", Long.class, jobId);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }

        Integer successCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        WHERE jd.job_id = ? AND s.state = 'SUCCEEDED'
                        """,
                Integer.class,
                jobId);
        Integer failedCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        WHERE jd.job_id = ? AND s.state = 'FAILED'
                        """,
                Integer.class,
                jobId);

        List<JobFailureRow> failures = jdbcTemplate.query(
                """
                        SELECT jd.external_id, jd.comment
                        FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        WHERE jd.job_id = ? AND s.state = 'FAILED'
                        """,
                (rs, rowNum) -> new JobFailureRow(rs.getString("external_id"), rs.getString("comment")),
                jobId);

        return Optional.of(new JobReportData(
                jobId,
                successCount != null ? successCount : 0,
                failedCount != null ? failedCount : 0,
                failures));
    }
}
