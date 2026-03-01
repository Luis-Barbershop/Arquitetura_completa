# 🔀 GUIA DE TRABALHO PARALELO — DEV 1, DEV 2 & DEV 3

**Data:** 01/03/2026  
**Contexto:** Etapas 0, 1 e 2 concluídas pelo Dev 1 (infra, user-service, barbershop-service)  
**Equipe:**  
- **Dev 1** — Backend (já fez Etapas 0-2)  
- **Dev 2** — Backend (começa agora)  
- **Dev 3** — Frontend exclusivo  

---

## ⚖️ DISTRIBUIÇÃO

### Esforço restante

| Serviço | Complexidade | Pts |
|---------|-------------|-----|
| schedule-service | 🔴 Alta | ~40 |
| payment-service | 🟡 Média-alta | ~20 |
| product-service | 🟡 Média | ~18 |
| notification-service | 🟢 Média-baixa | ~15 |
| Frontend (adaptar + criar) | 🟡 Média | ~20 |
| **TOTAL** | | **~113** |

### Quem faz o quê

| | **Dev 1** | **Dev 2** | **Dev 3** |
|---|---|---|---|
| **Serviços** | schedule-service (40) | notification-service (15) + payment-service (20) + product-service (18) | Frontend (20) |
| **Pts agora** | **~40** | **~53** | **~20** |
| **Trabalho prévio** | Etapas 0+1+2 (~100) | Nenhum | Nenhum |
| **Nº entregas** | 1 serviço (o mais complexo) | 3 serviços | Frontend inteiro |

**Justificativa:**
- Dev 1 já fez ~100 pts → pega só o schedule (mais complexo, mas 1 serviço)
- Dev 2 não fez nada → pega 3 serviços backend (notification + payment + product = 53 pts)
- Dev 3 cuida exclusivamente do frontend (adaptar services existentes + criar novos + páginas)

---

## 📐 MAPA ANTI-CONFLITO

```
DEV 1 — "Agendamento"              DEV 2 — "Notif + Pagam + Produtos"      DEV 3 — "Frontend"
──────────────────────              ─────────────────────────────────        ──────────────────
├── schedule-service/ EXCLUSIVO    ├── notification-service/ EXCLUSIVO     ├── frontend/ EXCLUSIVO
├── barbershop-service/            ├── payment-service/      EXCLUSIVO     │
│   └── 3 arquivos NOVOS          ├── product-service/      EXCLUSIVO     │
│                                   ├── api-gateway/                        │
│                                   │   └── application.yml (add rotas)     │
```

| Arquivo/Diretório | Dev 1 | Dev 2 | Dev 3 |
|-------------------|-------|-------|-------|
| `user-service/` | ❌ | ❌ | ❌ |
| `barbershop-service/` existentes | ❌ | ❌ | ❌ |
| `barbershop-service/` 3 arquivos NOVOS | ✅ | ❌ | ❌ |
| `schedule-service/` | ✅ | ❌ | ❌ |
| `notification-service/` | ❌ | ✅ | ❌ |
| `payment-service/` | ❌ | ✅ | ❌ |
| `product-service/` | ❌ | ✅ | ❌ |
| `api-gateway/application.yml` | ❌ | ✅ | ❌ |
| `frontend/` | ❌ | ❌ | ✅ |
| `docker-compose.yml` | ❌ | ❌ | ❌ |
| `backend/pom.xml` | ❌ | ❌ | ❌ |
| `init.sql` | ❌ | ❌ | ❌ |

### Branches
```bash
git checkout -b feat/schedule-service           # Dev 1
git checkout -b feat/notification-payment-product  # Dev 2
git checkout -b feat/frontend-integration       # Dev 3
```

### Ordem de merge (Semana 4)
```
1. Dev 1 merge primeiro (toca em barbershop-service com 3 arquivos novos)
2. Dev 2 rebase + merge (toca em api-gateway/application.yml)
3. Dev 3 rebase + merge (só frontend, zero conflito backend)
```

---

## 🗓️ TIMELINE

