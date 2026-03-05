# FRF + PRD + TD - Sistema CortaAí Backend
**Data:** Março 2026  
**Versão:** 1.0  
**Escopo:** Análise completa do backend em arquitetura de microserviços

---

## 1. FUNCTIONAL REQUIREMENTS DOCUMENT (FRF)

### 1.1 Visão Geral do Sistema
O sistema CortaAí é uma plataforma de agendamento e gestão para barbearias, implementada em **arquitetura de microserviços** com 8 serviços independentes:

| Serviço | Porta | Responsabilidade |
|---------|-------|------------------|
| **discovery-service** | 8761 | Eureka Registry (service discovery) |
| **api-gateway** | 8080 | Roteamento, autenticação Firebase |
| **user-service** | 8081 | Gestão de usuários (Barbers + Customers) |
| **barbershop-service** | 8082 | Gestão de barbearias e serviços |
| **schedule-service** | 8083 | Agendamentos e disponibilidade |
| **payment-service** | 8084 | Pagamentos via Mercado Pago |
| **notification-service** | 8085 | Notificações IN_APP e EMAIL |
| **product-service** | 8086 | E-commerce (produtos e pedidos) |

---

### 1.2 Regras de Negócio Atuais

#### **1.2.1 Gestão de Usuários (User Service)**

**Entidades:**
- `Barber`: Profissional de barbearia
  - Firebase UID único (autenticação social)
  - Email, telefone, CPF (validações de unicidade)
  - Horários de trabalho (workStartTime, workEndTime)
  - Foto de perfil (Cloudinary)
  - Associação com barbearia (barbershopId)

- `Customer`: Cliente
  - Firebase UID único
  - Email, telefone, CPF
  - Foto de perfil

**Fluxos de Autenticação:**

1. **Login Social (Google, Facebook, Apple, etc.)**
   ```
   Cliente → Firebase SDK (signInWithPopup) → Obtém ID Token
                ↓
   POST /api/auth/verify (token no body)
                ↓
   Verifica token Firebase → Auto-provisiona usuário
                ↓
   Retorna AuthResponseDTO com profileComplete (bool)
   ```

2. **Completar Perfil (CPF, Telefone, Horários)**
   ```
   Se profileComplete = false
                ↓
   POST /api/auth/customers/complete-profile (para clientes)
   POST /api/auth/barbers/complete-profile (para barbeiros)
   ```

**Validações:**
- CPF válido (algoritmo de validação)
- Email único por tipo de usuário
- Telefone único por tipo de usuário
- Barbeiro só pode estar em UMA barbearia por vez

---

#### **1.2.2 Gestão de Barbearias (Barbershop Service)**

**Entidades:**
- `Barbershop`: Representação da barbearia
  - CNPJ único
  - Owner (UUID do barbeiro proprietário)
  - Logo, banner (Cloudinary)
  - Endereço
  
- `Activity`: Serviço oferecido (corte, barba, etc.)
  - Nome, preço, duração em minutos
  - Foto (Cloudinary)
  - Ligado a uma Barbershop
  
- `BarbershopJoinRequest`: Solicitação de vínculo
  - Status: PENDING, APPROVED, REJECTED
  - Barbeiro solicita entrada via CNPJ
  - Owner aprova/rejeita
  
- `BarbershopHighlight`: Imagens de destaque/portfólio
  - Imagens para carrossel
  - Ligadas a Barbershop

**Fluxos:**

1. **Criar Barbearia**
   ```
   Barbeiro autenticado
                ↓
   POST /api/barbershops/register-my-shop (com logo)
                ↓
   Valida: não possui barbearia, CNPJ único
                ↓
   Cria Barbershop + atualiza barbershopId no user-service
                ↓
   Status: ATIVA
   ```

2. **Solicitar Entrada em Barbearia**
   ```
   Barbeiro autenticado + CNPJ da barbearia
                ↓
   POST /api/barbershops/join-request
                ↓
   Cria BarbershopJoinRequest (status=PENDING)
                ↓
   Owner consulta: GET /api/barbershops/my-shop/pending-requests
                ↓
   Owner aprova: POST /api/barbershops/my-shop/approve-request/{requestId}
                ↓
   Atualiza barbershopId do barbeiro no user-service
   ```

3. **Sair da Barbearia**
   ```
   Barbeiro autenticado
                ↓
   POST /api/barbershops/leave-shop
                ↓
   Remove associação: barbershopId = null
                ↓
   Status: DESVINCULADO
   ```

4. **Fechar Barbearia**
   ```
   Owner autenticado
                ↓
   DELETE /api/barbershops/my-shop/close (com confirmação)
                ↓
   Cascade delete: Activities, Highlights, JoinRequests
   ```

---

#### **1.2.3 Agendamentos (Schedule Service)**

**Entidades:**
- `Appointment`: Agendamento de cliente
  - customerID, barberId, barbershopId
  - Snapshots desnormalizados: customerName, barberName, barbershopName
  - startTime, endTime
  - totalPrice
  - status: SCHEDULED, CONFIRMED, IN_PROGRESS, CONCLUDED, CANCELLED, NO_SHOW

- `AppointmentActivity`: Atividades (serviços) do agendamento
  - Snapshots de cada Activity na hora do agendamento
  - activityId, activityName, price, durationMinutes

- `BarberBlock`: Bloqueio de agenda (almoço, férias, etc.)
  - barberId, startTime, endTime, reason
  - Impede novos agendamentos no intervalo

**Fluxos:**

1. **Criar Agendamento**
   ```
   Cliente autenticado + dados:
   - customerId (do header X-User-Id)
   - barberId
   - barbershopId
   - activityIds (lista)
   - startTime
   
                ↓
   Valida:
   - Customer existe (Feign → user-service)
   - Barber existe (Feign → user-service)
   - Barbershop existe (Feign → barbershop-service)
   - Activities existem (Feign → barbershop-service)
   - Sem conflito de horário para o barbeiro
   - Barbeiro não está bloqueado naquele horário
                ↓
   Calcula:
   - endTime = startTime + sum(durações das activities)
   - totalPrice = sum(preços das activities)
                ↓
   Cria Appointment + AppointmentActivities (snapshots)
                ↓
   Publica evento: AppointmentCreatedEvent (RabbitMQ)
                ↓
   Status: SCHEDULED
   ```

2. **Confirmar Agendamento**
   ```
   Barbeiro ou Owner autenticado
                ↓
   PUT /api/appointments/{id}/confirm
                ↓
   Status: SCHEDULED → CONFIRMED
   ```

