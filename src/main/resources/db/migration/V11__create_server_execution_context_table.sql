CREATE TABLE server_execution_context_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,

    subscription_id TEXT NOT NULL,
    step_type TEXT NOT NULL,
    mode TEXT NOT NULL, -- 'create', 'destroy', 'update', 'migrate'
    execution_status TEXT NOT NULL, -- 'in_progress', 'completed', 'failed'

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_server_execution_context_subscription_id FOREIGN KEY (subscription_id) REFERENCES subscription_ (subscription_id) ON DELETE CASCADE,
    CONSTRAINT server_execution_context_subscription_idunique UNIQUE (subscription_id)
);

-- Indexes
CREATE INDEX idx_server_execution_context_subscription_id ON server_execution_context_ (subscription_id);
CREATE INDEX idx_server_execution_context_execution_status ON server_execution_context_ (execution_status);

-- Update trigger
CREATE TRIGGER server_execution_context_last_updated
    BEFORE UPDATE ON server_execution_context_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();