CREATE TABLE user_ (
   -- Primary identifiers
   id BIGSERIAL PRIMARY KEY,
   clerk_id TEXT NOT NULL,
   customer_id TEXT NOT NULL,
   
   -- Business fields
   currency TEXT NOT NULL DEFAULT 'XXX',
   
   -- Audit fields
   deleted_at TIMESTAMP WITH TIME ZONE,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

   -- Constraints to ensure data integrity
   CONSTRAINT user_clerk_id_unique UNIQUE (clerk_id),
   CONSTRAINT user_customer_id_unique UNIQUE (customer_id)
);

-- Index for efficient lookups by clerk_id
CREATE INDEX idx_user_clerk_id ON user_ (clerk_id);
CREATE INDEX idx_user_customer_id ON user_ (customer_id);

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
   BEFORE UPDATE ON user_
   FOR EACH ROW
   EXECUTE FUNCTION update_last_updated_column();