3. **Cancelar Agendamento**
   ```
   Customer, Barber ou Owner autenticado
                ↓
   PUT /api/appointments/{id}/cancel
                ↓
   Status: qualquer → CANCELLED
                ↓
   Publica evento: AppointmentCancelledEvent
   ```

4. **Concluir Agendamento**
   ```
   Barbeiro autenticado (apenas barbeiro)
                ↓
   PUT /api/appointments/{id}/conclude
                ↓
   Status: CONCLUDED
                ↓
   Publica evento: AppointmentConcludedEvent
   ```

5. **Consultar Disponibilidade**
   ```
   GET /api/appointments/availability?barberId=X&date=YYYY-MM-DD
                ↓
   Busca horários trabalho do barber (Redis cache 5min)
                ↓
   Filtra: agendamentos + bloqueios do dia
                ↓
   Gera slots de 30 min disponíveis
   ```

**Validações:**
- Horários no intervalo de trabalho do barbeiro
- Sem sobreposição com agendamentos ativos
- Sem sobreposição com BarberBlocks
- Apenas barbeiro pode concluir agendamento

---

#### **1.2.4 Pagamentos (Payment Service)**

**Entidades:**
- `Transaction`: Transação de pagamento
  - appointmentId (referência externa)
  - customerId
  - amount (BigDecimal)
  - status: PENDING, APPROVED, REJECTED, CANCELLED, REFUNDED, IN_PROCESS
  - mpPreferenceId (Mercado Pago)
  - mpPaymentId (retornado no webhook)
  - checkoutUrl (link para checkout)

- `WebhookLog`: Log de webhooks (idempotência)
  - mpResourceId (ID do pagamento no MP)
  - eventType
  - processed (bool)
  - rawPayload

**Fluxos:**

1. **Criar Pagamento**
   ```
   Cliente autenticado + appointmentId
                ↓
   GET /api/appointments/{id} (Feign → schedule-service)
                ↓
   Valida: appointment pertence ao customer
                ↓
   Cria preferência no Mercado Pago:
   - Item: "Agendamento - {barbershop}"
   - Descrição: "Atendimento com {barber} em {data}"
   - Valor: totalPrice do agendamento
   - URL de notificação webhook
                ↓
   Salva Transaction (status=PENDING)
                ↓
   Retorna: checkoutUrl (Mercado Pago Checkout Pro)
                ↓
   Cliente é redirecionado para MP checkout
   ```

2. **Webhook do Mercado Pago**
   ```
   Mercado Pago → POST /api/payments/webhook
   (public, sem autenticação)
                ↓
   Verifica: já foi processado? (WebhookLog)
                ↓
   Consulta pagamento no MP via SDK
                ↓
   Mapeia status MP → PaymentStatus
                ↓
   Se status = APPROVED:
   - Atualiza Transaction (status=APPROVED)
   - Atualiza Appointment no schedule-service (status=CONFIRMED)
   - Publica evento: PaymentApprovedEvent (RabbitMQ)
                ↓
   Marca WebhookLog como processado
   ```

**Integrações Externas:**
- **Mercado Pago SDK 2.1.24**
  - Criar preferências (checkout)
  - Consultar pagamentos
  - Validar webhooks

---

#### **1.2.5 Notificações (Notification Service)**

**Entidades:**
- `Notification`: Notificação armazenada
  - userId
  - type: APPOINTMENT_CREATED, APPOINTMENT_CANCELLED, APPOINTMENT_CONCLUDED, PAYMENT_APPROVED
  - title, message
  - channel: IN_APP, EMAIL
  - read (bool)
  - createdAt

**Fluxos:**

1. **Listeners de Eventos (RabbitMQ)**
   ```
   Consome 4 queues com routing keys:
   
   a) appointment.created
      → Notifica customer + barber
      → Mensagem: "Seu agendamento foi confirmado para..."
   
   b) appointment.cancelled
      → Notifica a contraparte (customer ↔ barber)
      → Mensagem: "O {tipo} cancelou o agendamento"
   
   c) appointment.concluded
      → Notifica customer
      → Mensagem: "Seu atendimento foi concluído!"
   
   d) payment.approved
      → Notifica customer
      → Mensagem: "Seu pagamento de R$ X foi aprovado"
   ```

2. **Deduplicação (Redis)**
   ```
   Para cada evento (type, id):
   - Chave no Redis: "notification:{type}:{id}"
   - TTL: 24 horas
   - Evita notificações duplicadas se webhook é reprocessado
   ```

3. **Envio de Email (Background)**
   ```
   @Async EmailService.sendEmail(to, subject, body)
   - SMTP (Gmail, SendGrid, etc.)
   - Não bloqueia fluxo principal
   - Falhas são logadas mas não falham a transação
   ```

---

#### **1.2.6 Produtos & Pedidos (Product Service)**

**Entidades:**
- `Product`: Produto disponível para venda
  - barbershopId
  - name, description
  - price (BigDecimal)
  - category: SHAMPOO, CONDITIONER, POMADE, WAX, OIL, BEARD_OIL, AFTERSHAVE, RAZOR, SCISSORS, COMB, BRUSH, ACCESSORY, OTHER
  - stockQuantity (controle de estoque)
  - imageUrl (Cloudinary)
  - active (bool, soft delete)

- `Order`: Pedido de compra
  - customerId
  - barbershopId
  - status: PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED
  - totalPrice
  - items: List<OrderItem>

- `OrderItem`: Item individual do pedido
  - orderId
  - productId (snapshot)
  - productName (snapshot, nome no momento da compra)
  - price (snapshot, preço no momento)
  - quantity

- `StockMovement`: Auditoria de movimentação
  - productId
  - type: IN (entrada), OUT (saída/venda)
  - quantity
  - orderId (referência opcional)
  - reason

**Fluxos:**

1. **Criar Produto**
   ```
   Owner da barbearia autenticado
                ↓
   POST /api/products
   {
     barbershopId,
     name,
     description,
     price,
     category,
     stockQuantity,
     imageUrl
   }
                ↓
   Cria Product (active=true)
                ↓
   Se stockQuantity > 0:
   - Cria StockMovement (type=IN, reason="Estoque inicial")
                ↓
   Status: ATIVO
   ```

2. **Criar Pedido**
   ```
   Cliente autenticado + dados:
   {
     barbershopId,
     items: [
       { productId, quantity },
       ...
     ]
   }
                ↓
   Valida cada item:
   - Produto existe
   - Produto está ativo (active=true)
   - Estoque suficiente
                ↓
   Cria Order (status=PENDING)
   Cria OrderItems com snapshots:
   - productName (do produto atual)
   - price (do produto atual)
                ↓
   Baixa estoque: product.stockQuantity -= quantity
                ↓
   Cria StockMovement:
   - type=OUT
   - reason="Venda - Pedido"
   - orderId=X (atualizado após save)
                ↓
   Calcula totalPrice = sum(price * quantity)
                ↓
   Publica evento (se houver listener)
   ```

