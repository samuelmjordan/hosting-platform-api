CREATE TABLE job_audit_ (
    -- same fields as job_queue_
    id BIGSERIAL PRIMARY KEY,
    job_id TEXT NOT NULL,
    dedup_key TEXT,
    type TEXT,
    status TEXT,
    payload TEXT,
    retry_count INTEGER,
    maximum_retries INTEGER,
    error_message TEXT,
    delayed_until TIMESTAMP WITH TIME ZONE,
    processed_at TIMESTAMP WITH TIME ZONE,
    last_seen TIMESTAMP WITH TIME ZONE,
    duplicate_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE,
    last_updated TIMESTAMP WITH TIME ZONE,

    -- audit metadata
    archived_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- indexes for analytics queries
CREATE INDEX idx_job_audit_type_status ON job_audit_(type, status);
CREATE INDEX idx_job_audit_created_date ON job_audit_(created_at);
CREATE INDEX idx_job_audit_archived_date ON job_audit_(archived_at);
CREATE INDEX idx_job_audit_job_id ON job_audit_(job_id);