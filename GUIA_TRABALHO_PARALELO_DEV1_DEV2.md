# 🔀 GUIA DE TRABALHO PARALELO — DEV 1 & DEV 2

**Data:** 01/03/2026  
**Contexto:** Etapas 0, 1 e 2 concluídas (infra, user-service, barbershop-service)  
**Objetivo:** Completar as etapas 3–7 com dois devs trabalhando **simultaneamente em PCs distintos**, sem conflitos no merge final.

---

## 📐 ESTRATÉGIA ANTI-CONFLITO

### Princípio: Cada dev trabalha em diretórios 100% separados

```
DEV 1 — "Backend Core"              DEV 2 — "Serviços de Apoio + Frontend"
──────────────────────               ──────────────────────────────────────
├── schedule-service/  ← EXCLUSIVO   ├── notification-service/  ← EXCLUSIVO
│                                     ├── payment-service/       ← EXCLUSIVO
│                                     ├── product-service/       ← EXCLUSIVO
│                                     ├── frontend/              ← EXCLUSIVO
│                                     │
├── barbershop-service/ ← MÍNIMO*    │
│   └── controller/InternalBbCtrl*    │
│                                     │
│                                     │
└── PROGRESSO_MIGRACAO.md (update)    └── PROGRESSO_MIGRACAO.md (NÃO toque)
```

> **\*MÍNIMO:** Dev 1 APENAS cria o `InternalBarbershopController.java` no barbershop-service (arquivo NOVO, sem editar nada existente). Zero risco de conflito.

### Arquivos compartilhados — REGRAS RÍGIDAS

| Arquivo | Quem edita | Regra |
|---------|-----------|-------|
| `docker-compose.yml` | **NINGUÉM** | Já está pronto. Se precisar, combinar no chat. |
| `backend/pom.xml` (pai) | **NINGUÉM** | Já tem todos os modules. |
| `init.sql` | **NINGUÉM** | Já tem os 5 schemas. |
| `api-gateway/application.yml` | **DEV 2** (se precisar add rota de payments/products) |
| `PROGRESSO_MIGRACAO.md` | **DEV 1** atualiza só a parte dele. DEV 2 atualiza no merge. |

### Convenção de branches

```bash
# Dev 1
git checkout -b feat/schedule-service

# Dev 2
git checkout -b feat/support-services-frontend

# No final:
# 1. Dev 1 faz PR e merge na main primeiro (muda diretórios diferentes)
# 2. Dev 2 faz rebase na main e PR (sem conflitos pois não tocou nos mesmos arquivos)
```

---

## 🗓️ TIMELINE PARALELA

```
Semana   │  DEV 1 (Backend Core)               │  DEV 2 (Apoio + Frontend)
─────────┼──────────────────────────────────────┼───────────────────────────────────────
 S1      │ schedule-service: Models, Repos,     │ notification-service: Completo
         │ Feign Clients, Redis Config           │ (RabbitMQ listeners, templates email)
─────────┼──────────────────────────────────────┼───────────────────────────────────────
 S2      │ schedule-service: Service Impl       │ payment-service: Models, Checkout Pro
         │ (lógica core agendamento)             │ Webhook, Feign Client
─────────┼──────────────────────────────────────┼───────────────────────────────────────
 S3      │ schedule-service: Controller,        │ product-service: CRUD + Estoque
         │ InternalBarbershopController,         │ + Frontend: adaptar services/
         │ Testes                                │ + Frontend: novas telas
─────────┼──────────────────────────────────────┼───────────────────────────────────────
 S4      │         ████ MERGE + TESTE INTEGRADO + AJUSTES FINAIS ████
─────────┼──────────────────────────────────────┼───────────────────────────────────────
```

---

## 👨‍💻 DEV 1 — schedule-service + InternalBarbershopController

### ESCOPO TOTAL
- **Diretório principal:** `backend/schedule-service/` (EXCLUSIVO)
- **Arquivo novo no barbershop-service:** `InternalBarbershopController.java` + `BarbershopInfoDTO.java` + `ActivityInfoDTO.java` (arquivos NOVOS, sem editar existentes)
- **NÃO TOQUE:** `frontend/`, `notification-service/`, `payment-service/`, `product-service/`

