-- Core game server table
CREATE TABLE game_server_ (
    id BIGSERIAL PRIMARY KEY,
    server_id TEXT NOT NULL,
    subscription_id TEXT NOT NULL,
    plan_id TEXT NOT NULL,
    node_id TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT game_server_server_id_unique UNIQUE (server_id),
    CONSTRAINT fk_game_server_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscription_ (subscription_id),
    CONSTRAINT fk_game_server_plan FOREIGN KEY (plan_id)
        REFERENCES plan_ (plan_id),
    CONSTRAINT fk_game_server_node FOREIGN KEY (node_id)
        REFERENCES node_ (node_id)
);

-- Pterodactyl-specific details
CREATE TABLE pterodactyl_server_ (
    id BIGSERIAL PRIMARY KEY,
    server_id TEXT NOT NULL,
    pterodactyl_server_id BIGINT NOT NULL,
    allocation_id BIGINT,
    port BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pterodactyl_server_server_id_fk FOREIGN KEY (server_id) 
        REFERENCES game_server_(server_id),
    CONSTRAINT pterodactyl_server_pterodactyl_server_id_unique UNIQUE (pterodactyl_server_id)
);

-- DNS CNAME record details
CREATE TABLE dns_c_name_record_ (
    id BIGSERIAL PRIMARY KEY,
    server_id TEXT NOT NULL,
    c_name_record_id TEXT NOT NULL,
    zone_name TEXT NOT NULL,
    record_name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dns_c_record_server_id_fk FOREIGN KEY (server_id) 
        REFERENCES game_server_(server_id),
    CONSTRAINT dns_c_record_c_name_record_id_unique UNIQUE (c_name_record_id),
    CONSTRAINT dns_c_record_record_name_unique UNIQUE (record_name)
);

-- Indexes
CREATE INDEX idx_game_server_server_id ON game_server_(server_id);
CREATE INDEX idx_game_server_subscription_id ON game_server_(subscription_id);
CREATE INDEX idx_game_server_plan_id ON game_server_(plan_id);
CREATE INDEX idx_game_server_node_id ON game_server_(node_id);

CREATE INDEX idx_pterodactyl_server_server_id ON pterodactyl_server_(server_id);
CREATE INDEX idx_pterodactyl_server_pterodactyl_id ON pterodactyl_server_(pterodactyl_server_id);

CREATE INDEX idx_dns_c_name_record__server_id ON dns_c_name_record_(server_id);
CREATE INDEX idx_dns_c_name_record__c_name_record_id ON dns_c_name_record_(c_name_record_id);

-- Triggers for last_updated
CREATE TRIGGER update_game_server_last_updated
    BEFORE UPDATE ON game_server_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

CREATE TRIGGER update_pterodactyl_server_last_updated
    BEFORE UPDATE ON pterodactyl_server_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

CREATE TRIGGER update_dns_c_record_last_updated
    BEFORE UPDATE ON dns_c_name_record_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();