```
Semana │  DEV 1                       │  DEV 2                          │  DEV 3
───────┼──────────────────────────────┼─────────────────────────────────┼──────────────────────────
 S1    │ schedule: Models, Repos,     │ notification-service: COMPLETO  │ Mapear endpoints novos,
       │ Feign, Redis, RabbitMQ,      │ (listeners, email, dedup,       │ adaptar services existentes,
       │ InternalBarbershopCtrl       │  controller)                    │ criar novos service files
───────┼──────────────────────────────┼─────────────────────────────────┼──────────────────────────
 S2    │ schedule: AppointmentService │ payment-service: COMPLETO       │ Novas páginas e componentes
       │ (lógica core, conflitos,     │ (Mercado Pago, webhooks,        │ (Notifications, Payment,
       │  disponibilidade, eventos)   │  Feign, idempotência)           │  Products, Cart)
───────┼──────────────────────────────┼─────────────────────────────────┼──────────────────────────
 S3    │ schedule: Controllers,       │ product-service: COMPLETO       │ Integração visual,
       │ Internal endpoints,          │ (CRUD produtos, pedidos,        │ rotas React, testes
       │ testes                       │  estoque, controller)           │ manuais E2E
───────┼──────────────────────────────┼─────────────────────────────────┼──────────────────────────
 S4    │              ████ MERGE + TESTE INTEGRADO + AJUSTES FINAIS ████
```

---

## 👨‍💻 DEV 1 — schedule-service

**Diretórios:** `schedule-service/` (EXCLUSIVO) + 3 arquivos NOVOS no `barbershop-service/`  
**NÃO TOQUE:** `frontend/`, `notification-service/`, `payment-service/`, `product-service/`, `api-gateway/`

### SEMANA 1 — Fundação

#### D1-S1.1 — Enum
`AppointmentStatus`: SCHEDULED, CONFIRMED, IN_PROGRESS, CONCLUDED, CANCELLED, NO_SHOW

#### D1-S1.2 — Entidades
**`Appointment.java`** — id (UUID), customerId, barberId, barbershopId (desacoplados), customerName/barberName/barbershopName (snapshots), startTime, endTime, totalPrice, status, activities (OneToMany)

**`AppointmentActivity.java`** — id, activityId (ref externa), activityName/price/durationMinutes (snapshots), appointment (ManyToOne)

**`BarberBlock.java`** — id, barberId, startTime, endTime, reason

#### D1-S1.3 — DTOs (9)
CreateAppointmentDTO, AppointmentDTO, AppointmentActivityDTO, TimeSlotDTO, CreateBarberBlockDTO, BarberBlockDTO, UserInfoDTO, BarbershopInfoDTO, ActivityInfoDTO

#### D1-S1.4 — Repositórios
`AppointmentRepository` com `hasConflict()` query  
`BarberBlockRepository` com verificação de sobreposição

#### D1-S1.5 — Feign Clients
`UserServiceClient` → `/api/internal/users/`  
`BarbershopServiceClient` → `/api/internal/barbershops/`

#### D1-S1.6 — Configs
RedisConfig (TTL 5min), RabbitConfig (exchange cortaai.events), Application (@EnableJpaAuditing, @EnableFeignClients, @EnableCaching)

#### D1-S1.7 — InternalBarbershopController (3 arquivos NOVOS)
`barbershop-service/.../controller/InternalBarbershopController.java`  
`barbershop-service/.../dto/BarbershopInfoDTO.java`  
`barbershop-service/.../dto/ActivityInfoDTO.java`

#### D1-S1.8 — POM schedule-service
Adicionar: data-redis, amqp, openfeign, resilience4j, mapstruct, lombok + annotation processors

✅ CHECKPOINT: `./mvnw compile -pl schedule-service` OK

### SEMANA 2 — Lógica core

#### D1-S2.1 — AppointmentMapper (MapStruct)

#### D1-S2.2 — AppointmentService
- `createAppointment()`: Feign validações → calcular duração → conflito → block → snapshots → RabbitMQ evento
- `cancelAppointment()`: verificar permissão → CANCELLED → evento
- `concludeAppointment()`: verificar é barber → CONCLUDED → evento
- `getAvailability()`: Feign workHours (@Cacheable Redis) → agendamentos + bloqueios → slots 30min