---

### SEMANA 1 — Fundação do schedule-service

#### D1-S1.1 — Criar enums
**Arquivo:** `schedule-service/.../model/enums/AppointmentStatus.java`
```java
public enum AppointmentStatus {
    SCHEDULED, CONFIRMED, IN_PROGRESS, CONCLUDED, CANCELLED, NO_SHOW
}
```

#### D1-S1.2 — Criar entidades JPA

**Arquivo:** `schedule-service/.../model/Appointment.java`
```
Campos:
- UUID id (@UuidGenerator)
- UUID customerId        (desacoplado — vive no user-service)
- UUID barberId          (desacoplado)
- UUID barbershopId      (desacoplado — vive no barbershop-service)
- String customerName    (snapshot — desnormalizado)
- String barberName      (snapshot)
- String barbershopName  (snapshot)
- LocalDateTime startTime
- LocalDateTime endTime
- BigDecimal totalPrice  (soma das atividades)
- AppointmentStatus status
- OffsetDateTime dateCreated (@CreatedDate)
- OffsetDateTime lastUpdated (@LastModifiedDate)
- Set<AppointmentActivity> activities (@OneToMany cascade ALL)
```

**Arquivo:** `schedule-service/.../model/AppointmentActivity.java`
```
Campos:
- UUID id
- UUID activityId        (referência externa)
- String activityName    (snapshot)
- BigDecimal price       (snapshot)
- Integer durationMinutes (snapshot)
- Appointment appointment (@ManyToOne)
```

**Arquivo:** `schedule-service/.../model/BarberBlock.java`
```
Campos:
- UUID id
- UUID barberId
- LocalDateTime startTime
- LocalDateTime endTime
- String reason
- OffsetDateTime dateCreated
```

#### D1-S1.3 — Criar DTOs
**Diretório:** `schedule-service/.../dto/`

| DTO | Uso |
|-----|-----|
| `CreateAppointmentDTO` | Request: customerId, barberId, barbershopId, activityIds, startTime |
| `AppointmentDTO` | Response completo |
| `AppointmentActivityDTO` | Atividade dentro do agendamento |
| `TimeSlotDTO` | Slot de disponibilidade (startTime, endTime, available) |
| `CreateBarberBlockDTO` | Criar bloqueio de agenda |
| `BarberBlockDTO` | Response de bloqueio |
| `UserInfoDTO` | Recebido via Feign (user-service) |
| `BarbershopInfoDTO` | Recebido via Feign (barbershop-service) |
| `ActivityInfoDTO` | Recebido via Feign (barbershop-service) |

#### D1-S1.4 — Criar repositórios
**Diretório:** `schedule-service/.../repository/`

**`AppointmentRepository.java`**
```java
@Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.barberId = :barberId " +
       "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
       "AND a.startTime < :endTime AND a.endTime > :startTime")
boolean hasConflict(@Param("barberId") UUID barberId,
                    @Param("startTime") LocalDateTime startTime,
                    @Param("endTime") LocalDateTime endTime);

List<Appointment> findByCustomerIdOrderByStartTimeDesc(UUID customerId);
List<Appointment> findByBarberIdAndStartTimeBetween(UUID barberId, LocalDateTime start, LocalDateTime end);
List<Appointment> findByBarbershopIdAndStartTimeBetween(UUID shopId, LocalDateTime start, LocalDateTime end);
```

**`AppointmentActivityRepository.java`** — JpaRepository simples

**`BarberBlockRepository.java`**
```java
List<BarberBlock> findByBarberIdAndStartTimeBetween(UUID barberId, LocalDateTime start, LocalDateTime end);
boolean existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(UUID barberId, LocalDateTime end, LocalDateTime start);
```

#### D1-S1.5 — Criar Feign Clients
**Diretório:** `schedule-service/.../feign/`

