# Diagrama Entidade-Relacionamento (DER) - Arquitetura de Microservicos

Este DER representa o modelo fisico/logico identificado nas entidades JPA da aplicacao CortaAi.

Na arquitetura de microservicos, cada servico possui seu proprio banco. Por isso:

- Relacionamentos dentro do mesmo banco podem ser FKs reais.
- Relacionamentos entre bancos/servicos sao referencias logicas por UUID, sem FK fisica.
- Campos marcados como `ref` apontam para entidades mantidas por outro servico.
- Campos marcados como `snapshot` guardam uma copia do dado no momento da operacao.
- Views analiticas aparecem em uma secao separada, pois nao sao tabelas transacionais.

## Bancos

| Banco | Servico | Responsabilidade |
|---|---|---|
| `user_db` | `user-service` | Clientes, barbeiros, favoritos, atividades atribuidas e horarios de trabalho |
| `barbershop_db` | `barbershop-service` | Barbearias, servicos, avaliacoes, convites, destaques, comissoes e despesas |
| `schedule_db` | `schedule-service` | Agendamentos, atividades agendadas e bloqueios de agenda |
| `payment_db` | `payment-service` | Transacoes, webhooks e KPIs financeiros diarios |
| `product_db` | `product-service` | Produtos, categorias dinamicas e movimentacoes de estoque |
| `notification_db` | `notification-service` | Notificacoes e tokens de dispositivos |

## user_db

```mermaid
erDiagram
    CUSTOMERS {
        uuid id PK
        varchar_70 name
        varchar_128 tell UK
        varchar_256 email UK
        varchar_64 email_hash UK
        varchar_128 document_cpf UK
        varchar_255 password
        varchar_128 firebase_uid UK
        varchar_30 auth_provider
        varchar_20 role
        varchar_255 image_url
        varchar_255 image_url_public_id
        varchar_128 birth_date
        datetime date_created
        datetime last_updated
    }

    CUSTOMER_FAVORITE_BARBERSHOPS {
        uuid customer_id FK
        uuid barbershop_id "ref BARBERSHOPS"
    }

    BARBERS {
        uuid id PK
        varchar_70 name
        varchar_128 tell UK
        varchar_256 email UK
        varchar_64 email_hash UK
        varchar_128 document_cpf UK
        varchar_255 password
        varchar_128 firebase_uid UK
        varchar_30 auth_provider
        boolean is_owner
        boolean act_as_barber
        varchar_20 role
        uuid barbershop_id "ref BARBERSHOPS"
        time work_start_time
        time work_end_time
        varchar_128 birth_date
        varchar_255 image_url
        varchar_255 image_url_public_id
        text mp_access_token
        text mp_refresh_token
        varchar_256 mp_user_id
        varchar_100 mp_public_key
        datetime date_created
        datetime last_updated
    }

    BARBER_ASSIGNED_ACTIVITIES {
        uuid barber_id FK
        uuid activity_id "ref ACTIVITIES"
    }

    BARBER_WORK_BLOCKS {
        uuid id PK
        uuid barber_id "ref BARBERS"
        varchar_10 day_of_week
        time start_time
        time end_time
    }

    CUSTOMERS ||--o{ CUSTOMER_FAVORITE_BARBERSHOPS : "favorita"
    BARBERS ||--o{ BARBER_ASSIGNED_ACTIVITIES : "executa"
    BARBERS ||..o{ BARBER_WORK_BLOCKS : "define jornada"
```

## barbershop_db

