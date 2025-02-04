CREATE TABLE subscriptions (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    
    -- Subscription details
    status VARCHAR(50) NOT NULL,
    price_id TEXT NOT NULL,
    
    -- Period tracking
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT subscriptions_subscription_id_unique UNIQUE (subscription_id),
    CONSTRAINT fk_subscriptions_users FOREIGN KEY (customer_id)
        REFERENCES users (customer_id)
        ON DELETE RESTRICT
);

-- Indexes for performance
CREATE INDEX idx_subscriptions_customer_id ON subscriptions (customer_id);

-- Trigger to automatically update last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_subscriptions_last_updated
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();