#### D1-S2.3 — BarberBlockService (CRUD)

#### D1-S2.4 — Eventos RabbitMQ
```java
AppointmentCreatedEvent(appointmentId, customerId, barberId, barbershopId,
    customerName, barberName, barbershopName, startTime, totalPrice)
AppointmentCancelledEvent(appointmentId, customerId, barberId, cancelledBy)
AppointmentConcludedEvent(appointmentId, customerId, barberId, barbershopId)
```

✅ CHECKPOINT: Lógica core compila. Eventos publicados.

### SEMANA 3 — Controllers + testes

#### D1-S3.1 — AppointmentController
```
POST   /api/appointments
GET    /api/appointments/{id}
GET    /api/appointments/my-appointments
GET    /api/appointments/barber/{barberId}?date=
GET    /api/appointments/barbershop/{shopId}?date=
PUT    /api/appointments/{id}/cancel
PUT    /api/appointments/{id}/conclude
PUT    /api/appointments/{id}/confirm
GET    /api/appointments/availability?barberId=&date=
```

#### D1-S3.2 — BarberBlockController
```
POST   /api/appointments/barber-blocks
GET    /api/appointments/barber-blocks?barberId=&date=
DELETE /api/appointments/barber-blocks/{id}
```

#### D1-S3.3 — InternalAppointmentController (para Dev 2 usar no payment-service)
```
GET /api/internal/appointments/{id}
PUT /api/internal/appointments/{id}/payment-status
```

#### D1-S3.4 — Testes
- [ ] Criar agendamento com Feign mockado
- [ ] Conflito de horário detectado
- [ ] Disponibilidade calculada
- [ ] BarberBlock bloqueia
- [ ] Cancelamento com permissão

✅ **FINAL DEV 1:** schedule-service completo e testado.

---

## 👩‍💻 DEV 2 — notification-service + payment-service + product-service

**Diretórios:** `notification-service/`, `payment-service/`, `product-service/` (EXCLUSIVOS) + `api-gateway/application.yml`  
**NÃO TOQUE:** `user-service/`, `barbershop-service/`, `schedule-service/`, `frontend/`

### SEMANA 1 — notification-service (COMPLETO)

#### D2-S1.1 — Application + RabbitConfig
Queues: appointment.created/cancelled/concluded, payment.approved

#### D2-S1.2 — Notification model (JPA)
id, userId, type, title, message, channel, read, createdAt

#### D2-S1.3 — Event DTOs (bater com contrato Dev 1)
AppointmentCreatedEvent, AppointmentCancelledEvent, AppointmentConcludedEvent, PaymentApprovedEvent

#### D2-S1.4 — Listeners (@RabbitListener para cada queue)
- `onAppointmentCreated` → notificar customer + barber
- `onAppointmentCancelled` → notificar contraparte
- `onAppointmentConcluded` → pedir avaliação
- `onPaymentApproved` → confirmar pagamento

#### D2-S1.5 — EmailService (Resend API REST)
#### D2-S1.6 — NotificationService (orquestra canais)
#### D2-S1.7 — Deduplicação Redis (chave `notification:{type}:{id}`, TTL 24h)
#### D2-S1.8 — NotificationController
```
GET /api/notifications/my-notifications
PUT /api/notifications/{id}/read
GET /api/notifications/unread-count
```
#### D2-S1.9 — POM: amqp, data-redis, data-jpa, mysql, lombok

✅ CHECKPOINT: escuta RabbitMQ, cria notificações, envia email, dedup.

### SEMANA 2 — payment-service (COMPLETO)

#### D2-S2.1 — Transaction + WebhookLog models
#### D2-S2.2 — ScheduleServiceClient (Feign → /api/internal/appointments)
#### D2-S2.3 — PaymentService
- `createPayment`: Feign→schedule → Mercado Pago API → salvar Transaction
- `processWebhook`: idempotência → MP API → atualizar → Feign→schedule + RabbitMQ

