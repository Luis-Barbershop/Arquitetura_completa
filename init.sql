CREATE DATABASE IF NOT EXISTS user_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS barbershop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS schedule_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- notification-service usa MySQL (JPA) + Redis (deduplicação)

-- ── Tabela de blocos de horário (agenda flexível por barbeiro) ──────────────
-- Criada automaticamente pelo Hibernate (ddl-auto: update), mas mantemos aqui
-- como referência para ambientes onde o schema é gerenciado manualmente.
USE user_db;
CREATE TABLE IF NOT EXISTS barber_work_blocks (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    barber_id   VARCHAR(36) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL COMMENT 'MONDAY, TUESDAY, ..., SUNDAY',
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    INDEX idx_bwb_barber_day (barber_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
