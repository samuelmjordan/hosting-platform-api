CREATE TABLE users (
   -- Primary identifiers
   id BIGSERIAL PRIMARY KEY,
   clerk_id TEXT NOT NULL,
   customer_id TEXT NOT NULL,
   
   -- Business fields
   currency TEXT NOT NULL DEFAULT 'XXX',
   
   -- Audit fields
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

   -- Constraints to ensure data integrity
   CONSTRAINT users_clerk_id_unique UNIQUE (clerk_id),
   CONSTRAINT users_customer_id_unique UNIQUE (customer_id)
);

-- Index for efficient lookups by clerk_id
CREATE INDEX idx_users_clerk_id ON users (clerk_id);
CREATE INDEX idx_users_customer_id ON users (customer_id);

-- Trigger to automatically update last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to maintain last_updated
CREATE TRIGGER update_users_last_updated
   BEFORE UPDATE ON users
   FOR EACH ROW
   EXECUTE FUNCTION update_last_updated_column();