3. **Atualizar Status do Pedido**
   ```
   Owner da barbearia autenticado
                ↓
   PUT /api/orders/{id}/status?status=CONFIRMED
                ↓
   Validação de transição:
   PENDING → CONFIRMED → PREPARING → READY → DELIVERED
                ↓
   Status: novo status
   ```

4. **Cancelar Pedido**
   ```
   Cliente ou Owner autenticado
                ↓
   PUT /api/orders/{id}/status?status=CANCELLED
                ↓
   Se estava não-cancelado:
   - Retorna estoque:
     product.stockQuantity += quantity
   - Cria StockMovement:
     type=IN, reason="Cancelamento de pedido"
                ↓
   Status: CANCELLED
   ```

**Snapshots (Desnormalização):**
- Razão: evitar inconsistência se produto for deletado/atualizado
- OrderItem armazena productName + price na hora da compra
- Permite histórico preciso de pedidos antigos

---

### 1.3 Regras de Negócio de Segurança

#### **Autenticação**
- Firebase Authentication (social: Google, Facebook, Apple, etc.)
- API Gateway valida ID Token e injeta headers: `X-User-UID`, `X-User-Email`, `X-User-Name`, `X-User-Type`
- Serviços downstream confiam nos headers (não revalidam)

#### **Autorização**
- Endpoints internos (`/api/internal/**`) são públicos entre serviços
- Endpoints públicos (`/api/auth/verify`, listagens) não exigem token
- Endpoints protegidos exigem `X-User-UID` injetado pelo Gateway

#### **Isolamento de Dados**
- Cada serviço tem seu próprio banco de dados
- Comunicação inter-serviço via Feign Client (HTTP síncrono) ou RabbitMQ (assíncrono)
- Sem compartilhamento de banco de dados

---

## 2. PRODUCT REQUIREMENTS DOCUMENT (PRD)

### 2.1 Requisitos Funcionais por Serviço

#### **API Gateway**
- **RF1:** Rotear requisições para microserviços baseado em path
- **RF2:** Validar Firebase ID Token em todas as rotas protegidas
- **RF3:** Injetar headers de identidade (X-User-*) após validação
- **RF4:** Retornar 401 Unauthorized para tokens inválidos
- **RF5:** Permitir rotas públicas sem autenticação
- **RF6:** Agregação de Swagger UI de todos os serviços

#### **User Service**
- **RF7:** Autenticar usuários via Firebase (login social)
- **RF8:** Auto-provisionar usuários no primeiro acesso
- **RF9:** Completar perfil (CPF, telefone, horários)
- **RF10:** Validar CPF, email e telefone como únicos
- **RF11:** Gerenciar foto de perfil (upload Cloudinary)
- **RF12:** Listar barbeiros (público)
- **RF13:** Endpoints internos para resolução de IDs (Feign)
- **RF14:** Atualizar barbershopId quando barbeiro entra em barbearia

#### **Barbershop Service**
- **RF15:** Criar barbearia (apenas barbeiro owner)
- **RF16:** Gerenciar serviços (activities): criar, atualizar, deletar
- **RF17:** Sistema de solicitação de entrada (join requests)
- **RF18:** Aprovação/rejeição de solicitações
- **RF19:** Upload de logo, banner, fotos de serviços
- **RF20:** Gerenciar imagens de destaque (portfólio)
- **RF21:** Endpoints internos para barbershop details (Feign)
- **RF22:** Sair da barbearia (barbeiro desvincula)
- **RF23:** Fechar barbearia (owner deleta cascade)

#### **Schedule Service**
- **RF24:** Criar agendamentos (validação completa)
- **RF25:** Confirmar agendamentos
- **RF26:** Cancelar agendamentos com reembolso lógico
- **RF27:** Concluir agendamentos
- **RF28:** Consultar disponibilidade (horários livres)
- **RF29:** Bloquear agenda do barbeiro (lunch, férias)
- **RF30:** Usar Redis para cache de horários (TTL 5min)
- **RF31:** Integração com user-service (Feign)
- **RF32:** Integração com barbershop-service (Feign)
- **RF33:** Integração com payment-service (Feign)
- **RF34:** Publicar eventos no RabbitMQ (appointment.created, cancelled, concluded)

#### **Payment Service**
- **RF35:** Criar preferências de pagamento no Mercado Pago
- **RF36:** Gerar link de checkout
- **RF37:** Receber webhooks do Mercado Pago (públicos, sem auth)
- **RF38:** Processar webhooks com idempotência (WebhookLog)
- **RF39:** Atualizar status de transação baseado em webhook
- **RF40:** Atualizar appointment no schedule-service após aprovação
- **RF41:** Publicar evento payment.approved (RabbitMQ)
- **RF42:** Listar transações por customer

#### **Notification Service**
- **RF43:** Consumir eventos de RabbitMQ (4 tipos)
- **RF44:** Criar notificações IN_APP (armazenar em BD)
- **RF45:** Deduplicar notificações via Redis (TTL 24h)
- **RF46:** Enviar emails (assíncrono, não bloqueia)
- **RF47:** Listar notificações do usuário
- **RF48:** Marcar notificação como lida
- **RF49:** Contar notificações não lidas (badge)

#### **Product Service**
- **RF50:** Gerenciar catálogo de produtos
- **RF51:** Controle de estoque (incremento/decremento)
- **RF52:** Criar pedidos com validação de estoque
- **RF53:** Snapshots de preço e nome no pedido
- **RF54:** Atualizar status do pedido
- **RF55:** Cancelamento com retorno de estoque
- **RF56:** Auditoria de movimentação (StockMovement)
- **RF57:** Listar pedidos por customer ou barbershop

#### **Discovery Service**
- **RF58:** Registry central (Eureka)
- **RF59:** Service registration automática
- **RF60:** Service discovery automática
- **RF61:** Health checks periódicos

---

### 2.2 Requisitos Não-Funcionais

