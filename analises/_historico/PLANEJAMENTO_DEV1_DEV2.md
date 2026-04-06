# 📋 Planejamento de Tarefas — DEV 1 e DEV 2

> **Data:** 01 de março de 2026  
> **Baseado em:** `RELATORIO_ENDPOINTS_MONOLITO_VS_MICROSERVICOS.md`  
> **Branch:** `feature/migracao-microservicos`  
> **Objetivo:** Listar tudo que falta implementar, distribuir entre DEV 1 e DEV 2, e documentar o mapa completo da infraestrutura (RabbitMQ, Feign, Gateway).

---

## Índice

1. [Distribuição DEV 1 vs DEV 2](#1-distribuição-dev-1-vs-dev-2)
2. [Mapa Completo de Infraestrutura](#2-mapa-completo-de-infraestrutura)
   - [RabbitMQ — Exchange, Queues, Producers e Consumers](#21-rabbitmq--exchange-queues-producers-e-consumers)
   - [Feign Clients — Comunicação Síncrona](#22-feign-clients--comunicação-síncrona)
   - [API Gateway — Rotas](#23-api-gateway--rotas)
   - [Docker Compose — Containers e Portas](#24-docker-compose--containers-e-portas)
3. [Tarefas DEV 1 — user-service + barbershop-service + schedule-service](#3-tarefas-dev-1)
4. [Tarefas DEV 2 — payment-service + notification-service + product-service](#4-tarefas-dev-2)
5. [Tarefas COMPARTILHADAS — Infraestrutura](#5-tarefas-compartilhadas--infraestrutura)
6. [Cronograma Sugerido](#6-cronograma-sugerido)

---

## 1. Distribuição DEV 1 vs DEV 2

| | **DEV 1** | **DEV 2** |
|---|---|---|
| **Serviços** | `user-service`, `barbershop-service`, `schedule-service` | `payment-service`, `notification-service`, `product-service` |
| **Foco** | Serviços "core" do monolito (autenticação, barbearia, agendamento) | Serviços novos (pagamento, notificação, e-commerce) |
| **Infra** | `api-gateway` (JWT centralizado), `discovery-service` | `docker-compose.yml`, `.env` |
| **Já feito** | Controllers, Services, Feign, RabbitMQ producers, Models básicos | Controllers, Services, Feign, RabbitMQ consumers, Models básicos |
| **Pendente** | Campos faltantes nos models, `BarberActivity`, `WorkingHours`, `Review`, JWT no Gateway | Campos faltantes nos models, envio real de email/push, integração product→payment |

### Regra de Ouro: Quem toca em quê

| Arquivo/Diretório | DEV 1 | DEV 2 | Observação |
|---|---|---|---|
| `user-service/**` | ✅ | ❌ | DEV 1 exclusivo |
| `barbershop-service/**` | ✅ | ❌ | DEV 1 exclusivo |
| `schedule-service/**` | ✅ | ❌ | DEV 1 exclusivo |
| `payment-service/**` | ❌ | ✅ | DEV 2 exclusivo |
| `notification-service/**` | ❌ | ✅ | DEV 2 exclusivo |
| `product-service/**` | ❌ | ✅ | DEV 2 exclusivo |
| `api-gateway/**` | ✅ | ❌ | DEV 1 (JWT filter, security) |
| `discovery-service/**` | ✅ | ❌ | DEV 1 (se precisar ajustar) |
| `docker-compose.yml` | ❌ | ✅ | DEV 2 (novos containers, health) |
| `docker-compose.server.yml` | ❌ | ✅ | DEV 2 |
| `.env.example` | 🤝 | 🤝 | Combinar — cada um adiciona suas variáveis |
| `init.sql` | 🤝 | 🤝 | Combinar — cada um adiciona seus schemas |

---

## 2. Mapa Completo de Infraestrutura

### 2.1 RabbitMQ — Exchange, Queues, Producers e Consumers

#### Exchange

| Exchange | Tipo | Declarado em | Descrição |
|----------|------|-------------|-----------|
| `cortaai.events` | `TopicExchange` | `schedule-service`, `payment-service`, `notification-service` | Exchange central de todos os eventos do sistema |

#### Eventos, Producers e Consumers

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              EXCHANGE: cortaai.events (topic)                           │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   ┌──────────────────────┐                        ┌──────────────────────────────────┐  │
│   │   PRODUCER            │   appointment.created  │  CONSUMER                        │  │
│   │   schedule-service    │ ─────────────────────► │  notification-service            │  │
│   │   (AppointmentService │                        │  (NotificationEventListener)     │  │
│   │    .createAppointment)│                        │  Queue: notification.appointment │  │
│   └──────────────────────┘                        │         .created                 │  │
│                                                    └──────────────────────────────────┘  │
│                                                                                         │
│   ┌──────────────────────┐                        ┌──────────────────────────────────┐  │
│   │   PRODUCER            │  appointment.cancelled │  CONSUMER                        │  │
│   │   schedule-service    │ ─────────────────────► │  notification-service            │  │
│   │   (AppointmentService │                        │  (NotificationEventListener)     │  │
│   │    .cancelAppointment)│                        │  Queue: notification.appointment │  │
│   └──────────────────────┘                        │         .cancelled               │  │
│                                                    └──────────────────────────────────┘  │
│                                                                                         │
│   ┌──────────────────────┐                        ┌──────────────────────────────────┐  │
│   │   PRODUCER            │  appointment.concluded │  CONSUMER                        │  │
│   │   schedule-service    │ ─────────────────────► │  notification-service            │  │
│   │   (AppointmentService │                        │  (NotificationEventListener)     │  │
│   │  .concludeAppointment)│                        │  Queue: notification.appointment │  │
│   └──────────────────────┘                        │         .concluded               │  │
│                                                    └──────────────────────────────────┘  │
│                                                                                         │
│   ┌──────────────────────┐                        ┌──────────────────────────────────┐  │
│   │   PRODUCER            │    payment.approved    │  CONSUMER                        │  │
│   │   payment-service     │ ─────────────────────► │  notification-service            │  │
│   │   (PaymentService     │                        │  (NotificationEventListener)     │  │
│   │    .processWebhook)   │                        │  Queue: notification.payment     │  │
│   └──────────────────────┘                        │         .approved                │  │
│                                                    └──────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Tabela Detalhada

| Routing Key | Evento (DTO) | Producer (Serviço → Classe → Método) | Consumer (Serviço → Classe → Método) | Queue |
|---|---|---|---|---|
| `appointment.created` | `AppointmentCreatedEvent` | **schedule-service** → `AppointmentService.createAppointment()` | **notification-service** → `NotificationEventListener.onAppointmentCreated()` | `notification.appointment.created` |
| `appointment.cancelled` | `AppointmentCancelledEvent` | **schedule-service** → `AppointmentService.cancelAppointment()` | **notification-service** → `NotificationEventListener.onAppointmentCancelled()` | `notification.appointment.cancelled` |
| `appointment.concluded` | `AppointmentConcludedEvent` | **schedule-service** → `AppointmentService.concludeAppointment()` | **notification-service** → `NotificationEventListener.onAppointmentConcluded()` | `notification.appointment.concluded` |
| `payment.approved` | `PaymentApprovedEvent` | **payment-service** → `PaymentService.processWebhook()` | **notification-service** → `NotificationEventListener.onPaymentApproved()` | `notification.payment.approved` |

#### DTOs de Eventos (Campos)

| Evento | Campos |
|--------|--------|
| `AppointmentCreatedEvent` | `appointmentId`, `customerId`, `barberId`, `barbershopId`, `customerName`, `barberName`, `barbershopName`, `startTime`, `totalPrice` |
| `AppointmentCancelledEvent` | `appointmentId`, `customerId`, `barberId`, `cancelledBy` |
| `AppointmentConcludedEvent` | `appointmentId`, `customerId`, `barberId`, `barbershopId` |
| `PaymentApprovedEvent` | `transactionId`, `appointmentId`, `customerId`, `amount` |

#### Configuração de Conexão (application.yml de cada serviço)

Todos os serviços que usam RabbitMQ têm a mesma config:

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}   # No Docker: "rabbitmq" (nome do container)
    port: 5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
```

| Serviço | Tem config RabbitMQ? | Papel | RabbitConfig.java |
|---------|---------------------|-------|-------------------|
| **schedule-service** | ✅ | Producer | Declara exchange + `Jackson2JsonMessageConverter` |
| **payment-service** | ✅ | Producer | Declara exchange + converter + `RabbitTemplate` |
| **notification-service** | ✅ | Consumer | Declara exchange + 4 queues + 4 bindings + converter + `RabbitTemplate` |
| **barbershop-service** | ✅ | *(Nenhum ainda)* | Config existe no `application.yml` mas sem `RabbitConfig.java` |
| **product-service** | ✅ | *(Nenhum ainda)* | Config existe no `application.yml` mas sem `RabbitConfig.java` |
| **user-service** | ❌ | *(Nenhum)* | Sem config RabbitMQ |
| **api-gateway** | ❌ | *(N/A)* | Gateway não usa RabbitMQ |
| **discovery-service** | ❌ | *(N/A)* | Eureka não usa RabbitMQ |

#### Eventos FUTUROS (🔜 a implementar)

| Routing Key | Evento | Producer | Consumer | Propósito |
|---|---|---|---|---|
| `barbershop.join-request.created` | `JoinRequestCreatedEvent` | **barbershop-service** | **notification-service** | Notificar dono que barbeiro quer entrar |
| `barbershop.join-request.approved` | `JoinRequestApprovedEvent` | **barbershop-service** | **notification-service** | Notificar barbeiro que foi aprovado |
| `order.created` | `OrderCreatedEvent` | **product-service** | **notification-service** + **payment-service** | Notificar dono + criar pagamento para pedido |
| `product.low-stock` | `LowStockEvent` | **product-service** | **notification-service** | Alertar dono sobre estoque baixo |
| `user.deleted` | `UserDeletedEvent` | **user-service** | **schedule-service** + **barbershop-service** | Cancelar agendamentos futuros + remover vínculos |

---

### 2.2 Feign Clients — Comunicação Síncrona

```
┌──────────────────┐         GET /api/internal/users/{id}          ┌──────────────────┐
│                  │ ────────────────────────────────────────────► │                  │
│ barbershop-      │         GET /api/internal/users/by-email/{e}  │   user-service   │
│ service          │ ────────────────────────────────────────────► │                  │
│                  │         PUT /api/internal/users/{id}/barbershop│                 │
│ (UserService     │ ────────────────────────────────────────────► │                  │
│  Client)         │                                               │                  │
└──────────────────┘                                               └──────────────────┘

┌──────────────────┐         GET /api/internal/users/{id}          ┌──────────────────┐
│                  │ ────────────────────────────────────────────► │                  │
│ schedule-        │         GET /api/internal/users/by-email/{e}  │   user-service   │
│ service          │ ────────────────────────────────────────────► │                  │
│                  │                                               └──────────────────┘
│ (UserService     │
│  Client +        │         GET /api/internal/barbershops/{id}    ┌──────────────────┐
│  BarbershopSvc   │ ────────────────────────────────────────────► │                  │
│  Client)         │         GET /api/internal/barbershops/{id}/   │ barbershop-      │
│                  │              activities?ids=                   │ service          │
│                  │ ────────────────────────────────────────────► │                  │
└──────────────────┘                                               └──────────────────┘

┌──────────────────┐         GET /api/internal/appointments/{id}   ┌──────────────────┐
│                  │ ────────────────────────────────────────────► │                  │
│ payment-         │         PUT /api/internal/appointments/{id}/  │ schedule-        │
│ service          │              payment-status                   │ service          │
│                  │ ────────────────────────────────────────────► │                  │
│ (ScheduleSvc     │                                               │                  │
│  Client)         │                                               │                  │
└──────────────────┘                                               └──────────────────┘
```

#### Tabela Completa de Feign Clients

| Feign Client | Declarado em | Conecta a | Interface | Endpoints consumidos |
|---|---|---|---|---|
| `UserServiceClient` | **barbershop-service** | `user-service` | `@FeignClient(name="user-service", path="/api/internal/users")` | `GET /{id}`, `GET /by-email/{email}`, `PUT /{id}/barbershop` |
| `UserServiceClient` | **schedule-service** | `user-service` | `@FeignClient(name="user-service", path="/api/internal/users")` | `GET /{id}`, `GET /by-email/{email}` |
| `BarbershopServiceClient` | **schedule-service** | `barbershop-service` | `@FeignClient(name="barbershop-service", path="/api/internal/barbershops")` | `GET /{id}`, `GET /{shopId}/activities?ids=` |
| `ScheduleServiceClient` | **payment-service** | `schedule-service` | `@FeignClient(name="schedule-service")` | `GET /api/internal/appointments/{id}`, `PUT /api/internal/appointments/{id}/payment-status` |

> **Nota:** Todos os Feign Clients usam o **nome do serviço no Eureka** (`name="user-service"`), não IP. O Eureka resolve o IP automaticamente via `lb://`. ✅

---

### 2.3 API Gateway — Rotas

| ID da Rota | Path no Gateway | Serviço Destino | URI |
|---|---|---|---|
| `user-service` | `/api/customers/**`, `/api/barbers/**`, `/api/auth/**` | user-service | `lb://user-service` |
| `barbershop-service` | `/api/barbershops/**` | barbershop-service | `lb://barbershop-service` |
| `schedule-service` | `/api/appointments/**` | schedule-service | `lb://schedule-service` |
| `payment-service` | `/api/payments/**` | payment-service | `lb://payment-service` |
| `product-service` | `/api/products/**`, `/api/orders/**` | product-service | `lb://product-service` |
| `notification-service` | `/api/notifications/**` | notification-service | `lb://notification-service` |

> **Importante:** Os endpoints `/api/internal/**` **NÃO** são roteados pelo Gateway. Eles só são acessíveis dentro da rede Docker (entre containers).

---

### 2.4 Docker Compose — Containers e Portas

| Container | Imagem | Porta Host | Porta Interna | Depende de |
|---|---|---|---|---|
| `cortaai-mysql` | `mysql:8.0` | 3306 (local) / 3307 (server) | 3306 | — |
| `cortaai-rabbitmq` | `rabbitmq:3-management` | 5672+15672 (local) / 5673+15673 (server) | 5672+15672 | — |
| `cortaai-redis` | `redis:7-alpine` | 6379 (local) / 6380 (server) | 6379 | — |
| `discovery-service` | `eclipse-temurin:17-jdk` | 8761 | 8761 | — |
| `api-gateway` | `eclipse-temurin:17-jdk` | 8080 (local) / 8082 (server) | 8080 | discovery |
| `user-service` | `eclipse-temurin:17-jdk` | — | 8081 | db, discovery |
| `barbershop-service` | `eclipse-temurin:17-jdk` | — | 8082 | db, discovery, rabbitmq |
| `schedule-service` | `eclipse-temurin:17-jdk` | — | 8083 | db, discovery, rabbitmq, redis |
| `payment-service` | `eclipse-temurin:17-jdk` | — | 8084 | db, discovery, rabbitmq |
| `notification-service` | `eclipse-temurin:17-jdk` | — | 8085 | db, discovery, rabbitmq, redis |
| `product-service` | `eclipse-temurin:17-jdk` | — | 8086 | db, discovery, rabbitmq |
| `cortaai-frontend` | `node:18-alpine` | 5173 (server) | 5173 | — |

#### Variáveis de Ambiente (`.env`)

| Variável | Usado por | Descrição |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | db, todos os services | Senha do MySQL |
| `DB_USERNAME` | todos os services | Usuário do MySQL |
| `DB_PASSWORD` | todos os services | Senha do MySQL |
| `JWT_SECRET_KEY` | user-service, api-gateway | Chave secreta para assinar/validar JWT |
| `RABBITMQ_USER` | rabbitmq, schedule, payment, notification, barbershop, product | Usuário do RabbitMQ |
| `RABBITMQ_PASS` | rabbitmq, schedule, payment, notification, barbershop, product | Senha do RabbitMQ |
| `RABBITMQ_HOST` | schedule, payment, notification, barbershop, product | Host do RabbitMQ (`rabbitmq` no Docker) |
| `EUREKA_HOST` | todos os services | Host do Eureka (`discovery` no Docker) |
| `MP_ACCESS_TOKEN` | payment-service | Access token do Mercado Pago (sandbox) |
| `CLOUDINARY_CLOUD_NAME` | barbershop-service, user-service | Cloud name Cloudinary |
| `CLOUDINARY_API_KEY` | barbershop-service, user-service | API key Cloudinary |
| `CLOUDINARY_API_SECRET` | barbershop-service, user-service | API secret Cloudinary |
| `MAIL_HOST` | notification-service | Host SMTP |
| `MAIL_PORT` | notification-service | Porta SMTP |
| `MAIL_USERNAME` | notification-service | Email SMTP |
| `MAIL_PASSWORD` | notification-service | Senha SMTP |

---

## 3. Tarefas DEV 1

> **Serviços:** `user-service`, `barbershop-service`, `schedule-service`, `api-gateway`

### 3.1 barbershop-service — Campos e Models Faltantes

#### Tarefa 1.1 — Adicionar campos ao model `Barbershop`

**Arquivo:** `barbershop-service/src/main/java/.../model/Barbershop.java`

Campos a adicionar:

```java
@Column(length = 100)
private String city;

@Column(length = 2)
private String state;

@Column(name = "zip_code", length = 8)
private String zipCode;

@Column(precision = 10, scale = 8)
private BigDecimal latitude;

@Column(precision = 11, scale = 8)
private BigDecimal longitude;

@Column(length = 15)
private String phone;

@Column(columnDefinition = "TEXT")
private String description;

@Column(name = "is_active", nullable = false)
private boolean active = true;

@Column(name = "average_rating", precision = 2, scale = 1)
private BigDecimal averageRating = BigDecimal.ZERO;

@Column(name = "total_reviews")
private Integer totalReviews = 0;
```

- [ ] Adicionar os campos acima ao model
- [ ] Atualizar `BarbershopDTO`, `CreateBarbershopDTO`, `UpdateBarbershopDTO`
- [ ] Atualizar o `BarbershopService` para popular os novos campos
- [ ] Atualizar o `BarbershopInfoDTO` (internal) se necessário

#### Tarefa 1.2 — Adicionar campos ao model `Activity`

**Arquivo:** `barbershop-service/src/main/java/.../model/Activity.java`

```java
@Column(columnDefinition = "TEXT")
private String description;

@Column(length = 50)
private String category;  // CORTE, BARBA, COMBO, TRATAMENTO

@Column(name = "is_active", nullable = false)
private boolean active = true;
```

- [ ] Adicionar campos
- [ ] Atualizar DTOs (`ActivityDTO`, `CreateActivityDTO`, `UpdateActivityDTO`)
- [ ] Atualizar service

#### Tarefa 1.3 — Criar model `BarberActivity` (tabela pivô M:N) 🔴 CRÍTICO

**Novos arquivos a criar:**

```
barbershop-service/src/main/java/.../model/BarberActivity.java
barbershop-service/src/main/java/.../model/BarberActivityId.java  (chave composta)
barbershop-service/src/main/java/.../repository/BarberActivityRepository.java
barbershop-service/src/main/java/.../dto/BarberActivityDTO.java
```

Model:

```java
@Entity
@Table(name = "barber_activities")
@IdClass(BarberActivityId.class)
public class BarberActivity {
    @Id
    @Column(name = "barber_id", nullable = false, length = 36)
    private UUID barberId;  // UUID externo (user-service) — SEM @ManyToOne

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
}
```

- [ ] Criar model + IdClass
- [ ] Criar repository com queries: `findByBarberId(UUID)`, `findByActivityId(UUID)`
- [ ] Criar endpoints no `BarbershopController`:
  - `POST /api/barbershops/my-shop/barber-activities` — vincular barbeiro a serviço
  - `GET /api/barbershops/{shopId}/barber-activities/{barberId}` — listar serviços de um barbeiro
  - `DELETE /api/barbershops/my-shop/barber-activities/{barberId}/{activityId}` — desvincular
- [ ] Criar endpoint interno no `InternalBarbershopController`:
  - `GET /api/internal/barbershops/{shopId}/barber-activities/{barberId}` — retorna lista de UUIDs de atividades

#### Tarefa 1.4 — Criar model `BarbershopWorkingHours`

**Novos arquivos:**

```
barbershop-service/src/main/java/.../model/BarbershopWorkingHours.java
barbershop-service/src/main/java/.../repository/BarbershopWorkingHoursRepository.java
barbershop-service/src/main/java/.../dto/WorkingHoursDTO.java
```

- [ ] Criar model com: `barbershopId`, `dayOfWeek` (0-6), `openTime`, `closeTime`, `isClosed`
- [ ] Criar repository
- [ ] Criar endpoints no `BarbershopController`:
  - `GET /api/barbershops/{shopId}/working-hours`
  - `PUT /api/barbershops/my-shop/working-hours` — define/atualiza horários (lista de 7 dias)

#### Tarefa 1.5 — Criar model `Review`

**Novos arquivos:**

```
barbershop-service/src/main/java/.../model/Review.java
barbershop-service/src/main/java/.../repository/ReviewRepository.java
barbershop-service/src/main/java/.../dto/ReviewDTO.java
barbershop-service/src/main/java/.../dto/CreateReviewDTO.java
```

- [ ] Criar model com: `barbershopId`, `customerId` (UUID externo), `appointmentId` (UUID externo), `rating` (1-5), `comment`, `dateCreated`
- [ ] Criar repository
- [ ] Criar endpoints:
  - `POST /api/barbershops/{shopId}/reviews` — cliente cria review
  - `GET /api/barbershops/{shopId}/reviews` — listar reviews da barbearia
- [ ] Ao criar review, atualizar `averageRating` e `totalReviews` na tabela `barbershops`

#### Tarefa 1.6 — Publicar eventos RabbitMQ no barbershop-service

**Arquivo:** criar `barbershop-service/src/main/java/.../config/RabbitConfig.java`

Eventos a publicar:

| Ação | Routing Key | Evento |
|---|---|---|
| Barbeiro solicita entrada | `barbershop.join-request.created` | `JoinRequestCreatedEvent` |
| Dono aprova solicitação | `barbershop.join-request.approved` | `JoinRequestApprovedEvent` |

- [ ] Criar `RabbitConfig.java` com exchange e `RabbitTemplate`
- [ ] Criar DTOs dos eventos
- [ ] Publicar eventos no `BarbershopService` nos métodos correspondentes

---

### 3.2 schedule-service — Campos Faltantes

#### Tarefa 1.7 — Adicionar campos ao model `Appointment`

```java
@Column(name = "total_duration")
private Integer totalDuration;  // Duração total em minutos

@Column(name = "cancellation_reason", length = 255)
private String cancellationReason;

@Column(name = "payment_id", length = 36)
private UUID paymentId;  // Referência ao payment-service
```

- [ ] Adicionar campos ao model
- [ ] Calcular `totalDuration` automaticamente na criação (soma dos `durationMinutes` das atividades)
- [ ] Aceitar `cancellationReason` no `cancelAppointment()` (parâmetro opcional no body)
- [ ] Atualizar `AppointmentDTO` com novos campos

---

### 3.3 user-service — Segurança

#### Tarefa 1.8 — Proteger endpoints internos com `X-Internal-Token`

**Todos os serviços que têm `/api/internal/**`:**

- [ ] Criar um filter/interceptor que valida o header `X-Internal-Token` em requests para `/api/internal/**`
- [ ] Adicionar variável `INTERNAL_TOKEN` no `.env`
- [ ] Adicionar o header nos Feign Clients via `RequestInterceptor`:

```java
@Configuration
public class FeignConfig {
    @Value("${internal.token}")
    private String internalToken;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}
```

- [ ] Aplicar em: `user-service`, `barbershop-service`, `schedule-service`

---

### 3.4 api-gateway — JWT Centralizado

#### Tarefa 1.9 — Implementar validação JWT no Gateway

- [ ] Criar `JwtAuthenticationFilter` no api-gateway (Global Filter)
- [ ] O filter deve:
  1. Extrair o token do header `Authorization: Bearer <token>`
  2. Validar a assinatura JWT com `JWT_SECRET_KEY`
  3. Extrair `userId`, `email`, `role` do token
  4. Propagar como headers: `X-User-Id`, `X-User-Email`, `X-User-Role`
  5. Permitir sem auth: `/api/customers/register`, `/api/customers/login`, `/api/barbers/register`, `/api/barbers/login`, `/api/payments/webhook`, rotas de Swagger
- [ ] Remover `SecurityConfig` + `JwtAuthorizationFilter` do user-service (a validação agora é no Gateway)
- [ ] Nos microserviços downstream, trocar `Principal principal` por `@RequestHeader("X-User-Email") String email` ou `@RequestHeader("X-User-Id") UUID userId`

---

## 4. Tarefas DEV 2

> **Serviços:** `payment-service`, `notification-service`, `product-service`

### 4.1 payment-service — Campos e Integrações Faltantes

#### Tarefa 2.1 — Adicionar campos ao model `Transaction`

```java
@Column(name = "barbershop_id")
private UUID barbershopId;

@Column(name = "order_id")
private UUID orderId;  // Para compras de produtos (e-commerce)

@Column(name = "payment_method", length = 30)
private String paymentMethod;  // PIX, CREDIT_CARD, DEBIT

@Column(name = "platform_fee", precision = 10, scale = 2)
private BigDecimal platformFee = BigDecimal.ZERO;

@Column(name = "seller_amount", precision = 10, scale = 2)
private BigDecimal sellerAmount;

@Column(name = "mp_status", length = 50)
private String mpStatus;

@Column(name = "mp_status_detail", length = 100)
private String mpStatusDetail;

@Column(length = 3)
private String currency = "BRL";
```

- [ ] Adicionar campos ao model
- [ ] Atualizar `TransactionDTO` e `CreatePaymentDTO`
- [ ] Popular `barbershopId` buscando do agendamento via Feign
- [ ] Popular `platformFee` e `sellerAmount` no cálculo de split payment
- [ ] Salvar `mpStatus`/`mpStatusDetail` ao processar webhook

#### Tarefa 2.2 — Suportar pagamento de pedidos (e-commerce)

- [ ] Criar endpoint `POST /api/payments/create-order-payment` para gerar pagamento de pedido (não só de agendamento)
- [ ] Aceitar `orderId` em vez de `appointmentId`
- [ ] Buscar dados do pedido via Feign no product-service (criar `ProductServiceClient`)
- [ ] Criar `InternalOrderController` no product-service para expor `GET /api/internal/orders/{id}`

#### Tarefa 2.3 — Proteger endpoints internos

- [ ] Aplicar o mesmo padrão de `X-Internal-Token` nos Feign Clients do payment-service

---

### 4.2 notification-service — Canais Reais

#### Tarefa 2.4 — Implementar envio real de Email

- [ ] Configurar `spring-boot-starter-mail` (já no `application.yml`)
- [ ] Criar `EmailService` que envia email via SMTP
- [ ] Criar templates HTML com Thymeleaf para cada tipo de notificação
- [ ] No `NotificationService`, ao criar notificação, também enviar email se o canal incluir `EMAIL`

#### Tarefa 2.5 — Adicionar consumers para eventos de barbershop

Quando DEV 1 implementar os eventos de `barbershop-service`, adicionar:

- [ ] Nova queue: `notification.joinrequest.created`
- [ ] Nova queue: `notification.joinrequest.approved`
- [ ] Binding com routing keys: `barbershop.join-request.created`, `barbershop.join-request.approved`
- [ ] Listeners no `NotificationEventListener`:
  - `onJoinRequestCreated()` — notificar dono
  - `onJoinRequestApproved()` — notificar barbeiro

#### Tarefa 2.6 — Implementar scheduler de lembretes

- [ ] Criar `ReminderScheduler` com `@Scheduled(fixedDelay = 60000)` (a cada 1 minuto)
- [ ] Buscar agendamentos nas próximas 1h via Feign no schedule-service
- [ ] Criar endpoint interno no schedule-service: `GET /api/internal/appointments/upcoming?minutesBefore=60`
- [ ] Enviar notificação + email de lembrete
- [ ] Usar Redis para deduplicação (evitar enviar 2x)

---

### 4.3 product-service — Campos e Integrações Faltantes

#### Tarefa 2.7 — Adicionar campos ao model `Product`

```java
@Column(length = 50)
private String sku;

@Column(name = "min_stock")
private Integer minStock = 5;

@Column(name = "image_public_id")
private String imagePublicId;
```

- [ ] Adicionar campos
- [ ] Atualizar DTOs
- [ ] Ao criar venda (`OrderService.createOrder()`), verificar se estoque ficou abaixo de `minStock` e publicar evento `product.low-stock`

#### Tarefa 2.8 — Publicar eventos RabbitMQ no product-service

- [ ] Criar `RabbitConfig.java` com exchange e `RabbitTemplate`
- [ ] Eventos a publicar:

| Ação | Routing Key | Evento |
|---|---|---|
| Pedido criado | `order.created` | `OrderCreatedEvent` |
| Estoque baixo | `product.low-stock` | `LowStockEvent` |

- [ ] Criar DTOs dos eventos
- [ ] Publicar no `OrderService.createOrder()` e `ProductService` (check de estoque)

#### Tarefa 2.9 — Criar endpoint interno para pedidos

- [ ] `GET /api/internal/orders/{id}` — para o payment-service buscar dados do pedido
- [ ] Proteger com `X-Internal-Token`

---

## 5. Tarefas COMPARTILHADAS — Infraestrutura

| # | Tarefa | Responsável | Detalhe |
|---|--------|-------------|---------|
| 5.1 | Adicionar `INTERNAL_TOKEN` ao `.env.example` | DEV 1 | `INTERNAL_TOKEN=chave-secreta-interna-entre-servicos` |
| 5.2 | Garantir que o Gateway **NÃO roteia** `/api/internal/**` | DEV 1 | Já está correto (não há rota para `internal`), mas adicionar filtro explícito de bloqueio |
| 5.3 | Adicionar novas queues ao `notification-service` `RabbitConfig.java` | DEV 2 | Queues de `joinrequest` e `order` |
| 5.4 | Adicionar Resilience4j aos Feign Clients | DEV 1 + DEV 2 | Cada um nos seus serviços. Config básica de CircuitBreaker + Retry |
| 5.5 | Atualizar `init.sql` com tabelas novas | DEV 1 + DEV 2 | Adicionar: `barber_activities`, `barbershop_working_hours`, `reviews`, `refresh_tokens` |
| 5.6 | Atualizar `docker-compose.yml` se novos containers/volumes | DEV 2 | Se necessário |
| 5.7 | Testar todo o fluxo end-to-end | DEV 1 + DEV 2 juntos | Após ambos finalizarem |

---

## 6. Cronograma Sugerido

```
Semana │ DEV 1                                        │ DEV 2
───────┼──────────────────────────────────────────────┼──────────────────────────────────────────────
 S1    │ 1.1 Campos Barbershop                        │ 2.1 Campos Transaction
       │ 1.2 Campos Activity                          │ 2.7 Campos Product
       │ 1.3 BarberActivity (model+repo+endpoints)    │ 2.3 X-Internal-Token no payment-service
       │                                              │
───────┼──────────────────────────────────────────────┼──────────────────────────────────────────────
 S2    │ 1.4 BarbershopWorkingHours                   │ 2.4 Envio real de Email
       │ 1.5 Review (model+endpoints)                 │ 2.5 Consumers de joinrequest/order
       │ 1.6 Eventos RabbitMQ barbershop-service      │ 2.8 Eventos RabbitMQ product-service
       │ 1.7 Campos Appointment                       │ 2.9 Endpoint interno orders
       │                                              │
───────┼──────────────────────────────────────────────┼──────────────────────────────────────────────
 S3    │ 1.8 X-Internal-Token (user, barbershop,      │ 2.2 Pagamento de pedidos (e-commerce)
       │     schedule)                                │ 2.6 Scheduler de lembretes
       │ 1.9 JWT centralizado no Gateway              │
       │                                              │
───────┼──────────────────────────────────────────────┼──────────────────────────────────────────────
 S4    │ 5.4 Resilience4j (seus serviços)             │ 5.4 Resilience4j (seus serviços)
       │ 5.5 init.sql (suas tabelas)                  │ 5.5 init.sql (suas tabelas)
       │ 5.7 Teste E2E integrado                      │ 5.7 Teste E2E integrado
```

---

## Checklist Final de Merge

Antes de fazer merge na branch principal:

- [ ] Todos os endpoints do relatório marcados como 🔜 estão ✅
- [ ] `BarberActivity` M:N funcionando (criação de agendamento valida skills do barbeiro)
- [ ] Endpoints internos protegidos por `X-Internal-Token`
- [ ] JWT validado centralmente no Gateway (não mais no user-service)
- [ ] Eventos de `barbershop-service` fluindo para `notification-service`
- [ ] Eventos de `product-service` fluindo para `notification-service`
- [ ] Email sendo enviado de verdade (pelo menos em sandbox/mailtrap)
- [ ] Transaction com todos os campos para split payment
- [ ] Product com `sku`, `minStock`
- [ ] `docker compose up` sobe tudo sem erros
- [ ] Swagger UI mostra todos os 6 serviços com endpoints atualizados
- [ ] Testes manuais do fluxo: registro → login → criar barbearia → vincular barbeiro → agendar → pagar → notificar

---

> **Este planejamento reflete o estado do código em 01/03/2026.**  
> Atualize os checkboxes conforme as tarefas forem concluídas.
