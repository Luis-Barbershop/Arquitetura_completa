# 🧪 Roteiro de Testes — API CortaAi

> **Base URL Local:** `http://localhost:8080`  
> **Base URL Produção:** `https://api.cortaai.shop`  
> **Ferramenta:** Hoppscotch / Postman / Insomnia (importar `openapi.json` ou `collection.json`)  
> **Ambiente:** Importar `collectionEnv.json` e ativar o ambiente **CortaAi - Local Dev**

---

## � Como funciona a autenticação (leia antes de testar)

O sistema usa **Firebase Authentication**. O fluxo é:

```
POST /api/auth/email/register   →  cria conta no Firebase + perfil no banco
                                    retorna { idToken, profile }

POST /api/auth/email/login      →  autentica no Firebase
                                    retorna { idToken, refreshToken, ... }

Todas as rotas protegidas:
  Authorization: Bearer {idToken}
  → O API Gateway valida o token e injeta X-User-UID, X-User-Email, X-User-Type
```

**Importante: `POST /api/auth/verify`** é o fluxo de **login social** (Google, Apple, etc.) — não é usado no fluxo de e-mail/senha.

---

## 👥 Tipos de usuário

| Tipo | Como é criado | `role` |
|---|---|---|
| **CUSTOMER** | `register` com `userType: "CUSTOMER"` | `ROLE_CUSTOMER` |
| **BARBER** | `register` com `userType: "BARBER"` | `ROLE_BARBER` |
| **OWNER** | É um BARBER que **criou uma barbearia** via `POST /api/barbershops/register-my-shop` | `ROLE_OWNER` |

> ⚠️ **O barbeiro só vira OWNER no momento em que cria a sua própria barbearia.** O campo `isOwner` no `complete-profile` foi removido do fluxo real — o upgrade de role acontece automaticamente no `BarbershopService.createBarbershop()`.

---

## �📋 Índice