**`UserServiceClient.java`**
```java
@FeignClient(name = "user-service", path = "/api/internal/users")
public interface UserServiceClient {
    @GetMapping("/{id}")
    UserInfoDTO getUserById(@PathVariable("id") UUID id);

    @GetMapping("/by-email/{email}")
    UserInfoDTO getUserByEmail(@PathVariable("email") String email);
}
```

**`BarbershopServiceClient.java`**
```java
@FeignClient(name = "barbershop-service", path = "/api/internal/barbershops")
public interface BarbershopServiceClient {
    @GetMapping("/{id}")
    BarbershopInfoDTO getBarbershopById(@PathVariable("id") UUID id);

    @GetMapping("/{shopId}/activities")
    List<ActivityInfoDTO> getActivitiesByIds(@PathVariable("shopId") UUID shopId,
                                             @RequestParam("ids") List<UUID> ids);
}
```

#### D1-S1.6 — Criar configs
**`RedisConfig.java`** — Serializers + CacheManager (TTL 5min)  
**`RabbitConfig.java`** — Exchange `cortaai.events`, queues para appointment events  
**`ScheduleServiceApplication.java`** — adicionar `@EnableJpaAuditing`, `@EnableFeignClients`, `@EnableCaching`

#### D1-S1.7 — Criar InternalBarbershopController (NO BARBERSHOP-SERVICE!)

> ⚠️ Este é o ÚNICO arquivo que o Dev 1 cria FORA do schedule-service.  
> São 3 arquivos NOVOS no barbershop-service — não edita nenhum existente.

**Arquivo NOVO:** `barbershop-service/.../controller/InternalBarbershopController.java`
```
GET /api/internal/barbershops/{id}             → BarbershopInfoDTO
GET /api/internal/barbershops/{shopId}/activities?ids=... → List<ActivityInfoDTO>
```

**Arquivo NOVO:** `barbershop-service/.../dto/BarbershopInfoDTO.java`
```java
public record BarbershopInfoDTO(UUID id, UUID ownerId, String name, String cnpj, String address) {}
```

**Arquivo NOVO:** `barbershop-service/.../dto/ActivityInfoDTO.java`
```java
public record ActivityInfoDTO(UUID id, String activityName, BigDecimal price, Integer durationMinutes, UUID barbershopId) {}
```

**✅ CHECKPOINT S1:** `./mvnw compile -pl schedule-service` sem erros. Feign clients declarados. Redis config pronto.

---

### SEMANA 2 — Lógica Core de Agendamento

#### D1-S2.1 — Criar Mapper
**`AppointmentMapper.java`** (MapStruct)

#### D1-S2.2 — Implementar AppointmentService

**Arquivo:** `schedule-service/.../service/AppointmentService.java`

**Método `createAppointment(String callerEmail, CreateAppointmentDTO dto)`:**
```
1. Feign → user-service: validar customerId (CUSTOMER) e barberId (BARBER)
2. Feign → barbershop-service: validar barbershopId + buscar activities por IDs
3. Calcular totalDuration = soma(durationMinutes de cada activity)
4. Calcular endTime = startTime + totalDuration
5. Verificar conflito de horário (AppointmentRepository.hasConflict)
6. Verificar BarberBlock (barbeiro em folga?)
7. Criar Appointment com dados desnormalizados (nomes copiados)
8. Criar AppointmentActivities (snapshots)
9. Publicar AppointmentCreatedEvent no RabbitMQ
10. Retornar AppointmentDTO
```

**Método `cancelAppointment(String callerEmail, UUID appointmentId)`:**
```
1. Buscar appointment
2. Verificar que caller é o customer OU o barber OU o owner
3. Alterar status → CANCELLED
4. Publicar AppointmentCancelledEvent
```

**Método `concludeAppointment(String callerEmail, UUID appointmentId)`:**
```
1. Buscar appointment
2. Verificar que caller é o barber
3. Alterar status → CONCLUDED
4. Publicar AppointmentConcludedEvent
```