| Requisito | Descrição |
|-----------|-----------|
| **RNF1** | Autenticação centralizada no Gateway |
| **RNF2** | Criptografia TLS em trânsito |
| **RNF3** | Isolamento de dados por tenant (barbearia) |
| **RNF4** | Transações ACID em operações críticas |
| **RNF5** | Idempotência em webhooks |
| **RNF6** | Cache Redis com TTL apropriado |
| **RNF7** | Logs estruturados (SLF4J) |
| **RNF8** | Circuit breaker para Feign clients |
| **RNF9** | Retry automático (3 tentativas) |
| **RNF10** | Gravação de eventos para auditoria |
| **RNF11** | Soft delete para dados sensíveis |
| **RNF12** | Validação de entrada (JSR-303) |
| **RNF13** | Tratamento de erros com códigos HTTP apropriados |
| **RNF14** | Escalabilidade horizontal (stateless) |
| **RNF15** | Performance: tempo resposta < 500ms (90th percentile) |

---

### 2.3 Prioridades de Features

**P0 (Crítico):**
- Autenticação / Login
- Criar agendamento
- Listar disponibilidade
- Cancelar agendamento

**P1 (Alto):**
- Criar barbearia
- Gerenciar serviços (activities)
- Sistema de join requests
- Pagamento (Mercado Pago)

**P2 (Médio):**
- Notificações
- Produtos & Pedidos
- Upload de imagens

**P3 (Baixo):**
- Blocos de agenda
- Destaques (highlights)

---

## 3. TECHNICAL DESIGN (TD)

### 3.1 Diagrama de Fluxo de Dados (End-to-End)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FLUXO: Criar Agendamento                             │
└─────────────────────────────────────────────────────────────────────────────┘

1. Frontend → API Gateway (POST /api/appointments)
   Header: Authorization: Bearer <Firebase ID Token>
   Body: {customerId, barberId, barbershopId, activityIds[], startTime}

2. API Gateway
   ├─ Valida Firebase Token
   ├─ Injeta headers: X-User-UID, X-User-Email, X-User-Type
   └─ Roteia para schedule-service:8083

3. Schedule Service (POST /api/appointments)
   ├─ Valida customerId via Feign → user-service:8081
   ├─ Valida barberId via Feign → user-service:8081
   ├─ Valida barbershopId via Feign → barbershop-service:8082
   ├─ Busca activities via Feign → barbershop-service:8082
   ├─ Verifica conflito de horário (AppointmentRepository query)
   ├─ Verifica bloqueios (BarberBlockRepository query)
   ├─ Cria Appointment + AppointmentActivities (snapshots)
   ├─ Transação ACID (JPA)
   ├─ Publica evento: AppointmentCreatedEvent
   │   └─ RabbitMQ Topic: cortaai.events / Routing Key: appointment.created
   └─ Retorna AppointmentDTO (201 Created)

4. Notification Service (RabbitMQ Listener)
   ├─ Consome: queue:notification.appointment.created
   ├─ Deduplicação Redis: key=notification:APPOINTMENT_CREATED:appointmentId (TTL 24h)
   ├─ Cria Notification (customer): "Seu agendamento foi confirmado..."
   ├─ Cria Notification (barber): "Novo agendamento com {customer}..."
   └─ Armazena em BD (notification_db)

5. Frontend
   ├─ GET /api/notifications/my-notifications (com polling)
   │   Header: X-User-Id: {userId}
   ├─ Exibe lista de notificações IN_APP
   └─ PUT /api/notifications/{id}/read (marca como lida)

┌─────────────────────────────────────────────────────────────────────────────┐
│                        FLUXO: Processar Pagamento                            │
└─────────────────────────────────────────────────────────────────────────────┘

1. Cliente autenticado
   POST /api/payments/create
   Body: {appointmentId}
   Header: X-User-Id: {customerId}

2. Payment Service
   ├─ Valida: appointment pertence ao customer
   ├─ Busca Appointment via Feign → schedule-service:8083
   ├─ Cria Preferência (Mercado Pago SDK)
   │   ├─ Item: Agendamento
   │   ├─ Valor: totalPrice do agendamento
   │   ├─ Notification URL: /api/payments/webhook
   │   └─ External Reference: appointmentId (para rastreamento)
   ├─ Armazena Transaction (status=PENDING)
   │   ├─ mpPreferenceId (preferência MP)
   │   ├─ checkoutUrl (link Checkout Pro)
   │   └─ mpPaymentId (null até webhook)
   └─ Retorna TransactionDTO com checkoutUrl

3. Cliente → Mercado Pago Checkout
   ├─ Paga com cartão/boleto/PIX
   └─ MP redireciona de volta (success/failure)

4. Mercado Pago → Payment Service (Webhook)
   POST /api/payments/webhook (PUBLIC, sem auth)
   Body: {action, data: {id: paymentId}, type: "payment"}

5. Payment Service (Webhook Handler)
   ├─ Registra WebhookLog (mpResourceId=paymentId, processed=false)
   ├─ Consulta pagamento no MP SDK: Payment payment = client.get(paymentId)
   ├─ Mapeia status: approved → APPROVED, rejected → REJECTED, etc.
   ├─ Atualiza Transaction (status=APPROVED, mpPaymentId=paymentId)
   │
   ├─ SE status = APPROVED:
   │   ├─ Feign → schedule-service: PUT /api/internal/appointments/{id}/payment-status?status=CONFIRMED
   │   ├─ Publica evento: PaymentApprovedEvent
   │   │   └─ RabbitMQ: cortaai.events / payment.approved
   │   └─ Marca WebhookLog (processed=true)
   └─ Retorna 200 OK (MP exige sempre sucesso para não reenviar)

6. Notification Service (RabbitMQ)
   ├─ Consome: queue:notification.payment.approved
   ├─ Cria Notification (customer): "Seu pagamento de R$ X foi aprovado!"
   └─ Armazena em BD

7. Schedule Service (Feign response)
   ├─ Atualiza Appointment (status=CONFIRMED)
   └─ Persiste em BD
