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
-- =======================================================================

-- =======================================================================
-- 1. PAYMENT SERVICE: Performance Financeira dos Barbeiros
--    Cruza as transações aprovadas com a agenda para saber quem gerou a receita
-- =======================================================================
CREATE OR REPLACE VIEW payment_db.v_barber_financial_performance AS
SELECT
    a.barber_id                          AS barber_id,
    a.barber_name                        AS barber_name,
    SUM(t.amount)                        AS generated_revenue,
    COUNT(t.id)                          AS total_appointments
FROM payment_db.transactions t
JOIN schedule_db.appointments a
    ON a.id = BIN_TO_UUID(t.appointment_id)
WHERE t.status = 'APPROVED'
GROUP BY a.barber_id, a.barber_name;


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
WHERE a.status IN ('COMPLETED', 'CONCLUDED')
GROUP BY a.barber_id, a.barber_name, aa.activity_name;


-- =======================================================================
-- 3. SCHEDULE SERVICE: Termômetro da Agenda
--    Conta agendamentos ativos vs perdidos por dia/barbearia
-- =======================================================================
CREATE OR REPLACE VIEW schedule_db.v_agenda_thermometer AS
SELECT
    DATE(start_time)                                                                          AS agenda_date,
    barbershop_id,
    COUNT(id)                                                                                 AS total_appointments,
    SUM(CASE WHEN status IN ('CONFIRMED', 'IN_PROGRESS', 'SCHEDULED') THEN 1 ELSE 0 END)    AS active_appointments,
    SUM(CASE WHEN status IN ('CANCELLED', 'NO_SHOW')                  THEN 1 ELSE 0 END)    AS lost_appointments
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
-- 6. USER SERVICE: Retenção de Clientes (Recorrentes por Mês)
--    Lê appointments do schedule_db — cross-db read (somente para analytics)
-- =======================================================================
CREATE OR REPLACE VIEW user_db.v_customer_retention AS
SELECT
    DATE_FORMAT(a.start_time, '%Y-%m')      AS reference_month,
    COUNT(DISTINCT a.customer_id)           AS returning_customers
FROM schedule_db.appointments a
WHERE a.status IN ('COMPLETED', 'CONCLUDED')
GROUP BY DATE_FORMAT(a.start_time, '%Y-%m');
