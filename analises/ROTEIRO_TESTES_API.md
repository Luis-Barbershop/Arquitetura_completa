# 🧪 Roteiro de Testes — API CortaAi

> **Base URL Local:** `http://localhost:8080`  
> **Base URL Produção:** `https://api.cortaai.shop`  
> **Ferramenta:** Hoppscotch / Postman / Insomnia (importar `openapi.json` ou `collection.json`)  
> **Ambiente:** Importar `collectionEnv.json` e ativar o ambiente **CortaAi - Local Dev**

---

## 📋 Índice

1. [Pré-requisitos](#1-pré-requisitos)
2. [Fase 1 — Auth: Registro e Login](#2-fase-1--auth-registro-e-login)
3. [Fase 2 — Completar Perfis](#3-fase-2--completar-perfis)
4. [Fase 3 — Fluxo Owner: Criar Barbearia](#4-fase-3--fluxo-owner-criar-barbearia)
5. [Fase 4 — Fluxo Barbeiro: Entrar na Barbearia](#5-fase-4--fluxo-barbeiro-entrar-na-barbearia)
6. [Fase 5 — Atividades e Serviços](#6-fase-5--atividades-e-serviços)
7. [Fase 6 — Vincular Atividades ao Barbeiro](#7-fase-6--vincular-atividades-ao-barbeiro)
8. [Fase 7 — Agendamento Completo](#8-fase-7--agendamento-completo)
9. [Fase 8 — Pagamentos](#9-fase-8--pagamentos)
10. [Fase 9 — Produtos e Pedidos](#10-fase-9--produtos-e-pedidos)
11. [Fase 10 — Notificações](#11-fase-10--notificações)
12. [Fase 11 — Uploads de Mídia](#12-fase-11--uploads-de-mídia)
13. [Fase 12 — Operações Destrutivas](#13-fase-12--operações-destrutivas)
14. [Checklist Final](#14-checklist-final)

---

## 1. Pré-requisitos

- [ ] Docker rodando (`docker-compose up -d`)
- [ ] Serviços saudáveis: `api-gateway`, `user-service`, `barbershop-service`, `schedule-service`, `payment-service`, `product-service`, `notification-service`
- [ ] Banco PostgreSQL inicializado (`init.sql` executado)
- [ ] Firebase configurado (projeto ativo, autenticação por email habilitada)
- [ ] Ambiente `collectionEnv.json` importado e ativo na ferramenta de teste

**Variáveis que serão preenchidas ao longo dos testes:**

| Variável | Preenchida em |
|---|---|
| `token` | Fase 1 — Login |
| `customer_token` | Fase 1 — Login Cliente |
| `barber_token` | Fase 1 — Login Barbeiro |
| `owner_token` | Fase 1 — Login Owner |
| `customer_id` | Fase 2 — Completar Perfil Cliente |
| `barber_id` | Fase 2 — Completar Perfil Barbeiro |
| `barbershop_id` | Fase 3 — Criar Barbearia |
| `activity_id` | Fase 5 — Criar Atividade |
| `appointment_id` | Fase 7 — Criar Agendamento |
| `payment_id` | Fase 8 — Criar Pagamento |
| `product_id` | Fase 9 — Criar Produto |
| `order_id` | Fase 9 — Criar Pedido |
| `notification_id` | Fase 10 — Listar Notificações |
| `join_request_id` | Fase 4 — Solicitar Entrada |
| `block_id` | Fase 7 — Criar Bloqueio |
| `highlight_id` | Fase 11 — Adicionar Highlight |

---

## 2. Fase 1 — Auth: Registro e Login

### 1.1 Registrar Cliente

```
POST /api/auth/email/register
```

**Body:**
```json
{
  "email": "<<customer_email>>",
  "password": "<<customer_password>>",
  "userType": "CUSTOMER"
}
```

- [ ] Resposta `200` ou `201`
- [ ] Sem erro de "email já cadastrado" (se já existir, pule para 1.4)

---

### 1.2 Registrar Barbeiro

```
POST /api/auth/email/register
```

**Body:**
```json
{
  "email": "<<barber_email>>",
  "password": "<<barber_password>>",
  "userType": "BARBER"
}
```

- [ ] Resposta `200` ou `201`

---

### 1.3 Registrar Owner

```
POST /api/auth/email/register
```

**Body:**
```json
{
  "email": "<<owner_email>>",
  "password": "<<owner_password>>",
  "userType": "BARBER"
}
```

> ⚠️ Owner é registrado como `BARBER` — a flag `isOwner: true` é definida no complete-profile.

- [ ] Resposta `200` ou `201`

---

### 1.4 Login Cliente

```
POST /api/auth/email/login
```

**Body:**
```json
{
  "email": "<<customer_email>>",
  "password": "<<customer_password>>"
}
```

- [ ] Resposta `200` com `idToken` no body
- [ ] Salvar `idToken` na variável `customer_token`
- [ ] Salvar `localId` na variável `customer_id` (provisório)

---

### 1.5 Login Barbeiro

```
POST /api/auth/email/login
```

**Body:**
```json
{
  "email": "<<barber_email>>",
  "password": "<<barber_password>>"
}
```

- [ ] Resposta `200` com `idToken`
- [ ] Salvar em `barber_token`

---

### 1.6 Login Owner

```
POST /api/auth/email/login
```

**Body:**
```json
{
  "email": "<<owner_email>>",
  "password": "<<owner_password>>"
}
```

- [ ] Resposta `200` com `idToken`
- [ ] Salvar em `owner_token`

---

### 1.7 Verificar Token (opcional)

```
POST /api/auth/verify
```

**Body:**
```json
{
  "idToken": "<<customer_token>>",
  "userType": "CUSTOMER"
}
```

- [ ] Resposta `200`

---

### 1.8 GET /auth/me

```
GET /api/auth/me
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `200` com dados do perfil
- [ ] `id`, `email`, `userType: "CUSTOMER"` presentes

---

## 3. Fase 2 — Completar Perfis

### 2.1 Completar Perfil Cliente

```
POST /api/auth/customers/complete-profile
Authorization: Bearer <<customer_token>>
```

**Body:**
```json
{
  "name": "Gabriel Teste",
  "tell": "11999999999",
  "documentCPF": "12345678901"
}
```

- [ ] Resposta `200`
- [ ] Salvar o `id` retornado em `customer_id`

---

### 2.2 Completar Perfil Barbeiro

```
POST /api/auth/barbers/complete-profile
Authorization: Bearer <<barber_token>>
```

**Body:**
```json
{
  "name": "Barbeiro Teste",
  "tell": "11988887777",
  "documentCPF": "98765432100",
  "workStartTime": "08:00",
  "workEndTime": "18:00",
  "isOwner": false
}
```

- [ ] Resposta `200`
- [ ] Salvar `id` retornado em `barber_id`

---

### 2.3 Completar Perfil Owner

```
POST /api/auth/barbers/complete-profile
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "name": "Owner Teste",
  "tell": "11977776666",
  "documentCPF": "11122233344",
  "workStartTime": "08:00",
  "workEndTime": "20:00",
  "isOwner": true
}
```

- [ ] Resposta `200`
- [ ] `isOwner: true` confirmado

---

## 4. Fase 3 — Fluxo Owner: Criar Barbearia

### 3.1 Registrar Barbearia

```
POST /api/barbershops/register-my-shop
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form fields:**
| Campo | Valor |
|---|---|
| `name` | `Barbearia Teste` |
| `cnpj` | `12345678000199` |
| `address` | `Rua das Flores, 123 - SP` |
| `logo` | *(arquivo de imagem — opcional)* |

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` retornado em `barbershop_id`

---

### 3.2 Buscar Barbearia Criada

```
GET /api/barbershops/<<barbershop_id>>
```

- [ ] Resposta `200` com dados da barbearia
- [ ] `name`, `cnpj`, `address` corretos
- [ ] Endpoint **público** (sem token) ✅

---

### 3.3 Listar Todas as Barbearias

```
GET /api/barbershops
```

- [ ] Resposta `200` com array
- [ ] Barbearia criada aparece na lista
- [ ] Endpoint **público** ✅

---

### 3.4 Atualizar Barbearia

```
PUT /api/barbershops/my-shop
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "name": "Barbearia Teste Atualizada",
  "address": "Av. Paulista, 1000 - SP"
}
```

- [ ] Resposta `200` com dados atualizados

---

## 5. Fase 4 — Fluxo Barbeiro: Entrar na Barbearia

### 4.1 Barbeiro Solicita Entrada

```
POST /api/barbershops/join-request
Authorization: Bearer <<barber_token>>
```

**Body:**
```json
{
  "cnpj": "12345678000199"
}
```

- [ ] Resposta `200`
- [ ] Salvar `id` da solicitação em `join_request_id`

---

### 4.2 Owner Lista Solicitações Pendentes

```
GET /api/barbershops/my-shop/pending-requests
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `200` com array de solicitações
- [ ] Solicitação do barbeiro aparece
- [ ] Confirmar `join_request_id` salvo

---

### 4.3 Owner Aprova Solicitação

```
POST /api/barbershops/my-shop/approve-request/<<join_request_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `200`

---

### 4.4 Listar Barbeiros da Barbearia

```
GET /api/barbers/barbershop/<<barbershop_id>>
```

- [ ] Resposta `200`
- [ ] Barbeiro aprovado aparece na lista
- [ ] Endpoint **público** ✅

---

## 6. Fase 5 — Atividades e Serviços

### 5.1 Criar Atividade

```
POST /api/barbershops/my-shop/activities
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "activityName": "Corte Degradê",
  "price": 45.00,
  "durationMinutes": 40
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` retornado em `activity_id`

---

### 5.2 Listar Atividades da Barbearia

```
GET /api/barbershops/<<barbershop_id>>/activities
```

- [ ] Resposta `200` com array
- [ ] Atividade criada aparece
- [ ] Endpoint **público** ✅

---

### 5.3 Atualizar Atividade

```
PUT /api/barbershops/my-shop/activities/<<activity_id>>
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "activityName": "Corte Degradê Premium",
  "price": 55.00,
  "durationMinutes": 45
}
```

- [ ] Resposta `200` com dados atualizados

---

## 7. Fase 6 — Vincular Atividades ao Barbeiro

### 6.1 Barbeiro Vincula Atividades

```
POST /api/barbers/me/assign-activities
Authorization: Bearer <<barber_token>>
```

**Body:**
```json
{
  "activityIds": ["<<activity_id>>"]
}
```

- [ ] Resposta `200`

---

### 6.2 Barbeiro Consulta Suas Atividades

```
GET /api/barbers/me/my-activities
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200` com lista de atividades
- [ ] Atividade vinculada aparece

---

## 8. Fase 7 — Agendamento Completo

### 7.1 Consultar Disponibilidade

```
GET /api/appointments/availability?barberId=<<barber_id>>&date=2026-04-10
```

- [ ] Resposta `200` com slots disponíveis
- [ ] Slots refletem `workStartTime` / `workEndTime` do barbeiro

---

### 7.2 Criar Agendamento

```
POST /api/appointments
Authorization: Bearer <<customer_token>>
```

**Body:**
```json
{
  "customerId": "<<customer_id>>",
  "barberId": "<<barber_id>>",
  "barbershopId": "<<barbershop_id>>",
  "activityIds": ["<<activity_id>>"],
  "startTime": "2026-04-10T10:00:00"
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `appointment_id`

---

### 7.3 Buscar Agendamento por ID

```
GET /api/appointments/<<appointment_id>>
```

- [ ] Resposta `200` com dados do agendamento
- [ ] Status inicial: `PENDING` ou `SCHEDULED`

---

### 7.4 Listar Meus Agendamentos (Cliente)

```
GET /api/appointments/my-appointments
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `200` com array
- [ ] Agendamento criado aparece

---

### 7.5 Listar Agendamentos do Barbeiro por Data

```
GET /api/appointments/barber/<<barber_id>>?date=2026-04-10
```

- [ ] Resposta `200` com agendamentos do dia

---

### 7.6 Listar Agendamentos da Barbearia por Data

```
GET /api/appointments/barbershop/<<barbershop_id>>?date=2026-04-10
```

- [ ] Resposta `200`

---

### 7.7 Barbeiro Confirma Agendamento

```
PUT /api/appointments/<<appointment_id>>/confirm
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200`
- [ ] Status muda para `CONFIRMED`

---

### 7.8 Criar Bloqueio de Horário

```
POST /api/appointments/barber-blocks
Authorization: Bearer <<barber_token>>
```

**Body:**
```json
{
  "barberId": "<<barber_id>>",
  "startTime": "2026-04-10T12:00:00",
  "endTime": "2026-04-10T13:00:00",
  "reason": "Almoço"
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `block_id`

---

### 7.9 Listar Bloqueios

```
GET /api/appointments/barber-blocks?barberId=<<barber_id>>&date=2026-04-10
```

- [ ] Resposta `200` com bloqueio criado

---

### 7.10 Deletar Bloqueio

```
DELETE /api/appointments/barber-blocks/<<block_id>>
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `204`
- [ ] Bloqueio não aparece mais na listagem

---

## 9. Fase 8 — Pagamentos

### 8.1 Criar Pagamento

```
POST /api/payments/create
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

**Body:**
```json
{
  "appointmentId": "<<appointment_id>>"
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `payment_id`
- [ ] Link de pagamento MercadoPago retornado (se integrado)

---

### 8.2 Buscar Pagamento por ID

```
GET /api/payments/<<payment_id>>
```

- [ ] Resposta `200` com status do pagamento

---

### 8.3 Listar Meus Pagamentos

```
GET /api/payments/my-payments
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200` com array de pagamentos

---

### 8.4 Webhook (simulação)

```
POST /api/payments/webhook
```

**Body:**
```json
{
  "type": "payment",
  "data": { "id": "12345678" }
}
```

- [ ] Resposta `200`
- [ ] Endpoint **público** (sem token) ✅

---

### 8.5 Concluir Agendamento (após pagamento)

```
PUT /api/appointments/<<appointment_id>>/conclude
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200`
- [ ] Status muda para `COMPLETED` ou `CONCLUDED`

---

## 10. Fase 9 — Produtos e Pedidos

### 9.1 Criar Produto

```
POST /api/products
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "barbershopId": "<<barbershop_id>>",
  "name": "Pomada Modeladora",
  "description": "Fixação forte, brilho natural",
  "price": 29.90,
  "category": "HAIR_PRODUCT",
  "stockQuantity": 50,
  "imageUrl": ""
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `product_id`

---

### 9.2 Listar Produtos da Barbearia

```
GET /api/products?barbershopId=<<barbershop_id>>
```

- [ ] Resposta `200` com produto criado

---

### 9.3 Buscar Produto por ID

```
GET /api/products/<<product_id>>
```

- [ ] Resposta `200`

---

### 9.4 Atualizar Produto

```
PUT /api/products/<<product_id>>
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "name": "Pomada Pro Max",
  "price": 34.90
}
```

- [ ] Resposta `200`

---

### 9.5 Criar Pedido

```
POST /api/orders
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

**Body:**
```json
{
  "barbershopId": "<<barbershop_id>>",
  "items": [
    {
      "productId": "<<product_id>>",
      "quantity": 2
    }
  ]
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `order_id`

---

### 9.6 Listar Meus Pedidos (Cliente)

```
GET /api/orders/my-orders
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200` com pedido criado

---

### 9.7 Listar Pedidos da Barbearia

```
GET /api/orders/shop-orders?barbershopId=<<barbershop_id>>
```

- [ ] Resposta `200`

---

### 9.8 Atualizar Status do Pedido

```
PUT /api/orders/<<order_id>>/status
Authorization: Bearer <<owner_token>>
Content-Type: text/plain
```

**Body (texto puro):**
```
DELIVERED
```

> Valores válidos: `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`

- [ ] Resposta `200` com status atualizado

---

### 9.9 Deletar Produto (soft delete)

```
DELETE /api/products/<<product_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

## 11. Fase 10 — Notificações

### 10.1 Listar Minhas Notificações

```
GET /api/notifications/my-notifications
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200` com array de notificações
- [ ] Salvar `id` de uma notificação em `notification_id`

> 💡 Notificações são geradas automaticamente por agendamentos, pagamentos e mudanças de status.

---

### 10.2 Contar Notificações Não Lidas

```
GET /api/notifications/unread-count
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200` com contagem numérica

---

### 10.3 Marcar Notificação como Lida

```
PUT /api/notifications/<<notification_id>>/read
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200`
- [ ] Contagem de não lidas diminui (verificar 10.2 novamente)

---

## 12. Fase 11 — Uploads de Mídia

### 11.1 Upload de Logo da Barbearia

```
POST /api/barbershops/my-shop/upload-logo
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → arquivo `.jpg` ou `.png`

- [ ] Resposta `200` com URL da imagem

---

### 11.2 Upload de Banner

```
POST /api/barbershops/my-shop/upload-banner
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → arquivo `.jpg` ou `.png`

- [ ] Resposta `200` com URL do banner

---

### 11.3 Adicionar Highlight

```
POST /api/barbershops/my-shop/highlights
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → arquivo `.jpg` ou `.png`

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `highlight_id`

---

### 11.4 Deletar Highlight

```
DELETE /api/barbershops/my-shop/highlights/<<highlight_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 11.5 Upload de Foto de Atividade

```
POST /api/barbershops/my-shop/activities/<<activity_id>>/upload-photo
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → arquivo `.jpg` ou `.png`

- [ ] Resposta `200` com URL da foto

---

### 11.6 Upload de Foto do Cliente

```
POST /api/customers/me/upload-photo
Authorization: Bearer <<customer_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → arquivo `.jpg` ou `.png`

- [ ] Resposta `200` com URL da foto

---

## 13. Fase 12 — Operações Destrutivas

> ⚠️ **Execute estas etapas por último!** Elas removem dados criados durante os testes.

### 12.1 Cancelar Agendamento (Cliente)

> Use um agendamento diferente do utilizado nas fases 8 e 8.5.

```
PUT /api/appointments/<<appointment_id>>/cancel
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `200`
- [ ] Status muda para `CANCELLED`

---

### 12.2 Barbeiro Sai da Barbearia

```
POST /api/barbershops/leave-shop
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200`
- [ ] Barbeiro não aparece mais em `GET /api/barbers/barbershop/<<barbershop_id>>`

---

### 12.3 Remover Barbeiro Manualmente (Owner)

> Use outro `barberId` se quiser testar o remove sem o barbeiro sair voluntariamente.

```
DELETE /api/barbershops/my-shop/remove-barber/<<barber_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 12.4 Deletar Atividade

```
DELETE /api/barbershops/my-shop/activities/<<activity_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 12.5 Deletar Conta do Cliente

```
DELETE /api/customers/me
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `204`
- [ ] Login com o mesmo email deve falhar após deleção

---

### 12.6 Fechar Barbearia

> ⛔ **Último passo** — remove a barbearia permanentemente.

```
DELETE /api/barbershops/my-shop
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "password": "<<owner_password>>"
}
```

- [ ] Resposta `204`
- [ ] `GET /api/barbershops/<<barbershop_id>>` deve retornar `404`

---

## 14. Checklist Final

### ✅ Endpoints Públicos (sem autenticação)

| Endpoint | Testado |
|---|---|
| `POST /api/auth/email/register` | ☐ |
| `POST /api/auth/email/login` | ☐ |
| `POST /api/auth/verify` | ☐ |
| `GET /api/barbershops` | ☐ |
| `GET /api/barbershops/{id}` | ☐ |
| `GET /api/barbershops/{id}/activities` | ☐ |
| `GET /api/barbers` | ☐ |
| `GET /api/barbers/{id}` | ☐ |
| `GET /api/barbers/barbershop/{id}` | ☐ |
| `GET /api/products` | ☐ |
| `GET /api/products/{id}` | ☐ |
| `POST /api/payments/webhook` | ☐ |

### 🔐 Endpoints Protegidos por Token

| Módulo | Qtd. | Todos testados |
|---|---|---|
| Auth (protegidos) | 3 | ☐ |
| Customers | 3 | ☐ |
| Barbers | 3 | ☐ |
| Barbershops (escrita) | 12 | ☐ |
| Agendamentos | 8 | ☐ |
| Pagamentos | 2 | ☐ |
| Produtos/Pedidos (escrita) | 5 | ☐ |
| Notificações | 3 | ☐ |

### 🚨 Testes de Segurança (Bônus)

- [ ] Tentar acessar `PUT /api/barbershops/my-shop` com token de **cliente** → deve retornar `403`
- [ ] Tentar acessar `DELETE /api/barbershops/my-shop/remove-barber/{id}` com token de **barbeiro** → deve retornar `403`
- [ ] Tentar acessar qualquer rota protegida **sem token** → deve retornar `401`
- [ ] Tentar usar token expirado → deve retornar `401`
- [ ] Tentar criar agendamento em horário já ocupado → deve retornar `409` ou `400`
- [ ] Tentar criar agendamento fora do horário de trabalho do barbeiro → deve retornar `400`

---

*Gerado em: Abril/2026 | Branch: `feature/migracao-microservicos`*
