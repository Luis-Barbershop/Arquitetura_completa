-- ============================================================
-- PATCH: Saneamento da coluna act_as_barber (user-service)
-- Contexto: Registros criados antes da adição da coluna ficaram
--           com act_as_barber = NULL. O padrão de negócio é TRUE
--           (todo barbeiro ativo atua como barbeiro por padrão).
-- Executar UMA única vez no banco do user-service.
-- ============================================================

-- 1. Funcionários (is_owner = false ou nulo): sempre atuam, garantir true
UPDATE barbers
SET act_as_barber = true
WHERE (is_owner = false OR is_owner IS NULL)
  AND act_as_barber IS NULL;

-- 2. Owners com act_as_barber nulo: assume true (padrão até o owner desabilitar)
UPDATE barbers
SET act_as_barber = true
WHERE is_owner = true
  AND act_as_barber IS NULL;

-- Verificação (rode após o patch para confirmar):
-- SELECT id, name, is_owner, act_as_barber FROM barbers WHERE act_as_barber IS NULL;
-- Resultado esperado: 0 linhas