```mermaid
erDiagram
    BARBERSHOPS {
        uuid id PK
        uuid owner_id "ref BARBERS"
        varchar_255 name
        varchar_128 cnpj UK
        varchar_255 address
        varchar_255 logo_url
        varchar_255 logo_url_public_id
        varchar_255 banner_url
        varchar_255 banner_url_public_id
        double latitude
        double longitude
        datetime date_created
        datetime last_updated
    }

    ACTIVITIES {
        uuid id PK
        uuid barbershop_id FK
        varchar_255 activity_name
        decimal_10_2 price
        int duration_minutes
        varchar_255 image_url
        varchar_255 image_url_public_id
        datetime date_created
        datetime last_updated
    }

    BARBERSHOP_JOIN_REQUESTS {
        uuid id PK
        uuid barber_id "ref BARBERS"
        uuid barbershop_id FK
        varchar_50 status
        varchar_20 request_type
        datetime date_created
    }

    BARBERSHOP_HIGHLIGHTS {
        uuid id PK
        uuid barbershop_id FK
        varchar_255 image_url
        varchar_255 image_url_public_id
    }

    BARBERSHOP_REVIEWS {
        uuid id PK
        uuid customer_id "ref CUSTOMERS"
        uuid barbershop_id FK
        int rating
        varchar_500 comment
        datetime created_at
    }

    BARBER_COMMISSION_RULES {
        uuid id PK
        uuid barbershop_id "ref BARBERSHOPS"
        uuid barber_id "ref BARBERS"
        uuid activity_id FK
        decimal_5_2 percentage
        datetime created_at
    }

    FIXED_EXPENSES {
        uuid id PK
        uuid barbershop_id "ref BARBERSHOPS"
        varchar_30 category
        varchar_80 custom_name
        decimal_10_2 amount
        int month
        int year
        boolean recurring_monthly
        datetime created_at
    }

    BARBERSHOPS ||--o{ ACTIVITIES : "oferece"
    BARBERSHOPS ||--o{ BARBERSHOP_JOIN_REQUESTS : "recebe"
    BARBERSHOPS ||--o{ BARBERSHOP_HIGHLIGHTS : "possui"
    BARBERSHOPS ||--o{ BARBERSHOP_REVIEWS : "recebe"
    ACTIVITIES ||--o{ BARBER_COMMISSION_RULES : "parametriza"
```

## schedule_db

```mermaid
erDiagram
    APPOINTMENTS {
        uuid id PK
        uuid customer_id "ref CUSTOMERS"
        uuid barber_id "ref BARBERS"
        uuid barbershop_id "ref BARBERSHOPS"
        varchar_256 customer_name "snapshot"
        varchar_70 barber_name "snapshot"
        varchar_255 barbershop_name "snapshot"
        datetime start_time
        datetime end_time
        decimal_10_2 total_price
        varchar_50 status
        datetime date_created
        datetime last_updated
    }

    APPOINTMENT_ACTIVITIES {
        uuid id PK
        uuid appointment_id FK
        uuid activity_id "ref ACTIVITIES"
        varchar_255 activity_name "snapshot"
        decimal_10_2 price "snapshot"
        int duration_minutes "snapshot"
    }

    BARBER_BLOCKS {
        uuid id PK
        uuid barber_id "ref BARBERS"
        datetime start_time
        datetime end_time
        varchar_255 reason
        datetime date_created
    }

    APPOINTMENTS ||--o{ APPOINTMENT_ACTIVITIES : "inclui"
```

## payment_db

```mermaid
erDiagram
    TRANSACTIONS {
        uuid id PK
        uuid appointment_id "ref APPOINTMENTS"
        uuid customer_id "ref CUSTOMERS"
        uuid barbershop_id "ref BARBERSHOPS"
        decimal_10_2 amount
        decimal_10_2 gross_amount
        decimal_10_2 net_amount
        decimal_10_2 mp_fee_amount
        decimal_10_2 platform_fee_amount
        varchar_30 payment_method
        varchar status
        varchar mp_preference_id UK
        varchar mp_payment_id
        text checkout_url
        datetime created_at
        datetime updated_at
    }

    WEBHOOK_LOGS {
        uuid id PK
        varchar mp_resource_id UK
        varchar event_type
        text raw_payload
        boolean processed
        datetime received_at
    }

    DASHBOARD_KPI_DAILY {
        uuid id PK
        uuid barbershop_id "ref BARBERSHOPS"
        date reference_date
        decimal_12_2 approved_revenue
        int approved_transactions_count
        datetime updated_at
    }
```

## product_db

