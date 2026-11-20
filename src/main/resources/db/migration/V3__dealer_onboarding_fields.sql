-- Flyway Migration V3: Dealer Onboarding and KYC Verification
ALTER TABLE dealers 
    MODIFY COLUMN gstin VARCHAR(15) NULL,
    ADD COLUMN legal_entity_name VARCHAR(191) NULL AFTER dealership_name,
    ADD COLUMN pan_number VARCHAR(10) NULL AFTER gstin,
    ADD COLUMN dealer_code VARCHAR(50) NULL AFTER pan_number,
    ADD COLUMN dealership_type VARCHAR(50) NULL AFTER dealer_code,
    ADD COLUMN contact_person VARCHAR(100) NULL AFTER address,
    ADD COLUMN contact_email VARCHAR(191) NULL AFTER contact_person,
    ADD COLUMN contact_phone VARCHAR(20) NULL AFTER contact_email,
    ADD COLUMN pincode VARCHAR(10) NULL AFTER city,
    ADD COLUMN state VARCHAR(50) NULL AFTER pincode,
    ADD COLUMN onboarding_status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED' AFTER status;