1. [Pré-requisitos](#1-pré-requisitos)
2. [Fase 1 — Registrar e Logar: CUSTOMER](#2-fase-1--registrar-e-logar-customer)
3. [Fase 2 — Registrar e Logar: BARBER (futuro owner)](#3-fase-2--registrar-e-logar-barber-futuro-owner)
4. [Fase 3 — Registrar e Logar: BARBER (funcionário)](#4-fase-3--registrar-e-logar-barber-funcionário)
5. [Fase 4 — Owner cria a barbearia](#5-fase-4--owner-cria-a-barbearia)
6. [Fase 5 — Atividades e Serviços](#6-fase-5--atividades-e-serviços)
7. [Fase 6 — Barbeiro entra na barbearia](#7-fase-6--barbeiro-entra-na-barbearia)
8. [Fase 7 — Barbeiro vincula atividades](#8-fase-7--barbeiro-vincula-atividades)
9. [Fase 8 — Agendamento completo](#9-fase-8--agendamento-completo)
10. [Fase 9 — Pagamentos](#10-fase-9--pagamentos)
11. [Fase 10 — Produtos e Pedidos](#11-fase-10--produtos-e-pedidos)
12. [Fase 11 — Notificações](#12-fase-11--notificações)
13. [Fase 12 — Uploads de Mídia](#13-fase-12--uploads-de-mídia)
14. [Fase 13 — Operações Destrutivas](#14-fase-13--operações-destrutivas)
15. [Checklist Final](#15-checklist-final)

---

## 1. Pré-requisitos

- [ ] Docker rodando (`docker-compose up -d`)
- [ ] Serviços saudáveis: `api-gateway`, `user-service`, `barbershop-service`, `schedule-service`, `payment-service`, `product-service`, `notification-service`
- [ ] Banco PostgreSQL inicializado (`init.sql` executado)
- [ ] Firebase configurado (projeto ativo, autenticação por email/senha habilitada)
- [ ] Ambiente `collectionEnv.json` importado e ativo na ferramenta de teste

**Variáveis que serão preenchidas ao longo dos testes:**

| Variável | Preenchida em |
|---|---|
| `customer_token` | Fase 1 — Register CUSTOMER |
| `customer_id` | Fase 1 — Register CUSTOMER (campo `profile.id`) |
| `owner_token` | Fase 2 — Register BARBER (futuro owner) |
| `barber_token` | Fase 3 — Register BARBER (funcionário) |
| `barber_id` | Fase 3 — Register BARBER (campo `profile.id`) |
| `barbershop_id` | Fase 4 — Criar Barbearia |
| `activity_id` | Fase 5 — Criar Atividade |
| `join_request_id` | Fase 6 — Solicitar Entrada |
| `appointment_id` | Fase 8 — Criar Agendamento |
| `block_id` | Fase 8 — Criar Bloqueio |
| `payment_id` | Fase 9 — Criar Pagamento |
| `product_id` | Fase 10 — Criar Produto |
| `order_id` | Fase 10 — Criar Pedido |
| `notification_id` | Fase 11 — Listar Notificações |
| `highlight_id` | Fase 12 — Adicionar Highlight |

---

## 2. Fase 1 — Registrar e Logar: CUSTOMER

### 1.1 Registrar CUSTOMER

```
POST /api/auth/email/register
```

> 🔓 **Público** — sem token.  
> Cria o usuário no Firebase + provisiona no banco + completa o perfil em **uma única chamada**.  
> Retorna `{ idToken, refreshToken, expiresIn, localId, profile }`.

**Body:**
```json
{
  "email": "<<customer_email>>",
  "password": "<<customer_password>>",
  "userType": "CUSTOMER",
  "name": "Gabriel Teste",
  "tell": "11999999999",
  "documentCPF": "12345678901"
}
```

> ⚠️ `tell` deve ter 10–15 dígitos. `documentCPF` deve ter 11–14 chars numéricos.

**Resposta esperada `200`:**
```json
{
  "idToken": "eyJhbGci...",
  "refreshToken": "...",
  "expiresIn": "3600",
  "localId": "firebaseUID",
  "profile": {
    "id": "uuid-do-customer",
    "name": "Gabriel Teste",
    "email": "gb.chaves@hotmail.com",
    "userType": "CUSTOMER",
    "role": "ROLE_CUSTOMER",
    "profileComplete": true,
    "emailVerified": false,
    "verificationRequired": true
  }
}
```

> 💡 `verificationRequired: true` significa que o Firebase enviou um e-mail de verificação. O usuário **ainda consegue usar a API** — a verificação de e-mail é aplicada somente pelo gateway em ambientes configurados para exigi-la.

- [ ] Salvar `idToken` em `customer_token`
- [ ] Salvar `profile.id` em `customer_id`

---

### 1.2 Login CUSTOMER (para sessões futuras)

```
POST /api/auth/email/login
```

> 🔓 **Público** — sem token.

**Body:**
```json
{
  "email": "<<customer_email>>",
  "password": "<<customer_password>>"
}
```

**Resposta esperada `200`:**
```json
{
  "idToken": "eyJhbGci...",
  "refreshToken": "...",
  "expiresIn": "3600",
  "localId": "firebaseUID",
  "email": "gb.chaves@hotmail.com",
  "registered": true
}
```

- [ ] Salvar `idToken` em `customer_token`

---

### 1.3 Verificar Perfil do CUSTOMER

```
GET /api/auth/me
Authorization: Bearer <<customer_token>>
```

- [ ] `userType: "CUSTOMER"`
- [ ] `profileComplete: true`
- [ ] `role: "ROLE_CUSTOMER"`

---

## 3. Fase 2 — Registrar e Logar: BARBER (futuro owner)

### 2.1 Registrar BARBER — futuro owner

```
POST /api/auth/email/register
```

> 🔓 **Público** — sem token.  
> Registra como BARBER. **Ele só se torna OWNER ao criar a barbearia** (Fase 4).

**Body:**
```json
{
  "email": "<<owner_email>>",
  "password": "<<owner_password>>",
  "userType": "BARBER",
  "name": "Owner Teste",
  "tell": "11977776666",
  "documentCPF": "11122233344",
  "workStartTime": "08:00",
  "workEndTime": "20:00"
}
```

> ℹ️ Para BARBER são obrigatórios também `workStartTime` e `workEndTime` (formato `HH:mm`).  
> Não envie `isOwner` — esse campo é ignorado aqui; a promoção a owner ocorre em `POST /api/barbershops/register-my-shop`.

**Resposta esperada `200`:**
```json
{
  "idToken": "eyJhbGci...",
  "profile": {
    "id": "uuid-do-barber-owner",
    "userType": "BARBER",
    "role": "ROLE_BARBER",
    "isOwner": false,
    "barbershopId": null,
    "profileComplete": true
  }
}
```

- [ ] `role: "ROLE_BARBER"` e `isOwner: false` (ainda não é owner)
- [ ] Salvar `idToken` em `owner_token`

---

### 2.2 Login BARBER/owner (para sessões futuras)

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

- [ ] Salvar `idToken` em `owner_token`

---

## 4. Fase 3 — Registrar e Logar: BARBER (funcionário)

### 3.1 Registrar BARBER funcionário

```
POST /api/auth/email/register
```

**Body:**
```json
{
  "email": "<<barber_email>>",
  "password": "<<barber_password>>",
  "userType": "BARBER",
  "name": "Barbeiro Teste",
  "tell": "11988887777",
  "documentCPF": "98765432100",
  "workStartTime": "09:00",
  "workEndTime": "18:00"
}
```

**Resposta esperada `200`:**
```json
{
  "idToken": "eyJhbGci...",
  "profile": {
    "id": "uuid-do-barber",
    "userType": "BARBER",
    "role": "ROLE_BARBER",
    "isOwner": false,
    "barbershopId": null,
    "profileComplete": true
  }
}
```

- [ ] Salvar `idToken` em `barber_token`
- [ ] Salvar `profile.id` em `barber_id`

---

### 3.2 Login BARBER funcionário (para sessões futuras)

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

- [ ] Salvar `idToken` em `barber_token`

---

## 5. Fase 4 — Owner cria a barbearia

> 🔐 Exige token de um BARBER. Após essa chamada ele se torna OWNER.

### 4.1 Registrar Barbearia

```
POST /api/barbershops/register-my-shop
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

> ⚠️ Enviar como **multipart/form-data**. O campo `shop` é um JSON em texto (RequestPart), `file` é a imagem (opcional).

**Partes do form:**

| Part | Content-Type | Valor |
|---|---|---|
| `shop` | `application/json` | ver JSON abaixo |
| `file` | `image/jpeg` ou `image/png` | *(arquivo opcional)* |

**Conteúdo do part `shop`:**
```json
{
  "name": "Barbearia Teste",
  "cnpj": "12345678000199",
  "address": "Rua das Flores, 123"
}
```

> ⚠️ `cnpj` deve ter **exatamente 14 dígitos numéricos** sem pontuação.

**Resposta esperada `201`:**
```json
{
  "id": "uuid-da-barbearia",
  "name": "Barbearia Teste",
  "cnpj": "12345678000199",
  "address": "Rua das Flores, 123",
  "ownerId": "uuid-do-barber-owner"
}
```

- [ ] Salvar `id` em `barbershop_id`
- [ ] O serviço faz chamada interna ao user-service para setar `isOwner = true` no barbeiro

### 4.2 Confirmar que virou OWNER

```
GET /api/auth/me
Authorization: Bearer <<owner_token>>
```

> ⚠️ Pode ser necessário fazer um novo login (`POST /api/auth/email/login`) para obter um token atualizado com os novos claims do Firebase.

- [ ] `isOwner: true`
- [ ] `barbershopId: "<<barbershop_id>>"`
- [ ] `role: "ROLE_OWNER"` *(se o gateway já propagou os custom claims)*

---

### 4.3 Listar Barbearias (público)

```
GET /api/barbershops
```

- [ ] Barbearia criada aparece na lista ✅

---

### 4.4 Buscar Barbearia por ID (público)

```
GET /api/barbershops/<<barbershop_id>>
```

- [ ] Dados corretos ✅

---

### 4.5 Atualizar Barbearia

```
PUT /api/barbershops/my-shop
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "name": "Barbearia Atualizada",
  "address": "Av. Paulista, 1000"
}
```

- [ ] Resposta `200`

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

- [ ] Resposta `201`
- [ ] Salvar `id` em `activity_id`

---

### 5.2 Listar Atividades da Barbearia (público)

```
GET /api/barbershops/<<barbershop_id>>/activities
```

- [ ] Atividade criada aparece ✅

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

- [ ] Resposta `200`

---

## 7. Fase 6 — Barbeiro entra na barbearia

### 6.1 Barbeiro Solicita Entrada (pelo CNPJ)

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

- [ ] Resposta `202`

---

### 6.2 Owner Lista Solicitações Pendentes

```
GET /api/barbershops/my-shop/pending-requests
Authorization: Bearer <<owner_token>>
```

- [ ] Solicitação do barbeiro aparece
- [ ] Salvar `id` da solicitação em `join_request_id`

---

### 6.3 Owner Aprova a Solicitação

```
POST /api/barbershops/my-shop/approve-request/<<join_request_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 6.4 Confirmar que barbeiro está na barbearia (público)

```
GET /api/barbers/barbershop/<<barbershop_id>>
```

- [ ] Barbeiro aparece na lista ✅
- [ ] `barbershopId` do barbeiro agora é `<<barbershop_id>>`

---

## 8. Fase 7 — Barbeiro vincula atividades

### 7.1 Barbeiro Vincula Atividades a Si Mesmo

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

> ℹ️ Esta operação **substitui** todas as atividades anteriores — envie a lista completa.

- [ ] Resposta `200` com o set de UUIDs

---

### 7.2 Consultar Atividades Vinculadas

```
GET /api/barbers/me/my-activities
Authorization: Bearer <<barber_token>>
```

- [ ] `activity_id` aparece no set retornado

---

## 9. Fase 8 — Agendamento Completo

### 8.1 Verificar Disponibilidade (público)

```
GET /api/appointments/availability?barberId=<<barber_id>>&date=2026-04-10
```

- [ ] Resposta `200` com slots de horário disponíveis
- [ ] Horários dentro de `workStartTime`–`workEndTime` do barbeiro

---

### 8.2 Criar Agendamento

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

- [ ] Resposta `201`
- [ ] Salvar `id` em `appointment_id`

---

### 8.3 Buscar Agendamento por ID

```
GET /api/appointments/<<appointment_id>>
```

- [ ] Status: `PENDING` ou `SCHEDULED`

---

### 8.4 Meus Agendamentos (cliente)

```
GET /api/appointments/my-appointments
Authorization: Bearer <<customer_token>>
```

- [ ] Agendamento criado aparece

---

### 8.5 Agendamentos do Barbeiro por Data

```
GET /api/appointments/barber/<<barber_id>>?date=2026-04-10
```

- [ ] Agendamento aparece

---

### 8.6 Agendamentos da Barbearia por Data

```
GET /api/appointments/barbershop/<<barbershop_id>>?date=2026-04-10
```

- [ ] Agendamento aparece

---

### 8.7 Barbeiro Confirma Agendamento

```
PUT /api/appointments/<<appointment_id>>/confirm
Authorization: Bearer <<barber_token>>
```

- [ ] Status muda para `CONFIRMED`

---

### 8.8 Criar Bloqueio de Horário

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

- [ ] Salvar `id` em `block_id`

---

### 8.9 Listar Bloqueios

```
GET /api/appointments/barber-blocks?barberId=<<barber_id>>&date=2026-04-10
```

- [ ] Bloqueio aparece

---

### 8.10 Deletar Bloqueio

```
DELETE /api/appointments/barber-blocks/<<block_id>>
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `204`

---

## 10. Fase 9 — Pagamentos

### 9.1 Criar Pagamento

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

- [ ] Salvar `id` em `payment_id`

---

### 9.2 Buscar Pagamento por ID

```
GET /api/payments/<<payment_id>>
```

- [ ] Status do pagamento retornado

---

### 9.3 Meus Pagamentos

```
GET /api/payments/my-payments
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Lista com o pagamento criado

---

### 9.4 Webhook MercadoPago (simulação)

```
POST /api/payments/webhook
```

> 🔓 **Público** — sem token.

**Body:**
```json
{
  "type": "payment",
  "data": { "id": "12345678" }
}
```

- [ ] Resposta `200`

---

### 9.5 Concluir Agendamento (após pagamento)

```
PUT /api/appointments/<<appointment_id>>/conclude
Authorization: Bearer <<barber_token>>
```

- [ ] Status muda para `COMPLETED` / `CONCLUDED`

---

## 11. Fase 10 — Produtos e Pedidos

### 10.1 Criar Produto

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

> Categorias válidas: `HAIR_PRODUCT`, `BEARD_PRODUCT`, `SKIN_PRODUCT`, `ACCESSORY`

- [ ] Salvar `id` em `product_id`

---

### 10.2 Listar Produtos (público)

```
GET /api/products?barbershopId=<<barbershop_id>>
```

- [ ] Produto criado aparece ✅

---

### 10.3 Buscar Produto por ID (público)

```
GET /api/products/<<product_id>>
```

- [ ] Resposta `200`

---

### 10.4 Atualizar Produto

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

### 10.5 Criar Pedido

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

- [ ] Salvar `id` em `order_id`

---

### 10.6 Meus Pedidos

```
GET /api/orders/my-orders
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Pedido criado aparece

---

### 10.7 Pedidos da Barbearia

```
GET /api/orders/shop-orders?barbershopId=<<barbershop_id>>
```

- [ ] Pedido aparece

---

### 10.8 Atualizar Status do Pedido

```
PUT /api/orders/<<order_id>>/status
Authorization: Bearer <<owner_token>>
Content-Type: text/plain
```

**Body (texto puro, sem JSON):**
```
DELIVERED
```

> Ciclo de status: `PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `DELIVERED`  
> Cancelamento: `CANCELLED`

- [ ] Resposta `200`

---

## 12. Fase 11 — Notificações

### 11.1 Listar Minhas Notificações

```
GET /api/notifications/my-notifications
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

> 💡 Notificações são disparadas automaticamente a cada evento: agendamento criado, confirmado, concluído, pagamento processado, pedido atualizado.

- [ ] Lista de notificações retornada
- [ ] Salvar `id` de uma em `notification_id`

---

### 11.2 Contar Não Lidas

```
GET /api/notifications/unread-count
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Retorna número inteiro

---

### 11.3 Marcar como Lida

```
PUT /api/notifications/<<notification_id>>/read
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200`
- [ ] Contagem diminui ao repetir 11.2

---

## 13. Fase 12 — Uploads de Mídia

### 12.1 Upload Logo da Barbearia

```
POST /api/barbershops/my-shop/upload-logo
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL do Cloudinary

---

### 12.2 Upload Banner da Barbearia

```
POST /api/barbershops/my-shop/upload-banner
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL

---

### 12.3 Adicionar Highlight

```
POST /api/barbershops/my-shop/highlights
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `201` com URL
- [ ] Salvar `id` do highlight em `highlight_id`

---

### 12.4 Deletar Highlight

```
DELETE /api/barbershops/my-shop/highlights/<<highlight_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 12.5 Upload Foto de Atividade

```
POST /api/barbershops/my-shop/activities/<<activity_id>>/upload-photo
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL

---

### 12.6 Upload Foto do Cliente

```
POST /api/customers/me/upload-photo
Authorization: Bearer <<customer_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL

---

## 14. Fase 13 — Operações Destrutivas

> ⚠️ **Execute estas etapas por último!**

### 13.1 Cancelar Agendamento (cliente)

```
PUT /api/appointments/<<appointment_id>>/cancel
Authorization: Bearer <<customer_token>>
```

- [ ] Status muda para `CANCELLED`

---

### 13.2 Deletar Atividade

```
DELETE /api/barbershops/my-shop/activities/<<activity_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 13.3 Barbeiro Sai da Barbearia (voluntariamente)

```
POST /api/barbershops/leave-shop
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `204`
- [ ] Barbeiro some de `GET /api/barbers/barbershop/<<barbershop_id>>`

---

### 13.4 Owner Remove Barbeiro Manualmente

```
DELETE /api/barbershops/my-shop/remove-barber/<<barber_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 13.5 Deletar Produto (soft delete)

```
DELETE /api/products/<<product_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `204`

---

### 13.6 Deletar Conta do Cliente

```
DELETE /api/customers/me
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `204`

---

### 13.7 Fechar Barbearia ⛔

> **Irreversível** — remove a barbearia e desvincula todos os barbeiros.

```
DELETE /api/barbershops/my-shop/close
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{
  "password": "<<owner_password>>"
}
```

- [ ] Resposta `204`
- [ ] `GET /api/barbershops/<<barbershop_id>>` → `404`

---

## 15. Checklist Final

### 🔓 Endpoints Públicos (sem token)

| Endpoint | OK |
|---|---|
| `POST /api/auth/email/register` | ☐ |
| `POST /api/auth/email/login` | ☐ |
| `POST /api/auth/verify` *(fluxo social)* | ☐ |
| `GET /api/barbershops` | ☐ |
| `GET /api/barbershops/{id}` | ☐ |
| `GET /api/barbershops/{id}/activities` | ☐ |
| `GET /api/barbers` | ☐ |
| `GET /api/barbers/{id}` | ☐ |
| `GET /api/barbers/barbershop/{id}` | ☐ |
| `GET /api/products` | ☐ |
| `GET /api/products/{id}` | ☐ |
| `GET /api/appointments/availability` | ☐ |
| `POST /api/payments/webhook` | ☐ |

### 🔐 Endpoints Protegidos

| Módulo | Qtd. endpoints | Testados |
|---|---|---|
| Auth (me, complete-profile) | 3 | ☐ |
| Customers | 3 | ☐ |
| Barbers (update, my-activities, assign) | 3 | ☐ |
| Barbershops (criação, gestão, join) | 13 | ☐ |
| Agendamentos (criar, confirmar, concluir, cancelar, bloqueios) | 8 | ☐ |
| Pagamentos | 2 | ☐ |
| Produtos/Pedidos (escrita) | 5 | ☐ |
| Notificações | 3 | ☐ |

### 🚨 Testes de Segurança

- [ ] `PUT /api/barbershops/my-shop` com token de **CUSTOMER** → `403`
- [ ] `POST /api/barbershops/my-shop/activities` com token de **BARBER funcionário** → `403`
- [ ] Qualquer rota protegida **sem token** → `401`
- [ ] Token **expirado** → `401`
- [ ] Criar agendamento em horário **já ocupado** → `409` ou `400`
- [ ] Criar agendamento **fora do horário de trabalho** do barbeiro → `400`
- [ ] Barbeiro tentar criar barbearia com um CNPJ **já cadastrado** → `409`

---

*Gerado em: Abril/2026 | Branch: `feature/migracao-microservicos`*

