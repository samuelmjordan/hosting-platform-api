package com.mc_host.api.queue.v2.service;

import com.mc_host.api.queue.v2.model.Job;
import com.mc_host.api.queue.v2.model.JobStatus;
import com.mc_host.api.queue.v2.model.JobType;
import com.mc_host.api.repository.BaseRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JobRepository extends BaseRepository {

	public JobRepository(JdbcTemplate jdbc) {
		super(jdbc);
	}

	private final RowMapper<Job> jobMapper = (rs, rowNum) -> new Job(
		rs.getString("job_id"),
		rs.getString("dedup_key"),
		JobType.valueOf(rs.getString("type")),
		JobStatus.valueOf(rs.getString("status")),
		rs.getString("payload"),
		rs.getInt("retry_count"),
		rs.getInt("maximum_retries"),
		rs.getString("error_message"),
		rs.getTimestamp("delayed_until") != null ?
				rs.getTimestamp("delayed_until").toInstant() : null
	);

	public Optional<Job> findById(String jobId) {
		return selectOne(
			"SELECT * FROM job_queue_ WHERE job_id = ?",
			jobMapper,
			jobId
		);
	}

	public Optional<Job> findDuplicateJob(JobType type, String dedupKey) {
		return selectOne("""
			SELECT *
			FROM job_queue_
			WHERE type = ?
			AND dedup_key = ?
			AND status IN ('PENDING', 'RETRYING')
			""",
			jobMapper,
			type.name(),
			dedupKey
		);
	}

	public Optional<Job> findActiveDuplicateJobs(JobType type, String dedupKey) {
		return selectOne("""
			SELECT *
			FROM job_queue_
			WHERE type = ?
			AND dedup_key = ?
			AND status IN ('PROCESSING')
			""",
			jobMapper,
			type.name(),
			dedupKey
		);
	}

	public List<Job> findPendingJobs(int limit) {
		return selectMany(
			"""
			SELECT *
			FROM job_queue_
			WHERE status = 'PENDING'
			AND (delayed_until <= NOW())
			ORDER BY delayed_until ASC, job_id ASC
			LIMIT ?
			""",
			jobMapper,
			limit
		);
	}

	public List<Job> findRetryableJobs() {
		return selectMany(
			"""
			SELECT *
			FROM job_queue_
			WHERE status = 'RETRYING'
			AND (delayed_until <= NOW())
			ORDER BY delayed_until ASC
			""",
			jobMapper
		);
	}

	public List<Job> claimJobsByStatus(int batchSize, JobStatus status) {
		return selectMany(
			"""
			UPDATE job_queue_
			SET
				status = 'PROCESSING'
			WHERE job_id IN (
				SELECT job_id FROM job_queue_
				WHERE status = ?
				AND delayed_until <= NOW()
			ORDER BY delayed_until ASC, job_id ASC
			LIMIT ?
			FOR UPDATE SKIP LOCKED
			)
			RETURNING *;
			""",
			jobMapper,
			status.name(),
			batchSize
		);
	}

	public void insertJob(Job job) {
		execute(
			"""
			INSERT INTO job_queue_ (
				job_id,
				dedup_key,
				type,
				status,
				payload,
				retry_count,
				maximum_retries,
				delayed_until)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""",
			job.jobId(),
			job.dedupKey(),
			job.type().name(),
			job.status().name(),
			job.payload(),
			job.retryCount(),
			job.maximumRetries(),
			job.delayedUntil() != null ? java.sql.Timestamp.from(job.delayedUntil()) : null
		);
	}

	public void mergeJobKeepingEarliestSchedule(String jobId, String newPayload, Instant newDelayedUntil) {
		upsert(
				"""
				UPDATE job_queue_
				SET payload = ?,
					duplicate_count = duplicate_count + 1,
					last_seen = NOW(),
					delayed_until = LEAST(COALESCE(delayed_until, ?), COALESCE(?, delayed_until))
				WHERE job_id = ?
				""",
				ps -> {
					ps.setString(1, newPayload);
					ps.setTimestamp(2, newDelayedUntil != null ? java.sql.Timestamp.from(newDelayedUntil) : null);
					ps.setTimestamp(3, newDelayedUntil != null ? java.sql.Timestamp.from(newDelayedUntil) : null);
					ps.setString(4, jobId);
				}
		);
	}

	public void updateJobStatus(String jobId, JobStatus status, String errorMessage) {
		execute(
				"UPDATE job_queue_ SET status = ?, error_message = ?, processed_at = NOW() WHERE job_id = ?",
				status.name(),
				errorMessage,
				jobId
		);
	}

	public void updateJobForNonFailureRetry(String jobId, Instant delayedUntil) {
		execute(
			"""
			UPDATE job_queue_
			SET
				status = 'PENDING',
				delayed_until = ?
				WHERE job_id = ?
			""",
			java.sql.Timestamp.from(delayedUntil),
			jobId
		);
	}

	public void updateJobForRetry(String jobId, int newRetryCount, Instant delayedUntil, String errorMessage) {
		execute(
			"""
			UPDATE job_queue_
			SET status = 'RETRYING', retry_count = ?, delayed_until = ?, error_message = ?
			WHERE job_id = ?
			""",
			newRetryCount,
			java.sql.Timestamp.from(delayedUntil),
			errorMessage,
			jobId
		);
	}

	public void moveToDeadLetter(String jobId, String errorMessage) {
		execute(
				"UPDATE job_queue_ SET status = 'DEAD_LETTER', error_message = ?, processed_at = NOW() WHERE job_id = ?",
				errorMessage,
				jobId
		);
	}
}