```

---

### 3.2 Diagrama de Dependências entre Entidades

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MODELO DE DADOS (Polyglot)                              │
└─────────────────────────────────────────────────────────────────────────────┘

USER_DB (user-service)
├── Barber
│   ├── id (UUID, PK)
│   ├── email (UNIQUE)
│   ├── firebaseUid (UNIQUE)
│   ├── name, tell, documentCPF
│   ├── barbershopId (FOREIGN KEY → barbershop_db.Barbershop, NULLABLE)
│   ├── workStartTime, workEndTime
│   ├── imageUrl (Cloudinary URL)
│   ├── role (ROLE_BARBER, ROLE_OWNER)
│   └── authProvider (EMAIL, GOOGLE, FACEBOOK, APPLE, GITHUB, TWITTER)
│
└── Customer
    ├── id (UUID, PK)
    ├── email (UNIQUE)
    ├── firebaseUid (UNIQUE)
    ├── name, tell, documentCPF
    ├── imageUrl (Cloudinary URL)
    └── role (ROLE_CUSTOMER)


BARBERSHOP_DB (barbershop-service)
├── Barbershop
│   ├── id (UUID, PK)
│   ├── ownerId (UUID, FOREIGN KEY → user_db.Barber, NOT NULL)
│   ├── cnpj (UNIQUE, NOT NULL)
│   ├── name, address
│   ├── logoUrl, bannerUrl (Cloudinary)
│   ├── 1:N ─────────┐
│   │                 ↓
│   ├── Activity (1:N)
│   │   ├── id (UUID, PK)
│   │   ├── barbershopId (FK)
│   │   ├── activityName, price, durationMinutes
│   │   └── imageUrl
│   │
│   ├── BarbershopJoinRequest (1:N)
│   │   ├── id (UUID, PK)
│   │   ├── barberId (UUID, FK → user_db.Barber)
│   │   ├── barbershopId (FK)
│   │   ├── status (PENDING, APPROVED, REJECTED)
│   │   └── dateCreated
│   │
│   └── BarbershopHighlight (1:N)
│       ├── id (UUID, PK)
│       ├── barbershopId (FK)
│       └── imageUrl


SCHEDULE_DB (schedule-service)
├── Appointment
│   ├── id (UUID, PK)
│   ├── customerId (UUID, NOT NULL) ──→ user_db.Customer
│   ├── barberId (UUID, NOT NULL) ──→ user_db.Barber
│   ├── barbershopId (UUID, NOT NULL) ──→ barbershop_db.Barbershop
│   ├── customerName, barberName, barbershopName (SNAPSHOTS)
│   ├── startTime, endTime (LocalDateTime)
│   ├── totalPrice (Snapshot do cálculo de activities)
│   ├── status (SCHEDULED, CONFIRMED, IN_PROGRESS, CONCLUDED, CANCELLED, NO_SHOW)
│   ├── 1:N ──────────────────┐
│   │                           ↓
│   └── AppointmentActivity (1:N)
│       ├── id (UUID, PK)
│       ├── appointmentId (FK)
│       ├── activityId (UUID) ──→ barbershop_db.Activity
│       ├── activityName, price, durationMinutes (SNAPSHOTS)
│       └── (Denormalizados na hora da compra)
│
└── BarberBlock
    ├── id (UUID, PK)
    ├── barberId (UUID, FK) ──→ user_db.Barber
    ├── startTime, endTime
    └── reason


PAYMENT_DB (payment-service)
├── Transaction
│   ├── id (UUID, PK)
│   ├── appointmentId (UUID, NOT NULL) ──→ schedule_db.Appointment
│   ├── customerId (UUID, NOT NULL) ──→ user_db.Customer
│   ├── amount (BigDecimal)
│   ├── status (PENDING, APPROVED, REJECTED, CANCELLED, REFUNDED, IN_PROCESS)
│   ├── mpPreferenceId (Mercado Pago ID, UNIQUE, NULLABLE)
│   ├── mpPaymentId (Mercado Pago payment ID, NULLABLE)
│   └── checkoutUrl (Mercado Pago Checkout Pro URL)
│
└── WebhookLog
    ├── id (UUID, PK)
    ├── mpResourceId (UNIQUE, NOT NULL) — ID do payment no MP
    ├── eventType (payment, movement, etc.)
    ├── processed (BOOLEAN)
    └── rawPayload (JSON do webhook)


NOTIFICATION_DB (notification-service)
└── Notification
    ├── id (UUID, PK)
    ├── userId (UUID, NOT NULL) ──→ user_db.Barber ou Customer
    ├── type (APPOINTMENT_CREATED, APPOINTMENT_CANCELLED, APPOINTMENT_CONCLUDED, PAYMENT_APPROVED)
    ├── title, message
    ├── channel (IN_APP, EMAIL)
    ├── read (BOOLEAN)
    └── createdAt


PRODUCT_DB (product-service)
├── Product
│   ├── id (UUID, PK)
│   ├── barbershopId (UUID, NOT NULL) ──→ barbershop_db.Barbershop
│   ├── name, description
│   ├── price (BigDecimal)
│   ├── category (ENUM: SHAMPOO, CONDITIONER, POMADE, WAX, OIL, BEARD_OIL, AFTERSHAVE, RAZOR, SCISSORS, COMB, BRUSH, ACCESSORY, OTHER)
│   ├── stockQuantity (INTEGER)
│   ├── imageUrl (Cloudinary)
│   ├── active (BOOLEAN, soft delete)
│   └── 0:N ─────────┐
│                     ↓
├── Order
│   ├── id (UUID, PK)
│   ├── customerId (UUID, NOT NULL) ──→ user_db.Customer
│   ├── barbershopId (UUID, NOT NULL) ──→ barbershop_db.Barbershop
│   ├── status (PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED)
│   ├── totalPrice (BigDecimal, calculado)
│   ├── 1:N ───────────────┐
│   │                        ↓
│   └── OrderItem (1:N)
│       ├── id (UUID, PK)
│       ├── orderId (FK)
│       ├── productId (UUID) ──→ Product (SNAPSHOT)
│       ├── productName (SNAPSHOT)
│       ├── price (SNAPSHOT)
│       └── quantity
│
└── StockMovement
    ├── id (UUID, PK)
    ├── productId (UUID, FK) ──→ Product
    ├── type (IN, OUT)
    ├── quantity
    ├── orderId (UUID, NULLABLE) ──→ Order
    └── reason
```

---

### 3.3 Relação de Status das Entidades

#### **Appointment Status Transitions**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     APPOINTMENT STATUS LIFECYCLE                          │
└──────────────────────────────────────────────────────────────────────────┘

                    [POST /api/appointments]
                               ↓
                        ┌───────────────┐
                        │   SCHEDULED   │ (Inicial)
                        └───────────────┘
                         ↙ PUT /confirm  
                                    ↘
              ┌─────────────────────────────────────────────────────┐
              │                    CONFIRMED                         │
              │ (Barbeiro ou Owner confirmou agendamento)            │
              └─────────────────────────────────────────────────────┘
                         ↙ Automático      ↘ PUT /cancel
                                              ↓
              ┌───────────────────┐   ┌─────────────────┐
              │   IN_PROGRESS     │   │    CANCELLED    │ (Terminal)
              │  (Durante atend.)  │   │(Sem reembolso)  │
              └───────────────────┘   └─────────────────┘
                         ↓
              ┌───────────────────┐
              │    CONCLUDED      │ (Terminal)
              │  (Atend. realizado)│
              └───────────────────┘

    NO_SHOW: Estado terminal (barbeiro não compareceu)
              └─ Pode ser acionado por timeout ou manualmente

