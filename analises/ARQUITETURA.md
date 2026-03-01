# 🏗️ Arquitetura da Aplicação — CortaAí

> **Versão:** 2.0 — Arquitetura Alvo (To-Be)  
> **Data:** 28 de fevereiro de 2026

---

## 1. Visão Geral

**CortaAí** é um marketplace SaaS para barbearias, construído com **arquitetura de microsserviços**. Conecta clientes, barbeiros e donos de barbearias em uma plataforma única com agendamento, pagamentos, notificações e e-commerce.

```
                         ┌──────────────┐
                         │   BROWSER    │
                         └──────┬───────┘
                                │
                 ┌──────────────▼──────────────┐
                 │   Frontend React 19 / Vite   │
                 │      (cortaai-web :5173)      │
                 └──────────────┬──────────────┘
                                │ REST /api/*
                 ┌──────────────▼──────────────┐
                 │      API Gateway :8080       │
                 │   Spring Cloud Gateway       │
                 └──┬───┬───┬───┬───┬───┬──────┘
                    │   │   │   │   │   │
        ┌───────────┘   │   │   │   │   └───────────┐
        │       ┌───────┘   │   │   └───────┐       │
        ▼       ▼           ▼   ▼           ▼       ▼
   ┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
   │ user   ││barber- ││schedule││payment ││notific.││product │
   │service ││shop-svc││service ││service ││service ││service │
   │ :8081  ││ :8082  ││ :8083  ││ :8084  ││ :8085  ││ :8086  │
   └───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘
       │         │         │         │         │         │
       │  Eureka │  Eureka │  Eureka │  Eureka │  Eureka │  Eureka
       └────┬────┴────┬────┴────┬────┴────┬────┴────┬────┴────┘
            │         │         │         │         │
            ▼         ▼         ▼         ▼         ▼
   ┌─────────────────────────────────────────────────────────┐
   │            Discovery Service (Eureka :8761)              │
   └─────────────────────────────────────────────────────────┘

   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
   │ user_db  │  │barber_db │  │sched_db  │  │payment_db│  │product_db│
   └─────┬────┘  └─────┬────┘  └─────┬────┘  └─────┬────┘  └─────┬────┘
         └──────────────┴──────────┬──┴─────────────┴─────────────┘
                            ┌──────▼──────┐
                            │  MySQL 8.0  │
                            │    :3306    │
                            └─────────────┘

   ┌─────────────┐          ┌─────────────┐
   │  RabbitMQ   │          │    Redis    │
   │  :5672      │          │   :6379    │
   │  :15672 mgmt│          │            │
   └─────────────┘          └─────────────┘
```

---

## 2. Stack Tecnológica

| Camada | Tecnologia | Versão |
|---|---|---|
| **Frontend** | React + Vite | React 19 / Vite 7 |
| **HTTP Client** | Axios | 1.13.x |
| **Roteamento SPA** | React Router DOM | 7.x |
| **API Gateway** | Spring Cloud Gateway | — |
| **Service Discovery** | Netflix Eureka (Server + Client) | — |
| **Backend Framework** | Spring Boot | 3.3.4 |
| **Spring Cloud** | Spring Cloud | 2023.0.1 |
| **Segurança** | Spring Security + JJWT | 0.11.5 |
| **Comunicação Síncrona** | OpenFeign + Resilience4j | — |
| **Comunicação Assíncrona** | RabbitMQ (Spring AMQP) | — |
| **Cache** | Redis (Spring Data Redis) | 7 |
| **ORM** | Spring Data JPA / Hibernate | — |
| **Mapeamento DTO** | MapStruct + Lombok | 1.5.5 |
| **Banco de Dados** | MySQL | 8.0 |
| **Upload de Imagens** | Cloudinary | — |
| **Pagamentos** | Mercado Pago SDK | 2.1.24 |
| **Email** | Resend / Spring Mail | — |
| **Push** | Firebase Cloud Messaging | — |
| **Templates** | Thymeleaf (emails) | — |
| **Container Runtime** | Docker + Docker Compose | — |
| **JDK** | Eclipse Temurin | 17 |

