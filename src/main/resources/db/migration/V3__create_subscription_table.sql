CREATE TABLE subscription_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,

    -- Customer details
    customer_id TEXT NOT NULL,
    
    -- Subscription details
    status_ TEXT NOT NULL,
    price_id TEXT NOT NULL,
    
    -- Period tracking
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT subscription_subscription_id_unique UNIQUE (subscription_id),
    CONSTRAINT fk_subscription_users FOREIGN KEY (customer_id)
        REFERENCES user_ (customer_id),
    CONSTRAINT fk_subscription_prices FOREIGN KEY (price_id)
        REFERENCES price_ (price_id)
);

-- Indexes for performance
CREATE INDEX idx_subscription_subscription_id ON subscription_ (subscription_id);
CREATE INDEX idx_subscription_customer_id ON subscription_ (customer_id);

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_subscription_last_updated
    BEFORE UPDATE ON subscription_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();