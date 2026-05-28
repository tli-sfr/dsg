package com.ringcentral.dsg.persistence.repo;

import com.ringcentral.dsg.persistence.model.JobContext;
import com.ringcentral.dsg.persistence.model.JobReportData;
import com.ringcentral.dsg.persistence.model.JobReportData.JobFailureRow;
import com.ringcentral.dsg.persistence.model.JobSummaryData;
import java.sql.Timestamp;
import java.time.Instant;
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

    public Optional<JobContext> findJobContext(long jobId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                            SELECT j.id, j.account_id, j.directory_type_id, t.type, s.state
                            FROM job j
                            JOIN job_type t ON t.id = j.type_id
                            JOIN job_state s ON s.id = j.state_id
                            WHERE j.id = ?
                            """,
                    (rs, rowNum) -> new JobContext(
                            rs.getLong("id"),
                            rs.getString("account_id"),
                            rs.getInt("directory_type_id"),
                            rs.getString("type"),
                            rs.getString("state")),
                    jobId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void updateJobState(long jobId, String stateName) {
        jdbcTemplate.update(
                """
                        UPDATE job j
                        JOIN job_state s ON s.state = ?
                        SET j.state_id = s.id
                        WHERE j.id = ?
                        """,
                stateName,
                jobId);
    }

    public boolean hasNonTerminalJobDetails(long jobId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        WHERE jd.job_id = ?
                          AND s.state NOT IN ('SUCCEEDED', 'FAILED', 'COMPLETED', 'CANCELLED')
                        """,
                Integer.class,
                jobId);
        return count != null && count > 0;
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

    public Optional<JobReportData> findJobReportForAccount(long jobId, String accountId) {
        Optional<JobReportHeader> header = findJobReportHeader(jobId, accountId);
        if (header.isEmpty()) {
            return Optional.empty();
        }
        JobReportHeader job = header.get();

        List<JobFailureRow> failures = jdbcTemplate.query(
                """
                        SELECT jd.external_id, ot.type AS operation, jd.comment
                        FROM job_detail jd
                        JOIN job_state s ON s.id = jd.state_id
                        JOIN operation_type ot ON ot.id = jd.operation_id
                        WHERE jd.job_id = ? AND s.state = 'FAILED'
                        ORDER BY jd.id
                        """,
                (rs, rowNum) -> new JobFailureRow(
                        rs.getString("external_id"),
                        rs.getString("operation"),
                        rs.getString("comment")),
                jobId);

        return Optional.of(new JobReportData(
                job.jobId(),
                job.accountId(),
                job.jobType(),
                job.syncDirection(),
                job.state(),
                job.startedAt(),
                job.completedAt(),
                job.successCount(),
                job.failedCount(),
                failures));
    }

    public Optional<Long> findLatestJobIdForAccount(String accountId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT id FROM job WHERE account_id = ? ORDER BY id DESC LIMIT 1",
                    Long.class,
                    accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<JobSummaryData> listJobsForAccount(String accountId, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT j.id,
                               t.type AS job_type,
                               CASE j.direction_id
                                   WHEN 1 THEN 'DIR_TO_RC'
                                   WHEN 2 THEN 'RC_TO_DIR'
                                   ELSE 'DIR_TO_RC'
                               END AS sync_direction,
                               s.state,
                               j.created_on AS started_at,
                               CASE
                                   WHEN s.state IN ('COMPLETED', 'CANCELLED') THEN j.updated_on
                                   ELSE NULL
                               END AS completed_at,
                               (SELECT COUNT(*) FROM job_detail jd
                                JOIN job_state js ON js.id = jd.state_id
                                WHERE jd.job_id = j.id AND js.state = 'SUCCEEDED') AS success_count,
                               (SELECT COUNT(*) FROM job_detail jd
                                JOIN job_state js ON js.id = jd.state_id
                                WHERE jd.job_id = j.id AND js.state = 'FAILED') AS failed_count
                        FROM job j
                        JOIN job_type t ON t.id = j.type_id
                        JOIN job_state s ON s.id = j.state_id
                        WHERE j.account_id = ?
                        ORDER BY j.id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new JobSummaryData(
                        rs.getLong("id"),
                        rs.getString("job_type"),
                        rs.getString("sync_direction"),
                        rs.getString("state"),
                        toInstant(rs.getTimestamp("started_at")),
                        toInstant(rs.getTimestamp("completed_at")),
                        rs.getInt("success_count"),
                        rs.getInt("failed_count")),
                accountId,
                limit);
    }

    private Optional<JobReportHeader> findJobReportHeader(long jobId, String accountId) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    """
                            SELECT j.id,
                                   j.account_id,
                                   t.type AS job_type,
                                   CASE j.direction_id
                                       WHEN 1 THEN 'DIR_TO_RC'
                                       WHEN 2 THEN 'RC_TO_DIR'
                                       ELSE 'DIR_TO_RC'
                                   END AS sync_direction,
                                   s.state,
                                   j.created_on AS started_at,
                                   CASE
                                       WHEN s.state IN ('COMPLETED', 'CANCELLED') THEN j.updated_on
                                       ELSE NULL
                                   END AS completed_at,
                                   (SELECT COUNT(*) FROM job_detail jd
                                    JOIN job_state js ON js.id = jd.state_id
                                    WHERE jd.job_id = j.id AND js.state = 'SUCCEEDED') AS success_count,
                                   (SELECT COUNT(*) FROM job_detail jd
                                    JOIN job_state js ON js.id = jd.state_id
                                    WHERE jd.job_id = j.id AND js.state = 'FAILED') AS failed_count
                            FROM job j
                            JOIN job_type t ON t.id = j.type_id
                            JOIN job_state s ON s.id = j.state_id
                            WHERE j.id = ? AND j.account_id = ?
                            """,
                    (rs, rowNum) -> new JobReportHeader(
                            rs.getLong("id"),
                            rs.getString("account_id"),
                            rs.getString("job_type"),
                            rs.getString("sync_direction"),
                            rs.getString("state"),
                            toInstant(rs.getTimestamp("started_at")),
                            toInstant(rs.getTimestamp("completed_at")),
                            rs.getInt("success_count"),
                            rs.getInt("failed_count")),
                    jobId,
                    accountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private record JobReportHeader(
            long jobId,
            String accountId,
            String jobType,
            String syncDirection,
            String state,
            Instant startedAt,
            Instant completedAt,
            int successCount,
            int failedCount) {}
}