#### D2-S2.4 — PaymentController + WebhookController
```
POST /api/payments/create
GET  /api/payments/{id}
GET  /api/payments/my-payments
POST /api/payments/webhook
```
#### D2-S2.5 — POM: mercadopago-sdk, openfeign, amqp, resilience4j, lombok, mapstruct
#### D2-S2.6 — Application: @EnableFeignClients, @EnableJpaAuditing

✅ CHECKPOINT: cria checkout, processa webhook, publica PaymentApprovedEvent.

### SEMANA 3 — product-service (COMPLETO)

#### D2-S3.1 — Models
Product, Order, OrderItem, StockMovement + enums (ProductCategory, OrderStatus, MovementType)

#### D2-S3.2 — Repositories + DTOs + Mappers

#### D2-S3.3 — ProductService + ProductController
```
POST   /api/products
GET    /api/products?barbershopId=
GET    /api/products/{id}
PUT    /api/products/{id}
DELETE /api/products/{id}
```

#### D2-S3.4 — OrderService + OrderController
```
POST   /api/orders
GET    /api/orders/my-orders
GET    /api/orders/shop-orders?barbershopId=
PUT    /api/orders/{id}/status
```
Lógica: validar estoque → criar Order+Items (snapshots) → StockMovement OUT

#### D2-S3.5 — POM + Application
#### D2-S3.6 — Rotas no Gateway
```yaml
- id: payment-service
  uri: lb://payment-service
  predicates:
    - Path=/api/payments/**
- id: notification-service
  uri: lb://notification-service
  predicates:
    - Path=/api/notifications/**
- id: product-service
  uri: lb://product-service
  predicates:
    - Path=/api/products/**, /api/orders/**
```

✅ **FINAL DEV 2:** notification + payment + product completos. Gateway atualizado.

---

## 🎨 DEV 3 — Frontend

**Diretório:** `frontend/` (EXCLUSIVO)  
**NÃO TOQUE:** Nada no `backend/`

### O QUE PRECISA SABER ANTES DE COMEÇAR

O frontend já existe com:
- `src/services/api.js` — Axios com baseURL `/api` e interceptor JWT
- `src/services/authService.js` — Login/registro
- `src/services/barbershopService.js` — CRUD barbearias
- `src/services/appointmentService.js` — Agendamentos
- `src/AppRoutes.jsx` — Rotas React
- `src/components/` — Componentes existentes (AgendamentoPage, BarberPage, HomePage, Login, Sign_In)
- Vite proxy já configurado: `/api` → `http://localhost:8080` (Gateway)

**Endpoints que Dev 1 e Dev 2 estão criando:**
```
# schedule-service (Dev 1)
POST/GET/PUT  /api/appointments/*
GET           /api/appointments/availability?barberId=&date=

# notification-service (Dev 2)
GET           /api/notifications/my-notifications
PUT           /api/notifications/{id}/read
GET           /api/notifications/unread-count

# payment-service (Dev 2)
POST          /api/payments/create
GET           /api/payments/{id}
GET           /api/payments/my-payments

# product-service (Dev 2)
GET           /api/products?barbershopId=
POST          /api/orders
GET           /api/orders/my-orders
```

### SEMANA 1 — Adaptar + novos services

#### D3-S1.1 — Verificar/adaptar services existentes
- `authService.js` → verificar endpoints `/api/customers/register`, `/api/customers/login`, `/api/barbers/register`, `/api/barbers/login`
- `barbershopService.js` → verificar endpoints `/api/barbershops/*`
- `appointmentService.js` → verificar endpoints `/api/appointments/*`

#### D3-S1.2 — Criar novos services

**`src/services/notificationService.js`**
```javascript
import api from './api';
export const getMyNotifications = () => api.get('/notifications/my-notifications');
export const markAsRead = (id) => api.put(`/notifications/${id}/read`);
export const getUnreadCount = () => api.get('/notifications/unread-count');
```

**`src/services/paymentService.js`**
```javascript
import api from './api';
export const createPayment = (data) => api.post('/payments/create', data);
export const getPaymentById = (id) => api.get(`/payments/${id}`);
export const getMyPayments = () => api.get('/payments/my-payments');
```