**Método `getAvailability(UUID barberId, LocalDate date)`:**
```
1. Feign → user-service: buscar workStartTime, workEndTime do barber (cache Redis 5min)
2. Buscar agendamentos do barber naquele dia
3. Buscar bloqueios do barber naquele dia
4. Calcular slots de 30min entre workStart e workEnd
5. Marcar slots ocupados
6. Retornar List<TimeSlotDTO>
```

**Métodos de listagem:**
- `getMyAppointments(String email)` → por customer
- `getBarberSchedule(UUID barberId, LocalDate date)` → agenda do dia
- `getBarbershopSchedule(UUID shopId, LocalDate date)` → agenda da loja

#### D1-S2.3 — Implementar BarberBlockService
- CRUD de bloqueios de agenda (férias, folgas)
- Verificar que barberId pertence ao caller

#### D1-S2.4 — Publicação de eventos RabbitMQ

**Eventos publicados:**
```java
// exchange = "cortaai.events"
public record AppointmentCreatedEvent(UUID appointmentId, UUID customerId, UUID barberId, 
    UUID barbershopId, String customerName, String barberName, String barbershopName,
    LocalDateTime startTime, BigDecimal totalPrice) {}

public record AppointmentCancelledEvent(UUID appointmentId, UUID customerId, UUID barberId,
    String cancelledBy) {}

public record AppointmentConcludedEvent(UUID appointmentId, UUID customerId, UUID barberId,
    UUID barbershopId) {}
```

**✅ CHECKPOINT S2:** Lógica de agendamento compila. Validação de conflito funciona. Eventos são publicados.

---

### SEMANA 3 — Controller, Testes e Integração

#### D1-S3.1 — Criar AppointmentController

```
POST   /api/appointments                              → Criar agendamento
GET    /api/appointments/{id}                          → Detalhar
GET    /api/appointments/my-appointments               → Meus agendamentos (customer)
GET    /api/appointments/barber/{barberId}?date=...    → Agenda do barbeiro
GET    /api/appointments/barbershop/{shopId}?date=...  → Agenda da loja
PUT    /api/appointments/{id}/cancel                   → Cancelar
PUT    /api/appointments/{id}/conclude                 → Concluir
PUT    /api/appointments/{id}/confirm                  → Confirmar
GET    /api/appointments/availability?barberId=&date=  → Slots disponíveis
```

#### D1-S3.2 — Criar BarberBlockController
```
POST   /api/appointments/barber-blocks                 → Criar bloqueio
GET    /api/appointments/barber-blocks?barberId=&date= → Listar bloqueios
DELETE /api/appointments/barber-blocks/{id}             → Remover bloqueio
```

#### D1-S3.3 — Criar endpoint interno (para payment-service)
**Arquivo NOVO:** `schedule-service/.../controller/InternalAppointmentController.java`
```
GET /api/internal/appointments/{id}  → AppointmentDTO (para payment-service buscar dados)
PUT /api/internal/appointments/{id}/payment-status → void (payment-service atualiza status)
```

#### D1-S3.4 — Testes unitários
- [ ] Testar criação de agendamento com Feign mockado
- [ ] Testar detecção de conflito de horário
- [ ] Testar cálculo de disponibilidade
- [ ] Testar BarberBlock bloqueando agendamento
- [ ] Testar cancelamento (permissões)

#### D1-S3.5 — Teste de integração manual
- [ ] Subir: MySQL → RabbitMQ → Redis → Discovery → Gateway → user-service → barbershop-service → schedule-service
- [ ] Fluxo: cadastrar → criar barbearia → criar atividade → agendar → verificar dados desnormalizados → cancelar → verificar evento no RabbitMQ

**✅ CHECKPOINT FINAL DEV 1:** schedule-service completo e testado. InternalBarbershopController criado. Eventos no RabbitMQ fluindo.

---

## 👩‍💻 DEV 2 — notification-service + payment-service + product-service + Frontend

