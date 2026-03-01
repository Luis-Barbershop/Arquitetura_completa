# Diagrama Entidade-Relacionamento (DER) — Arquitetura de Microservicos

> **Nota:** Na arquitetura de microservicos, cada servico possui seu proprio banco de dados.
> Relacionamentos entre servicos sao feitos por **IDs desacoplados** (nao ha FK real entre bancos).
> IDs marcados como `ref` indicam referencias logicas a entidades em outro servico.

---

## Visao por Servico

### user_db (user-service)

```mermaid
erDiagram
    CUSTOMERS {
        uuid id PK
        varchar_70 name
        varchar_11 tell UK
        varchar_70 email UK
        varchar_11 document_cpf UK
        varchar_255 password
        varchar_20 role
        timestamp date_created
        timestamp last_updated
        varchar_255 image_url
        varchar_255 image_url_public_id
    }
    BARBERS {
        uuid id PK
        varchar_70 name
        varchar_11 tell UK
        varchar_70 email UK
        varchar_11 document_cpf UK
        varchar_255 password
        boolean is_owner
        varchar_20 role
        uuid barbershop_id "ref barbershop-service"
        time work_start_time
        time work_end_time
        varchar_255 image_url
        varchar_255 image_url_public_id
        timestamp date_created
        timestamp last_updated
    }
```

---

### barbershop_db (barbershop-service)

```mermaid
erDiagram
    BARBERSHOPS {
        uuid id PK
        uuid owner_id "ref user-service BARBERS"
        varchar_255 name
        varchar_14 cnpj UK
        varchar_255 address
        varchar_255 logo_url
        varchar_255 logo_url_public_id
        varchar_255 banner_url
        varchar_255 banner_url_public_id
        timestamp date_created
        timestamp last_updated
    }
    ACTIVITIES {
        uuid id PK
        uuid barbershop_id FK
        varchar_255 activity_name
        decimal_10_2 price
        int duration_minutes
        varchar_255 image_url
        varchar_255 image_url_public_id
        timestamp date_created
        timestamp last_updated
    }
    BARBERSHOP_JOIN_REQUESTS {
        uuid id PK
        uuid barber_id "ref user-service BARBERS"
        uuid barbershop_id FK
        varchar_50 status "PENDING ACCEPTED REJECTED"
        timestamp date_created
    }
    BARBERSHOP_HIGHLIGHTS {
        uuid id PK
        uuid barbershop_id FK
        varchar_255 image_url
        varchar_255 image_url_public_id
    }

    BARBERSHOPS ||--o{ ACTIVITIES : "oferece"
    BARBERSHOPS ||--o{ BARBERSHOP_JOIN_REQUESTS : "recebe pedido"
    BARBERSHOPS ||--o{ BARBERSHOP_HIGHLIGHTS : "possui destaque"
```

---

### schedule_db (schedule-service)

```mermaid
erDiagram
    APPOINTMENTS {
        uuid id PK
        uuid customer_id "ref user-service CUSTOMERS"
        uuid barber_id "ref user-service BARBERS"
        uuid barbershop_id "ref barbershop-service"
        varchar_70 customer_name "snapshot"
        varchar_70 barber_name "snapshot"
        varchar_255 barbershop_name "snapshot"
        datetime start_time
        datetime end_time
        decimal_10_2 total_price
        varchar_50 status "SCHEDULED CONFIRMED CANCELLED CONCLUDED"
        timestamp date_created
        timestamp last_updated
    }
    APPOINTMENT_ACTIVITIES {
        uuid id PK
        uuid appointment_id FK
        uuid activity_id "ref barbershop-service ACTIVITIES"
        varchar_255 activity_name "snapshot"
        decimal_10_2 price "snapshot"
        int duration_minutes "snapshot"
    }
    BARBER_BLOCKS {
        uuid id PK
        uuid barber_id "ref user-service BARBERS"
        datetime start_time
        datetime end_time
        varchar_255 reason
        timestamp date_created
    }

    APPOINTMENTS ||--o{ APPOINTMENT_ACTIVITIES : "inclui"
```

---

### payment_db (payment-service)

```mermaid
erDiagram
    TRANSACTIONS {
        uuid id PK
        uuid appointment_id "ref schedule-service"
        uuid customer_id "ref user-service"
        decimal amount
        varchar status "PENDING APPROVED REJECTED REFUNDED"
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
```

---

### product_db (product-service)

```mermaid
erDiagram
    PRODUCTS {
        uuid id PK
        uuid barbershop_id "ref barbershop-service"
        varchar name
        text description
        decimal price
        varchar category "HAIR BEARD SKINCARE ACCESSORY OTHER"
        int stock_quantity
        varchar image_url
        boolean active
        datetime created_at
        datetime updated_at
    }
    ORDERS {
        uuid id PK
        uuid customer_id "ref user-service"
        uuid barbershop_id "ref barbershop-service"
        varchar status "PENDING CONFIRMED CANCELLED"
        decimal total_price
        datetime created_at
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
        varchar type "IN OUT ADJUSTMENT"
        int quantity
        uuid order_id "ref ORDERS opcional"
        varchar reason
        datetime created_at
    }

    PRODUCTS ||--o{ ORDER_ITEMS : "compoe"
    PRODUCTS ||--o{ STOCK_MOVEMENTS : "movimenta estoque"
    ORDERS ||--o{ ORDER_ITEMS : "contem"
```

---

### notification_db (notification-service)

```mermaid
erDiagram
    NOTIFICATIONS {
        uuid id PK
        uuid user_id "ref user-service"
        varchar type "APPOINTMENT_CREATED APPOINTMENT_CANCELLED PAYMENT_APPROVED"
        varchar title
        text message
        varchar channel "IN_APP EMAIL PUSH"
        boolean is_read
        datetime created_at
    }
```

---

## Visao Geral — Referencias entre Servicos

```
user-service (user_db)
  CUSTOMERS ---ref---> schedule-service.APPOINTMENTS.customer_id
            ---ref---> payment-service.TRANSACTIONS.customer_id
            ---ref---> product-service.ORDERS.customer_id
            ---ref---> notification-service.NOTIFICATIONS.user_id
  BARBERS   ---ref---> barbershop-service.BARBERSHOP_JOIN_REQUESTS.barber_id
            ---ref---> schedule-service.APPOINTMENTS.barber_id
            ---ref---> schedule-service.BARBER_BLOCKS.barber_id

barbershop-service (barbershop_db)
  BARBERSHOPS ---ref---> schedule-service.APPOINTMENTS.barbershop_id
              ---ref---> product-service.PRODUCTS.barbershop_id
              ---ref---> product-service.ORDERS.barbershop_id
  ACTIVITIES  ---ref---> schedule-service.APPOINTMENT_ACTIVITIES.activity_id

schedule-service (schedule_db)
  APPOINTMENTS ---ref---> payment-service.TRANSACTIONS.appointment_id
```