---

## 3. Microsserviços — Detalhamento

### 3.1 Discovery Service `:8761`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | Registro e descoberta de serviços |
| **Tecnologia** | Spring Cloud Netflix Eureka Server |
| **Banco** | Nenhum |

### 3.2 API Gateway `:8080`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | Ponto de entrada único, roteamento, rate limiting |
| **Rotas** | `/api/customers/**`, `/api/barbers/**`, `/api/auth/**` → `user-service` |
| | `/api/barbershops/**` → `barbershop-service` |
| | `/api/appointments/**` → `schedule-service` |
| | `/api/payments/**` → `payment-service` |
| | `/api/products/**`, `/api/orders/**` → `product-service` |
| | `/api/notifications/**` → `notification-service` |
| **Load Balancing** | Client-side via `lb://` (Eureka) |

### 3.3 User Service `:8081`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | Autenticação (JWT + OAuth2), cadastro, perfis |
| **Banco** | `user_db` |
| **Entidades** | Users (Customer/Barber unificado), RefreshTokens |
| **Endpoints** | Login, Register, Social Login, Perfil |
| **Roles** | `ROLE_CUSTOMER`, `ROLE_BARBER`, `ROLE_OWNER` |

### 3.4 Barbershop Service `:8082`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | Gestão de barbearias, serviços, equipe, avaliações |
| **Banco** | `barbershop_db` |
| **Entidades** | Barbershop, Activity, BarberActivity, JoinRequest, Highlight, Review, WorkingHours |
| **Comunicação** | Feign → user-service · RabbitMQ (eventos) |
| **Resiliência** | Resilience4j (circuit breaker + retry) |

### 3.5 Schedule Service `:8083`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | Agendamentos, disponibilidade, bloqueios de agenda |
| **Banco** | `schedule_db` |
| **Entidades** | Appointment, AppointmentActivity, BarberBlock |
| **Comunicação** | Feign → user-service + barbershop-service · RabbitMQ · Redis (cache) |

### 3.6 Payment Service `:8084`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | Pagamentos via Mercado Pago, webhooks, split payment |
| **Banco** | `payment_db` |
| **Entidades** | Transaction, PaymentWebhookLog |
| **Comunicação** | Feign → schedule-service · RabbitMQ (eventos) |

### 3.7 Notification Service `:8085`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | Email, push, WebSocket, WhatsApp |
| **Banco** | Redis (sem MySQL) |
| **Canais** | Resend (email), FCM (push), STOMP (in-app) |
| **Comunicação** | Consumer RabbitMQ · Redis (dedup + cache) |

### 3.8 Product Service `:8086`

| Item | Detalhe |
|---|---|
| **Responsabilidade** | e-Commerce de produtos de barbearia |
| **Banco** | `product_db` |
| **Entidades** | Product, Order, OrderItem, StockMovement |
| **Comunicação** | Feign → payment-service · RabbitMQ (eventos) |

---

## 4. Banco de Dados — Database per Service

```sql
-- init.sql
CREATE DATABASE IF NOT EXISTS user_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS barbershop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS schedule_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- notification-service usa Redis (sem schema MySQL)
```

| Schema | Serviço | Tabelas Principais |
|---|---|---|
| `user_db` | user-service | users, refresh_tokens |
| `barbershop_db` | barbershop-service | barbershops, activities, barber_activities, join_requests, highlights, reviews, working_hours |
| `schedule_db` | schedule-service | appointments, appointment_activities, barber_blocks |
| `payment_db` | payment-service | transactions, payment_webhooks_log |
| `product_db` | product-service | products, orders, order_items, stock_movements |

---

## 5. Estrutura de Pacotes (Backend)