Transições Válidas:
├─ SCHEDULED → CONFIRMED (barbeiro/owner)
├─ SCHEDULED → CANCELLED (customer/barbeiro/owner)
├─ CONFIRMED → IN_PROGRESS (automático)
├─ IN_PROGRESS → CONCLUDED (barbeiro)
├─ IN_PROGRESS → CANCELLED (barbeiro/owner)
├─ CONFIRMED → CANCELLED (barbeiro/owner)
└─ Qualquer → NO_SHOW (timeout)

Eventos Publicados:
├─ SCHEDULED: AppointmentCreatedEvent
├─ CANCELLED: AppointmentCancelledEvent
└─ CONCLUDED: AppointmentConcludedEvent
```

#### **Transaction Status Transitions**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    TRANSACTION STATUS LIFECYCLE                           │
└──────────────────────────────────────────────────────────────────────────┘

            [POST /api/payments/create]
                       ↓
            ┌────────────────────┐
            │      PENDING       │ (Inicial)
            │ (Link de checkout  │
            │  gerado, cliente   │
            │  não pagou)        │
            └────────────────────┘
             ↙ Webhook MP     ↘ Webhook MP
                               ↓
    ┌──────────────────┐  ┌─────────────────┐
    │    APPROVED      │  │    REJECTED     │ (Terminal)
    │ (Pagamento OK)   │  │ (Recusado ou    │
    └──────────────────┘  │  expirou)       │
            ↓             └─────────────────┘
    Publica event
    payment.approved
    
    └─ IN_PROCESS (estado intermediário se MP não responder)
    └─ CANCELLED (reembolso)
    └─ REFUNDED (reembolso completo)

Eventos Publicados:
└─ APPROVED: PaymentApprovedEvent → RabbitMQ
```

#### **Order Status Transitions**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       ORDER STATUS LIFECYCLE                              │
└──────────────────────────────────────────────────────────────────────────┘

        [POST /api/orders]
           ↓
    ┌─────────────┐
    │   PENDING   │ (Inicial, aguardando confirmação)
    └─────────────┘
       ↓ (Owner confirma)
    ┌─────────────┐
    │  CONFIRMED  │ (Owner aceita o pedido)
    └─────────────┘
       ↓ (Owner inicia preparo)
    ┌─────────────┐
    │  PREPARING  │ (Produtos sendo preparados)
    └─────────────┘
       ↓ (Owner finaliza preparo)
    ┌─────────────┐
    │    READY    │ (Pedido pronto para entrega/retirada)
    └─────────────┘
       ↓ (Cliente retira ou é entregue)
    ┌─────────────┐
    │  DELIVERED  │ (Terminal)
    └─────────────┘

    Caminho Alternativo:
    PENDING → CANCELLED (Terminal)
             ├─ Retorna estoque
             └─ Cria StockMovement (IN, reason="Cancelamento")
```

#### **BarbershopJoinRequest Status**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    JOIN REQUEST STATUS LIFECYCLE                          │
└──────────────────────────────────────────────────────────────────────────┘

    [Barbeiro solicita entrada]
           ↓
    ┌──────────────┐
    │   PENDING    │ (Aguardando resposta do owner)
    └──────────────┘
       ↙          ↘
    APPROVED    REJECTED (Terminal)
       ↓
    Atualiza barbershopId do barbeiro no user-service
    (Terminal: barbeiro agora está vinculado)
```

---

### 3.4 Comunicação Inter-Serviços

#### **Síncrona (Feign Client)**

```
Schedule Service → User Service
└─ GET /api/internal/users/{id}
└─ GET /api/internal/users/by-email/{email}

Schedule Service → Barbershop Service
└─ GET /api/internal/barbershops/{id}
└─ GET /api/internal/barbershops/{shopId}/activities?ids=[...]

Payment Service → Schedule Service
├─ GET /api/internal/appointments/{id}
└─ PUT /api/internal/appointments/{id}/payment-status?status=CONFIRMED

Barbershop Service → User Service
├─ GET /api/internal/users/{id}
├─ GET /api/internal/users/by-email/{email}
└─ PUT /api/internal/users/{id}/barbershop (barbershopId)

Integração com Circuit Breaker (Resilience4j):
├─ Schedule Service: retry 3x, wait 500ms, circuit breaker 50% failure
└─ Barbershop Service: idem
```

#### **Assíncrona (RabbitMQ - Topic Exchange)**

```
Exchange: cortaai.events (TopicExchange)

Publicadores:
├─ schedule-service → appointment.created
├─ schedule-service → appointment.cancelled
├─ schedule-service → appointment.concluded
├─ payment-service → payment.approved
└─ (possível: product-service → order events)

Subscribers:
├─ notification-service:
│   ├─ Queue: notification.appointment.created
│   │   └─ Routing Key: appointment.created
│   ├─ Queue: notification.appointment.cancelled
│   │   └─ Routing Key: appointment.cancelled
│   ├─ Queue: notification.appointment.concluded
│   │   └─ Routing Key: appointment.concluded
│   └─ Queue: notification.payment.approved
│       └─ Routing Key: payment.approved

Deduplicação:
└─ Redis: key = "notification:{TYPE}:{EVENT_ID}" (TTL 24h)
   └─ Evita notificações duplicadas se webhook for reprocessado
```

---

### 3.5 Arquitetura de Dados

#### **Estratégia de Persistência**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    POLYGLOT PERSISTENCE                             │
└─────────────────────────────────────────────────────────────────────┘

MySQL 8.0 (JPA/Hibernate):
├─ user_db (user-service)
├─ barbershop_db (barbershop-service)
├─ schedule_db (schedule-service)
├─ payment_db (payment-service)
├─ notification_db (notification-service)
└─ product_db (product-service)

Redis 7 (Cache + Deduplicação):
├─ schedule-service: Cache barber work hours (5 min TTL)
├─ notification-service: Deduplication keys (24h TTL)
└─ General: Session, rate limiting (não implementado)

RabbitMQ 3 (Mensageria):
├─ Exchange: cortaai.events (Topic)
├─ Queues: 4 (appointment.*, payment.*)
└─ Durabilidade: Persistent messages

Cloudinary (Storage de Imagens):
├─ user-service: foto de perfil
├─ barbershop-service: logo, banner, fotos de serviços, destaques
└─ product-service: foto de produto
```

#### **Transações e Consistency**

```
Operações ACID (JPA @Transactional):
├─ Criar agendamento (incluindo validações)
├─ Criar pedido (incluindo snapshot de preço)
├─ Atualizar status de transação (no webhook)
└─ Cancelar pedido (retornar estoque)

