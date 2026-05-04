-- LGPD hardening: prepare columns that are encrypted by JPA converters.
-- Run once before deploying the version that enables encryption/backfill.

ALTER TABLE appointments MODIFY COLUMN customer_name VARCHAR(256) NOT NULL;
