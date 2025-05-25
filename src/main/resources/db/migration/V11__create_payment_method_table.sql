CREATE TABLE payment_method_ (
 -- Primary identifiers
 id BIGSERIAL PRIMARY KEY,
 payment_method_id TEXT NOT NULL,
 customer_id TEXT NOT NULL,
 
 -- Payment method type and metadata
 payment_method_type TEXT NOT NULL, -- 'card', 'apple_pay', 'google_pay', 'samsung_pay', 'sepa'
 display_name TEXT NOT NULL, -- computed display string
 
 -- Polymorphic data storage
 payment_data JSONB NOT NULL, -- type-specific fields
 
 -- Status and metadata
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 is_default BOOLEAN NOT NULL DEFAULT FALSE,
 
 -- Audit fields
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 
 -- Constraints
 CONSTRAINT fk_payment_method_customer_id FOREIGN KEY (customer_id) REFERENCES user_(customer_id) ON DELETE CASCADE,
 CONSTRAINT payment_method_id_unique UNIQUE (payment_method_id),
 CONSTRAINT payment_method_type_valid CHECK (payment_method_type IN ('card', 'apple_pay', 'google_pay', 'samsung_pay', 'sepa')),
 CONSTRAINT one_default_per_customer EXCLUDE USING btree (customer_id WITH =) WHERE (is_default = true AND is_active = true)
);

-- Indexes
CREATE INDEX idx_payment_method_customer_id ON payment_method_ (customer_id);
CREATE INDEX idx_payment_method_active ON payment_method_ (customer_id, is_active);
CREATE INDEX idx_payment_method_default ON payment_method_ (customer_id) WHERE is_default = true;
CREATE INDEX idx_payment_data_gin ON payment_method_ USING GIN (payment_data);
CREATE INDEX idx_payment_method_type ON payment_method_ (payment_method_type);

-- Update trigger
CREATE TRIGGER update_payment_method_last_updated
    BEFORE UPDATE ON payment_method_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();