Idempotência:
├─ Webhooks: Verifica WebhookLog antes de processar
├─ Operações Feign: Retry automático (circuit breaker)
└─ RabbitMQ: Consumers devem ser idempotentes (deduplicação Redis)

Eventual Consistency:
├─ Eventos publicados assincronamente via RabbitMQ
├─ Notificações criadas após agendamento/pagamento
└─ Delay aceitável: até 5 segundos típicamente
```

---

### 3.6 Stack Técnico

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TECHNOLOGY STACK                             │
└─────────────────────────────────────────────────────────────────────┘

Backend:
├─ Java 17
├─ Spring Boot 3.3.4
├─ Spring Cloud 2023.0.3
│   ├─ Netflix Eureka (Service Discovery)
│   ├─ Spring Cloud Gateway (API Gateway)
│   ├─ OpenFeign (Feign Client)
│   └─ Resilience4j (Circuit Breaker)
├─ Spring Data JPA (ORM)
├─ Hibernate (JPA Implementation)
├─ Spring Security (Authentication)
├─ Spring Validation (JSR-303/JSR-380)
├─ Firebase Admin SDK 9.3.0 (Auth + ID Token Verification)
├─ Mercado Pago SDK 2.1.24 (Pagamentos)
├─ Cloudinary SDK (Image Storage)
├─ MapStruct (DTO Mapping)
├─ Lombok (Boilerplate Reduction)
└─ SpringDoc OpenAPI 2.5.0 (Swagger UI)

Persistence:
├─ MySQL 8.0
├─ Redis 7
├─ HikariCP (Connection Pooling)
└─ Flyway (opcional, não visto no código)

Messaging:
├─ RabbitMQ 3
├─ Spring AMQP
└─ Jackson (JSON Serialization)

Build:
├─ Maven 3.8.x
├─ multi-module project
└─ Docker multi-stage builds

Monitoring/Logging:
├─ SLF4J + Logback
├─ Spring Boot Actuator
└─ (Sem observability completa no código atual)
```

---

### 3.7 Padrões de Design Implementados

```
┌─────────────────────────────────────────────────────────────────────┐
│                      DESIGN PATTERNS                                 │
└─────────────────────────────────────────────────────────────────────┘

1. Microservices Architecture
   └─ 8 serviços independentes, cada um com:
      ├─ Controller (HTTP endpoints)
      ├─ Service (lógica de negócio)
      ├─ Repository (persistência)
      └─ Banco de dados próprio

2. Database per Service
   └─ Cada microserviço possui seu próprio MySQL database
   └─ Evita coupling de dados

3. API Gateway Pattern
   └─ API Gateway centraliza autenticação e roteamento
   └─ Serviços downstream confiam em headers injetados

4. Event-Driven Architecture
   └─ RabbitMQ para comunicação assíncrona
   └─ Notification Service consome eventos de agendamento/pagamento

5. Feign Client (Synchronous Communication)
   └─ Schedule Service chama user-service, barbershop-service
   └─ Payment Service chama schedule-service
   └─ Circuit breaker + retry automático

6. Snapshot Pattern (Denormalization)
   └─ OrderItem copia productName + price na hora da compra
   └─ AppointmentActivity copia activityName + price + duration
   └─ Evita referências quebradas se dados originais mudam/deletam

7. Soft Delete Pattern
   └─ Product.active = false (não deleta fisicamente)
   └─ Mantém histórico de pedidos antigos

8. Repository Pattern
   └─ Spring Data JPA repositories (CRUD + custom queries)
   └─ Abstração da persistência

9. Mapper Pattern
   └─ MapStruct para converter Model ↔ DTO
   └─ Desacoplamento entre camadas

10. DTOs (Data Transfer Objects)
    └─ Separação entre Model (BD) e API (responses)
    └─ Reduz verbosidade, melhora segurança

11. Validation Pattern
    └─ JSR-303 annotations (@NotNull, @Email, etc.)
    └─ Validação automática no @RequestBody

12. Circuit Breaker Pattern (Resilience4j)
    └─ Falhas em Feign clients ativam circuit breaker
    └─ Evita cascata de falhas

13. Caching Pattern
    └─ Redis cache para barber work hours (5 min TTL)
    └─ Reduz chamadas ao user-service

14. Idempotency Pattern
    └─ WebhookLog para webhooks do Mercado Pago
    └─ Evita duplicação se webhook for reprocessado

15. SAGA Pattern (Implicit, via Events)
    └─ Agendamento → Notificação → Pagamento → Confirmação
    └─ Orquestração via RabbitMQ events
```

---

### 3.8 Pontos Críticos e Riscos Técnicos

```
┌─────────────────────────────────────────────────────────────────────┐
│                      RISCOS E MELHORIAS                              │
└─────────────────────────────────────────────────────────────────────┘

🚨 CRÍTICO:

1. Header X-User-Id vs X-User-UID
   ├─ Inconsistência: produto usa X-User-Id
   ├─ Schedule usa headers injetados pelo Gateway
   ├─ AÇÃO: Padronizar para X-User-Id em todos os serviços

2. Webhook de Pagamento Público (sem validação)
   ├─ POST /api/payments/webhook aceita qualquer payload
   ├─ Risco: Spoofing de webhooks
   ├─ AÇÃO: Validar signature do Mercado Pago (SDK suporta)

3. Ausência de Transações Distribuídas
   ├─ Criar agendamento + publicar evento (RabbitMQ)
   ├─ Se RabbitMQ falhar, agendamento fica orfão
   ├─ AÇÃO: Usar outbox pattern ou compensating transactions

4. Erro em StockMovement.orderId = null
   ├─ OrderService tenta atualizar orderId após save
   ├─ Lógica: varredura global de movimentos com orderId=null (INEFICIENTE)
   ├─ AÇÃO: Associar orderId na criação ou usar relacionamento JPA

5. Deduplicação via Redis é frágil
   ├─ Se Redis cai, notificações são duplicadas
   ├─ AÇÃO: Usar transactional outbox ou CDC (Change Data Capture)

⚠️ ALTO:

6. Sem autenticação em endpoints internos (/api/internal/**)
   ├─ Qualquer pessoa pode chamar Feign endpoints
   ├─ AÇÃO: Usar service-to-service authentication (mTLS, JWT service account)

7. Sem rate limiting
   ├─ Usuário pode spam de agendamentos/pedidos
   ├─ AÇÃO: Implementar rate limiting no Gateway ou serviços

8. Sem logging estruturado
   ├─ Logs apenas em console (SLF4J/Logback)
   ├─ AÇÃO: Centralizar em ELK/Splunk/Datadog

9. Sem métricas de negócio
   ├─ Sem visibilidade de KPIs (agendamentos criados, recusas, etc.)
   ├─ AÇÃO: Adicionar Micrometer + Prometheus

10. Timeout em Feign clients não configurado
    ├─ Se user-service cai, schedule-service trava
    ├─ AÇÃO: Configurar ConnectTimeout + ReadTimeout

⚡ MÉDIO:

11. Email service é assíncrono mas sem retry
    ├─ Se send falhar, não há retry automático
    ├─ AÇÃO: Usar Redis queue ou AWS SQS

12. Soft delete não filtra automaticamente
    ├─ Product.active=false não é filtrado nas queries
    ├─ AÇÃO: Adicionar @Where ou @Filter hibernante

13. Sem versionamento de API
    ├─ Mudanças em /api/* quebram clientes
    ├─ AÇÃO: Versionar endpoints (/api/v1/, /api/v2/)

14. Sem contract testing entre serviços
    ├─ Mudança em Feign client pode quebrar consumers
    ├─ AÇÃO: Usar Spring Cloud Contract ou Pact

15. Cache Redis sem invalidação estratégica
    ├─ Se barbeiro muda horário, cache desatualiza em 5 min
    ├─ AÇÃO: Publicar evento para invalidar cache
```

