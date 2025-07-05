CREATE TABLE invoice_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    invoice_id TEXT NOT NULL,
    invoice_number TEXT NOT NULL DEFAULT 'pending',

    -- Foreign keys
    customer_id TEXT NOT NULL,
    subscription_id TEXT NOT NULL,

    -- Payment details
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    collection_method TEXT,
    payment_method TEXT,
    currency TEXT NOT NULL,
    minor_amount BIGINT NOT NULL,

    -- Metadata
    invoice_created_at TIMESTAMP NOT NULL,
    link TEXT NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT invoice_invoice_id_unique UNIQUE (invoice_id),
    CONSTRAINT fk_invoice_user_ FOREIGN KEY (customer_id)
        REFERENCES user_ (customer_id)
);

-- Indexes

-- Trigger to automatically update last_updated timestamp
CREATE TRIGGER update_invoice_last_updated
    BEFORE UPDATE ON invoice_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();