```mermaid
erDiagram
    CATEGORIES {
        uuid id PK
        varchar_80 name
        uuid barbershop_id "ref BARBERSHOPS"
        datetime created_at
    }

    PRODUCTS {
        uuid id PK
        uuid barbershop_id "ref BARBERSHOPS"
        varchar name
        text description
        decimal price
        varchar category
        uuid category_id FK
        int stock_quantity
        int min_stock_quantity
        varchar image_url
        boolean active
        datetime created_at
        datetime updated_at
    }

    STOCK_MOVEMENTS {
        uuid id PK
        uuid product_id "ref PRODUCTS"
        varchar type
        int quantity
        decimal_10_2 unit_sale_price
        varchar_500 notes
        varchar reason
        datetime created_at
    }

    CATEGORIES ||--o{ PRODUCTS : "classifica"
    PRODUCTS ||..o{ STOCK_MOVEMENTS : "movimenta"
```

## notification_db

```mermaid
erDiagram
    NOTIFICATIONS {
        uuid id PK
        uuid user_id "ref CUSTOMERS ou BARBERS"
        varchar type
        varchar title
        text message
        varchar channel
        boolean is_read
        datetime created_at
    }

    DEVICE_TOKENS {
        uuid id PK
        uuid user_id "ref CUSTOMERS ou BARBERS"
        varchar platform
        varchar_512 token UK
        boolean active
        datetime created_at
        datetime updated_at
    }
```

## Views analiticas

| Banco | View | Campos principais |
|---|---|---|
| `user_db` | `v_customer_acquisition` | `reference_month`, `new_customers` |
| `user_db` | `v_customer_retention` | `reference_month`, `returning_customers` |
| `schedule_db` | `v_agenda_thermometer` | `agenda_date`, `barbershop_id`, `total_appointments`, `active_appointments`, `lost_appointments` |
| `schedule_db` | `v_barber_skill_matrix` | `barber_id`, `activity_name`, `barbershop_id`, `barber_name`, `times_executed`, `total_generated_by_activity` |
| `payment_db` | `v_daily_financial_summary` | resumo financeiro diario |
| `payment_db` | `v_barber_financial_performance` | `barber_id`, `barber_name`, `barbershop_id`, `total_appointments`, `generated_revenue`, `contribution_percentage` |
| `product_db` | `v_stock_health_alert` | `product_id`, `barbershop_id`, `product_name`, `category`, `current_stock`, `predicted_minimum`, `requires_restock` |

## Referencias logicas entre servicos

```text
user-service
  CUSTOMERS.id
    -> barbershop-service.BARBERSHOP_REVIEWS.customer_id
    -> schedule-service.APPOINTMENTS.customer_id
    -> payment-service.TRANSACTIONS.customer_id
    -> notification-service.NOTIFICATIONS.user_id
    -> notification-service.DEVICE_TOKENS.user_id

  BARBERS.id
    -> barbershop-service.BARBERSHOPS.owner_id
    -> barbershop-service.BARBERSHOP_JOIN_REQUESTS.barber_id
    -> barbershop-service.BARBER_COMMISSION_RULES.barber_id
    -> schedule-service.APPOINTMENTS.barber_id
    -> schedule-service.BARBER_BLOCKS.barber_id

barbershop-service
  BARBERSHOPS.id
    -> user-service.BARBERS.barbershop_id
    -> user-service.CUSTOMER_FAVORITE_BARBERSHOPS.barbershop_id
    -> schedule-service.APPOINTMENTS.barbershop_id
    -> payment-service.TRANSACTIONS.barbershop_id
    -> payment-service.DASHBOARD_KPI_DAILY.barbershop_id
    -> product-service.PRODUCTS.barbershop_id
    -> product-service.CATEGORIES.barbershop_id

  ACTIVITIES.id
    -> user-service.BARBER_ASSIGNED_ACTIVITIES.activity_id
    -> schedule-service.APPOINTMENT_ACTIVITIES.activity_id

schedule-service
  APPOINTMENTS.id
    -> payment-service.TRANSACTIONS.appointment_id
```