```
backend/
├── pom.xml                              # POM pai (multi-module Maven)
├── mvnw / mvnw.cmd                      # Maven Wrapper
├── src/                                 # Código legado (monólito — referência)
│
├── discovery-service/
│   ├── pom.xml
│   └── src/main/java/.../discoveryservice/
│       └── DiscoveryServiceApplication.java
│
├── api-gateway/
│   ├── pom.xml
│   └── src/main/java/.../apigateway/
│       ├── ApiGatewayApplication.java
│       ├── config/
│       ├── filter/
│       └── exception/
│
├── user-service/
│   ├── pom.xml
│   └── src/main/java/.../userservice/
│       ├── UserServiceApplication.java
│       ├── config/          # SecurityConfig, JwtFilter, WebConfig
│       ├── controller/      # CustomerController, BarberController
│       ├── dto/             # LoginDTO, CustomerCreateDTO, etc.
│       ├── exception/       # NotFoundException, etc.
│       ├── event/           # UserDeletedEvent, etc.
│       ├── mapper/          # MapStruct mappers
│       ├── model/enums/     # BarberSkills, JoinRequestStatus
│       ├── repository/      # Spring Data JPA
│       ├── service/impl/    # Lógica de negócio
│       ├── service/storage/ # Cloudinary
│       └── validador/       # CPF, Email, Tell validators
│
├── barbershop-service/
│   ├── pom.xml
│   └── src/main/java/.../barbershopservice/
│       ├── BarbershopServiceApplication.java
│       ├── config/          # Cloudinary, Swagger, CORS
│       ├── controller/      # BarbershopController
│       ├── dto/
│       ├── exception/
│       ├── event/           # Eventos RabbitMQ
│       ├── feign/           # UserServiceClient
│       ├── mapper/
│       ├── model/enums/
│       ├── repository/
│       ├── service/impl/
│       ├── service/storage/ # Cloudinary upload
│       └── validator/       # CNPJ validator
│
├── schedule-service/
│   ├── pom.xml
│   └── src/main/java/.../scheduleservice/
│       ├── ScheduleServiceApplication.java
│       ├── config/
│       ├── controller/      # AppointmentsController
│       ├── dto/
│       ├── exception/
│       ├── event/           # AppointmentCreatedEvent
│       ├── feign/           # UserServiceClient, BarbershopServiceClient
│       ├── mapper/
│       ├── model/enums/     # AppointmentStatus
│       ├── repository/
│       └── service/impl/
│
├── payment-service/
│   ├── pom.xml
│   └── src/main/java/.../paymentservice/
│       ├── PaymentServiceApplication.java
│       ├── config/          # MercadoPagoConfig
│       ├── controller/      # PaymentController, WebhookController
│       ├── dto/
│       ├── exception/
│       ├── event/           # PaymentApprovedEvent
│       ├── feign/           # ScheduleServiceClient
│       ├── mapper/
│       ├── model/enums/     # PaymentStatus, PaymentMethod
│       ├── repository/
│       └── service/impl/
│
├── notification-service/
│   ├── pom.xml
│   └── src/main/java/.../notificationservice/
│       ├── NotificationServiceApplication.java
│       ├── config/          # RabbitMQ, WebSocket, Redis
│       ├── controller/      # NotificationController (WebSocket)
│       ├── dto/
│       ├── event/           # Eventos consumidos do RabbitMQ
│       ├── listener/        # RabbitMQ listeners
│       ├── model/enums/     # NotificationType, Channel
│       ├── repository/
│       ├── service/impl/
│       ├── service/channel/ # EmailChannel, PushChannel, WhatsAppChannel
│       └── template/        # Templates lógicos de notificação
│   └── src/main/resources/
│       └── templates/       # Thymeleaf email templates
│
└── product-service/
    ├── pom.xml
    └── src/main/java/.../productservice/
        ├── ProductServiceApplication.java
        ├── config/
        ├── controller/      # ProductController, OrderController
        ├── dto/
        ├── exception/
        ├── event/           # StockLowEvent, OrderCreatedEvent
        ├── feign/           # PaymentServiceClient
        ├── mapper/
        ├── model/enums/     # OrderStatus, ProductCategory
        ├── repository/
        └── service/impl/
```

---

## 6. Infraestrutura Docker

