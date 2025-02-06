CREATE TABLE users (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    clerk_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints to ensure data integrity
    CONSTRAINT users_clerk_id_unique UNIQUE (clerk_id),
    CONSTRAINT users_customer_id_unique UNIQUE (customer_id)
);

-- Index for efficient lookups by clerk_id
CREATE INDEX idx_users_clerk_id ON users (clerk_id);
CREATE INDEX idx_users_customer_id ON users (customer_id);