### ESCOPO TOTAL
- **Diretórios EXCLUSIVOS:** `notification-service/`, `payment-service/`, `product-service/`, `frontend/`
- **Pode editar:** `api-gateway/application.yml` (adicionar rotas de payments e products)
- **NÃO TOQUE:** `user-service/`, `barbershop-service/`, `schedule-service/`

---

### SEMANA 1 — notification-service (Completo)

#### D2-S1.1 — Configurar Application
**`NotificationServiceApplication.java`** — `@EnableScheduling` (para retry de notificações falhas)

#### D2-S1.2 — Criar models (Redis ou JPA simples)

**`Notification.java`**
```
- UUID id
- UUID userId
- String type (APPOINTMENT_CREATED, APPOINTMENT_CANCELLED, PAYMENT_APPROVED, etc.)
- String title
- String message
- String channel (EMAIL, PUSH, IN_APP)
- boolean read
- OffsetDateTime createdAt
```

#### D2-S1.3 — Criar RabbitMQ Config + Listeners

**`RabbitConfig.java`** — Declarar queues:
```
cortaai.events.appointment.created
cortaai.events.appointment.cancelled
cortaai.events.appointment.concluded
cortaai.events.payment.approved
cortaai.events.barber.approved
```

**`AppointmentEventListener.java`** — `@RabbitListener`:
```java
@RabbitListener(queues = "cortaai.events.appointment.created")
public void onAppointmentCreated(AppointmentCreatedEvent event) {
    // 1. Criar notificação para customer: "Agendamento confirmado com {barberName}"
    // 2. Criar notificação para barber: "Novo agendamento de {customerName}"
    // 3. Enviar email (se configurado)
}
```

**`PaymentEventListener.java`** — `@RabbitListener`

#### D2-S1.4 — Implementar canais de envio

**`EmailService.java`** — Integração com Resend API (REST):
```
POST https://api.resend.com/emails
Headers: Authorization: Bearer {RESEND_API_KEY}
Body: { from, to, subject, html }
```

**`NotificationService.java`** — Lógica de orquestração (decide canal, formata mensagem)

#### D2-S1.5 — Criar NotificationController
```
GET /api/notifications/my-notifications         → List<NotificationDTO> (do user logado)
PUT /api/notifications/{id}/read                → Marcar como lida
GET /api/notifications/unread-count             → Integer
```

#### D2-S1.6 — Deduplicação com Redis
- Chave: `notification:{eventType}:{entityId}`
- TTL: 24h
- Antes de enviar → verificar se chave existe

**✅ CHECKPOINT S1:** notification-service escuta RabbitMQ, cria notificações, envia email via Resend.

---

### SEMANA 2 — payment-service

#### D2-S2.1 — Criar entidades

**`Transaction.java`**
```
- UUID id
- UUID appointmentId (referência ao schedule-service)
- UUID customerId
- UUID barbershopId
- BigDecimal amount
- String paymentMethod (PIX, CREDIT_CARD, DEBIT_CARD)
- PaymentStatus status (PENDING, APPROVED, REJECTED, REFUNDED)
- String mpPaymentId (ID do Mercado Pago)
- String mpPreferenceId
- String checkoutUrl (URL de redirect)
- OffsetDateTime dateCreated
- OffsetDateTime lastUpdated
```

**`WebhookLog.java`** — Para idempotência
```
- Long id
- String mpPaymentId
- String eventType
- String payload (JSON raw)
- boolean processed
- OffsetDateTime receivedAt
```

#### D2-S2.2 — Criar Feign Client

**`ScheduleServiceClient.java`**
```java
@FeignClient(name = "schedule-service", path = "/api/internal/appointments")
public interface ScheduleServiceClient {
    @GetMapping("/{id}")
    AppointmentInfoDTO getAppointmentById(@PathVariable("id") UUID id);

    @PutMapping("/{id}/payment-status")
    void updatePaymentStatus(@PathVariable("id") UUID id, @RequestBody String status);
}
```

#### D2-S2.3 — Implementar PaymentService

