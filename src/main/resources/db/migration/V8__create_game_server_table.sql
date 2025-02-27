CREATE TABLE game_server_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    server_id TEXT NOT NULL,

    -- External identifier
    pterodactyl_server_id BIGINT,

    -- Payment and specification details
    subscription_id TEXT NOT NULL,
    plan_id TEXT NOT NULL,

    -- Infra
    node_id TEXT,
    allocation_id BIGINT,
    port BIGINT,

    -- DNS
    c_name_record_id TEXT,
    zone_name TEXT,
    record_name TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT game_server_server_id_unique UNIQUE (server_id),
    CONSTRAINT game_server_c_name_record_id_unique UNIQUE (c_name_record_id),
    CONSTRAINT game_server_record_name_unique UNIQUE (record_name),
    CONSTRAINT game_server_pterodactyl_server_id_unique UNIQUE (pterodactyl_server_id),
    CONSTRAINT fk_game_server_subscription_ FOREIGN KEY (subscription_id)
        REFERENCES subscription_ (subscription_id),
    CONSTRAINT fk_game_server_plan_ FOREIGN KEY (plan_id)
        REFERENCES plan_ (plan_id),
    CONSTRAINT fk_game_server_node_ FOREIGN KEY (node_id)
        REFERENCES node_ (node_id)
);

-- Indexes
CREATE INDEX idx_game_server_server_id ON game_server_ (server_id);
CREATE INDEX idx_game_server_subscription_id ON game_server_ (subscription_id);
CREATE INDEX idx_game_server_plan_id ON game_server_ (plan_id);
CREATE INDEX idx_game_server_node_id ON game_server_ (node_id);

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_plan_last_updated
    BEFORE UPDATE ON game_server_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();