ALTER TABLE tenants
    ADD COLUMN whatsapp_phone_number_id VARCHAR(255);

ALTER TABLE tenants
    ADD CONSTRAINT uk_tenants_whatsapp_phone_number_id UNIQUE (whatsapp_phone_number_id);
