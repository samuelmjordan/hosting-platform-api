CREATE TABLE subscription_user_metadata_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    title TEXT NOT NULL,
    caption TEXT NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT subscription_user_metadata_subscription_id_unique UNIQUE (subscription_id),
    CONSTRAINT fk_subscription_user_metadata_subscription_ FOREIGN KEY (subscription_id)
        REFERENCES subscription_ (subscription_id) ON DELETE CASCADE
);

-- Indexes

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_subscription_user_metadata_last_updated
    BEFORE UPDATE ON subscription_user_metadata_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();