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
				-- additional safety: ensure no other job with same dedup is already processing
				AND NOT EXISTS (
					SELECT 1 FROM job_queue_ j2 
					WHERE j2.type = job_queue_.type 
					AND j2.dedup_key = job_queue_.dedup_key 
					AND j2.status = 'PROCESSING'
				)
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

	public Job upsertJob(Job job) {
		return selectOne(
			"""
			INSERT INTO job_queue_ (
				job_id,
				dedup_key,
				type,
				status,
				payload,
				retry_count,
				maximum_retries,
				delayed_until
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (type, dedup_key) 
			WHERE status IN ('PENDING', 'RETRYING')
			DO UPDATE SET
				payload = EXCLUDED.payload,
				duplicate_count = job_queue_.duplicate_count + 1,
				last_seen = NOW(),
				delayed_until = LEAST(
					COALESCE(job_queue_.delayed_until, EXCLUDED.delayed_until), 
					COALESCE(EXCLUDED.delayed_until, job_queue_.delayed_until)
				)
			RETURNING *
			""",
			jobMapper,
			job.jobId(),
			job.dedupKey(),
			job.type().name(),
			job.status().name(),
			job.payload(),
			job.retryCount(),
			job.maximumRetries(),
			job.delayedUntil() != null ? java.sql.Timestamp.from(job.delayedUntil()) : null
		).orElseThrow(() -> new RuntimeException("upsert failed to return job"));
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

	public int archiveAndDeleteCompletedJobs() {
		return execute("""
			WITH archived AS (
				INSERT INTO job_audit_ (
					job_id,
					dedup_key,
					type,
					status,
					payload,
					retry_count,
					maximum_retries,
					error_message,
					delayed_until,
					processed_at,
					last_seen,
					duplicate_count,
					created_at,
					last_updated
				)
				SELECT
					job_id,
					dedup_key,
					type,
					status,
					payload,
					retry_count,
					maximum_retries,
					error_message,
					delayed_until,
					processed_at,
					last_seen,
					duplicate_count,
					created_at,
					last_updated
				FROM job_queue_
				WHERE status IN ('COMPLETED', 'DEAD_LETTER')
				AND processed_at < NOW() - INTERVAL '1 minute'
				RETURNING job_id
			)
			DELETE FROM job_queue_
			WHERE job_id IN (SELECT job_id FROM archived)
			""");
	}

	public boolean tryAcquireCleanupLock() {
		try {
			return selectOne(
				"SELECT pg_try_advisory_lock(12345678)",
				(rs, rowNum) -> rs.getBoolean(1)
			).orElse(false);
		} catch (Exception e) {
			return false;
		}
	}

	public void releaseCleanupLock() {
		try {
			execute("SELECT pg_advisory_unlock(12345678)");
		} catch (Exception e) {
		}
	}
}