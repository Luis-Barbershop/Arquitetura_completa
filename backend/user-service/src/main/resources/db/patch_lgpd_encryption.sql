-- LGPD hardening: prepare columns that are encrypted by JPA converters.
-- Run once before deploying the version that enables encryption/backfill.

ALTER TABLE customers MODIFY COLUMN document_cpf VARCHAR(128);
ALTER TABLE customers MODIFY COLUMN tell VARCHAR(128);
ALTER TABLE customers MODIFY COLUMN email VARCHAR(256) NOT NULL;
ALTER TABLE customers MODIFY COLUMN birth_date VARCHAR(128);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);
CREATE UNIQUE INDEX uk_customers_email_hash ON customers (email_hash);

ALTER TABLE barbers MODIFY COLUMN document_cpf VARCHAR(128);
ALTER TABLE barbers MODIFY COLUMN tell VARCHAR(128);
ALTER TABLE barbers MODIFY COLUMN email VARCHAR(256) NOT NULL;
ALTER TABLE barbers MODIFY COLUMN birth_date VARCHAR(128);
ALTER TABLE barbers MODIFY COLUMN mp_user_id VARCHAR(256);
ALTER TABLE barbers ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);
CREATE UNIQUE INDEX uk_barbers_email_hash ON barbers (email_hash);
