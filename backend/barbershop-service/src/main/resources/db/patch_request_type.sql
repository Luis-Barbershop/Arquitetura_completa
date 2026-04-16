-- =============================================================
-- Patch: adicionar coluna request_type à tabela barbershop_join_requests
-- Executar UMA VEZ no banco de produção se o serviço retornar 500
-- em /barbershops/my-invites ou /barbershops/my-shop/join-requests
-- =============================================================

-- MySQL (dev local)
ALTER TABLE barbershop_join_requests
    ADD COLUMN IF NOT EXISTS request_type VARCHAR(20) NOT NULL DEFAULT 'JOIN';

-- Normalizar rows antigos que possam ter NULL
UPDATE barbershop_join_requests
SET request_type = 'JOIN'
WHERE request_type IS NULL OR request_type = '';

-- Remover o DEFAULT após normalização (opcional — Hibernate gerencia o valor)
-- ALTER TABLE barbershop_join_requests ALTER COLUMN request_type DROP DEFAULT;