**Método `createPayment(CreatePaymentDTO dto)`:**
```
1. Feign → schedule-service: buscar appointment (para obter preço, nomes)
2. Montar PreferenceRequest do Mercado Pago
3. Chamar API MP → obter init_point (URL checkout)
4. Salvar Transaction (status PENDING)
5. Retornar { checkoutUrl, transactionId }
```

**Método `processWebhook(WebhookPayload payload)`:**
```
1. Verificar se já processou (WebhookLog idempotência)
2. Salvar log
3. Buscar payment no MP por ID
4. Atualizar Transaction
5. Se APPROVED:
   a. Feign → schedule-service: atualizar appointment status
   b. RabbitMQ → publicar PaymentApprovedEvent
6. Marcar log como processed
```

#### D2-S2.4 — Criar controllers

**`PaymentController.java`**
```
POST /api/payments/create               → { checkoutUrl, transactionId }
GET  /api/payments/{id}                  → TransactionDTO
GET  /api/payments/my-payments           → List<TransactionDTO>
```

**`WebhookController.java`**
```
POST /api/payments/webhook               → 200 (Mercado Pago callback)
```

#### D2-S2.5 — Adicionar rota no Gateway
**Arquivo:** `api-gateway/application.yml` (ÚNICO arquivo compartilhado que Dev 2 edita)
```yaml
# Adicionar ABAIXO das rotas existentes:
- id: payment-service
  uri: lb://payment-service
  predicates:
    - Path=/api/payments/**

- id: product-service
  uri: lb://product-service
  predicates:
    - Path=/api/products/**, /api/orders/**
```

**✅ CHECKPOINT S2:** payment-service cria checkout, recebe webhook, atualiza appointment via Feign.

---

### SEMANA 3 — product-service + Frontend

#### D2-S3.1 — product-service: Models

**`Product.java`**
```
- UUID id
- UUID barbershopId
- String name
- String description
- BigDecimal price
- Integer stockQuantity
- Integer minStock (para alertas)
- String imageUrl
- String imageUrlPublicId
- String category (SHAMPOO, POMADA, CERA, OUTRO)
- boolean active
```

**`Order.java`**
```
- UUID id
- UUID customerId
- UUID barbershopId
- OrderStatus status (PENDING, PAID, PREPARING, READY, DELIVERED, CANCELLED)
- BigDecimal total
- OffsetDateTime dateCreated
- Set<OrderItem> items
```

**`OrderItem.java`**
```
- UUID id
- UUID productId (referência)
- String productName (snapshot)
- BigDecimal unitPrice (snapshot)
- Integer quantity
- Order order (@ManyToOne)
```

**`StockMovement.java`**
```
- UUID id
- UUID productId
- String type (IN, OUT, ADJUSTMENT)
- Integer quantity
- String reason
- OffsetDateTime createdAt
```

#### D2-S3.2 — product-service: Service + Controller

**`ProductController.java`**
```
POST   /api/products                            → Criar produto (dono)
GET    /api/products?barbershopId=              → Listar por barbearia (público)
GET    /api/products/{id}                       → Detalhar
PUT    /api/products/{id}                       → Atualizar
DELETE /api/products/{id}                       → Desativar

POST   /api/orders                              → Criar pedido (customer)
GET    /api/orders/my-orders                    → Meus pedidos
GET    /api/orders/shop-orders?barbershopId=    → Pedidos da loja (dono)
PUT    /api/orders/{id}/status                  → Atualizar status (dono)
```

#### D2-S3.3 — Frontend: adaptar services

**Arquivo:** `frontend/src/services/appointmentService.js`
- Verificar que endpoints apontam para `/api/appointments/*`

**Arquivo:** `frontend/src/services/barbershopService.js`
- Verificar que endpoints apontam para `/api/barbershops/*`

**Arquivo NOVO:** `frontend/src/services/paymentService.js`
```javascript
import api from './api';
export const createPayment = (appointmentId) => api.post('/payments/create', { appointmentId });
export const getMyPayments = () => api.get('/payments/my-payments');
```

