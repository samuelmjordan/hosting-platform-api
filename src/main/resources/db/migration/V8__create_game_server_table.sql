-- Pterodactyl-specific details
CREATE TABLE pterodactyl_server_ (
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    pterodactyl_server_uid TEXT NOT NULL,
    pterodactyl_server_id BIGINT NOT NULL,
    allocation_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pterodactyl_server_subscription_id_fk FOREIGN KEY (subscription_id) 
        REFERENCES subscription_(subscription_id),
    CONSTRAINT pterodactyl_server_allocation_id_fk FOREIGN KEY (allocation_id) 
        REFERENCES pterodactyl_allocation_(allocation_id),
    CONSTRAINT pterodactyl_server_pterodactyl_server_id_unique UNIQUE (pterodactyl_server_id),
    CONSTRAINT pterodactyl_server_pterodactyl_server_uid_unique UNIQUE (pterodactyl_server_uid)
);
CREATE INDEX idx_pterodactyl_server_subscription_id ON pterodactyl_server_(subscription_id);
CREATE INDEX idx_pterodactyl_server_pterodactyl_id ON pterodactyl_server_(pterodactyl_server_id);

-- DNS CNAME record details
CREATE TABLE dns_c_name_record_ (
    id BIGSERIAL PRIMARY KEY,
    subscription_id TEXT NOT NULL,
    c_name_record_id TEXT NOT NULL,
    zone_id TEXT NOT NULL,
    zone_name TEXT NOT NULL,
    record_name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dns_c_record_subscription_id_fk FOREIGN KEY (subscription_id) 
        REFERENCES subscription_(subscription_id),
    CONSTRAINT dns_c_record_c_name_record_id_unique UNIQUE (c_name_record_id),
    CONSTRAINT dns_c_record_record_name_unique UNIQUE (record_name)
);
CREATE INDEX idx_dns_c_name_record__subscription_id ON dns_c_name_record_(subscription_id);
CREATE INDEX idx_dns_c_name_record__c_name_record_id ON dns_c_name_record_(c_name_record_id);

-- Triggers for last_updated
CREATE TRIGGER update_pterodactyl_server_last_updated
    BEFORE UPDATE ON pterodactyl_server_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();

CREATE TRIGGER update_dns_c_record_last_updated
    BEFORE UPDATE ON dns_c_name_record_
    FOR EACH ROW
    EXECUTE FUNCTION update_last_updated_column();