-- =======================================================================
-- CortaAi — Views analíticas do Dashboard
-- =======================================================================
-- ATENÇÃO: Execute este script APÓS o primeiro `docker compose up -d`,
-- quando o Hibernate já tiver criado todas as tabelas.
--
-- Uso local:
--   docker exec -i cortaai-mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> < views.sql
--
-- Uso no servidor (ZimaOS):
--   docker exec -i cortaai-mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> < views.sql
--
-- ⚠️  AVISO ARQUITETURAL — Views cross-database:
--     As views marcadas com [CROSS-DB] realizam JOINs entre bancos distintos
--     (ex: payment_db × schedule_db × barbershop_db). Isso só funciona porque
--     todos os bancos rodam na MESMA instância MySQL (single-container).
--     Essas views são exclusivas para fins analíticos/dashboard — NUNCA devem
--     ser consultadas por código de microsserviço diretamente.
--     Em uma migração futura para bancos isolados, estas views devem ser
--     substituídas por endpoints de analytics com dados materializados.
-- =======================================================================

-- =======================================================================
-- 1. PAYMENT SERVICE: Performance Financeira dos Barbeiros                [CROSS-DB]
--    ⚠️  Cruza: payment_db × schedule_db × barbershop_db
--    Requer MySQL single-instance. NÃO usar em código de microsserviço.
--    Calcula receita por barbeiro considerando regras de comissão quando
--    existentes, ou valor bruto do agendamento quando não há regra definida.
-- =======================================================================
CREATE OR REPLACE VIEW payment_db.v_barber_financial_performance AS
WITH gross_by_barber AS (
    SELECT
        a.barber_id,
        a.barber_name,
        t.barbershop_id,
        SUM(t.amount) AS gross_revenue,
        COUNT(t.id) AS total_appointments
    FROM payment_db.transactions t
    JOIN schedule_db.appointments a
        ON a.id = BIN_TO_UUID(t.appointment_id)
    WHERE t.status = 'APPROVED'
    GROUP BY a.barber_id, a.barber_name, t.barbershop_id
),
commission_by_barber AS (
    SELECT
        a.barber_id,
        t.barbershop_id,
        SUM(aa.price * COALESCE(bcr.percentage, 0) / 100) AS commission_revenue,
        COUNT(bcr.id) AS commission_rule_count
    FROM payment_db.transactions t
    JOIN schedule_db.appointments a
        ON a.id = BIN_TO_UUID(t.appointment_id)
    JOIN schedule_db.appointment_activities aa
        ON aa.appointment_id = a.id
    LEFT JOIN barbershop_db.barber_commission_rules bcr
        ON bcr.barbershop_id = t.barbershop_id
       AND bcr.barber_id = a.barber_id
       AND bcr.activity_id = aa.activity_id
    WHERE t.status = 'APPROVED'
    GROUP BY a.barber_id, t.barbershop_id
),
effective_revenue AS (
    SELECT
        g.barber_id,
        g.barber_name,
        g.barbershop_id,
        CASE
            WHEN COALESCE(c.commission_rule_count, 0) > 0
                THEN COALESCE(c.commission_revenue, 0)
            ELSE g.gross_revenue
        END AS generated_revenue,
        g.total_appointments
    FROM gross_by_barber g
    LEFT JOIN commission_by_barber c
        ON c.barber_id = g.barber_id
       AND c.barbershop_id = g.barbershop_id
)
SELECT
    e.barber_id                                                          AS barber_id,
    e.barber_name                                                        AS barber_name,
    e.barbershop_id                                                      AS barbershop_id,
    e.generated_revenue                                                  AS generated_revenue,
    e.total_appointments                                                 AS total_appointments,
    ROUND(
        100.0 * e.generated_revenue /
        NULLIF(SUM(e.generated_revenue) OVER (PARTITION BY e.barbershop_id), 0),
    2)                                                                   AS contribution_percentage
FROM effective_revenue e;


-- =======================================================================
-- 2. SCHEDULE SERVICE: Matriz de Habilidades
--    Identifica quais serviços (degradê, barba, etc.) o barbeiro mais executa
-- =======================================================================
CREATE OR REPLACE VIEW schedule_db.v_barber_skill_matrix AS
SELECT
    a.barber_id,
    a.barber_name,
    aa.activity_name,
    COUNT(aa.id)      AS times_executed,
    SUM(aa.price)     AS total_generated_by_activity
