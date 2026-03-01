# 📋 CortaAí — Plano Mestre de Reestruturação e Evolução

> **Versão:** 1.0  
> **Data:** 26 de fevereiro de 2026  
> **Objetivo:** Documento de referência para a migração completa do monólito para microserviços, incorporação de novos módulos (Social Login, Pagamentos, Notificações, e-Commerce) e deploy em ambiente Linux/Docker de produção.

---

## Índice

1. [Visão Geral da Aplicação](#1-visão-geral-da-aplicação)
2. [Diagnóstico Técnico do Estado Atual](#2-diagnóstico-técnico-do-estado-atual)
3. [Arquitetura Alvo (To-Be)](#3-arquitetura-alvo-to-be)
4. [Estratégia de Banco de Dados](#4-estratégia-de-banco-de-dados)
5. [Fase 0 — Correções e Fundação](#5-fase-0--correções-e-fundação)
6. [Fase 1 — Desacoplamento do Monólito](#6-fase-1--desacoplamento-do-monólito)
7. [Fase 2 — Autenticação Social (OAuth2 / OIDC)](#7-fase-2--autenticação-social-oauth2--oidc)
8. [Fase 3 — Sistema de Pagamentos (Mercado Pago)](#8-fase-3--sistema-de-pagamentos-mercado-pago)
9. [Fase 4 — Sistema de Notificações](#9-fase-4--sistema-de-notificações)
10. [Fase 5 — Módulo e-Commerce de Barbeiros](#10-fase-5--módulo-e-commerce-de-barbeiros)
11. [Fase 6 — Dashboards e Business Intelligence](#11-fase-6--dashboards-e-business-intelligence)
12. [Infraestrutura Docker / Linux (Produção)](#12-infraestrutura-docker--linux-produção)
13. [Segurança e Resiliência](#13-segurança-e-resiliência)
14. [Observabilidade e Monitoramento](#14-observabilidade-e-monitoramento)
15. [Estratégia de Testes](#15-estratégia-de-testes)
16. [Cronograma Macro (Sprints)](#16-cronograma-macro-sprints)
17. [Sugestões de Diferenciais Competitivos](#17-sugestões-de-diferenciais-competitivos)

---

## 1. Visão Geral da Aplicação

### 1.1 O que é o CortaAí

O CortaAí é um **marketplace SaaS para barbearias** que visa substituir agendas de papel, WhatsApp e planilhas por uma plataforma única e integrada. Ele conecta três atores:

| Ator | Papel |
|---|---|
| **Cliente** | Busca barbearias, visualiza serviços/preços, agenda horários, acompanha status, paga online |
| **Barbeiro** | Gerencia perfil profissional, define horário de trabalho, vincula-se a uma loja, consulta sua agenda |
| **Barbeiro Dono (Owner)** | Registra e administra barbearia, cadastra serviços, gerencia equipe, aprova solicitações, visualiza relatórios |

### 1.2 Módulos de Negócio Existentes

| Módulo | Descrição | Regras de Negócio Chave |
|---|---|---|
| **Autenticação** | Registro e login de Clientes e Barbeiros via e-mail/senha + JWT. Roles: `CUSTOMER`, `BARBER`, `OWNER` | RN1-RN4, RN9-RN10 |
| **Gestão de Barbearia** | CRUD de barbearia (nome, CNPJ, endereço), upload de logo/banner/destaques (Cloudinary) | RN7-RN8, RN10 |
| **Gestão de Equipe** | Barbeiro solicita entrada via CNPJ → Dono aprova/rejeita. Barbeiro define horário de trabalho e skills | RN6, RN20-RN22 |
| **Catálogo de Serviços** | CRUD de Activities (nome, preço, duração, imagem) vinculados à barbearia | RN13-RN14 |
| **Agendamento (Core)** | Criação, atualização, cancelamento e conclusão de agendamentos. Validação de conflitos de horário, pertencimento, skills | RN11-RN19 |
| **Descoberta** | Endpoints públicos para listagem de barbearias, serviços e barbeiros | RN1 |

### 1.3 Stack Tecnológica Atual

| Camada | Tecnologia |
|---|---|
| Backend | Java 17, Spring Boot 3.3.4, Spring Security, Spring Data JPA, MapStruct, Lombok |
| Frontend | React 19, Vite 7, Axios, React Router DOM 7 |
| Banco de Dados | MySQL 8.0 |
| Armazenamento de Mídia | Cloudinary |
| Infra Original | AWS Lambda + RDS + API Gateway (monólito serverless) |
| Infra em Migração | Docker Compose + Eureka + Spring Cloud Gateway |

---

## 2. Diagnóstico Técnico do Estado Atual

### 2.1 Mapa de Entidades do Monólito (`backend/src`)

```
┌─────────────────────────────────────────────────────────────┐
│                        MONÓLITO                              │
│                                                              │
│  Customer ──┐                                                │
│             ├──→ Appointments ←──┤                           │
│  Barber ────┘        │           │                           │
│    │                 │      BarbershopJoinRequest             │
│    │          Activity (M:N)     │                           │
│    │                 │           │                           │
│    └──→ Barbershop ──┘           │                           │
│              │                   │                           │
│         BarbershopHighlight      │                           │
│                                  │                           │
│  barber_activities (M:N)         │                           │
│  appointment_activities (M:N)    │                           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Estado da Migração para Microserviços

| Serviço | Status | Observação |
|---|---|---|
| `discovery-service` (Eureka, :8761) | ✅ Configurado | pom.xml + application.yml OK |
| `api-gateway` (Spring Cloud Gateway, :8080) | ✅ Configurado | Rotas definidas para 3 serviços |
| `user-service` (:8081) | ⚠️ Parcialmente migrado | Models (Barber, Customer) migrados, controllers e services existem, **mas importam classes do monólito** |
| `barbershop-service` (:8082) | 🔴 Quase vazio | Apenas `BarbershopServiceApplication.java` + configs |
| `schedule-service` (:8083) | 🔴 Quase vazio | Apenas `ScheduleServiceApplication.java` + configs |

### 2.3 Problemas Críticos Identificados

| # | Problema | Severidade | Detalhe |
|---|---|---|---|
| P1 | **user-service importa pacotes do monólito** | 🔴 Crítico | `CustomerController` importa `ifsp.edu.projeto.cortaai.dto.*` e `ifsp.edu.projeto.cortaai.service.*`. O serviço **não compila sozinho**. |
| P2 | **pom.xml do user-service com XML inválido** | 🔴 Crítico | A dependência `cloudinary-http44` está **fora** da tag `<dependencies>`. Maven não compila. |
| P3 | **user-service e monólito usam Spring Boot diferentes** | 🟡 Médio | Monólito: `3.3.4`, user-service: `3.2.3`, barbershop-service: `3.2.3`. Pode causar incompatibilidades de JWT. |
| P4 | **Appointments acessa 4 bancos diferentes** | 🔴 Crítico | `AppointmentsServiceImpl` faz `BarberRepository`, `CustomerRepository`, `BarbershopRepository`, `ActivityRepository` — tabelas que estarão em 3 bancos separados. JOINs impossíveis. |
| P5 | **barber_activities cruza user_db e barbershop_db** | 🔴 Crítico | Barber vive em `user_db`, Activity em `barbershop_db`. O M:N não pode existir como FK cruzada. |
| P6 | **Sem mecanismo de comunicação inter-serviço** | 🔴 Crítico | Nenhum Feign Client, nem message broker. Serviços não conversam. |
| P7 | **JWT gerado em user-service mas validado em todos** | 🟡 Médio | Cada serviço replica `JwtAuthorizationFilter`. A chave secreta precisa ser compartilhada. |
| P8 | **Docker Compose incompleto** | 🟡 Médio | Faltam `barbershop-service` e `schedule-service` no `docker-compose.yml`. |
| P9 | **Frontend sem proxy no vite.config.js** | 🟡 Médio | `baseURL: '/api'` mas não há proxy configurado → CORS em dev. |
| P10 | **Barber no user-service perdeu campos** | 🟡 Médio | O model `Barber` no user-service não tem `workStartTime`, `workEndTime`, `activities` (M:N), `imageUrl`. |

---

## 3. Arquitetura Alvo (To-Be)

### 3.1 Diagrama de Componentes

```
                    ┌──────────────┐
                    │   INTERNET   │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │    Nginx     │  ← Reverse Proxy + SSL (porta 443)
                    │  (Container) │
                    └──────┬───────┘
                           │
              ┌────────────┴────────────┐
              │                         │
     ┌────────▼────────┐      ┌────────▼────────┐
     │   Frontend       │      │   API Gateway   │  ← Spring Cloud Gateway (:8080)
     │   React/Nginx    │      │   (Container)   │     Validação JWT centralizada
     │   (:3000)        │      └────────┬────────┘     Rate Limiting
     └──────────────────┘               │
                                        │ (Eureka lb://)
              ┌─────────────────────────┼─────────────────────────┐
              │                         │                         │
     ┌────────▼────────┐      ┌────────▼────────┐      ┌────────▼────────┐
     │  user-service    │      │ barbershop-     │      │ schedule-       │
     │  (:8081)         │      │ service (:8082) │      │ service (:8083) │
     │                  │      │                 │      │                 │
     │ • Auth/JWT       │      │ • Barbershop    │      │ • Appointments  │
     │ • Social Login   │      │ • Activity      │      │ • Availability  │
     │ • Customer       │      │ • Highlights    │      │ • Calendar sync │
     │ • Barber (perfil)│      │ • JoinRequest   │      │                 │
     └────────┬────────┘      │ • BarberSkills  │      └────────┬────────┘
              │                │ • Cloudinary    │               │
              │                └────────┬────────┘               │
              │                         │                        │
    ┌─────────▼──┐            ┌─────────▼──┐           ┌────────▼───┐
    │  user_db   │            │barbershop_db│           │schedule_db │
    │  (MySQL)   │            │  (MySQL)    │           │  (MySQL)   │
    └────────────┘            └─────────────┘           └────────────┘

     ┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
     │ payment-service  │     │notification-svc  │     │ product-service  │
     │ (:8084)          │     │ (:8085)          │     │ (:8086)          │
     │                  │     │                  │     │                  │
     │ • Mercado Pago   │     │ • Email (Resend) │     │ • e-Commerce     │
     │ • Transações     │     │ • Push (FCM)     │     │ • Catálogo       │
     │ • Split payment  │     │ • SMS (opção)    │     │ • Estoque        │
     │ • Webhooks       │     │ • WhatsApp (opt) │     │ • Avaliações     │
     └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
              │                       │                        │
    ┌─────────▼──┐           ┌────────▼───┐           ┌───────▼────┐
    │payment_db  │           │   Redis    │           │product_db  │
    │  (MySQL)   │           │  (pub/sub  │           │  (MySQL)   │
    └────────────┘           │  + cache)  │           └────────────┘
                             └────────────┘

     ┌──────────────────────────────────────────┐
     │           Infraestrutura Comum            │
     │                                           │
     │  • Discovery Service (Eureka :8761)       │
     │  • RabbitMQ (:5672 / :15672 management)   │
     │  • Redis (:6379) — cache + pub/sub        │
     │  • MySQL 8.0 (:3306) — instância única,   │
     │    schemas separados                      │
     │  • Prometheus + Grafana (monitoramento)   │
     └──────────────────────────────────────────┘
```

### 3.2 Princípios Arquiteturais

| Princípio | Implementação |
|---|---|
| **Database per Service** | Cada serviço tem seu schema MySQL dedicado. Sem JOINs cruzados. |
| **API-First** | Contratos OpenAPI 3.0 definidos antes da implementação. |
| **Smart Endpoints, Dumb Pipes** | Lógica nos serviços, comunicação via REST (síncrono) e RabbitMQ (assíncrono). |
| **Design for Failure** | Circuit Breaker (Resilience4j), Retry, Fallback em toda chamada inter-serviço. |
| **Single Responsibility** | Cada serviço é dono de um bounded context claro. |
| **Shared Nothing** | Sem banco compartilhado. Dados replicados via eventos quando necessário. |
| **Twelve-Factor App** | Configs via variáveis de ambiente, logs para stdout, processos stateless. |

### 3.3 Escolha de Comunicação Inter-Serviço

| Tipo | Ferramenta | Quando usar |
|---|---|---|
| **Síncrono** | Spring Cloud OpenFeign + Eureka | Quando o chamador precisa da resposta imediatamente (ex: validar se barbeiro pertence à barbearia antes de criar agendamento) |
| **Assíncrono** | RabbitMQ (Spring AMQP) | Quando a ação é eventual e não bloqueia o fluxo (ex: notificar cliente, processar pagamento, atualizar dashboard) |
| **Cache** | Redis (Spring Data Redis) | Dados quentes de leitura frequente (ex: catálogo de serviços, perfil público da barbearia) |

---

## 4. Estratégia de Banco de Dados

### 4.1 Schemas Dedicados (Database per Service)

Todos os schemas rodam na **mesma instância MySQL 8.0** em Docker (economia de recursos), mas são logicamente isolados. Em produção, podem ser promovidos a instâncias separadas sem mudança de código.

```sql
-- init.sql (atualizado)
CREATE DATABASE IF NOT EXISTS user_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS barbershop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS schedule_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- notification-service usa Redis (sem schema MySQL)
```

### 4.2 Distribuição de Tabelas por Schema

#### `user_db` (user-service)

```sql
-- Tabela de usuários unificada com coluna discriminadora
CREATE TABLE users (
    id              CHAR(36)     PRIMARY KEY,  -- UUID
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    tell            VARCHAR(15)  UNIQUE,
    document_cpf    VARCHAR(11)  UNIQUE,
    password_hash   VARCHAR(255),               -- NULL se login via OAuth
    image_url       VARCHAR(500),
    image_public_id VARCHAR(255),
    user_type       ENUM('CUSTOMER','BARBER') NOT NULL,
    role            VARCHAR(30)  NOT NULL DEFAULT 'ROLE_CUSTOMER',
    is_owner        BOOLEAN      NOT NULL DEFAULT FALSE,
    barbershop_id   CHAR(36),                   -- Referência externa (barbershop_db)
    work_start_time TIME,
    work_end_time   TIME,
    -- Campos para Social Login
    oauth_provider  VARCHAR(30),                -- 'GOOGLE', 'FACEBOOK', 'GITHUB'
    oauth_provider_id VARCHAR(255),             -- ID do usuário no provider
    -- Auditoria
    date_created    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_email (email),
    INDEX idx_users_type (user_type),
    INDEX idx_users_barbershop (barbershop_id),
    INDEX idx_users_oauth (oauth_provider, oauth_provider_id)
);

-- Tokens de refresh para rotação segura
CREATE TABLE refresh_tokens (
    id          CHAR(36)     PRIMARY KEY,
    user_id     CHAR(36)     NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  DATETIME     NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_token (token),
    INDEX idx_refresh_user (user_id)
);
```

> **Decisão de Design:** Unificar `Customer` e `Barber` em uma tabela `users` com coluna `user_type`. **Justificativa:**
> - Ambos têm campos idênticos (name, email, tell, cpf, password).
> - Simplifica Social Login (um fluxo para todos).
> - Um `Customer` pode se tornar `Barber` no futuro sem duplicar registro.
> - Campos exclusivos de Barber (`is_owner`, `work_*_time`, `barbershop_id`) são nullable para Customers.
> - **Se preferir manter separado**: manter duas tabelas é válido, mas duplica a lógica de auth.

#### `barbershop_db` (barbershop-service)

```sql
CREATE TABLE barbershops (
    id              CHAR(36)     PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    cnpj            VARCHAR(14)  NOT NULL UNIQUE,
    address         VARCHAR(500),
    city            VARCHAR(100),
    state           VARCHAR(2),
    zip_code        VARCHAR(8),
    latitude        DECIMAL(10,8),       -- Para busca por geolocalização
    longitude       DECIMAL(11,8),
    phone           VARCHAR(15),
    description     TEXT,
    logo_url        VARCHAR(500),
    logo_public_id  VARCHAR(255),
    banner_url      VARCHAR(500),
    banner_public_id VARCHAR(255),
    owner_id        CHAR(36)     NOT NULL, -- Referência externa ao user-service
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    average_rating  DECIMAL(2,1) DEFAULT 0.0,
    total_reviews   INT          DEFAULT 0,
    date_created    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_shop_cnpj (cnpj),
    INDEX idx_shop_owner (owner_id),
    INDEX idx_shop_location (latitude, longitude),
    INDEX idx_shop_active (is_active)
);

CREATE TABLE activities (
    id               CHAR(36)       PRIMARY KEY,
    barbershop_id    CHAR(36)       NOT NULL,
    activity_name    VARCHAR(255)   NOT NULL,
    description      TEXT,
    price            DECIMAL(10,2)  NOT NULL,
    duration_minutes INT            NOT NULL,
    image_url        VARCHAR(500),
    image_public_id  VARCHAR(255),
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    category         VARCHAR(50),           -- 'CORTE', 'BARBA', 'COMBO', 'TRATAMENTO'
    date_created     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (barbershop_id) REFERENCES barbershops(id) ON DELETE CASCADE,
    INDEX idx_activity_shop (barbershop_id),
    INDEX idx_activity_category (category)
);

-- Tabela pivô: quais barbeiros executam quais serviços (barberId é referência externa)
CREATE TABLE barber_activities (
    barber_id   CHAR(36) NOT NULL,   -- Referência externa ao user-service
    activity_id CHAR(36) NOT NULL,

    PRIMARY KEY (barber_id, activity_id),
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    INDEX idx_ba_barber (barber_id)
);

CREATE TABLE barbershop_join_requests (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    barber_id      CHAR(36)     NOT NULL,   -- Referência externa
    barbershop_id  CHAR(36)     NOT NULL,
    status         ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    date_created   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (barbershop_id) REFERENCES barbershops(id) ON DELETE CASCADE,
    UNIQUE KEY uk_barber_shop (barber_id, barbershop_id),
    INDEX idx_jr_status (barbershop_id, status)
);

CREATE TABLE barbershop_highlights (
    id              CHAR(36)     PRIMARY KEY,
    barbershop_id   CHAR(36)     NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    image_public_id VARCHAR(255),
    sort_order      INT          DEFAULT 0,

    FOREIGN KEY (barbershop_id) REFERENCES barbershops(id) ON DELETE CASCADE,
    INDEX idx_highlight_shop (barbershop_id)
);

-- Avaliações (novo recurso)
CREATE TABLE reviews (
    id             CHAR(36)       PRIMARY KEY,
    barbershop_id  CHAR(36)       NOT NULL,
    customer_id    CHAR(36)       NOT NULL,  -- Referência externa
    appointment_id BIGINT,                    -- Referência externa (schedule_db)
    rating         TINYINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment        TEXT,
    date_created   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (barbershop_id) REFERENCES barbershops(id) ON DELETE CASCADE,
    UNIQUE KEY uk_review_appointment (appointment_id),
    INDEX idx_review_shop (barbershop_id)
);

-- Horários de funcionamento da barbearia (novo recurso)
CREATE TABLE barbershop_working_hours (
    id            CHAR(36)       PRIMARY KEY,
    barbershop_id CHAR(36)       NOT NULL,
    day_of_week   TINYINT        NOT NULL,  -- 0=DOM, 1=SEG, ..., 6=SAB
    open_time     TIME           NOT NULL,
    close_time    TIME           NOT NULL,
    is_closed     BOOLEAN        NOT NULL DEFAULT FALSE,

    FOREIGN KEY (barbershop_id) REFERENCES barbershops(id) ON DELETE CASCADE,
    UNIQUE KEY uk_shop_day (barbershop_id, day_of_week)
);
```

#### `schedule_db` (schedule-service)

```sql
CREATE TABLE appointments (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    barbershop_id   CHAR(36)     NOT NULL,  -- Referência externa
    barber_id       CHAR(36)     NOT NULL,  -- Referência externa
    customer_id     CHAR(36)     NOT NULL,  -- Referência externa
    -- Dados desnormalizados para performance (evitar chamadas REST em toda listagem)
    barbershop_name VARCHAR(255),
    barber_name     VARCHAR(100),
    customer_name   VARCHAR(100),
    start_time      DATETIME     NOT NULL,
    end_time        DATETIME     NOT NULL,
    status          ENUM('SCHEDULED','CONFIRMED','IN_PROGRESS','CONCLUDED','CANCELLED','NO_SHOW')
                    NOT NULL DEFAULT 'SCHEDULED',
    total_price     DECIMAL(10,2),
    total_duration  INT,          -- Minutos
    cancellation_reason VARCHAR(255),
    payment_id      CHAR(36),     -- Referência externa (payment_db)
    date_created    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_appt_barber (barber_id, start_time),
    INDEX idx_appt_customer (customer_id),
    INDEX idx_appt_shop (barbershop_id, start_time),
    INDEX idx_appt_status (status),
    INDEX idx_appt_conflict (barber_id, start_time, end_time, status)
);

CREATE TABLE appointment_activities (
    appointment_id BIGINT    NOT NULL,
    activity_id    CHAR(36)  NOT NULL,
    -- Dados desnormalizados (snapshot do momento do agendamento)
    activity_name  VARCHAR(255),
    price          DECIMAL(10,2),
    duration_minutes INT,

    PRIMARY KEY (appointment_id, activity_id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE
);

-- Bloqueios de agenda do barbeiro (férias, folgas, etc.)
CREATE TABLE barber_blocks (
    id          CHAR(36)     PRIMARY KEY,
    barber_id   CHAR(36)     NOT NULL,
    start_time  DATETIME     NOT NULL,
    end_time    DATETIME     NOT NULL,
    reason      VARCHAR(255),
    date_created DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_block_barber (barber_id, start_time, end_time)
);
```

#### `payment_db` (payment-service)

```sql
CREATE TABLE transactions (
    id                  CHAR(36)       PRIMARY KEY,
    appointment_id      BIGINT,                        -- Referência externa
    order_id            CHAR(36),                      -- Para compras e-commerce
    customer_id         CHAR(36)       NOT NULL,       -- Referência externa
    barbershop_id       CHAR(36)       NOT NULL,       -- Referência externa
    amount              DECIMAL(10,2)  NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    status              ENUM('PENDING','APPROVED','REJECTED','REFUNDED','CANCELLED')
                        NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(30),                   -- 'PIX', 'CREDIT_CARD', 'DEBIT'
    -- Dados do Mercado Pago
    mp_payment_id       VARCHAR(100),
    mp_preference_id    VARCHAR(100),
    mp_status           VARCHAR(50),
    mp_status_detail    VARCHAR(100),
    -- Split de pagamento
    platform_fee        DECIMAL(10,2)  DEFAULT 0.00,   -- Taxa da plataforma CortaAí
    seller_amount       DECIMAL(10,2),                  -- Valor líquido para a barbearia
    date_created        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tx_appointment (appointment_id),
    INDEX idx_tx_customer (customer_id),
    INDEX idx_tx_shop (barbershop_id),
    INDEX idx_tx_mp (mp_payment_id)
);

CREATE TABLE payment_webhooks_log (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    mp_event_id  VARCHAR(100),
    event_type   VARCHAR(50),
    payload      JSON,
    processed    BOOLEAN      NOT NULL DEFAULT FALSE,
    date_created DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_wh_event (mp_event_id)
);
```

#### `product_db` (product-service — e-Commerce)

```sql
CREATE TABLE products (
    id             CHAR(36)       PRIMARY KEY,
    barbershop_id  CHAR(36)       NOT NULL,       -- Referência externa
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          DECIMAL(10,2)  NOT NULL,
    image_url      VARCHAR(500),
    image_public_id VARCHAR(255),
    category       VARCHAR(50),                    -- 'POMADA', 'SHAMPOO', 'CERA', etc.
    sku            VARCHAR(50),
    stock_quantity INT            NOT NULL DEFAULT 0,
    min_stock      INT            DEFAULT 5,       -- Alerta de estoque baixo
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    date_created   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_prod_shop (barbershop_id),
    INDEX idx_prod_category (category)
);

CREATE TABLE orders (
    id             CHAR(36)       PRIMARY KEY,
    customer_id    CHAR(36)       NOT NULL,
    barbershop_id  CHAR(36)       NOT NULL,
    status         ENUM('PENDING','PAID','PREPARING','READY','DELIVERED','CANCELLED')
                   NOT NULL DEFAULT 'PENDING',
    total_amount   DECIMAL(10,2)  NOT NULL,
    payment_id     CHAR(36),                       -- Referência a payment_db
    date_created   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_order_customer (customer_id),
    INDEX idx_order_shop (barbershop_id)
);

CREATE TABLE order_items (
    id          CHAR(36)       PRIMARY KEY,
    order_id    CHAR(36)       NOT NULL,
    product_id  CHAR(36)       NOT NULL,
    quantity    INT            NOT NULL,
    unit_price  DECIMAL(10,2)  NOT NULL,

    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_oi_order (order_id)
);

-- Movimentações de estoque
CREATE TABLE stock_movements (
    id            CHAR(36)       PRIMARY KEY,
    product_id    CHAR(36)       NOT NULL,
    movement_type ENUM('IN','OUT','ADJUSTMENT') NOT NULL,
    quantity      INT            NOT NULL,
    reference_id  CHAR(36),                       -- order_id ou ajuste manual
    notes         VARCHAR(255),
    date_created  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_sm_product (product_id)
);
```

### 4.3 Estratégia de Integridade Referencial

Como não há FKs entre schemas diferentes, a integridade é garantida por:

| Estratégia | Detalhe |
|---|---|
| **Referência por UUID** | Colunas como `barber_id CHAR(36)` armazenam o UUID do user-service, sem FK. |
| **Validação via Feign** | Antes de criar um agendamento, o schedule-service chama `user-service` e `barbershop-service` via Feign para validar que os IDs existem. |
| **Dados desnormalizados** | `appointments` armazena `barber_name`, `customer_name` para evitar chamadas REST em toda listagem. Atualizados via eventos. |
| **Eventos de lifecycle** | Quando um barbeiro é deletado, o `user-service` publica `UserDeletedEvent` no RabbitMQ. O `schedule-service` cancela agendamentos futuros e o `barbershop-service` remove vínculos. |
| **Soft delete** | Entidades críticas usam `is_active` ao invés de DELETE físico, preservando histórico. |

### 4.4 Diagrama ER Resumido (Novo)

```
user_db                    barbershop_db                  schedule_db
┌────────────┐             ┌─────────────────┐            ┌────────────────┐
│   users    │             │  barbershops    │            │  appointments  │
│ (Customer  │◄─ UUID ────►│                 │◄─ UUID ───►│                │
│  & Barber) │             │  owner_id ──────┼─ UUID ───► │  barber_id     │
└─────┬──────┘             ├─────────────────┤            │  customer_id   │
      │                    │  activities     │            │  barbershop_id │
      │                    ├─────────────────┤            ├────────────────┤
      │                    │  barber_        │            │  appointment_  │
      │                    │  activities     │            │  activities    │
      │                    │  (barber_id=UUID)│           └────────────────┘
      │                    ├─────────────────┤
      │                    │  join_requests  │            payment_db
      │                    │  (barber_id=UUID)│           ┌────────────────┐
      │                    ├─────────────────┤            │  transactions  │
      │                    │  reviews        │            └────────────────┘
      │                    │  (customer_id=  │
      │                    │   UUID)         │            product_db
      │                    ├─────────────────┤            ┌────────────────┐
      │                    │  highlights     │            │  products      │
      │                    ├─────────────────┤            │  orders        │
      │                    │  working_hours  │            │  order_items   │
      │                    └─────────────────┘            │  stock_mvmt    │
      │                                                   └────────────────┘
      │
┌─────▼──────┐
│  refresh_  │
│  tokens    │
└────────────┘
```

---

## 5. Fase 0 — Correções e Fundação

> **Objetivo:** Antes de qualquer feature nova, corrigir tudo que impede a compilação e o deploy.  
> **Duração estimada:** 1 Sprint (2 semanas)

### Passo 0.1 — Corrigir `pom.xml` do user-service

- [ ] Mover a dependência `cloudinary-http44` para **dentro** da tag `<dependencies>`.
- [ ] Alinhar a versão do Spring Boot para `3.3.4` (mesma do parent).
- [ ] Alinhar `spring-cloud.version` para `2023.0.1` em todos os módulos.

### Passo 0.2 — Eliminar imports cruzados no user-service

- [ ] Copiar para o pacote `userservice` todas as classes referenciadas do monólito:
  - `NotFoundException`, `ReferenceException` → `userservice/exception/`
  - `StorageService`, `CloudinaryStorageServiceImpl` → `userservice/service/storage/`
  - `BeforeDeleteCustomer`, `BeforeDeleteBarber` → `userservice/event/`
  - `UploadResultDTO` → `userservice/dto/`
  - DTOs de Customer (CustomerDTO, CustomerCreateDTO, LoginDTO, LoginResponseDTO) → `userservice/dto/`
- [ ] Atualizar todos os imports para o pacote local.
- [ ] Validar que `mvn compile -pl user-service` funciona isoladamente.

### Passo 0.3 — Completar o model `Barber` no user-service

- [ ] Adicionar os campos ausentes:
  ```java
  private LocalTime workStartTime;
  private LocalTime workEndTime;
  private String imageUrl;
  private String imageUrlPublicId;
  ```

### Passo 0.4 — Padronizar parent POM (Multi-module Maven)

- [ ] Fazer `user-service` e `barbershop-service` usarem o parent POM do monólito (já configurado para `schedule-service`), ou criar um BOM (Bill of Materials) separado.
- [ ] Resultado: todas as versões de dependência gerenciadas em um lugar só.

### Passo 0.5 — Configurar vite.config.js com proxy

```javascript
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
});
```

### Passo 0.6 — Completar docker-compose.yml

- [ ] Adicionar containers para `barbershop-service` e `schedule-service`.
- [ ] Adicionar `healthcheck` em todos os containers.
- [ ] Adicionar `depends_on` com condições (`condition: service_healthy`).

---

## 6. Fase 1 — Desacoplamento do Monólito

> **Objetivo:** Migrar toda a lógica de negócio do monólito para os 3 microserviços.  
> **Duração estimada:** 3 Sprints (6 semanas)

### Sprint 1.1 — barbershop-service (4 semanas)

#### Passo 1.1.1 — Criar entidades JPA

- [ ] `Barbershop` (com novos campos: city, state, latitude, longitude, etc.)
- [ ] `Activity` (com campo `category`)
- [ ] `BarbershopHighlight`
- [ ] `BarbershopJoinRequest` (com `barber_id` como `UUID` simples, sem `@ManyToOne`)
- [ ] `BarberActivity` (tabela pivô com `barber_id` como UUID simples)
- [ ] `BarbershopWorkingHours` (novo)
- [ ] `Review` (novo)

#### Passo 1.1.2 — Criar Feign Client para user-service

```java
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/internal/users/{id}")
    UserInfoDTO getUserById(@PathVariable UUID id);

    @GetMapping("/api/internal/users/by-email/{email}")
    UserInfoDTO getUserByEmail(@PathVariable String email);

    @PutMapping("/api/internal/users/{id}/barbershop")
    void updateUserBarbershopId(@PathVariable UUID id, @RequestBody UUID barbershopId);
}
```

> **Convenção:** Endpoints inter-serviço usam prefixo `/api/internal/` e são protegidos por header secreto (`X-Internal-Token`), não expostos pelo API Gateway.

#### Passo 1.1.3 — Migrar toda a lógica do `BarbershopController`

- [ ] Migrar todos os 18 endpoints do controller do monólito.
- [ ] Substituir `BarberRepository.findByEmail()` por chamada Feign.
- [ ] Ao aprovar JoinRequest, chamar Feign para atualizar `barbershopId` no user-service.

#### Passo 1.1.4 — Adicionar Resilience4j

```yaml
# barbershop-service application.yml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
  retry:
    instances:
      userService:
        max-attempts: 3
        wait-duration: 500ms
```

### Sprint 1.2 — schedule-service (2 semanas)

#### Passo 1.2.1 — Criar entidades JPA

- [ ] `Appointment` (com dados desnormalizados: nomes)
- [ ] `AppointmentActivity` (snapshot com nome/preço/duração)
- [ ] `BarberBlock` (novo)

#### Passo 1.2.2 — Criar Feign Clients

```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/internal/users/{id}")
    UserInfoDTO getUserById(@PathVariable UUID id);

    @GetMapping("/api/internal/users/by-email/{email}")
    UserInfoDTO getUserByEmail(@PathVariable String email);
}

@FeignClient(name = "barbershop-service")
public interface BarbershopServiceClient {
    @GetMapping("/api/internal/barbershops/{id}")
    BarbershopInfoDTO getBarbershopById(@PathVariable UUID id);

    @GetMapping("/api/internal/barbershops/{shopId}/activities")
    List<ActivityInfoDTO> getActivitiesByIds(@PathVariable UUID shopId,
                                             @RequestParam List<UUID> ids);

    @GetMapping("/api/internal/barbershops/{shopId}/barber-activities/{barberId}")
    List<UUID> getBarberActivityIds(@PathVariable UUID shopId,
                                     @PathVariable UUID barberId);
}
```

#### Passo 1.2.3 — Migrar `AppointmentsServiceImpl`

- [ ] Substituir todos os `Repository.findById()` cruzados por chamadas Feign.
- [ ] Na criação do agendamento, o fluxo fica:
  1. Feign → user-service: validar customer e barber
  2. Feign → barbershop-service: validar barbearia, serviços e skills do barbeiro
  3. Validar conflitos localmente (só precisa do `schedule_db`)
  4. Salvar com dados desnormalizados (nomes)
  5. Publicar evento `AppointmentCreatedEvent` no RabbitMQ

#### Passo 1.2.4 — Migrar endpoint de disponibilidade

- [ ] Trazer a lógica de `getAvailableSlots` para o schedule-service.
- [ ] Buscar `workStartTime`/`workEndTime` do barbeiro via Feign (com cache Redis de 5min).

### Sprint 1.3 — Limpeza e Validação

- [ ] Validar que todos os 3 serviços compilam e sobem independentemente.
- [ ] Testes de integração com Testcontainers (MySQL + RabbitMQ).
- [ ] Remover o módulo monólito `backend/src` do build (deixar apenas como referência histórica).
- [ ] Atualizar rotas do API Gateway.

---

## 7. Fase 2 — Autenticação Social (OAuth2 / OIDC)

> **Objetivo:** Login com Google, Facebook e GitHub, além de alternativas gratuitas ao AWS Cognito.  
> **Duração estimada:** 2 Sprints (4 semanas)

### 7.1 Alternativas Gratuitas ao AWS Cognito

| Solução | Tipo | Custo | Recomendação |
|---|---|---|---|
| **Spring Security OAuth2 Client** (nativo) | Biblioteca | ✅ Gratuito | ⭐ **RECOMENDADO** — Sem vendor lock-in, controle total, já faz parte do Spring Boot |
| **Keycloak** | Self-hosted (Docker) | ✅ Gratuito | ✅ Excelente — IAM completo, protocolo OIDC, admin UI. Peso extra no deploy. |
| **SuperTokens** | Self-hosted ou Cloud | ✅ Free tier generoso (5000 MAU) | ✅ Boa alternativa, simples de integrar |
| **Supabase Auth** | Cloud | ✅ Free tier (50k MAU) | ✅ Bom, mas dependência externa |
| **Auth0** | Cloud | ✅ Free tier (7500 MAU) | ⚠️ Vendor lock-in, pode ter limitações futuras |
| **Firebase Auth** | Cloud (Google) | ✅ Gratuito para auth | ⚠️ Dependência do Google Cloud |
| **Logto** | Self-hosted ou Cloud | ✅ Open-source | ✅ Alternativa moderna ao Keycloak, mais leve |

### 7.2 Estratégia Recomendada: Spring Security OAuth2 Client + Keycloak (opcional)

**Abordagem A — Spring Nativo (Mais Simples):**

O `user-service` implementa OAuth2 diretamente usando Spring Security:

```yaml
# user-service application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email
          facebook:
            client-id: ${FACEBOOK_CLIENT_ID}
            client-secret: ${FACEBOOK_CLIENT_SECRET}
            scope: email, public_profile
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user, user:email
```

**Abordagem B — Keycloak (Mais Robusto):**

Keycloak roda como container Docker e gerencia todos os provedores:

```yaml
# docker-compose.yml
keycloak:
  image: quay.io/keycloak/keycloak:24.0
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_PASSWORD}
  command: start-dev
  ports: ["8180:8080"]
```

### 7.3 Fluxo de Social Login

```
1. Frontend → Redireciona para /oauth2/authorization/google
2. Google → Redirect callback com code
3. user-service → Troca code por access_token + id_token
4. user-service → Extrai email, name, picture do id_token
5. user-service → Busca/Cria usuário na tabela users (via oauth_provider + oauth_provider_id)
6. user-service → Gera JWT interno do CortaAí (com roles, claims)
7. user-service → Redirect para frontend com token na URL (ou cookie HttpOnly)
8. Frontend → Armazena token, navega para /homepage
```

### 7.4 Passos de Implementação

- [ ] **Passo 2.1:** Registrar aplicação OAuth nos providers (Google Console, Facebook Developers, GitHub Settings)
- [ ] **Passo 2.2:** Adicionar `spring-boot-starter-oauth2-client` ao `user-service`
- [ ] **Passo 2.3:** Implementar `CustomOAuth2UserService` que faz upsert na tabela `users`
- [ ] **Passo 2.4:** Implementar `OAuth2AuthenticationSuccessHandler` que gera JWT e redireciona
- [ ] **Passo 2.5:** Adicionar lógica de "linkar conta" — se o email já existe como cadastro manual, vincular ao provider OAuth
- [ ] **Passo 2.6:** Atualizar frontend com botões "Continuar com Google/Facebook/GitHub"
- [ ] **Passo 2.7:** Implementar refresh token com rotação segura (tabela `refresh_tokens`)
- [ ] **Passo 2.8:** Manter login tradicional (email/senha) como fallback

---

## 8. Fase 3 — Sistema de Pagamentos (Mercado Pago)

> **Objetivo:** Integrar pagamento online para serviços agendados e compras de produtos.  
> **Duração estimada:** 2 Sprints (4 semanas)

### 8.1 Visão Geral

```
Cliente agenda serviço → schedule-service cria appointment (status: PENDING_PAYMENT)
                       → publica evento AppointmentCreated
                       → payment-service recebe evento
                       → cria Preference no Mercado Pago (Checkout Pro ou Transparente)
                       → retorna link de pagamento ao frontend
                       
Cliente paga → Mercado Pago → Webhook POST /api/payments/webhook
                             → payment-service processa
                             → publica PaymentApprovedEvent
                             → schedule-service atualiza appointment (status: CONFIRMED)
                             → notification-service envia confirmação ao cliente e barbeiro
```

### 8.2 Modelo de Negócio: Split de Pagamento

O Mercado Pago suporta **Marketplace com Split Payment**:

- **Barbearia** recebe o valor do serviço/produto (ex: R$50,00)
- **Plataforma CortaAí** recebe uma taxa (ex: 5% = R$2,50)
- A divisão é automática via `marketplace_fee` da API do MP

### 8.3 Passos de Implementação

- [ ] **Passo 3.1:** Criar conta de Marketplace no Mercado Pago (modo Sandbox para testes)
- [ ] **Passo 3.2:** Criar `payment-service` com Spring Boot
- [ ] **Passo 3.3:** Adicionar SDK do Mercado Pago:
  ```xml
  <dependency>
      <groupId>com.mercadopago</groupId>
      <artifactId>sdk-java</artifactId>
      <version>2.1.24</version>
  </dependency>
  ```
- [ ] **Passo 3.4:** Implementar criação de Preference (Checkout Pro):
  - Itens: serviços do agendamento (nome, preço, quantidade)
  - `back_urls`: success/failure/pending
  - `notification_url`: webhook
  - `marketplace_fee`: taxa da plataforma
- [ ] **Passo 3.5:** Implementar controller de Webhook (`POST /api/payments/webhook`)
  - Validar assinatura do MP
  - Salvar log em `payment_webhooks_log` (idempotência)
  - Atualizar status da transação
  - Publicar evento no RabbitMQ
- [ ] **Passo 3.6:** Implementar fluxo de reembolso automático (cancelamento dentro de 24h antes)
- [ ] **Passo 3.7:** Implementar Checkout Transparente (cartão de crédito inline — melhor UX)
- [ ] **Passo 3.8:** Implementar PIX como método de pagamento (via Mercado Pago)
- [ ] **Passo 3.9:** No frontend: integrar `@mercadopago/sdk-react` para o Checkout Bricks
- [ ] **Passo 3.10:** Onboarding da barbearia: cada Dono conecta sua conta MP (OAuth do MP) para receber pagamentos

### 8.4 Modos de Pagamento

| Modo | Quando | Obrigatório? |
|---|---|---|
| **Pagamento na hora (padrão)** | Cliente agenda e paga após o serviço no balcão | Funciona sem integração |
| **Pagamento antecipado (online)** | Cliente paga ao confirmar o agendamento | ✅ Reduz no-show drasticamente |
| **Pagamento de sinal** | Cliente paga % ao agendar, restante na barbearia | Meio-termo |
| **Compra de produto** | Cliente compra produto no e-commerce | Via payment-service |

---

## 9. Fase 4 — Sistema de Notificações

> **Objetivo:** Notificar clientes e barbeiros sobre eventos do sistema.  
> **Duração estimada:** 1 Sprint (2 semanas)

### 9.1 Canais de Notificação

| Canal | Ferramenta | Custo | Uso |
|---|---|---|---|
| **Email** | [Resend](https://resend.com) | ✅ 3.000/mês grátis | Confirmações, recibos, marketing |
| **Email (alternativa)** | [Brevo (ex-Sendinblue)](https://brevo.com) | ✅ 300/dia grátis | |
| **Push Web** | [Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging) | ✅ Gratuito | Lembrete 1h antes, status do agendamento |
| **WhatsApp** | [Evolution API](https://github.com/EvolutionAPI/evolution-api) (self-hosted) | ✅ Gratuito (open-source) | Lembrete, confirmação (cuidado com policy do WhatsApp) |
| **WhatsApp (oficial)** | Meta Cloud API | ⚠️ Pago por mensagem | Para escala, se necessário |
| **SMS** | [Twilio](https://twilio.com) | ⚠️ Pago (~$0.05/msg) | Fallback para quem não tem app |
| **In-App** | WebSocket (STOMP) via Spring | ✅ Gratuito | Notificações em tempo real no frontend |

### 9.2 Arquitetura do notification-service

```
RabbitMQ                    notification-service              Canais
┌──────────────┐           ┌───────────────────┐           ┌────────┐
│ appointment. │──────────►│ NotificationRouter │──────────►│ Email  │
│ created      │           │                   │           ├────────┤
├──────────────┤           │ Decide canal +    │──────────►│ Push   │
│ appointment. │──────────►│ template baseado   │           ├────────┤
│ cancelled    │           │ na preferência do │──────────►│WhatsApp│
├──────────────┤           │ usuário e no tipo  │           ├────────┤
│ payment.     │──────────►│ do evento         │──────────►│In-App  │
│ approved     │           └───────────────────┘           └────────┘
├──────────────┤                    │
│ reminder.    │                    ▼
│ schedule     │           ┌───────────────────┐
└──────────────┘           │     Redis          │
                           │ (deduplicação +    │
                           │  preferências      │
                           │  do usuário cache) │
                           └───────────────────┘
```

### 9.3 Tipos de Notificação

| Evento | Destinatário | Canal | Template |
|---|---|---|---|
| Agendamento criado | Cliente + Barbeiro | Email + Push | "Seu agendamento foi confirmado para {data} às {hora}" |
| Lembrete (1h antes) | Cliente | Push + WhatsApp | "Lembrete: seu corte é às {hora} na {barbearia}" |
| Agendamento cancelado | Cliente ou Barbeiro | Email + Push | "O agendamento {id} foi cancelado" |
| Pagamento confirmado | Cliente | Email | "Pagamento de R${valor} confirmado" |
| Solicitação de entrada | Dono | Push + In-App | "O barbeiro {nome} quer entrar na sua equipe" |
| Solicitação aprovada | Barbeiro | Push + Email | "Você foi aprovado na {barbearia}!" |
| Produto com estoque baixo | Dono | Push + In-App | "O produto {nome} está com estoque abaixo de {min}" |
| Nova avaliação | Dono | In-App | "Novo review de {cliente}: ⭐{rating}" |

### 9.4 Passos de Implementação

- [ ] **Passo 4.1:** Criar `notification-service` com Spring Boot + Spring AMQP
- [ ] **Passo 4.2:** Configurar RabbitMQ exchanges e queues (topic exchange)
- [ ] **Passo 4.3:** Implementar templates de email com Thymeleaf
- [ ] **Passo 4.4:** Integrar Resend API para envio de email transacional
- [ ] **Passo 4.5:** Integrar Firebase Cloud Messaging para push
- [ ] **Passo 4.6:** Implementar WebSocket (STOMP) para notificações in-app
- [ ] **Passo 4.7:** Implementar scheduler (`@Scheduled`) para lembretes pré-agendamento
- [ ] **Passo 4.8:** Criar tabela de preferências do usuário (quais canais ativar)
- [ ] **Passo 4.9:** No frontend: implementar Service Worker para receber push

---

## 10. Fase 5 — Módulo e-Commerce de Barbeiros

> **Objetivo:** Permitir que barbearias vendam produtos (pomadas, shampoos, etc.) online.  
> **Duração estimada:** 2 Sprints (4 semanas)

### 10.1 Funcionalidades

| Feature | Descrição |
|---|---|
| **Catálogo de Produtos** | Dono cadastra produtos com nome, preço, foto, categoria, SKU e estoque |
| **Vitrine Pública** | Clientes navegam produtos de uma barbearia (na mesma página do perfil) |
| **Carrinho** | Carrinho de compras por barbearia (frontend, localStorage/context) |
| **Checkout** | Checkout via Mercado Pago (reutiliza payment-service) |
| **Gestão de Pedidos** | Dono visualiza pedidos, atualiza status (PREPARANDO → PRONTO → ENTREGUE) |
| **Controle de Estoque** | Quantidade atualizada automaticamente a cada venda. Alertas de estoque baixo. |
| **Upsell no Agendamento** | Sugerir produtos ao cliente durante o fluxo de agendamento ("Adicione uma pomada ao seu corte!") |

### 10.2 Passos de Implementação

- [ ] **Passo 5.1:** Criar `product-service` com Spring Boot
- [ ] **Passo 5.2:** Criar entidades: Product, Order, OrderItem, StockMovement
- [ ] **Passo 5.3:** CRUD de produtos (Dono)
- [ ] **Passo 5.4:** API pública de listagem por barbearia
- [ ] **Passo 5.5:** Criar pedido + integrar com payment-service
- [ ] **Passo 5.6:** Gerenciamento de pedidos (Dono)
- [ ] **Passo 5.7:** Controle de estoque automático + alerta
- [ ] **Passo 5.8:** Frontend: componente de vitrine + carrinho + checkout
- [ ] **Passo 5.9:** Frontend: upsell sugestivo no fluxo de agendamento

---

## 11. Fase 6 — Dashboards e Business Intelligence

> **Objetivo:** Transformar dados em decisões de negócio para donos de barbearias.  
> **Duração estimada:** 2 Sprints (4 semanas)

### 11.1 Dashboards para o Dono

| Dashboard | Métricas |
|---|---|
| **Visão Geral** | Faturamento do mês, agendamentos do dia, taxa de cancelamento, nota média |
| **Financeiro** | Receita por período (dia/semana/mês), receita por serviço, ticket médio, projeção |
| **Equipe** | Agendamentos por barbeiro, receita por barbeiro, taxa de conclusão, horários de pico |
| **Serviços** | Serviço mais agendado, receita por serviço, duração média real vs. estimada |
| **Clientes** | Novos clientes, taxa de retorno, clientes frequentes, CLV (Customer Lifetime Value) |
| **Produtos** | Vendas por produto, estoque atual, alertas, margem de lucro |

### 11.2 Estratégia Técnica

Os dados para dashboards vêm de queries agregadas nos respectivos serviços, expostas por endpoints dedicados:

```
GET /api/barbershops/my-shop/dashboard/overview?period=MONTH
GET /api/barbershops/my-shop/dashboard/revenue?from=2026-01-01&to=2026-01-31
GET /api/barbershops/my-shop/dashboard/team-performance?period=WEEK
GET /api/appointments/barbershop/my-shop/stats?period=MONTH
```

Para escala futura: criar views materializadas ou uma camada CQRS com tabelas pré-agregadas atualizadas via eventos.

### 11.3 Passos

- [ ] **Passo 6.1:** Criar endpoints de agregação no schedule-service (stats de agendamentos)
- [ ] **Passo 6.2:** Criar endpoints de agregação no barbershop-service (serviços, equipe)
- [ ] **Passo 6.3:** Criar endpoints de agregação no payment-service (financeiro)
- [ ] **Passo 6.4:** Criar endpoints de agregação no product-service (vendas, estoque)
- [ ] **Passo 6.5:** Frontend: tela de Dashboard com gráficos (usar biblioteca **Recharts** ou **Chart.js**)
- [ ] **Passo 6.6:** Exportação de relatórios em PDF (com **JasperReports** ou **OpenPDF**)

---

## 12. Infraestrutura Docker / Linux (Produção)

### 12.1 Docker Compose de Produção

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  # ======== INFRAESTRUTURA ========
  mysql:
    image: mysql:8.0
    container_name: cortaai-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql_data:/var/lib/mysql
    ports:
      - "127.0.0.1:3306:3306"  # Apenas localhost
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 1G

  redis:
    image: redis:7-alpine
    container_name: cortaai-redis
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    container_name: cortaai-rabbitmq
    restart: always
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports:
      - "127.0.0.1:5672:5672"
      - "127.0.0.1:15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 15s
      timeout: 10s
      retries: 5

  # ======== SERVICE DISCOVERY ========
  discovery:
    build:
      context: ./backend
      dockerfile: discovery-service/Dockerfile
    container_name: discovery-service
    restart: always
    ports:
      - "127.0.0.1:8761:8761"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 10

  # ======== API GATEWAY ========
  gateway:
    build:
      context: ./backend
      dockerfile: api-gateway/Dockerfile
    container_name: api-gateway
    restart: always
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      - EUREKA_URI=http://discovery:8761/eureka/
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
    depends_on:
      discovery:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5

  # ======== BUSINESS SERVICES ========
  user-service:
    build:
      context: ./backend
      dockerfile: user-service/Dockerfile
    container_name: user-service
    restart: always
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/user_db?useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - EUREKA_URI=http://discovery:8761/eureka/
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - REDIS_HOST=redis
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    depends_on:
      mysql:
        condition: service_healthy
      discovery:
        condition: service_healthy
      redis:
        condition: service_healthy

  barbershop-service:
    build:
      context: ./backend
      dockerfile: barbershop-service/Dockerfile
    container_name: barbershop-service
    restart: always
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/barbershop_db?useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - EUREKA_URI=http://discovery:8761/eureka/
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - CLOUDINARY_URL=${CLOUDINARY_URL}
      - RABBITMQ_HOST=rabbitmq
    depends_on:
      mysql:
        condition: service_healthy
      discovery:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  schedule-service:
    build:
      context: ./backend
      dockerfile: schedule-service/Dockerfile
    container_name: schedule-service
    restart: always
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/schedule_db?useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - EUREKA_URI=http://discovery:8761/eureka/
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - RABBITMQ_HOST=rabbitmq
      - REDIS_HOST=redis
    depends_on:
      mysql:
        condition: service_healthy
      discovery:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  payment-service:
    build:
      context: ./backend
      dockerfile: payment-service/Dockerfile
    container_name: payment-service
    restart: always
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/payment_db?useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - EUREKA_URI=http://discovery:8761/eureka/
      - MERCADO_PAGO_ACCESS_TOKEN=${MP_ACCESS_TOKEN}
      - RABBITMQ_HOST=rabbitmq
    depends_on:
      mysql:
        condition: service_healthy
      discovery:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  notification-service:
    build:
      context: ./backend
      dockerfile: notification-service/Dockerfile
    container_name: notification-service
    restart: always
    environment:
      - RABBITMQ_HOST=rabbitmq
      - REDIS_HOST=redis
      - RESEND_API_KEY=${RESEND_API_KEY}
      - FCM_CREDENTIALS_PATH=/app/config/firebase-adminsdk.json
    depends_on:
      rabbitmq:
        condition: service_healthy
      redis:
        condition: service_healthy

  product-service:
    build:
      context: ./backend
      dockerfile: product-service/Dockerfile
    container_name: product-service
    restart: always
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/product_db?useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - EUREKA_URI=http://discovery:8761/eureka/
      - RABBITMQ_HOST=rabbitmq
      - CLOUDINARY_URL=${CLOUDINARY_URL}
    depends_on:
      mysql:
        condition: service_healthy
      discovery:
        condition: service_healthy

  # ======== FRONTEND ========
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: cortaai-frontend
    restart: always
    ports:
      - "127.0.0.1:3000:80"

  # ======== REVERSE PROXY + SSL ========
  nginx:
    image: nginx:alpine
    container_name: cortaai-nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/certbot:/var/www/certbot:ro
    depends_on:
      - frontend
      - gateway

  # ======== OBSERVABILIDADE ========
  prometheus:
    image: prom/prometheus:latest
    container_name: cortaai-prometheus
    restart: always
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "127.0.0.1:9090:9090"

  grafana:
    image: grafana/grafana:latest
    container_name: cortaai-grafana
    restart: always
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
    ports:
      - "127.0.0.1:3001:3000"
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  mysql_data:
  redis_data:
  rabbitmq_data:
  grafana_data:
```

### 12.2 Dockerfile Padrão para Microserviços (Multi-stage)

```dockerfile
# backend/user-service/Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY ../mvnw ../pom.xml ./
COPY ../.mvn .mvn
COPY user-service/pom.xml user-service/
RUN ./mvnw dependency:go-offline -pl user-service -am
COPY user-service/src user-service/src
RUN ./mvnw package -pl user-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 12.3 Nginx Config (SSL + Proxy)

```nginx
# nginx/nginx.conf
upstream api_gateway {
    server gateway:8080;
}

upstream frontend {
    server frontend:80;
}

server {
    listen 80;
    server_name cortaai.com.br;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name cortaai.com.br;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    # Frontend
    location / {
        proxy_pass http://frontend;
    }

    # API
    location /api/ {
        proxy_pass http://api_gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket para notificações
    location /ws/ {
        proxy_pass http://api_gateway;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 12.4 Provisionamento em Linux (VPS)

Requisitos mínimos para produção inicial:

| Recurso | Recomendação |
|---|---|
| **VPS** | 4 vCPU, 8GB RAM, 80GB SSD (Hetzner, DigitalOcean, Contabo) |
| **OS** | Ubuntu 24.04 LTS |
| **Docker** | Docker Engine 26+ com Docker Compose v2 |
| **SSL** | Let's Encrypt via Certbot (gratuito) |
| **Domínio** | cortaai.com.br (Registro.br) |
| **Firewall** | UFW: apenas portas 22 (SSH), 80, 443 |
| **Backup** | Cron job: `mysqldump` diário → S3/MinIO |

---

## 13. Segurança e Resiliência

### 13.1 Checklist de Segurança

| # | Medida | Implementação |
|---|---|---|
| S1 | **HTTPS everywhere** | Nginx termina SSL (Let's Encrypt). Tráfego interno em rede Docker isolada. |
| S2 | **JWT com rotação** | Access token (15 min) + Refresh token (7 dias) com rotação a cada uso. |
| S3 | **Senhas com BCrypt** | Já implementado. Manter `BCryptPasswordEncoder` com strength 12. |
| S4 | **Validação de entrada** | Jakarta Bean Validation em todos os DTOs. Sanitizar HTML (OWASP). |
| S5 | **Rate Limiting** | Spring Cloud Gateway `RequestRateLimiter` com Redis (100 req/min por IP). |
| S6 | **CORS restrito** | Permitir apenas `https://cortaai.com.br`. |
| S7 | **Secrets via env vars** | Nunca hardcode. Usar `.env` + Docker secrets. |
| S8 | **SQL Injection** | Usar apenas JPA/Spring Data (parameterized queries). Nunca concatenar SQL. |
| S9 | **Auditoria** | Spring Data JPA Auditing (`@CreatedDate`, `@LastModifiedDate`) em todas as entidades. |
| S10 | **Endpoints internos** | Prefixo `/api/internal/**` protegidos por header `X-Internal-Token`, não roteados pelo Gateway. |
| S11 | **Dependency scanning** | Dependabot ou Snyk no GitHub. |
| S12 | **Container security** | Imagens alpine, usuário não-root, read-only filesystem onde possível. |
| S13 | **LGPD** | Endpoint `DELETE /api/users/me` para exclusão de conta. Política de privacidade. |

### 13.2 Padrões de Resiliência

| Padrão | Ferramenta | Cenário |
|---|---|---|
| **Circuit Breaker** | Resilience4j | Se o user-service ficar fora, o barbershop-service não trava — retorna dados em cache ou fallback. |
| **Retry** | Resilience4j | Tentativas automáticas em falhas transientes (timeout de rede). |
| **Timeout** | Feign + Resilience4j | Toda chamada Feign tem timeout de 3s. |
| **Bulkhead** | Resilience4j | Isolar thread pools por downstream service. |
| **Fallback** | Código customizado | Se não conseguir buscar nome do barbeiro, exibir "Barbeiro" genérico. |
| **Idempotência** | Redis + chave única | Webhooks do Mercado Pago processados uma única vez. |
| **Dead Letter Queue** | RabbitMQ DLQ | Mensagens que falharem 3x vão para DLQ para análise manual. |
| **Health Checks** | Spring Actuator | `/actuator/health` em todos os serviços para Docker healthcheck. |
| **Graceful Shutdown** | Spring Boot | `server.shutdown=graceful` para completar requisições em andamento. |

---

## 14. Observabilidade e Monitoramento

### 14.1 Stack de Observabilidade

| Pilar | Ferramenta | Objetivo |
|---|---|---|
| **Métricas** | Micrometer + Prometheus + Grafana | JVM, latência de endpoints, taxa de erros, conexões DB |
| **Logs** | SLF4J + Logback → stdout → Docker logs | Logs estruturados em JSON, correlação por `traceId` |
| **Tracing** | Micrometer Tracing + Zipkin (ou Tempo) | Rastrear uma request do Gateway até o banco, passando por múltiplos serviços |
| **Alertas** | Grafana Alerting | Notificar via Slack/Email se error rate > 5% ou p99 > 2s |

### 14.2 Configuração Padrão (Spring Boot Actuator)

```yaml
# Adicionar a todos os application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0  # 100% em dev, 10% em prod
```

---

## 15. Estratégia de Testes

### 15.1 Pirâmide de Testes

```
        ╱  E2E (Playwright)  ╲          ← 5% dos testes
       ╱   Contract (Pact)    ╲         ← 10%
      ╱  Integration (Testcont.)╲       ← 25%
     ╱    Unit (JUnit + Mockito) ╲      ← 60%
    ╱─────────────────────────────╲
```

### 15.2 Testes por Camada

| Tipo | Ferramenta | O que testa | Exemplo |
|---|---|---|---|
| **Unitário** | JUnit 5 + Mockito | Lógica de serviço isolada | `AppointmentService.create()` com Feign mockado |
| **Integração** | Spring Boot Test + Testcontainers | Serviço completo com banco real | POST /api/appointments com MySQL e RabbitMQ em container |
| **Contrato** | Spring Cloud Contract ou Pact | Contrato entre serviços (Feign) | schedule-service espera que user-service retorne `UserInfoDTO` no formato correto |
| **E2E** | Playwright | Jornada completa do usuário | Login → Buscar barbearia → Agendar → Pagar → Ver "Meus Agendamentos" |

### 15.3 Cobertura Mínima

| Métrica | Target |
|---|---|
| Cobertura de linha (unitário + integração) | ≥ 80% |
| Cobertura de branch | ≥ 70% |
| Todos os happy paths E2E | 100% |
| Todos os fluxos de erro documentados (RN) | 100% |

---

## 16. Cronograma Macro (Sprints)

| Sprint | Duração | Fase | Entregas |
|---|---|---|---|
| **S0** | 2 semanas | Fase 0 | Correções de build, proxy, docker-compose, eliminação de imports cruzados |
| **S1** | 2 semanas | Fase 1.1 | barbershop-service: entidades, repos, Feign client, 50% dos endpoints |
| **S2** | 2 semanas | Fase 1.1/1.2 | barbershop-service: 100% endpoints + schedule-service: entidades, Feign, availability |
| **S3** | 2 semanas | Fase 1.2/1.3 | schedule-service: 100% endpoints + testes integração + limpeza do monólito |
| **S4** | 2 semanas | Fase 2 | Social Login: Google + Facebook + GitHub. Refresh tokens. Frontend atualizado |
| **S5** | 2 semanas | Fase 2/3 | Finalização Social Login + payment-service: SDK MP, Checkout Pro, Webhooks |
| **S6** | 2 semanas | Fase 3 | payment-service: PIX, split, frontend Checkout Bricks + notificação-service base |
| **S7** | 2 semanas | Fase 4 | notification-service: Email, Push, In-App. Lembretes. Templates |
| **S8** | 2 semanas | Fase 5 | product-service: CRUD, estoque, vitrine. Frontend e-commerce |
| **S9** | 2 semanas | Fase 5/6 | Checkout de produtos + Dashboards: tela principal com gráficos |
| **S10** | 2 semanas | Fase 6/Infra | Dashboards avançados + Infra produção: Dockerfile, Nginx, SSL, monitoring |
| **S11** | 2 semanas | QA/Deploy | Testes E2E, performance, hardening de segurança, deploy em VPS Linux |
| **S12** | 2 semanas | Buffer | Correções, polimento de UX, documentação final, go-live |

**Total: ~26 semanas (6 meses)**

---

## 17. Sugestões de Diferenciais Competitivos

Para tornar o CortaAí o **melhor marketplace de barbeiros** do mercado:

### 17.1 Funcionalidades Premium

| Feature | Descrição | Impacto |
|---|---|---|
| 🗺️ **Busca por Geolocalização** | "Barbearias perto de mim" usando coordenadas (latitude/longitude no barbershop) | Alto — UX mobile-first |
| ⭐ **Sistema de Avaliações** | Reviews com nota + comentário após cada agendamento concluído | Alto — Confiança e SEO |
| 🔄 **Agendamento Recorrente** | "Agendar toda terça às 18h" com renovação automática | Alto — Fidelização |
| 💳 **Programa de Fidelidade** | "A cada 10 cortes, 1 grátis" com cartão virtual de pontos | Alto — Retenção |
| 📊 **Ranking de Barbeiros** | Ranking público por nota, número de atendimentos e especialidade | Médio — Gamificação |
| 📸 **Portfólio do Barbeiro** | Galeria de fotos "antes/depois" dos cortes realizados | Alto — Marketing |
| 💬 **Chat In-App** | Chat entre cliente e barbeiro para dúvidas pré-agendamento | Médio — UX |
| 🏷️ **Cupons e Promoções** | Dono cria cupons (% desconto ou valor fixo) com validade | Alto — Aquisição |
| 🔔 **Lista de Espera Inteligente** | Se não há horário, cliente entra em fila e é notificado quando abre vaga | Alto — Conversão |
| 📱 **PWA (Progressive Web App)** | Instalar no celular como app, funcionar offline para ver agenda | Alto — Experiência mobile |
| 🌙 **Modo Escuro** | Tema dark no frontend | Baixo — Mas todo app moderno tem |
| 🎯 **Onboarding Guiado** | Tutorial interativo no primeiro acesso do Dono (configurar loja passo a passo) | Alto — Ativação |
| 📤 **Compartilhamento Social** | "Acabei de cortar na {barbearia}!" com link + foto | Médio — Marketing orgânico |
| 🤖 **Bot WhatsApp** | Agendar e consultar horários por WhatsApp (via Evolution API) | Alto — Acessibilidade |

### 17.2 Modelo de Monetização

| Plano | Preço Sugerido | Inclui |
|---|---|---|
| **Free** | R$0 | 1 barbearia, 3 barbeiros, agendamentos ilimitados, sem e-commerce |
| **Pro** | R$49/mês | Barbeiros ilimitados, e-commerce, dashboards, notificações, 0% sobre pagamentos |
| **Enterprise** | R$149/mês | Multi-unidades, API de integração, relatórios avançados, suporte prioritário |
| **Taxa marketplace** | 3-5% por transação | Sobre pagamentos online processados |

---

## Convenções e Padrões

### Nomenclatura de Código

| Item | Convenção | Exemplo |
|---|---|---|
| Pacote Java | lowercase, separado por `.` | `ifsp.edu.projeto.cortaai.userservice.service.impl` |
| Classe | UpperCamelCase | `AppointmentService`, `UserInfoDTO` |
| Método | lowerCamelCase | `createAppointment()`, `findByEmail()` |
| Variável | lowerCamelCase | `startTime`, `barbershopId` |
| Constante | UPPER_SNAKE_CASE | `MAX_RETRY_ATTEMPTS` |
| Endpoint REST | kebab-case no path | `/api/barbershops/my-shop/pending-requests` |
| Evento RabbitMQ | dot notation | `appointment.created`, `payment.approved` |
| Coluna DB | snake_case | `barbershop_id`, `date_created` |
| Componente React | UpperCamelCase | `AppointmentCard.jsx` |
| CSS Module | camelCase | `styles.appointmentCard` |
| Branch Git | kebab-case | `feature/social-login`, `fix/appointment-conflict` |
| Commit | Conventional Commits | `feat(schedule): add conflict validation` |

### Estrutura de Pacotes (por Microserviço)

```
ifsp.edu.projeto.cortaai.{servicename}/
├── {ServiceName}Application.java
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthorizationFilter.java
│   ├── FeignConfig.java
│   ├── RabbitConfig.java
│   └── RedisConfig.java
├── controller/
│   ├── {Entity}Controller.java
│   └── InternalController.java       ← Endpoints inter-serviço
├── dto/
│   ├── request/
│   │   └── Create{Entity}Request.java
│   ├── response/
│   │   └── {Entity}Response.java
│   └── internal/                      ← DTOs para comunicação Feign
│       └── UserInfoDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java    ← @ControllerAdvice
│   ├── NotFoundException.java
│   └── BusinessRuleException.java
├── feign/
│   ├── UserServiceClient.java
│   └── BarbershopServiceClient.java
├── mapper/
│   └── {Entity}Mapper.java            ← MapStruct
├── model/
│   ├── {Entity}.java
│   └── enums/
├── repository/
│   └── {Entity}Repository.java
├── service/
│   ├── {Entity}Service.java           ← Interface
│   └── impl/
│       └── {Entity}ServiceImpl.java
├── event/
│   ├── publisher/
│   │   └── AppointmentEventPublisher.java
│   └── listener/
│       └── PaymentEventListener.java
└── validator/
    └── {Custom}Validator.java
```

---

## Checklist Final (Definição de "Pronto")

- [ ] Todos os 6 microserviços compilam e sobem independentemente
- [ ] Todos os testes unitários passam (>80% cobertura)
- [ ] Testes de integração com Testcontainers passam
- [ ] Testes de contrato entre serviços passam
- [ ] Docker Compose produção sobe com `docker compose -f docker-compose.prod.yml up -d`
- [ ] SSL funcional com Let's Encrypt
- [ ] Login Social (Google + 1 outro) funcional
- [ ] Pagamento via Mercado Pago (PIX + Cartão) funcional em sandbox
- [ ] Notificações por email e push funcionais
- [ ] Dashboards com pelo menos 3 gráficos renderizando dados reais
- [ ] E-commerce com catálogo + carrinho + checkout funcional
- [ ] Monitoramento (Prometheus + Grafana) com dashboard de saúde
- [ ] Rate limiting ativo no Gateway
- [ ] Scan de segurança sem vulnerabilidades críticas
- [ ] Documentação da API (Swagger) acessível e atualizada
- [ ] README.md atualizado com instruções de setup

---

> **Este documento é vivo.** Atualize-o a cada Sprint Review com o status real de cada passo.  
> Versione-o no Git junto ao código.