**`src/services/productService.js`**
```javascript
import api from './api';
export const getProductsByShop = (shopId) => api.get(`/products?barbershopId=${shopId}`);
export const getProductById = (id) => api.get(`/products/${id}`);
export const createOrder = (order) => api.post('/orders', order);
export const getMyOrders = () => api.get('/orders/my-orders');
```

#### D3-S1.3 — Criar Context para carrinho
**`src/contexts/CartContext.jsx`** — Context API com estado do carrinho (add/remove/clear)

#### D3-S1.4 — Criar Context para notificações
**`src/contexts/NotificationContext.jsx`** — Polling de unreadCount a cada 30s

### SEMANA 2 — Novas páginas e componentes

#### D3-S2.1 — Componente NotificationBell
- Ícone com badge de contagem
- Dropdown com lista de notificações recentes
- Clicar marca como lida

#### D3-S2.2 — Página de Notificações
**`src/pages/Notifications.jsx`** — Lista completa de notificações

#### D3-S2.3 — Componente PaymentCheckout
- Botão "Pagar" que chama `createPayment` e redireciona ao Mercado Pago
- Props: appointmentId

#### D3-S2.4 — Páginas de retorno do pagamento
**`src/pages/PaymentSuccess.jsx`** — Mensagem de sucesso + link para "Meus agendamentos"  
**`src/pages/PaymentFailure.jsx`** — Mensagem de erro + botão "Tentar novamente"

#### D3-S2.5 — Componente ProductCard
- Card com foto, nome, preço, botão "Adicionar ao carrinho"

#### D3-S2.6 — Página de Produtos
**`src/pages/Products.jsx`** — Grid de produtos da barbearia com filtro por categoria

#### D3-S2.7 — Página de Carrinho
**`src/pages/Cart.jsx`** — Lista de itens, quantidade, total, botão "Finalizar pedido"

#### D3-S2.8 — Página de Meus Pedidos
**`src/pages/MyOrders.jsx`** — Lista de pedidos com status

### SEMANA 3 — Integração e polish

#### D3-S3.1 — Atualizar AppRoutes.jsx
Adicionar rotas:
```
/notifications
/payment/success
/payment/failure
/barbershop/:id/products
/cart
/my-orders
```

#### D3-S3.2 — Integrar NotificationBell no Header/Navbar

#### D3-S3.3 — Integrar PaymentCheckout na tela de agendamento

#### D3-S3.4 — Integrar ProductCard na página da barbearia

#### D3-S3.5 — Testes manuais E2E
> ⚠️ Dev 3 pode testar com mock/placeholder enquanto backend não está mergeado.  
> Após merge na S4, testar com backend real.

- [ ] Login → buscar barbearia → ver atividades → agendar
- [ ] Ver notificações → marcar como lida
- [ ] Agendar → pagar → retorno sucesso/falha
- [ ] Vitrine de produtos → carrinho → pedido
- [ ] Meus agendamentos / Meus pedidos

#### D3-S3.6 — Responsividade mobile
Verificar que todas as novas páginas funcionam em mobile.

✅ **FINAL DEV 3:** Frontend completo com todos os novos services, páginas e componentes.

---

## 🤝 SEMANA 4 — MERGE

### Ordem de merge
```bash
# 1. Dev 1 primeiro (toca em barbershop-service)
git checkout main && git pull
git merge feat/schedule-service && git push

# 2. Dev 2 segundo (toca em api-gateway)
git fetch origin && git rebase origin/main
git checkout main && git merge feat/notification-payment-product && git push

# 3. Dev 3 por último (só frontend, zero conflito)
git fetch origin && git rebase origin/main
git checkout main && git merge feat/frontend-integration && git push
```

### Checklist pós-merge