FROM schedule_db.appointments a
JOIN schedule_db.appointment_activities aa
    ON a.id = aa.appointment_id
WHERE a.status IN ('COMPLETED', 'CONCLUDED', 'WALK_IN')
GROUP BY a.barber_id, a.barber_name, aa.activity_name;


-- =======================================================================
-- 3. SCHEDULE SERVICE: Termômetro da Agenda
--    Conta agendamentos por categoria de status por dia/barbearia
--    Cobre todos os status: SCHEDULED, PAYMENT_PENDING, EXPIRED,
--    CONFIRMED, IN_PROGRESS, WALK_IN, COMPLETED, CONCLUDED,
--    CANCELLED, NO_SHOW
-- =======================================================================
CREATE OR REPLACE VIEW schedule_db.v_agenda_thermometer AS
SELECT
    DATE(start_time)                                                                                              AS agenda_date,
    barbershop_id,
    COUNT(id)                                                                                                     AS total_appointments,
    SUM(CASE WHEN status IN ('CONFIRMED', 'IN_PROGRESS')                   THEN 1 ELSE 0 END)                    AS active_appointments,
    SUM(CASE WHEN status = 'WALK_IN'                                       THEN 1 ELSE 0 END)                    AS walkin_appointments,
    SUM(CASE WHEN status IN ('SCHEDULED', 'PAYMENT_PENDING', 'EXPIRED')    THEN 1 ELSE 0 END)                    AS pending_appointments,
    SUM(CASE WHEN status IN ('COMPLETED', 'CONCLUDED')                     THEN 1 ELSE 0 END)                    AS completed_appointments,
    SUM(CASE WHEN status IN ('CANCELLED', 'NO_SHOW')                       THEN 1 ELSE 0 END)                    AS lost_appointments
FROM schedule_db.appointments
GROUP BY DATE(start_time), barbershop_id;


-- =======================================================================
-- 4. PRODUCT SERVICE: Alerta de Saúde do Estoque
--    Compara estoque atual com mínimo exigido
-- =======================================================================
CREATE OR REPLACE VIEW product_db.v_stock_health_alert AS
SELECT
    id                                                                    AS product_id,
    name                                                                  AS product_name,
    category,
    stock_quantity                                                        AS current_stock,
    min_stock_quantity                                                    AS predicted_minimum,
    CASE WHEN stock_quantity <= min_stock_quantity THEN 1 ELSE 0 END     AS requires_restock
FROM product_db.products
WHERE active = 1;


-- =======================================================================
-- 5. USER SERVICE: Aquisição de Clientes (Novos por Mês)
-- =======================================================================
CREATE OR REPLACE VIEW user_db.v_customer_acquisition AS
SELECT
    DATE_FORMAT(date_created, '%Y-%m')  AS reference_month,
    COUNT(id)                           AS new_customers
FROM user_db.customers
GROUP BY DATE_FORMAT(date_created, '%Y-%m');


-- =======================================================================
-- 6. USER SERVICE: Retenção de Clientes (Recorrentes por Mês)            [CROSS-DB]
--    ⚠️  Cruza: user_db × schedule_db
--    Requer MySQL single-instance. NÃO usar em código de microsserviço.
--    Conta clientes distintos com agendamentos concluídos por mês.
-- =======================================================================
CREATE OR REPLACE VIEW user_db.v_customer_retention AS
SELECT
    DATE_FORMAT(a.start_time, '%Y-%m')      AS reference_month,
    COUNT(DISTINCT a.customer_id)           AS returning_customers
FROM schedule_db.appointments a
WHERE a.status IN ('COMPLETED', 'CONCLUDED')
GROUP BY DATE_FORMAT(a.start_time, '%Y-%m');


-- =======================================================================
-- 7. PAYMENT SERVICE: Ticket Médio por Barbearia e por Mês
--    Receita total / número de transações aprovadas — por barbearia e mês
-- =======================================================================
CREATE OR REPLACE VIEW payment_db.v_avg_ticket AS
SELECT
    t.barbershop_id                                                       AS barbershop_id,
    DATE_FORMAT(t.created_at, '%Y-%m')                                   AS reference_month,
    COUNT(t.id)                                                           AS total_transactions,
    ROUND(SUM(t.amount), 2)                                              AS total_revenue,
    ROUND(AVG(t.amount), 2)                                              AS avg_ticket
FROM payment_db.transactions t
WHERE t.status = 'APPROVED'
GROUP BY t.barbershop_id, DATE_FORMAT(t.created_at, '%Y-%m');
