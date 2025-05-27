-- Hetzner-specific details
CREATE TABLE cloud_node_ (
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    node_id BIGINT NOT NULL,
    hetzner_region TEXT,
    ipv4 TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT cloud_node_subscription_id_fk FOREIGN KEY (subscription_id) 
        REFERENCES subscription_(subscription_id),
    CONSTRAINT cloud_node_ipv4_unique UNIQUE (ipv4)
);
CREATE INDEX idx_cloud_node_subscription_id ON cloud_node_(subscription_id);
CREATE INDEX idx_cloud_node_node_id ON cloud_node_(node_id);

-- Pterodactyl node details
CREATE TABLE pterodactyl_node_ (
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    pterodactyl_node_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pterodactyl_node_subscription_id_fk FOREIGN KEY (subscription_id) 
        REFERENCES subscription_(subscription_id),
    CONSTRAINT pterodactyl_node_pterodactyl_node_id_unique UNIQUE (pterodactyl_node_id)
);
CREATE INDEX idx_pterodactyl_node_subscription_id ON pterodactyl_node_(subscription_id);
CREATE INDEX idx_pterodactyl_node_pterodactyl_id ON pterodactyl_node_(pterodactyl_node_id);

-- Pterodactyl allocation details
CREATE TABLE pterodactyl_allocation_ (
    id BIGSERIAL PRIMARY KEY,
    allocation_id BIGINT NOT NULL,
    ip TEXT NOT NULL,
    port BIGINT NOT NULL,
    alias TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pterodactyl_node_id TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pterodactyl_allocation_pterodactyl_node_id_fk FOREIGN KEY (pterodactyl_node_id) 
        REFERENCES pterodactyl_node_(pterodactyl_node_id) ON DELETE CASCADE,
    CONSTRAINT pterodactyl_allocation_allocation_id_unique UNIQUE (allocation_id),
    CONSTRAINT pterodactyl_allocation_ip_port_unique UNIQUE (ip, port)
);
CREATE INDEX idx_pterodactyl_allocation_allocation_id ON pterodactyl_allocation_(allocation_id);
CREATE INDEX idx_pterodactyl_allocation_pterodactyl_node_id ON pterodactyl_allocation_(pterodactyl_node_id);

-- DNS record details
CREATE TABLE dns_a_record_ (
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    a_record_id TEXT NOT NULL,
    zone_id TEXT NOT NULL,
    zone_name TEXT NOT NULL,
    record_name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dns_a_record_subscription_id_fk FOREIGN KEY (subscription_id) REFERENCES subscription_(subscription_id),
    CONSTRAINT dns_a_record_a_record_id_unique UNIQUE (a_record_id),
    CONSTRAINT dns_a_record_record_name_unique UNIQUE (record_name),
    CONSTRAINT dns_a_record_ipv4_unique UNIQUE (content)
);
CREATE INDEX idx_dns_a_record_subscription_id ON dns_a_record_(subscription_id);
CREATE INDEX idx_dns_a_record_a_record_id ON dns_a_record_(a_record_id);

-- Triggers for last_updated
CREATE TRIGGER update_cloud_node_last_updated
    BEFORE UPDATE ON cloud_node_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

CREATE TRIGGER update_pterodactyl_node_last_updated
    BEFORE UPDATE ON pterodactyl_node_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

CREATE TRIGGER update_dns_a_record_last_updated
    BEFORE UPDATE ON dns_a_record_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();