**Arquivo NOVO:** `frontend/src/services/notificationService.js`
```javascript
import api from './api';
export const getMyNotifications = () => api.get('/notifications/my-notifications');
export const markAsRead = (id) => api.put(`/notifications/${id}/read`);
export const getUnreadCount = () => api.get('/notifications/unread-count');
```

**Arquivo NOVO:** `frontend/src/services/productService.js`
```javascript
import api from './api';
export const getProductsByShop = (shopId) => api.get(`/products?barbershopId=${shopId}`);
export const createOrder = (order) => api.post('/orders', order);
export const getMyOrders = () => api.get('/orders/my-orders');
```

#### D2-S3.4 — Frontend: novas páginas/componentes

**Novos componentes (diretório `frontend/src/components/`):**
- `NotificationBell/` — Ícone com badge de contagem + dropdown
- `PaymentCheckout/` — Botão "Pagar" que redireciona ao MP
- `ProductList/` — Grid de produtos da barbearia
- `Cart/` — Carrinho com Context API

**Novas páginas (diretório `frontend/src/pages/`):**
- `Notifications.jsx`
- `PaymentSuccess.jsx` / `PaymentFailure.jsx`
- `Products.jsx`
- `Cart.jsx`

**✅ CHECKPOINT FINAL DEV 2:** notification, payment e product services completos. Frontend com novos services e páginas.

---

## 🤝 SEMANA 4 — MERGE E INTEGRAÇÃO

### Passo 1: Dev 1 faz merge primeiro
```bash
# Dev 1
git checkout main
git pull origin main
git merge feat/schedule-service
git push origin main
```

### Passo 2: Dev 2 faz rebase e merge
```bash
# Dev 2
git checkout feat/support-services-frontend
git fetch origin main
git rebase origin/main
# Único possível conflito: api-gateway/application.yml (resolver manualmente se houver)
git checkout main
git merge feat/support-services-frontend
git push origin main
```

### Passo 3: Teste integrado conjunto
```bash
# Subir tudo
docker compose up -d
cd backend && ./mvnw spring-boot:run -pl discovery-service &
./mvnw spring-boot:run -pl api-gateway &
./mvnw spring-boot:run -pl user-service &
./mvnw spring-boot:run -pl barbershop-service &
./mvnw spring-boot:run -pl schedule-service &          # Dev 1
./mvnw spring-boot:run -pl notification-service &       # Dev 2
./mvnw spring-boot:run -pl payment-service &            # Dev 2
./mvnw spring-boot:run -pl product-service &            # Dev 2
cd ../frontend && npm run dev &
```

### Passo 4: Checklist de validação cruzada

| # | Teste | Dev responsável |
|---|-------|----------------|
| 1 | Cadastrar customer + barber + barbearia + atividade | Ambos |
| 2 | Agendar → notification-service recebe evento | Dev 1 cria, Dev 2 verifica notificação |
| 3 | Pagar agendamento → webhook → appointment atualizado | Dev 2 cria payment, Dev 1 verifica appointment |
| 4 | Cancelar → notificação enviada | Dev 1 cancela, Dev 2 verifica |
| 5 | Criar produto + pedir + pagar | Dev 2 fluxo completo |
| 6 | Frontend: login → buscar barbearia → agendar → pagar | Ambos |
| 7 | Eureka Dashboard: 7 serviços registrados | Ambos verificam |
| 8 | RabbitMQ Management: mensagens fluindo | Ambos verificam |

---

## 📁 MAPA DE ARQUIVOS — QUEM TOCA NO QUÊ

