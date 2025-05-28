CREATE TABLE server_execution_context_ (
    -- Primary identifiers
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,

    -- Provisioning status
    step_type TEXT NOT NULL,
    mode TEXT NOT NULL, -- 'create', 'destroy', 'update', 'migrate'
    execution_status TEXT NOT NULL, -- 'in_progress', 'completed', 'failed'

    -- Existing resources
    node_id BIGINT,
    a_record_id TEXT,
    pterodactyl_node_id BIGINT,
    allocation_id BIGINT,
    pterodactyl_server_id BIGINT,
    c_name_record_id BIGINT,

    -- Migration resources
    new_node_id BIGINT,
    new_a_record_id TEXT,
    new_pterodactyl_node_id BIGINT,
    new_allocation_id BIGINT,
    new_pterodactyl_server_id BIGINT,
    new_c_name_record_id BIGINT,

    -- Desired state
    region TEXT NOT NULL,
    specification_id TEXT NOT NULL,

    -- User data
    title TEXT NOT NULL,
    caption TEXT NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_server_execution_context_subscription_id FOREIGN KEY (subscription_id) 
        REFERENCES subscription_ (subscription_id) ON DELETE CASCADE,

    CONSTRAINT fk_server_execution_context_node_id FOREIGN KEY (node_id) 
        REFERENCES cloud_node_ (node_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_a_record_id FOREIGN KEY (a_record_id)    
        REFERENCES dns_a_record_ (a_record_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_pterodactyl_node_id FOREIGN KEY (pterodactyl_node_id) 
        REFERENCES pterodactyl_node_ (pterodactyl_node_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_allocation_id FOREIGN KEY (allocation_id) 
        REFERENCES pterodactyl_allocation_ (allocation_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_pterodactyl_server_id FOREIGN KEY (pterodactyl_server_id) 
        REFERENCES pterodactyl_server_ (pterodactyl_server_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_c_name_record_id FOREIGN KEY (c_name_record_id) 
        REFERENCES dns_c_name_record_ (c_name_record_id) ON DELETE SET NULL,

    CONSTRAINT fk_server_execution_context_new_node_id FOREIGN KEY (new_node_id) 
        REFERENCES cloud_node_ (node_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_new_a_record_id FOREIGN KEY (new_a_record_id)    
        REFERENCES dns_a_record_ (a_record_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_new_pterodactyl_node_id FOREIGN KEY (new_pterodactyl_node_id) 
        REFERENCES pterodactyl_node_ (pterodactyl_node_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_new_allocation_id FOREIGN KEY (new_allocation_id) 
        REFERENCES pterodactyl_allocation_ (allocation_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_new_pterodactyl_server_id FOREIGN KEY (new_pterodactyl_server_id) 
        REFERENCES pterodactyl_server_ (pterodactyl_server_id) ON DELETE SET NULL,
    CONSTRAINT fk_server_execution_context_new_c_name_record_id FOREIGN KEY (new_c_name_record_id) 
        REFERENCES dns_c_name_record_ (c_name_record_id) ON DELETE SET NULL,

    CONSTRAINT fk_server_execution_context_specification_id FOREIGN KEY (specification_id) 
        REFERENCES game_server_specification_ (specification_id),
        
    CONSTRAINT server_execution_context_subscription_id_unique UNIQUE (subscription_id)
);

-- Indexes for foreign keys
CREATE INDEX idx_server_execution_context_subscription_id ON server_execution_context_ (subscription_id);
CREATE INDEX idx_server_execution_context_execution_status ON server_execution_context_ (execution_status);
CREATE INDEX idx_server_execution_context_node_id ON server_execution_context_ (node_id);
CREATE INDEX idx_server_execution_context_a_record_id ON server_execution_context_ (a_record_id);
CREATE INDEX idx_server_execution_context_pterodactyl_node_id ON server_execution_context_ (pterodactyl_node_id);
CREATE INDEX idx_server_execution_context_allocation_id ON server_execution_context_ (allocation_id);
CREATE INDEX idx_server_execution_context_pterodactyl_server_id ON server_execution_context_ (pterodactyl_server_id);
CREATE INDEX idx_server_execution_context_c_name_record_id ON server_execution_context_ (c_name_record_id);
CREATE INDEX idx_server_execution_context_new_node_id ON server_execution_context_ (new_node_id);
CREATE INDEX idx_server_execution_context_new_a_record_id ON server_execution_context_ (new_a_record_id);
CREATE INDEX idx_server_execution_context_new_pterodactyl_node_id ON server_execution_context_ (new_pterodactyl_node_id);
CREATE INDEX idx_server_execution_context_new_allocation_id ON server_execution_context_ (new_allocation_id);
CREATE INDEX idx_server_execution_context_new_pterodactyl_server_id ON server_execution_context_ (new_pterodactyl_server_id);
CREATE INDEX idx_server_execution_context_new_c_name_record_id ON server_execution_context_ (new_c_name_record_id);
CREATE INDEX idx_server_execution_context_specification_id ON server_execution_context_ (specification_id);

-- Update trigger
CREATE TRIGGER server_execution_context_last_updated
    BEFORE UPDATE ON server_execution_context_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();