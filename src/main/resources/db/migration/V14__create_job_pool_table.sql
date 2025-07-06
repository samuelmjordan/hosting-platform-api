CREATE TABLE job_queue_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    job_id TEXT NOT NULL UNIQUE,
    dedup_key TEXT NOT NULL,

    -- Metadata
    type TEXT NOT NULL,
    status TEXT NOT NULL,
    payload TEXT NOT NULL,

    -- Retry data
    retry_count INTEGER NOT NULL DEFAULT 0,
    maximum_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,

    -- Scheduling
    delayed_until TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Audit fields
    processed_at TIMESTAMP WITH TIME ZONE,
    last_seen TIMESTAMP WITH TIME ZONE,
    duplicate_count INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- then add your partial unique indexes
CREATE UNIQUE INDEX idx_unique_dedup_pending_retrying
ON job_queue_ (type, dedup_key)
WHERE status = 'PENDING';

CREATE UNIQUE INDEX idx_unique_dedup_processing
ON job_queue_ (type, dedup_key)
WHERE status = 'PROCESSING';

-- Indexes
CREATE INDEX idx_job_queue_pending_ready ON job_queue_(status, delayed_until, job_id)
    WHERE status = 'PENDING';
CREATE INDEX idx_job_queue_retrying_ready ON job_queue_(delayed_until)
    WHERE status = 'RETRYING';
CREATE INDEX idx_job_queue_processing ON job_queue_(job_id)
    WHERE status = 'PROCESSING';
CREATE INDEX idx_job_queue_cleanup ON job_queue_(status, processed_at)
    WHERE status IN ('COMPLETED', 'DEAD_LETTER');
CREATE INDEX idx_job_queue_dedup_active ON job_queue_(type, dedup_key, status);

-- Update trigger for last_updated
CREATE TRIGGER update_job_queue_last_updated
    BEFORE UPDATE ON job_queue_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();