# CortaAi — Diagrama ER Visual (Mermaid)

> Diagrama unificado de todas as entidades da plataforma CortaAi.
> - Linhas **sólidas** (`--`) = FK real (mesmo banco de dados)
> - Linhas **tracejadas** (`..`) = referência lógica cross-service (sem FK real)
> - Campos de auditoria (`date_created`, `last_updated`) e imagem omitidos para clareza.
> - Detalhes completos em [`der.md`](der.md).

```mermaid
---
title: CortaAi - Diagrama Entidade-Relacionamento
---
erDiagram

    %% ── user-service (user_db) ──

    CUSTOMERS {
        uuid id PK
        varchar name
        varchar tell UK
        varchar email UK
        varchar document_cpf UK
        varchar password
        varchar role
    }

    BARBERS {
        uuid id PK
        varchar name
        varchar tell UK
        varchar email UK
        varchar document_cpf UK
        varchar password
        boolean is_owner
        varchar role
        uuid barbershop_id "ref BARBERSHOPS"
        time work_start_time
        time work_end_time
    }

    %% ── barbershop-service (barbershop_db) ──

    BARBERSHOPS {
        uuid id PK
        uuid owner_id "ref BARBERS"
        varchar name
        varchar cnpj UK
        varchar address
    }

    ACTIVITIES {
        uuid id PK
        uuid barbershop_id FK
        varchar activity_name
        decimal price
        int duration_minutes
    }

    BARBERSHOP_JOIN_REQUESTS {
        uuid id PK
        uuid barber_id "ref BARBERS"
        uuid barbershop_id FK
        varchar status
    }

    BARBERSHOP_HIGHLIGHTS {
        uuid id PK
        uuid barbershop_id FK
        varchar image_url
    }

    %% ── schedule-service (schedule_db) ──

    APPOINTMENTS {
        uuid id PK
        uuid customer_id "ref CUSTOMERS"
        uuid barber_id "ref BARBERS"
        uuid barbershop_id "ref BARBERSHOPS"
        varchar customer_name "snapshot"
        varchar barber_name "snapshot"
        varchar barbershop_name "snapshot"
        datetime start_time
        datetime end_time
        decimal total_price
        varchar status
    }

    APPOINTMENT_ACTIVITIES {
        uuid id PK
        uuid appointment_id FK
        uuid activity_id "ref ACTIVITIES"
        varchar activity_name "snapshot"
        decimal price "snapshot"
        int duration_minutes "snapshot"
    }

    BARBER_BLOCKS {
        uuid id PK
        uuid barber_id "ref BARBERS"
        datetime start_time
        datetime end_time
        varchar reason
    }

    %% ── payment-service (payment_db) ──

    TRANSACTIONS {
        uuid id PK
        uuid appointment_id "ref APPOINTMENTS"
        uuid customer_id "ref CUSTOMERS"
        decimal amount
        varchar status
        varchar mp_preference_id UK
        varchar mp_payment_id
        text checkout_url
    }

    WEBHOOK_LOGS {
        uuid id PK
        varchar mp_resource_id UK
        varchar event_type
        text raw_payload
        boolean processed
    }

    %% ── product-service (product_db) ──

    PRODUCTS {
        uuid id PK
        uuid barbershop_id "ref BARBERSHOPS"
        varchar name
        text description
        decimal price
        varchar category
        int stock_quantity
        boolean active
    }

    ORDERS {
        uuid id PK
        uuid customer_id "ref CUSTOMERS"
        uuid barbershop_id "ref BARBERSHOPS"
        varchar status
        decimal total_price
    }

    ORDER_ITEMS {
        uuid id PK
        uuid order_id FK
        uuid product_id "ref PRODUCTS"
        varchar product_name "snapshot"
        decimal price "snapshot"
        int quantity
    }

    STOCK_MOVEMENTS {
        uuid id PK
        uuid product_id "ref PRODUCTS"
        varchar type
        int quantity
        uuid order_id "ref ORDERS"
        varchar reason
    }

    %% ── notification-service (notification_db) ──

    NOTIFICATIONS {
        uuid id PK
        uuid user_id "ref CUSTOMERS ou BARBERS"
        varchar type
        varchar title
        text message
        varchar channel
        boolean is_read
    }

    %% ── Relacionamentos: Barbearia ──
    BARBERS ||..o| BARBERSHOPS : "e dono de"
    BARBERSHOPS ||--o{ ACTIVITIES : "oferece"
    BARBERSHOPS ||--o{ BARBERSHOP_JOIN_REQUESTS : "recebe pedido"
    BARBERSHOPS ||--o{ BARBERSHOP_HIGHLIGHTS : "destaque"
    BARBERS ||..o{ BARBERSHOP_JOIN_REQUESTS : "solicita entrada"

    %% ── Relacionamentos: Agendamento ──
    CUSTOMERS ||..o{ APPOINTMENTS : "agenda"
    BARBERS ||..o{ APPOINTMENTS : "atende"
    BARBERSHOPS ||..o{ APPOINTMENTS : "local"
    APPOINTMENTS ||--o{ APPOINTMENT_ACTIVITIES : "inclui"
    ACTIVITIES ||..o{ APPOINTMENT_ACTIVITIES : "servico"
    BARBERS ||..o{ BARBER_BLOCKS : "bloqueia horario"

    %% ── Relacionamentos: Pagamento ──
    APPOINTMENTS ||..o| TRANSACTIONS : "gera pagamento"
    CUSTOMERS ||..o{ TRANSACTIONS : "paga"

    %% ── Relacionamentos: Produtos ──
    BARBERSHOPS ||..o{ PRODUCTS : "vende"
    PRODUCTS ||--o{ ORDER_ITEMS : "compoe"
    PRODUCTS ||--o{ STOCK_MOVEMENTS : "movimenta estoque"
    CUSTOMERS ||..o{ ORDERS : "compra"
    BARBERSHOPS ||..o{ ORDERS : "recebe pedido"
    ORDERS ||--o{ ORDER_ITEMS : "contem"

    %% ── Relacionamentos: Notificacao ──
    CUSTOMERS ||..o{ NOTIFICATIONS : "recebe"
```
