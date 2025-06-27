CREATE TABLE resource_claim_ (
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    hetzner_node_id BIGINT NOT NULL,
    ram_gb BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT resource_claim_subscription_id_fk FOREIGN KEY (subscription_id)
        REFERENCES subscription_(subscription_id),
    CONSTRAINT resource_claim_hetzner_node_id_fk FOREIGN KEY (hetzner_node_id)
        REFERENCES node_(hetzner_node_id)
);

CREATE INDEX idx_resource_claim_subscription_id ON resource_claim_(subscription_id);
CREATE INDEX idx_resource_claim_hetzner_node_id ON resource_claim_(hetzner_node_id);

CREATE TRIGGER update_resource_claim_last_updated
    BEFORE UPDATE ON resource_claim_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();