```
┌──────────────────────────────────────────────────────────────────┐
│                       Docker Compose                              │
│                                                                   │
│  INFRA:                                                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  MySQL 8.0  │  │  RabbitMQ   │  │    Redis    │              │
│  │   :3306     │  │ :5672/:15672│  │   :6379     │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│                                                                   │
│  DISCOVERY & GATEWAY:                                             │
│  ┌─────────────┐  ┌─────────────┐                                │
│  │  Eureka     │  │ API Gateway │                                │
│  │   :8761     │  │   :8080     │                                │
│  └─────────────┘  └─────────────┘                                │
│                                                                   │
│  NEGÓCIO:                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                         │
│  │user-svc  │ │barber-svc│ │sched-svc │                         │
│  │  :8081   │ │  :8082   │ │  :8083   │                         │
│  └──────────┘ └──────────┘ └──────────┘                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                         │
│  │pay-svc   │ │notif-svc │ │prod-svc  │                         │
│  │  :8084   │ │  :8085   │ │  :8086   │                         │
│  └──────────┘ └──────────┘ └──────────┘                         │
│                                                                   │
│  FRONTEND:                                                        │
│  ┌─────────────┐                                                  │
│  │ cortaai-web │  Volume: db_data                                │
│  │   :5173     │                                                  │
│  └─────────────┘                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Ordem de inicialização (depends_on):
```
db (healthy) ──► discovery ──► gateway
                           ──► user-service
rabbitmq (healthy) ────────┤
redis (healthy) ───────────┤
                           ──► barbershop-service
                           ──► schedule-service
                           ──► payment-service
                           ──► notification-service
                           ──► product-service

frontend (independente)
```

---

## 7. Comunicação Inter-Serviço

| Tipo | Ferramenta | Quando |
|---|---|---|
| **Síncrono** | OpenFeign + Eureka | Validação imediata (ex: barbeiro pertence à barbearia?) |
| **Assíncrono** | RabbitMQ (Spring AMQP) | Eventos eventuais (ex: notificar, processar pagamento) |
| **Cache** | Redis | Dados quentes (ex: catálogo de serviços, perfil público) |

### Endpoints internos (inter-serviço)
Prefixo `/api/internal/` — não exposto pelo Gateway:
```
user-service:
  GET  /api/internal/users/{id}
  GET  /api/internal/users/by-email/{email}
  PUT  /api/internal/users/{id}/barbershop

barbershop-service:
  GET  /api/internal/barbershops/{id}
  GET  /api/internal/barbershops/{shopId}/activities
  GET  /api/internal/barbershops/{shopId}/barber-activities/{barberId}
```

---

## 8. Padrões Arquiteturais

| Padrão | Implementação |
|---|---|
| **API Gateway** | Spring Cloud Gateway centraliza roteamento |
| **Service Registry** | Netflix Eureka (registro + lookup) |
| **Client-Side Load Balancing** | `lb://` resolve via Eureka |
| **Database per Service** | Cada serviço tem schema isolado |
| **JWT Stateless Auth** | Token gerado no user-service |
| **DTO Pattern** | MapStruct converte entidades ↔ DTOs |
| **Repository Pattern** | Spring Data JPA Repositories |
| **Circuit Breaker** | Resilience4j em chamadas Feign |
| **Event-Driven** | RabbitMQ para comunicação assíncrona |
| **CQRS (futuro)** | Dados desnormalizados em appointments |
| **Twelve-Factor App** | Configs via env vars, logs stdout |

---

## 9. Portas e Containers

| Serviço | Container | Porta | Banco |
|---|---|---|---|
| MySQL | cortaai-mysql | `3306` | — |
| RabbitMQ | cortaai-rabbitmq | `5672` / `15672` | — |
| Redis | cortaai-redis | `6379` | — |
| Discovery | discovery-service | `8761` | — |
| API Gateway | api-gateway | `8080` | — |
| User Service | user-service | `8081` | user_db |
| Barbershop Service | barbershop-service | `8082` | barbershop_db |
| Schedule Service | schedule-service | `8083` | schedule_db |
| Payment Service | payment-service | `8084` | payment_db |
| Notification Service | notification-service | `8085` | Redis |
| Product Service | product-service | `8086` | product_db |
| Frontend | cortaai-web | `5173` | — |
