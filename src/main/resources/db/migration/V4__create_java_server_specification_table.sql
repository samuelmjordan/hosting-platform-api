CREATE TABLE java_server_specification_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    specification_id TEXT NOT NULL,
    
    -- Specification details
    title TEXT NOT NULL,
    caption TEXT NOT NULL,
    ram_gb TEXT NOT NULL,
    vcpu TEXT NOT NULL,
    ssd_gb TEXT NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT java_server_specification_specification_id_unique UNIQUE (specification_id)
);

-- Indexes
CREATE INDEX idx_java_server_specification_id_specification_id ON java_server_specification_ (specification_id);

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_java_server_specification_last_updated
    BEFORE UPDATE ON java_server_specification_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();