| # | Teste | Quem valida |
|---|-------|------------|
| 1 | Eureka: 7 serviços registrados | Dev 1 + Dev 2 |
| 2 | Agendar → evento RabbitMQ → notificação | Dev 1 cria, Dev 2 verifica |
| 3 | Pagar → webhook → appointment atualizado | Dev 2 paga, Dev 1 verifica |
| 4 | Cancelar → notificação enviada | Dev 1 cancela, Dev 2 verifica |
| 5 | Produto → pedido → estoque baixado | Dev 2 |
| 6 | Frontend: login → agendar → pagar | Dev 3 + Dev 1 |
| 7 | Frontend: notificações | Dev 3 + Dev 2 |
| 8 | Frontend: produtos → carrinho → pedido | Dev 3 + Dev 2 |
| 9 | Frontend: mobile responsivo | Dev 3 |

---

## 📋 CONTRATO DE INTERFACE

> ⚠️ Os 3 devs DEVEM concordar nisto ANTES de começar a codificar.

### Eventos RabbitMQ (exchange: `cortaai.events`, tipo topic)

| Evento | Routing Key | Quem publica | Quem consome |
|--------|------------|-------------|-------------|
| AppointmentCreatedEvent | `appointment.created` | Dev 1 | Dev 2 (notification) |
| AppointmentCancelledEvent | `appointment.cancelled` | Dev 1 | Dev 2 (notification) |
| AppointmentConcludedEvent | `appointment.concluded` | Dev 1 | Dev 2 (notification) |
| PaymentApprovedEvent | `payment.approved` | Dev 2 | Dev 2 (notification) |

**Payload dos eventos:**
```
AppointmentCreatedEvent:   { appointmentId, customerId, barberId, barbershopId, customerName, barberName, barbershopName, startTime, totalPrice }
AppointmentCancelledEvent: { appointmentId, customerId, barberId, cancelledBy }
AppointmentConcludedEvent: { appointmentId, customerId, barberId, barbershopId }
PaymentApprovedEvent:      { transactionId, appointmentId, customerId, amount }
```

### Endpoints internos (NÃO expostos no Gateway)

| Endpoint | Criado por | Consumido por |
|----------|-----------|--------------|
| `GET /api/internal/users/{id}` | Já existe | Dev 1 (schedule) |
| `GET /api/internal/users/by-email/{email}` | Já existe | Dev 1 (schedule) |
| `GET /api/internal/barbershops/{id}` | Dev 1 (novo no barbershop-svc) | Dev 1 (schedule) |
| `GET /api/internal/barbershops/{shopId}/activities?ids=` | Dev 1 (novo) | Dev 1 (schedule) |
| `GET /api/internal/appointments/{id}` | Dev 1 (schedule) | Dev 2 (payment) |
| `PUT /api/internal/appointments/{id}/payment-status` | Dev 1 (schedule) | Dev 2 (payment) |

### Endpoints públicos (para Dev 3 integrar no frontend)

| Serviço | Endpoints | Dev backend |
|---------|-----------|------------|
| schedule | `POST/GET /api/appointments/*`, `GET /api/appointments/availability` | Dev 1 |
| notification | `GET /api/notifications/my-notifications`, `PUT .../read`, `GET .../unread-count` | Dev 2 |
| payment | `POST /api/payments/create`, `GET /api/payments/*` | Dev 2 |
| product | `GET /api/products?barbershopId=`, `POST /api/orders`, `GET /api/orders/*` | Dev 2 |

---

## ⚠️ REGRAS

1. **`git pull origin main`** antes de criar branch
2. **Contrato de interface** combinado ANTES de codificar
3. **Status update diário** entre os 3 devs
4. **Não editar arquivos do outro** — se precisar, combinar no chat
5. **`./mvnw compile -pl {service}`** antes de push (Dev 1 e Dev 2)
6. **`npm run build`** antes de push (Dev 3)
7. **Commits padronizados:**
   ```
   feat(schedule-service): add Appointment model and repository
   feat(notification-service): add RabbitMQ listener for appointment events
   feat(payment-service): integrate Mercado Pago Checkout Pro
   feat(product-service): add Order flow with stock management
   feat(frontend): add notification bell component
   feat(frontend): add payment checkout page
   ```
8. **Merge:** Dev 1 → Dev 2 → Dev 3 (nessa ordem)
9. **Dev 3 pode mockar respostas** enquanto backend não está pronto (usar dados estáticos/localStorage para desenvolver UI)

