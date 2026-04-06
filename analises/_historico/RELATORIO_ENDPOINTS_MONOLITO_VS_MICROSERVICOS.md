# 📋 Relatório Comparativo: Endpoints do Monolito vs. Microserviços

> **Data:** 01 de março de 2026  
> **Branch:** `feature/migracao-microservicos`  
> **Objetivo:** Comparar todos os endpoints que existiam no monolito com os endpoints atuais dos microserviços, identificando o que foi migrado, o que deixou de existir (por mudança de estratégia/arquitetura), e o que ainda falta implementar.

---

## Índice

1. [Legenda](#1-legenda)
2. [Visão Geral da Mudança Arquitetural](#2-visão-geral-da-mudança-arquitetural)
3. [user-service — Autenticação & Perfil](#3-user-service--autenticação--perfil)
4. [barbershop-service — Gestão de Barbearia](#4-barbershop-service--gestão-de-barbearia)
5. [schedule-service — Agendamentos](#5-schedule-service--agendamentos)
6. [payment-service — Pagamentos (NOVO)](#6-payment-service--pagamentos-novo)
7. [notification-service — Notificações (NOVO)](#7-notification-service--notificações-novo)
8. [product-service — E-commerce (NOVO)](#8-product-service--e-commerce-novo)
9. [Endpoints Internos (Inter-Serviço)](#9-endpoints-internos-inter-serviço)
10. [API Gateway — Rotas Configuradas](#10-api-gateway--rotas-configuradas)
11. [O que NÃO vai mais existir (Mudança de Estratégia)](#11-o-que-não-vai-mais-existir-mudança-de-estratégia)
12. [O que FALTA implementar](#12-o-que-falta-implementar)
13. [Resumo Quantitativo](#13-resumo-quantitativo)

---

## 1. Legenda

| Símbolo | Significado |
|---------|-------------|
| ✅ | Migrado e funcionando nos microserviços |
| 🆕 | Endpoint NOVO que não existia no monolito |
| ❌ | Não existe mais (removido por mudança de estratégia) |
| 🔜 | Planejado mas ainda NÃO implementado |
| ⚠️ | Existe mas com diferenças significativas em relação ao monolito |

---

## 2. Visão Geral da Mudança Arquitetural

### Antes (Monolito)
- **1 único backend** Spring Boot (`backend/src`) com todos os controllers, services e repositories
- **1 banco de dados** MySQL (`cortaai_db`) com todas as tabelas juntas
- Relacionamentos diretos via `@ManyToOne`, `@OneToMany` e JOINs SQL
- Autenticação JWT local
- Sem comunicação inter-serviço (tudo no mesmo processo)

### Agora (Microserviços)
- **8 módulos Maven** independentes: `api-gateway`, `discovery-service`, `user-service`, `barbershop-service`, `schedule-service`, `payment-service`, `notification-service`, `product-service`
- **5 bancos de dados** separados (Database per Service): `user_db`, `barbershop_db`, `schedule_db`, `payment_db`, `product_db`
- Comunicação síncrona via **Feign Client** (inter-serviço)
- Comunicação assíncrona via **RabbitMQ** (eventos)
- **Eureka** para service discovery
- **Spring Cloud Gateway** como ponto único de entrada
- Endpoints `/api/internal/**` para comunicação entre serviços (não expostos no Gateway)

### Consequências da Mudança
- **Não existem mais JOINs cruzados** entre entidades de serviços diferentes (ex: Appointment não faz JOIN com Customer)
- **Dados desnormalizados** são usados para evitar chamadas REST em toda listagem (ex: `customerName` salvo direto no Appointment)
- Relacionamentos M:N que cruzavam bancos (como `barber_activities`) precisam de nova estratégia
- Endpoints de "descoberta" que antes faziam JOINs complexos agora fazem chamadas Feign

---

## 3. user-service — Autenticação & Perfil

> **Porta:** 8081 | **Banco:** `user_db` | **Base Path:** `/api/customers`, `/api/barbers`

### 3.1 CustomerController (`/api/customers`)

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `GET` | `/api/customers` | `/api/customers` | ✅ | Lista todos os clientes |
| `GET` | `/api/customers/{id}` | `/api/customers/{id}` | ✅ | Busca cliente por ID |
| `POST` | `/api/customers` (JSON) | `/api/customers/register` (multipart) | ⚠️ | Mudou a rota e passou a aceitar upload de foto no registro |
| `POST` | `/api/customers/login` | `/api/customers/login` | ✅ | Login com email/senha, retorna JWT |
| `PUT` | `/api/customers/{id}` | `/api/customers/me` | ⚠️ | Agora usa `Principal` (JWT) em vez de `{id}` na URL — mais seguro |
| `DELETE` | `/api/customers/{id}` | `/api/customers/me` | ⚠️ | Agora usa `Principal` (JWT) em vez de `{id}` na URL |
| `POST` | *(não existia)* | `/api/customers/me/upload-photo` | 🆕 | Upload de foto de perfil separado do registro |

### 3.2 BarberController (`/api/barbers`)

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `POST` | `/api/barbers` | `/api/barbers/register` | ⚠️ | Rota alterada de `/api/barbers` para `/api/barbers/register` |
| `POST` | `/api/barbers/login` | `/api/barbers/login` | ✅ | Login de barbeiro |
| `PUT` | `/api/barbers/{id}` | `/api/barbers/{id}` | ✅ | Atualizar perfil do barbeiro |
| `GET` | `/api/barbers/{id}` | `/api/barbers/{id}` | ✅ | Buscar barbeiro por ID |
| `GET` | `/api/barbers` | `/api/barbers` | ✅ | Listar todos os barbeiros |
| `GET` | `/api/barbers/barbershop/{barbershopId}` | `/api/barbers/barbershop/{barbershopId}` | ✅ | Listar barbeiros de uma barbearia |
| `DELETE` | `/api/barbers/{id}` | *(não existe)* | ❌ | Remoção de barbeiro agora é via `leave-shop` ou `remove-barber` no barbershop-service |

---

## 4. barbershop-service — Gestão de Barbearia

> **Porta:** 8082 | **Banco:** `barbershop_db` | **Base Path:** `/api/barbershops`

### 4.1 BarbershopController (`/api/barbershops`)

#### Leitura Pública

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `GET` | `/api/barbershops` | `/api/barbershops` | ✅ | Lista todas as barbearias |
| `GET` | `/api/barbershops/{id}` | `/api/barbershops/{shopId}` | ✅ | Busca barbearia por ID |
| `GET` | `/api/barbershops/{id}/activities` | `/api/barbershops/{shopId}/activities` | ✅ | Lista serviços de uma barbearia |

#### Gestão do Dono (Owner)

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `POST` | `/api/barbershops` | `/api/barbershops/register-my-shop` (multipart) | ⚠️ | Rota alterada, agora aceita upload de logo junto ao registro |
| `PUT` | `/api/barbershops/{id}` | `/api/barbershops/my-shop` | ⚠️ | Agora usa `Principal` (JWT) para identificar o dono |
| `DELETE` | `/api/barbershops/{id}` | `/api/barbershops/my-shop/close` | ⚠️ | Rota alterada para `/my-shop/close`, requer confirmação via DTO |
| `POST` | `/api/barbershops/{id}/activities` | `/api/barbershops/my-shop/activities` | ⚠️ | Agora usa `Principal` em vez de `{id}` |
| `PUT` | `/api/barbershops/{id}/activities/{actId}` | `/api/barbershops/my-shop/activities/{activityId}` | ⚠️ | Idem |
| `DELETE` | `/api/barbershops/{id}/activities/{actId}` | `/api/barbershops/my-shop/activities/{activityId}` | ⚠️ | Idem |
| `DELETE` | *(via endpoint direto)* | `/api/barbershops/my-shop/remove-barber/{barberId}` | ✅ | Remover barbeiro da equipe |

#### Join Requests (Solicitação de Entrada)

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `POST` | `/api/barbershops/join` | `/api/barbershops/join-request` | ⚠️ | Barbeiro solicita entrada via CNPJ |
| `GET` | `/api/barbershops/{id}/pending-requests` | `/api/barbershops/my-shop/pending-requests` | ⚠️ | Agora usa `Principal` |
| `POST` | `/api/barbershops/approve/{reqId}` | `/api/barbershops/my-shop/approve-request/{requestId}` | ⚠️ | Agora usa `Principal`, rota mais descritiva |
| `POST` | *(não existia)* | `/api/barbershops/leave-shop` | 🆕 | Barbeiro sai da barbearia voluntariamente |

#### Gestão de Imagens

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `POST` | `/api/barbershops/{id}/logo` | `/api/barbershops/my-shop/upload-logo` | ⚠️ | Agora usa `Principal` |
| `POST` | `/api/barbershops/{id}/banner` | `/api/barbershops/my-shop/upload-banner` | ⚠️ | Agora usa `Principal` |
| `POST` | `/api/barbershops/{id}/activities/{actId}/photo` | `/api/barbershops/my-shop/activities/{activityId}/upload-photo` | ⚠️ | Agora usa `Principal` |
| `POST` | `/api/barbershops/{id}/highlights` | `/api/barbershops/my-shop/highlights` | ⚠️ | Agora usa `Principal` |
| `DELETE` | `/api/barbershops/{id}/highlights/{hlId}` | `/api/barbershops/my-shop/highlights/{highlightId}` | ⚠️ | Agora usa `Principal` |

---

## 5. schedule-service — Agendamentos

> **Porta:** 8083 | **Banco:** `schedule_db` | **Base Path:** `/api/appointments`

### 5.1 AppointmentController (`/api/appointments`)

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `POST` | `/api/appointments` | `/api/appointments` | ✅ | Criar agendamento (agora valida via Feign em vez de JOIN direto) |
| `GET` | `/api/appointments/{id}` | `/api/appointments/{id}` | ✅ | Buscar agendamento por ID |
| `GET` | `/api/appointments/my-appointments` | `/api/appointments/my-appointments` | ✅ | Meus agendamentos (Customer ou Barber) |
| `GET` | `/api/appointments/barber/{barberId}` | `/api/appointments/barber/{barberId}?date=` | ✅ | Agenda do barbeiro (agora com filtro por data obrigatório) |
| `GET` | `/api/appointments/barbershop/{shopId}` | `/api/appointments/barbershop/{shopId}?date=` | ✅ | Agenda da barbearia (com filtro por data) |
| `PUT` | `/api/appointments/{id}/cancel` | `/api/appointments/{id}/cancel` | ✅ | Cancelar agendamento |
| `PUT` | `/api/appointments/{id}/conclude` | `/api/appointments/{id}/conclude` | ✅ | Concluir agendamento |
| `PUT` | `/api/appointments/{id}/confirm` | `/api/appointments/{id}/confirm` | ✅ | Confirmar agendamento |
| `GET` | `/api/appointments/availability` | `/api/appointments/availability?barberId=&date=` | ✅ | Consultar horários disponíveis |
| `PUT` | `/api/appointments/{id}` (update genérico) | *(não existe)* | ❌ | Substituído por ações específicas (`/cancel`, `/conclude`, `/confirm`) — mais seguro |

### 5.2 BarberBlockController (`/api/appointments/barber-blocks`) — 🆕

| Método | Endpoint (Monolito) | Endpoint (Microserviço) | Status | Observações |
|--------|---------------------|-------------------------|--------|-------------|
| `POST` | *(não existia)* | `/api/appointments/barber-blocks` | 🆕 | Criar bloqueio de agenda (férias, folga) |
| `GET` | *(não existia)* | `/api/appointments/barber-blocks?barberId=&date=` | 🆕 | Listar bloqueios do barbeiro |
| `DELETE` | *(não existia)* | `/api/appointments/barber-blocks/{id}` | 🆕 | Remover bloqueio |

---

## 6. payment-service — Pagamentos (NOVO)

> **Porta:** 8084 | **Banco:** `payment_db` | **Base Path:** `/api/payments`  
> ⚡ Este serviço é inteiramente NOVO — não existia no monolito.

### 6.1 PaymentController (`/api/payments`)

| Método | Endpoint | Status | Observações |
|--------|----------|--------|-------------|
| `POST` | `/api/payments/create` | 🆕 | Cria pagamento para um agendamento via Mercado Pago. Retorna URL de checkout |
| `GET` | `/api/payments/{id}` | 🆕 | Busca transação por ID |
| `GET` | `/api/payments/my-payments` | 🆕 | Lista pagamentos do usuário logado (via header `X-User-Id`) |

### 6.2 WebhookController (`/api/payments`)

| Método | Endpoint | Status | Observações |
|--------|----------|--------|-------------|
| `POST` | `/api/payments/webhook` | 🆕 | Recebe notificações do Mercado Pago. Endpoint público (sem auth) |

---

## 7. notification-service — Notificações (NOVO)

> **Porta:** 8085 | **Armazenamento:** MySQL + Redis | **Base Path:** `/api/notifications`  
> ⚡ Este serviço é inteiramente NOVO — não existia no monolito.

### 7.1 NotificationController (`/api/notifications`)

| Método | Endpoint | Status | Observações |
|--------|----------|--------|-------------|
| `GET` | `/api/notifications/my-notifications` | 🆕 | Lista notificações do usuário (via header `X-User-Id`) |
| `PUT` | `/api/notifications/{id}/read` | 🆕 | Marca notificação como lida |
| `GET` | `/api/notifications/unread-count` | 🆕 | Retorna contagem de notificações não lidas |

### 7.2 Listeners RabbitMQ (não são endpoints REST, mas recebem eventos)

| Evento recebido | Ação | Status |
|------------------|------|--------|
| `AppointmentCreatedEvent` | Cria notificação para cliente e barbeiro | 🆕 |
| `AppointmentCancelledEvent` | Cria notificação de cancelamento | 🆕 |
| `AppointmentConcludedEvent` | Cria notificação de conclusão | 🆕 |
| `PaymentApprovedEvent` | Cria notificação de pagamento aprovado | 🆕 |

---

## 8. product-service — E-commerce (NOVO)

> **Porta:** 8086 | **Banco:** `product_db` | **Base Path:** `/api/products`, `/api/orders`  
> ⚡ Este serviço é inteiramente NOVO — não existia no monolito.

### 8.1 ProductController (`/api/products`)

| Método | Endpoint | Status | Observações |
|--------|----------|--------|-------------|
| `POST` | `/api/products` | 🆕 | Criar produto (dono da barbearia) |
| `GET` | `/api/products?barbershopId=` | 🆕 | Listar produtos de uma barbearia (somente ativos) |
| `GET` | `/api/products/{id}` | 🆕 | Buscar produto por ID |
| `PUT` | `/api/products/{id}` | 🆕 | Atualizar produto |
| `DELETE` | `/api/products/{id}` | 🆕 | Desativar produto (soft delete) |

### 8.2 OrderController (`/api/orders`)

| Método | Endpoint | Status | Observações |
|--------|----------|--------|-------------|
| `POST` | `/api/orders` | 🆕 | Criar pedido de produtos (via header `X-User-Id`) |
| `GET` | `/api/orders/my-orders` | 🆕 | Listar pedidos do cliente |
| `GET` | `/api/orders/shop-orders?barbershopId=` | 🆕 | Listar pedidos da barbearia (para o dono) |
| `PUT` | `/api/orders/{id}/status?status=` | 🆕 | Atualizar status do pedido (PENDING → PAID → PREPARING → READY → DELIVERED) |

---

## 9. Endpoints Internos (Inter-Serviço)

> Estes endpoints são usados **exclusivamente** para comunicação entre microserviços via Feign Client.  
> **NÃO são expostos** pelo API Gateway. Não existiam no monolito (não eram necessários).

### 9.1 InternalUserController (`/api/internal/users`) — user-service

| Método | Endpoint | Consumido por | Descrição |
|--------|----------|---------------|-----------|
| `GET` | `/api/internal/users/{id}` | schedule-service, barbershop-service | Busca usuário (Customer ou Barber) por UUID |
| `GET` | `/api/internal/users/by-email/{email}` | barbershop-service | Busca usuário por email |
| `PUT` | `/api/internal/users/{id}/barbershop` | barbershop-service | Atualiza `barbershopId` do barbeiro (ao aprovar JoinRequest) |

### 9.2 InternalBarbershopController (`/api/internal/barbershops`) — barbershop-service

| Método | Endpoint | Consumido por | Descrição |
|--------|----------|---------------|-----------|
| `GET` | `/api/internal/barbershops/{id}` | schedule-service | Busca info básica da barbearia |
| `GET` | `/api/internal/barbershops/{shopId}/activities?ids=` | schedule-service | Busca atividades por lista de IDs (para snapshot no agendamento) |

### 9.3 InternalAppointmentController (`/api/internal/appointments`) — schedule-service

| Método | Endpoint | Consumido por | Descrição |
|--------|----------|---------------|-----------|
| `GET` | `/api/internal/appointments/{id}` | payment-service | Busca dados do agendamento para criar pagamento |
| `PUT` | `/api/internal/appointments/{id}/payment-status` | payment-service | Atualiza status de pagamento no agendamento |

---

## 10. API Gateway — Rotas Configuradas

> **Porta:** 8080 | **Tecnologia:** Spring Cloud Gateway (reativo)  
> O Gateway é o **único ponto de entrada** para o frontend. Ele roteia para os serviços via Eureka.

| Rota no Gateway | Serviço Destino | Paths Roteados |
|------------------|-----------------|----------------|
| `user-service` | `lb://user-service` | `/api/customers/**`, `/api/barbers/**`, `/api/auth/**` |
| `barbershop-service` | `lb://barbershop-service` | `/api/barbershops/**` |
| `schedule-service` | `lb://schedule-service` | `/api/appointments/**` |
| `payment-service` | `lb://payment-service` | `/api/payments/**` |
| `product-service` | `lb://product-service` | `/api/products/**`, `/api/orders/**` |
| `notification-service` | `lb://notification-service` | `/api/notifications/**` |

**Rotas de Swagger (api-docs):**

| Rota | Serviço |
|------|---------|
| `/v3/api-docs/user-service` | user-service |
| `/v3/api-docs/barbershop-service` | barbershop-service |
| `/v3/api-docs/schedule-service` | schedule-service |
| `/v3/api-docs/payment-service` | payment-service |
| `/v3/api-docs/notification-service` | notification-service |
| `/v3/api-docs/product-service` | product-service |

> **Swagger UI Agregado:** Acessível via `http://gateway:8080/webjars/swagger-ui/index.html` com dropdown para selecionar cada serviço.

---

## 11. O que NÃO vai mais existir (Mudança de Estratégia)

Estes endpoints/funcionalidades existiam no monolito mas foram **intencionalmente removidos ou substituídos** por conta da nova arquitetura de microserviços:

### 11.1 Endpoints Eliminados

| Endpoint Antigo | Motivo da Remoção | Substituto |
|-----------------|-------------------|------------|
| `PUT /api/appointments/{id}` (update genérico) | Inseguro — permitia alterar qualquer campo. Na nova arquitetura, ações são explícitas e validadas | `PUT /{id}/cancel`, `PUT /{id}/conclude`, `PUT /{id}/confirm` |
| `DELETE /api/barbers/{id}` (exclusão direta) | A remoção de barbeiro agora é tratada pelo barbershop-service como ação de negócio, não como CRUD simples | `POST /api/barbershops/leave-shop` (barbeiro sai) ou `DELETE /api/barbershops/my-shop/remove-barber/{id}` (dono remove) |
| `PUT /api/barbershops/{id}` (via ID na URL) | Inseguro — qualquer um com o ID poderia tentar editar. Agora usa JWT para identificar o dono | `PUT /api/barbershops/my-shop` (via `Principal`) |
| `PUT /api/customers/{id}` (via ID na URL) | Idem — inseguro | `PUT /api/customers/me` (via `Principal`) |
| `DELETE /api/customers/{id}` (via ID na URL) | Idem — inseguro | `DELETE /api/customers/me` (via `Principal`) |

### 11.2 Conceitos Arquiteturais Eliminados

| Conceito Antigo | Por que não existe mais | Novo Conceito |
|-----------------|------------------------|---------------|
| **JOINs diretos** entre Customer/Barber/Barbershop/Appointment | Bancos separados (Database per Service) — JOINs entre schemas são impossíveis | **Feign Client** para validação síncrona + **dados desnormalizados** para leitura |
| **`@ManyToOne` Appointment → Customer** | Customer vive no `user_db`, Appointment no `schedule_db` | `customerId` (UUID) + `customerName` (snapshot desnormalizado) |
| **`@ManyToOne` Appointment → Barber** | Idem | `barberId` (UUID) + `barberName` (snapshot) |
| **`@ManyToOne` Appointment → Barbershop** | Idem | `barbershopId` (UUID) + `barbershopName` (snapshot) |
| **`@ManyToMany` Barber ↔ Activity** (tabela `barber_activities` com FK nos dois lados) | Barber está no `user_db`, Activity no `barbershop_db` — FK cruzada impossível | `barber_activities` fica no `barbershop_db` com `barber_id` como UUID simples (sem FK) — validação via Feign |
| **Módulo monolítico** (`backend/src`) | Dividido em 6 microserviços de negócio + 2 de infraestrutura | `user-service`, `barbershop-service`, `schedule-service`, `payment-service`, `notification-service`, `product-service`, `api-gateway`, `discovery-service` |

---

## 12. O que FALTA implementar

### 12.1 Prioridade ALTA 🔴 — Funcionalidade do monolito que está incompleta

| Item | Serviço | Detalhe | Impacto |
|------|---------|---------|---------|
| **Model `BarberActivity`** (tabela pivô M:N) | barbershop-service | Não existe model, repository, nem endpoints para vincular barbeiro a serviço. Sem isso, não se sabe quais serviços cada barbeiro pode executar | 🔴 Bloqueia validação correta na criação de agendamento |
| **Endpoint `getBarberActivityIds`** | barbershop-service (internal) | Planejado no Feign do schedule-service, mas o endpoint no barbershop-service não existe. Necessário: `GET /api/internal/barbershops/{shopId}/barber-activities/{barberId}` | 🔴 Bloqueado pelo item acima |
| **Proteção dos endpoints internos** | Todos os serviços | Os endpoints `/api/internal/**` estão abertos — qualquer pessoa pode chamá-los. Devem ser protegidos por header `X-Internal-Token` e NÃO roteados pelo Gateway | 🔴 Vulnerabilidade de segurança |

### 12.2 Prioridade MÉDIA 🟡 — Campos faltantes nos models

| Item | Serviço | Detalhe |
|------|---------|---------|
| Campos `city`, `state`, `latitude`, `longitude`, `phone`, `description` no **Barbershop** | barbershop-service | Necessários para busca por geolocalização e informações de contato |
| Campos `averageRating`, `totalReviews` no **Barbershop** | barbershop-service | Necessários quando o sistema de avaliações for implementado |
| Campo `isActive` no **Barbershop** | barbershop-service | Para soft delete |
| Campos `category`, `description`, `isActive` na **Activity** | barbershop-service | Para filtragem e soft delete de serviços |
| Campo `totalDuration` no **Appointment** | schedule-service | Duração total do agendamento em minutos |
| Campo `cancellationReason` no **Appointment** | schedule-service | Motivo do cancelamento |
| Campo `paymentId` no **Appointment** | schedule-service | Referência cruzada ao payment-service |
| Campos `barbershopId`, `orderId`, `paymentMethod`, `platformFee`, `sellerAmount`, `mpStatus`, `mpStatusDetail` na **Transaction** | payment-service | Necessários para split de pagamento e e-commerce |
| Campos `sku`, `minStock`, `imagePublicId` no **Product** | product-service | SKU, alerta de estoque baixo, e link Cloudinary |

### 12.3 Prioridade MÉDIA 🟡 — Features planejadas

| Item | Serviço | Detalhe |
|------|---------|---------|
| **Model `BarbershopWorkingHours`** | barbershop-service | Horário de funcionamento da barbearia por dia da semana (NOVO recurso) |
| **Model `Review`** | barbershop-service | Sistema de avaliações com nota e comentário (NOVO recurso) |
| **Validação JWT centralizada no Gateway** | api-gateway | Hoje só o user-service tem `SecurityConfig`. O Gateway deveria validar JWT e propagar `X-User-Id` para todos os serviços downstream |
| **Resilience4j** (Circuit Breaker + Retry) | Todos os serviços com Feign | Se um serviço cair, as chamadas Feign travam sem fallback |
| **Rate Limiting** | api-gateway | `RequestRateLimiter` com Redis para proteger contra abuso |

### 12.4 Prioridade BAIXA 🟢 — Features novas futuras

| Item | Serviço | Detalhe |
|------|---------|---------|
| **Social Login** (Google/Facebook/GitHub) | user-service | OAuth2/OIDC com `spring-boot-starter-oauth2-client`. Campos `oauthProvider`, `oauthProviderId` no model |
| **Refresh Tokens** | user-service | Tabela `refresh_tokens` com rotação segura |
| **Tabela unificada `users`** (Customer + Barber) | user-service | Plano sugere, mas não é obrigatório. Mantidas tabelas separadas |
| **Envio real de Email** (Resend API) | notification-service | Hoje só salva notificação no banco — não envia email de verdade |
| **Envio real de Push** (Firebase Cloud Messaging) | notification-service | Idem |
| **WebSocket** (STOMP) para notificações em tempo real | notification-service | In-App real-time |
| **Scheduler de lembretes** (`@Scheduled`) | notification-service | "Lembrete: seu corte é daqui 1 hora" |
| **Integração pedidos → payment-service** | product-service | Checkout de produtos via Mercado Pago |
| **Alerta de estoque baixo** | product-service | Notificação ao dono quando `stockQuantity < minStock` |
| **Reembolso automático** | payment-service | Cancelamento dentro de X horas gera reembolso |
| **Prometheus + Grafana** | Infraestrutura | Monitoramento de métricas |
| **Nginx + SSL (Let's Encrypt)** | Infraestrutura | Reverse proxy para produção |
| **Dashboards de BI** | Vários serviços | Endpoints de agregação (stats, receita, performance) |

---

## 13. Resumo Quantitativo

### Endpoints por Serviço

| Serviço | Endpoints Públicos | Endpoints Internos | Total |
|---------|--------------------|--------------------|-------|
| **user-service** | 13 | 3 | 16 |
| **barbershop-service** | 19 | 2 | 21 |
| **schedule-service** | 12 | 2 | 14 |
| **payment-service** | 4 | 0 | 4 |
| **notification-service** | 3 | 0 | 3 |
| **product-service** | 9 | 0 | 9 |
| **TOTAL** | **60** | **7** | **67** |

### Comparação Monolito vs. Microserviços

| Métrica | Monolito | Microserviços |
|---------|----------|---------------|
| Endpoints de negócio | ~35 | 60 (públicos) |
| Endpoints internos (inter-serviço) | 0 (desnecessários) | 7 |
| Bancos de dados | 1 (`cortaai_db`) | 5 schemas separados |
| Módulos de negócio | 1 (monolítico) | 6 microserviços |
| Módulos de infra | 0 | 2 (gateway + discovery) |
| Comunicação assíncrona | Nenhuma | RabbitMQ (4 tipos de evento) |
| Cache | Nenhum | Redis |
| API Documentation (Swagger) | 1 spec | 6 specs agregados no Gateway |

### Status da Migração

| Categoria | Quantidade |
|-----------|------------|
| ✅ Endpoints migrados com sucesso | 25 |
| ⚠️ Endpoints migrados com mudanças | 14 |
| 🆕 Endpoints novos | 28 |
| ❌ Endpoints removidos intencionalmente | 5 |
| 🔜 Endpoints/features por implementar | 15+ |

---

> **Este relatório reflete o estado do código em 01/03/2026** na branch `feature/migracao-microservicos`.  
> Deve ser atualizado conforme novos endpoints forem implementados.
