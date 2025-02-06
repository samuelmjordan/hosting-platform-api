CREATE TABLE prices (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    price_id TEXT NOT NULL,

    -- Product details
    product_id TEXT NOT NULL,
    spec_id TEXT NOT NULL,

    -- Archival details
    active BOOLEAN NOT NULL,

    -- Currencies
    currency TEXT NOT NULL,
    minor_amount INTEGER NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT prices_price_id_unique UNIQUE (price_id),
    CONSTRAINT prices_spec_id_currency_unique UNIQUE (spec_id, currency)
);

-- Indexes for performance
CREATE INDEX idx_prices_price_id ON prices (price_id);
CREATE INDEX idx_prices_spec_id ON prices (spec_id);

-- Trigger to automatically update last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_prices_last_updated
    BEFORE UPDATE ON prices
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();