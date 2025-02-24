CREATE TABLE node_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    node_id TEXT NOT NULL,

    -- External identifiers
    pterodactyl_node_id TEXT,
    hetzner_node_id BIGINT,

    -- Info
    dedicated BOOLEAN,
    ipv4 TEXT,
    hetzner_region TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT node_node_id_unique UNIQUE (node_id),
    CONSTRAINT node_pterodactyl_node_id_unique UNIQUE (pterodactyl_node_id),
    CONSTRAINT node_hetzner_node_id_unique UNIQUE (hetzner_node_id),
    CONSTRAINT node_ipv4_unique UNIQUE (ipv4)
);

-- Indexes
CREATE INDEX idx_node_node_id ON node_ (node_id);
CREATE INDEX idx_node_pterodactyl_node_id ON node_ (pterodactyl_node_id);
CREATE INDEX idx_node_hetzner_node_id_unique ON node_ (hetzner_node_id);

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_plan_last_updated
    BEFORE UPDATE ON node_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();