---

### 3.9 Fluxo de Deploy

```
┌─────────────────────────────────────────────────────────────────────┐
│                       CI/CD ESPERADO                                 │
└─────────────────────────────────────────────────────────────────────┘

1. Develop faz commit
   └─ GitHub/GitLab webhook → CI pipeline

2. CI Pipeline (GitHub Actions / GitLab CI)
   ├─ Maven clean package (testa + compila cada módulo)
   ├─ Docker build (imagem multi-stage)
   ├─ Push para Docker Registry
   └─ Deploy para Kubernetes / Docker Swarm

3. Docker Compose (Desenvolvimento Local)
   ├─ discovery-service (Eureka)
   ├─ mysql (auto-create databases via init.sql)
   ├─ rabbitmq (auto-create exchanges/queues)
   ├─ redis
   ├─ api-gateway
   ├─ user-service
   ├─ barbershop-service
   ├─ schedule-service
   ├─ payment-service
   ├─ notification-service
   ├─ product-service
   └─ frontend (Node 20)

4. Healthchecks
   ├─ MySQL: mysqladmin ping
   ├─ RabbitMQ: rabbitmqctl status
   ├─ Redis: redis-cli ping
   ├─ Eureka: GET /actuator/health
   ├─ Serviços: GET /actuator/health (delay 30s para discovery)
```

---

### 3.10 Melhorias Recomendadas (Backlog)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PRODUCT BACKLOG (Priorizado)                      │
└─────────────────────────────────────────────────────────────────────┘

Sprint 1 (Crítico):
├─ Padronizar header de usuário (X-User-Id)
├─ Validar signature em webhook do Mercado Pago
├─ Corrigir lógica de StockMovement.orderId
└─ Adicionar autenticação em endpoints /api/internal/**

Sprint 2 (Alta Prioridade):
├─ Implementar outbox pattern para RabbitMQ
├─ Configurar timeouts e retries em Feign clients
├─ Adicionar rate limiting no API Gateway
├─ Centralizar logs (ELK/Splunk)
└─ Adicionar métricas (Micrometer + Prometheus)

Sprint 3 (Médio):
├─ Implementar versionamento de API (/api/v1/*)
├─ Adicionar contract testing (Spring Cloud Contract)
├─ Melhorar cache invalidation strategy
├─ Email retry via Redis queue
└─ Implementar soft delete filters (Hibernate @Where)

Sprint 4 (Low Priority):
├─ Observabilidade completa (distributed tracing)
├─ Testes de carga (Gatling, k6)
├─ Documentação automática de APIs (AsyncAPI para eventos)
└─ Conformidade LGPD (right-to-forget, data export)
```

---

## 4. MATRIZ DE RASTREABILIDADE

```
┌─────────────────────────────────────────────────────────────────────┐
│         FRF → PRD → TD → Implementação                               │
└─────────────────────────────────────────────────────────────────────┘

RF7: Autenticar usuários via Firebase
├─ PRD: RF7
├─ TD: AuthController.verify(), FirebaseAuthServiceImpl, FirebaseConfig
└─ Implementação: user-service + api-gateway

RF24: Criar agendamentos (validação completa)
├─ PRD: RF24-34
├─ TD: AppointmentService.createAppointment(), AppointmentController
├─ Integração: schedule-service + user-service + barbershop-service + RabbitMQ
└─ Validações: conflito horário, bloqueios, estoque, permissões

RF35-42: Pagamentos
├─ PRD: RF35-42
├─ TD: PaymentService, WebhookController, MercadoPagoConfig
├─ Integração: payment-service + schedule-service + RabbitMQ
└─ Externos: Mercado Pago SDK

RF43-49: Notificações
├─ PRD: RF43-49
├─ TD: NotificationService, NotificationEventListener, DeduplicationService
├─ Integração: notification-service (consome 4 topics RabbitMQ) + Redis
└─ Externos: SMTP (EmailService)

RF50-57: Produtos & Pedidos
├─ PRD: RF50-57
├─ TD: ProductService, OrderService, StockMovementRepository
├─ Integração: product-service (isolado)
└─ Dados: PostgreSQL product_db
```

---

## 5. CONCLUSÃO

O sistema CortaAí é uma **arquitetura de microserviços bem estruturada** com:

✅ **Pontos Fortes:**
- Separação clara de responsabilidades
- Autenticação centralizada (Firebase + Gateway)
- Comunicação assíncrona via RabbitMQ
- Isolamento de dados (DB per service)
- Snapshots para evitar inconsistências
- Soft deletes para auditoria
- Validação completa em pontos críticos

⚠️ **Áreas de Melhoria:**
- Inconsistências em nomes de headers
- Segurança de webhooks
- Falta de transações distribuídas
- Logging e observabilidade básicos
- Sem versionamento de API
- Endpoints internos sem autenticação

📋 **Próximas Ações:**
1. Resolver itens críticos (headers, webhooks, transações)
2. Implementar observabilidade
3. Adicionar cobertura de testes
4. Documentar contatos/contratos entre serviços
5. Preparar plano de escalabilidade

---

**Documento gerado:** Março 2026  
**Escopo:** Backend CortaAí  
**Versão:** 1.0

