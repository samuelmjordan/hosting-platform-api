CREATE TABLE plan_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    plan_id TEXT NOT NULL,
    specification_id TEXT NOT NULL,
    price_id TEXT NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT plan_plan_id_unique UNIQUE (plan_id),
    CONSTRAINT plan_price_id_unique UNIQUE (price_id),
    CONSTRAINT fk_plan_game_server_specification FOREIGN KEY (specification_id)
        REFERENCES game_server_specification_ (specification_id),
    CONSTRAINT fk_plan_price FOREIGN KEY (price_id)
        REFERENCES price_ (price_id)
);

-- Indexes
CREATE INDEX idx_plan_id_plan_id ON plan_ (plan_id);
CREATE INDEX idx_plan_id_specification_id ON plan_ (specification_id);
CREATE INDEX idx_plan_id_price_id ON plan_ (price_id);

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_plan_last_updated
    BEFORE UPDATE ON plan_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();