```
Arquitetura_completa/
├── backend/
│   ├── pom.xml                              ← NINGUÉM edita
│   ├── docker-compose.yml                   ← NINGUÉM edita
│   ├── init.sql                             ← NINGUÉM edita
│   │
│   ├── discovery-service/                   ← NINGUÉM edita
│   ├── api-gateway/
│   │   └── application.yml                  ← DEV 2 (add rotas payment/product)
│   │
│   ├── user-service/                        ← NINGUÉM edita (já migrado)
│   │
│   ├── barbershop-service/
│   │   ├── (tudo existente)                 ← NINGUÉM edita
│   │   ├── controller/
│   │   │   └── InternalBarbershopCtrl.java  ← DEV 1 (arquivo NOVO)
│   │   └── dto/
│   │       ├── BarbershopInfoDTO.java       ← DEV 1 (arquivo NOVO)
│   │       └── ActivityInfoDTO.java         ← DEV 1 (arquivo NOVO)
│   │
│   ├── schedule-service/                    ← DEV 1 (EXCLUSIVO)
│   │   └── (TUDO)
│   │
│   ├── notification-service/                ← DEV 2 (EXCLUSIVO)
│   │   └── (TUDO)
│   │
│   ├── payment-service/                     ← DEV 2 (EXCLUSIVO)
│   │   └── (TUDO)
│   │
│   └── product-service/                     ← DEV 2 (EXCLUSIVO)
│       └── (TUDO)
│
├── frontend/                                ← DEV 2 (EXCLUSIVO)
│   └── (TUDO)
│
└── PROGRESSO_MIGRACAO.md                    ← DEV 1 atualiza. DEV 2 no merge.
```

---

## 📋 CONTRATO DE INTERFACE (ACORDO ENTRE DEVS)

Os dois devs precisam combinar ANTES de começar quais DTOs e endpoints internos serão usados. Este é o contrato:

### Eventos RabbitMQ (Dev 1 publica → Dev 2 consome)

| Evento | Exchange | Routing Key | Payload |
|--------|----------|-------------|---------|
| `AppointmentCreatedEvent` | `cortaai.events` | `appointment.created` | `{ appointmentId, customerId, barberId, barbershopId, customerName, barberName, barbershopName, startTime, totalPrice }` |
| `AppointmentCancelledEvent` | `cortaai.events` | `appointment.cancelled` | `{ appointmentId, customerId, barberId, cancelledBy }` |
| `AppointmentConcludedEvent` | `cortaai.events` | `appointment.concluded` | `{ appointmentId, customerId, barberId, barbershopId }` |
| `PaymentApprovedEvent` | `cortaai.events` | `payment.approved` | `{ transactionId, appointmentId, customerId, amount }` |

### Endpoints internos do schedule-service (Dev 1 cria → Dev 2 usa)

| Método | Endpoint | Response | Consumidor |
|--------|----------|----------|------------|
| `GET` | `/api/internal/appointments/{id}` | `AppointmentDTO` | payment-service |
| `PUT` | `/api/internal/appointments/{id}/payment-status` | `void` | payment-service |

### Endpoints internos do barbershop-service (Dev 1 cria → Dev 1 usa)

| Método | Endpoint | Response | Consumidor |
|--------|----------|----------|------------|
| `GET` | `/api/internal/barbershops/{id}` | `BarbershopInfoDTO` | schedule-service |
| `GET` | `/api/internal/barbershops/{shopId}/activities?ids=` | `List<ActivityInfoDTO>` | schedule-service |

---

## ⚠️ REGRAS FINAIS

1. **Antes de começar:** Ambos devem fazer `git pull origin main` para ter o mesmo ponto de partida.
2. **Comunicação diária:** Pelo menos 1 status update por dia (via chat/stand-up).
3. **Não editar arquivos do outro:** Se precisar, comunicar antes.
4. **Testes locais antes do push:** Cada dev valida que seu serviço compila (`./mvnw compile -pl {service}`).
5. **Commit granular:** Um commit por funcionalidade (model, service, controller, etc.).
6. **Mensagens de commit padronizadas:**
   ```
   feat(schedule-service): add Appointment model and repository
   feat(notification-service): add RabbitMQ listener for appointment events
   feat(payment-service): integrate Mercado Pago Checkout Pro
   feat(frontend): add payment checkout page
   ```
7. **Dev 1 faz merge primeiro** (pois toca em 1 arquivo do barbershop-service).
8. **Dev 2 faz rebase depois** (resolver conflito no gateway yml se houver).

