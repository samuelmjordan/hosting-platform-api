-- Base
CREATE TABLE node_ (
    id BIGSERIAL PRIMARY KEY,
    hetzner_node_id BIGINT NOT NULL,
    hetzner_region TEXT NOT NULL,
    ipv4 TEXT NOT NULL,
    dedicated BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT node_hetzner_node_id_unique UNIQUE (hetzner_node_id)
);

CREATE INDEX idx_node_hetzner_node_id ON node_(hetzner_node_id);

CREATE TRIGGER update_node_last_updated
    BEFORE UPDATE ON node_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

-------------------------------------------------------------------------------------------------------------------------

-- Dedicated
CREATE TABLE dedicated_node_ (
    id BIGSERIAL PRIMARY KEY,
    hetzner_node_id BIGINT NOT NULL,
    dedicated_product TEXT NOT NULL,
    active BOOLEAN NOT NULL,
    total_ram_gb BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT dedicated_node_hetzner_node_id_fk FOREIGN KEY (hetzner_node_id)
        REFERENCES node_(hetzner_node_id) ON DELETE CASCADE,
    CONSTRAINT dedicated_node_hetzner_node_id_unique UNIQUE (hetzner_node_id)
);

CREATE INDEX idx_dedicated_node_hetzner_node_id ON dedicated_node_(hetzner_node_id);

CREATE TRIGGER update_dedicated_node_last_updated
    BEFORE UPDATE ON dedicated_node_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

-------------------------------------------------------------------------------------------------------------------------

-- Cloud
CREATE TABLE cloud_node_ (
    id BIGSERIAL PRIMARY KEY,
    hetzner_node_id BIGINT NOT NULL,
    cloud_product TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT cloud_node_hetzner_node_id_fk FOREIGN KEY (hetzner_node_id)
        REFERENCES node_(hetzner_node_id) ON DELETE CASCADE,
    CONSTRAINT cloud_node_hetzner_node_id_unique UNIQUE (hetzner_node_id)
);

CREATE INDEX idx_cloud_node_hetzner_node_id ON cloud_node_(hetzner_node_id);

CREATE TRIGGER update_cloud_node_last_updated
    BEFORE UPDATE ON cloud_node_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();