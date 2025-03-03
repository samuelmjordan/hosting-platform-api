-- Core node table
CREATE TABLE node_ (
    id BIGSERIAL PRIMARY KEY,
    node_id TEXT NOT NULL,
    dedicated BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT node_node_id_unique UNIQUE (node_id)
);

-- Hetzner-specific details
CREATE TABLE hetzner_node_ (
    id BIGSERIAL PRIMARY KEY,
    node_id TEXT NOT NULL,
    hetzner_node_id BIGINT NOT NULL,
    hetzner_region TEXT,
    ipv4 TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT hetzner_node_node_id_fk FOREIGN KEY (node_id) REFERENCES node_(node_id),
    CONSTRAINT hetzner_node_hetzner_node_id_unique UNIQUE (hetzner_node_id),
    CONSTRAINT hetzner_node_ipv4_unique UNIQUE (ipv4)
);

-- Pterodactyl-specific details
CREATE TABLE pterodactyl_node_ (
    id BIGSERIAL PRIMARY KEY,
    node_id TEXT NOT NULL,
    pterodactyl_node_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pterodactyl_node_node_id_fk FOREIGN KEY (node_id) REFERENCES node_(node_id),
    CONSTRAINT pterodactyl_node_pterodactyl_node_id_unique UNIQUE (pterodactyl_node_id)
);

-- DNS record details
CREATE TABLE dns_a_record_ (
    id BIGSERIAL PRIMARY KEY,
    node_id TEXT NOT NULL,
    a_record_id TEXT NOT NULL,
    zone_id TEXT NOT NULL,
    zone_name TEXT NOT NULL,
    record_name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dns_a_record_node_id_fk FOREIGN KEY (node_id) REFERENCES node_(node_id),
    CONSTRAINT dns_a_record_a_record_id_unique UNIQUE (a_record_id),
    CONSTRAINT dns_a_record_record_name_unique UNIQUE (record_name),
    CONSTRAINT dns_a_record_ipv4_unique UNIQUE (content)
);

-- Indexes
CREATE INDEX idx_node_node_id ON node_(node_id);
CREATE INDEX idx_hetzner_node_node_id ON hetzner_node_(node_id);
CREATE INDEX idx_hetzner_node_hetzner_id ON hetzner_node_(hetzner_node_id);
CREATE INDEX idx_pterodactyl_node_node_id ON pterodactyl_node_(node_id);
CREATE INDEX idx_pterodactyl_node_pterodactyl_id ON pterodactyl_node_(pterodactyl_node_id);
CREATE INDEX idx_dns_a_record_node_id ON dns_a_record_(node_id);
CREATE INDEX idx_dns_a_record_a_record_id ON dns_a_record_(a_record_id);

-- Triggers for last_updated
CREATE TRIGGER update_node_last_updated
    BEFORE UPDATE ON node_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

CREATE TRIGGER update_hetzner_node_last_updated
    BEFORE UPDATE ON hetzner_node_
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