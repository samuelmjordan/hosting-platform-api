CREATE TABLE java_server_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    server_id TEXT NOT NULL,
    pterodactyl_id TEXT,

    -- Infra identifier
    hetzner_id TEXT,

    -- Payment and specification details
    subscription_id TEXT NOT NULL,
    plan_id TEXT NOT NULL,

    -- Server states
    provisioning_state TEXT NOT NULL,
    retry_count SMALLINT NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT java_server_server_id_unique UNIQUE (server_id),
    CONSTRAINT java_server_pterodactyl_id_unique UNIQUE (pterodactyl_id),
    CONSTRAINT fk_java_server_subscription_ FOREIGN KEY (subscription_id)
        REFERENCES subscription_ (subscription_id),
    CONSTRAINT fk_java_server_plan_ FOREIGN KEY (plan_id)
        REFERENCES plan_ (plan_id)
);

-- Indexes
CREATE INDEX idx_java_server_server_id ON java_server_ (server_id);
CREATE INDEX idx_java_server_pterodactyl_id ON java_server_ (pterodactyl_id);
CREATE INDEX idx_java_server_hetzner_id ON java_server_ (hetzner_id);
CREATE INDEX idx_java_server_subscription_id ON java_server_ (subscription_id);
CREATE INDEX idx_java_server_plan_id ON java_server_ (plan_id);

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_plan_last_updated
    BEFORE UPDATE ON java_server_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();