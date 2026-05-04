-- LGPD hardening: prepare columns that are encrypted by JPA converters.
-- Run once before deploying the version that enables encryption/backfill.

ALTER TABLE barbershops MODIFY COLUMN cnpj VARCHAR(128) NOT NULL;
