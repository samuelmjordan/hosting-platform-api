CREATE TABLE price_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    price_id TEXT NOT NULL,

    -- Product details
    product_id TEXT NOT NULL,

    -- Archival details
    active BOOLEAN NOT NULL,

    -- Currencies as jsonb
    minor_amounts JSONB NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT prices_price_id_unique UNIQUE (price_id),
    CONSTRAINT valid_currencies CHECK (jsonb_object_keys(minor_amounts) <@ ARRAY['USD', 'EUR', 'GBP']::text[])
);

-- Indexes for performance
CREATE INDEX idx_price_price_id ON price_ (price_id);
CREATE INDEX idx_price_product_id ON price_ (product_id);
CREATE INDEX idx_price_currencies ON price_ USING gin (minor_amounts);

CREATE TRIGGER update_prices_last_updated
    BEFORE UPDATE ON price_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();