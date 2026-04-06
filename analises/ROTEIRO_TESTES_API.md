# 🧪 Roteiro de Testes — API CortaAí

> **Versão:** 2.0 (Abril/2026) | **Branch:** `feature/migracao-microservicos`
>
> **Base URL Local:** `http://localhost:8080`
> **Base URL Produção:** `https://api.cortaai.shop`
>
> **Ferramenta:** Hoppscotch / Postman / Insomnia
> — importe `collection.json` + `collectionEnv.json` na raiz do projeto.
>
> **Autenticação:** Firebase `idToken` → `Authorization: Bearer <token>`

---

## 📋 Índice

1. [Conceitos Fundamentais](#1-conceitos-fundamentais)
2. [Configuração Inicial](#2-configuração-inicial)
3. [Fase 1 — Autenticação e Cadastro](#3-fase-1--autenticação-e-cadastro)
4. [Fase 2 — Perfil do Cliente](#4-fase-2--perfil-do-cliente)
5. [Fase 3 — Owner cria a Barbearia](#5-fase-3--owner-cria-a-barbearia)
6. [Fase 4 — Gestão de Barbeiros](#6-fase-4--gestão-de-barbeiros)
7. [Fase 5 — Atividades e Serviços](#7-fase-5--atividades-e-serviços)
8. [Fase 6 — Barbeiro entra na Barbearia](#8-fase-6--barbeiro-entra-na-barbearia)
9. [Fase 7 — Barbeiro vincula Atividades](#9-fase-7--barbeiro-vincula-atividades)
10. [Fase 8 — Agendamento Completo](#10-fase-8--agendamento-completo)
11. [Fase 9 — Pagamentos](#11-fase-9--pagamentos)
12. [Fase 10 — Produtos e Pedidos](#12-fase-10--produtos-e-pedidos)
13. [Fase 11 — Notificações](#13-fase-11--notificações)
14. [Fase 12 — Uploads de Mídia](#14-fase-12--uploads-de-mídia)
15. [Fase 13 — Testes Negativos e Segurança](#15-fase-13--testes-negativos-e-segurança)
16. [Fase 14 — Operações Destrutivas](#16-fase-14--operações-destrutivas)
17. [Tabela de Todos os Endpoints](#17-tabela-de-todos-os-endpoints)
18. [Sequência Mínima (30 min)](#18-sequência-mínima-30-min)
19. [Checklist Final](#19-checklist-final)

---

## 1. Conceitos Fundamentais

### Como funciona a autenticação

```
POST /api/auth/email/register   →  cria conta no Firebase + perfil no banco
                                    retorna { idToken, refreshToken, profile }

POST /api/auth/email/login      →  autentica no Firebase
                                    retorna { idToken, refreshToken, ... }

Todas as rotas protegidas:
  Authorization: Bearer {idToken}
  → API Gateway valida o token e injeta:
      X-User-UID    = Firebase UID do usuário
      X-User-Email  = e-mail do usuário
      X-User-Type   = BARBER | CUSTOMER
```

> ⚠️ `POST /api/auth/verify` é o fluxo de **login social** (Google, Apple, etc.) — NÃO é usado no fluxo de e-mail/senha.

### Tipos de usuário

| Tipo | Como é criado | `role` retornado |
|------|--------------|-----------------|
| **CUSTOMER** | `register` com `userType: "CUSTOMER"` | `ROLE_CUSTOMER` |
| **BARBER** | `register` com `userType: "BARBER"` | `ROLE_BARBER` |
| **OWNER** | BARBER que executa `POST /api/barbershops/register-my-shop` | `ROLE_OWNER` |

> ⚠️ O barbeiro só vira OWNER **no momento em que cria sua própria barbearia**. A promoção de role acontece automaticamente no `BarbershopService.createBarbershop()`, que chama o `user-service` interno para atualizar o custom claim `isOwner=true` no Firebase.

### Verificação de e-mail

Ao registrar por e-mail/senha, o Firebase envia um link de verificação para `https://web.cortaai.shop/verify-email`. O API Gateway bloqueia rotas protegidas quando `email_verified=false` para o provider `password`.

---

## 2. Configuração Inicial

### Variáveis de ambiente (configure no Hoppscotch / Postman)

| Variável | Valor inicial | Preenchida em |
|----------|--------------|---------------|
| `base_url` | `https://api.cortaai.shop` ou `http://localhost:8080` | manual |
| `customer_email` | `cliente@teste.com` | manual |
| `customer_password` | `Teste@123` | manual |
| `barber_email` | `barbeiro@teste.com` | manual |
| `barber_password` | `Teste@123` | manual |
| `owner_email` | `owner@teste.com` | manual |
| `owner_password` | `Teste@123` | manual |
| `customer_token` | — | Fase 1.1 (register) |
| `customer_id` | — | Fase 1.1 (register) |
| `owner_token` | — | Fase 1.3 (register) |
| `barber_token` | — | Fase 1.2 (register) |
| `barber_id` | — | Fase 1.2 (register) |
| `barbershop_id` | — | Fase 3.1 (criar barbearia) |
| `activity_id` | — | Fase 5.1 (criar atividade) |
| `join_request_id` | — | Fase 6.2 (listar pendentes) |
| `appointment_id` | — | Fase 8.2 (criar agendamento) |
| `block_id` | — | Fase 8.8 (criar bloqueio) |
| `payment_id` | — | Fase 9.1 (criar pagamento) |
| `product_id` | — | Fase 10.1 (criar produto) |
| `order_id` | — | Fase 10.5 (criar pedido) |
| `notification_id` | — | Fase 11.1 (listar notificações) |
| `highlight_id` | — | Fase 12.3 (adicionar highlight) |

### Pré-requisitos de infraestrutura

- [ ] `docker-compose up -d` com todos os serviços saudáveis
- [ ] Eureka Dashboard acessível em `http://localhost:8761` — todos os serviços registrados
- [ ] Firebase: projeto ativo, auth por e-mail/senha habilitado, domínio `web.cortaai.shop` em Authorized Domains
- [ ] Banco MySQL inicializado (`init.sql` executado — schemas `user_db`, `barbershop_db`, `schedule_db`, `payment_db`, `product_db`)

---

## 3. Fase 1 — Autenticação e Cadastro

### 1.1 — Registrar CUSTOMER

```http
POST /api/auth/email/register
```

> 🔓 Público. Cria conta no Firebase + provisiona perfil no banco em uma única chamada.

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

> `tell` deve ter 10–15 dígitos. `documentCPF` deve ter 11–14 chars numéricos.

**Resposta `200`:**
```json
{
  "idToken": "eyJhbGci...",
  "refreshToken": "...",
  "expiresIn": "3600",
  "localId": "firebaseUID",
  "profile": {
    "id": "uuid-do-customer",
    "name": "Gabriel Teste",
    "email": "cliente@teste.com",
    "userType": "CUSTOMER",
    "role": "ROLE_CUSTOMER",
    "profileComplete": true,
    "emailVerified": false,
    "verificationRequired": true
  }
}
```

> 💡 `verificationRequired: true` indica que o Firebase enviou o e-mail de verificação para `web.cortaai.shop/verify-email`.
> ⚠️ Não registre o mesmo e-mail duas vezes — Firebase retorna erro `EMAIL_EXISTS`.

- [ ] Salvar `idToken` em `customer_token`
- [ ] Salvar `profile.id` em `customer_id`

---

### 1.2 — Registrar BARBER (funcionário)

```http
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

> Para `BARBER` são obrigatórios `workStartTime` e `workEndTime` no formato `HH:mm`.

**Resposta `200`:**
```json
{
  "idToken": "...",
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

### 1.3 — Registrar BARBER (futuro Owner)

```http
POST /api/auth/email/register
```

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

> Ele ainda tem `ROLE_BARBER`. Vira `ROLE_OWNER` **somente ao criar a barbearia** (Fase 3.1). Não envie `isOwner` — é ignorado aqui.

- [ ] Salvar `idToken` em `owner_token`
- [ ] `role: "ROLE_BARBER"` e `isOwner: false` na resposta

---

### 1.4 — Login CUSTOMER

```http
POST /api/auth/email/login
```

**Body:**
```json
{ "email": "<<customer_email>>", "password": "<<customer_password>>" }
```

**Resposta `200`:**
```json
{
  "idToken": "eyJhbGci...",
  "refreshToken": "...",
  "expiresIn": "3600",
  "localId": "firebaseUID",
  "email": "cliente@teste.com",
  "registered": true
}
```

- [ ] Salvar `idToken` em `customer_token`

---

### 1.5 — Login BARBER (funcionário)

```http
POST /api/auth/email/login
```

**Body:** `{ "email": "<<barber_email>>", "password": "<<barber_password>>" }`

- [ ] Salvar `idToken` em `barber_token`

---

### 1.6 — Login OWNER

```http
POST /api/auth/email/login
```

**Body:** `{ "email": "<<owner_email>>", "password": "<<owner_password>>" }`

- [ ] Salvar `idToken` em `owner_token`

---

### 1.7 — Obter Perfil do Usuário Logado

```http
GET /api/auth/me
Authorization: Bearer <<customer_token>>
```

> Execute para cada usuário trocando o token.

- [ ] Campo `id` presente
- [ ] `role` = `ROLE_CUSTOMER` ou `ROLE_BARBER`
- [ ] Confirmar `customer_id` salvo corresponde ao `id` do customer

---

### 1.8 — Login Social (fluxo Google/Apple)

```http
POST /api/auth/verify
```

> 🔓 Público. Recebe um `idToken` já obtido pelo **Firebase SDK no frontend** (Google SignIn, Apple SignIn, etc.).

**Body:**
```json
{ "idToken": "<<firebase_id_token_do_frontend>>", "userType": "CUSTOMER" }
```

**Resposta `200`:**
```json
{
  "profileComplete": false,
  "userType": "CUSTOMER",
  "id": "...",
  "role": "ROLE_CUSTOMER"
}
```

> Se `profileComplete=false`, executar passo 1.9.
> ⚠️ Só funciona com token real gerado pelo Firebase SDK — não testável via Hoppscotch puro.

---

### 1.9 — Completar Perfil (fluxo social)

```http
POST /api/auth/customers/complete-profile
Authorization: Bearer <<customer_token>>
```

**Body:** `{ "name": "Gabriel", "tell": "11999999999", "documentCPF": "12345678901" }`

> Usar **somente** se `/auth/verify` retornou `profileComplete=false`.

- [ ] Resposta `200`

---

### 1.10 — Completar Perfil de Barbeiro (fluxo social)

```http
POST /api/auth/barbers/complete-profile
Authorization: Bearer <<barber_token>>
```

**Body:**
```json
{
  "name": "Barbeiro Teste",
  "tell": "11988887777",
  "documentCPF": "98765432100",
  "workStartTime": "09:00",
  "workEndTime": "18:00"
}
```

- [ ] Resposta `200`

---

### 1.11 — Esqueci minha senha

```http
POST /api/auth/email/forgot-password
```

> 🔓 Público.

**Body:** `{ "email": "<<owner_email>>" }`

- [ ] Resposta `204`
- [ ] E-mail de recuperação enviado pelo Firebase

---

### 1.12 — Alterar Senha

```http
POST /api/auth/email/change-password
```

> 🔓 Público (token vai no body).

**Body:** `{ "idToken": "<<owner_token>>", "newPassword": "NovaSenha@123" }`

- [ ] Resposta `204`
- [ ] Após alterar, o token antigo fica inválido — fazer novo login

---

### 1.13 — Verificar se E-mail Existe

```http
GET /api/auth/email/exists?email=<<customer_email>>
```

> 🔓 Público.

- [ ] Resposta `200`: `{ "exists": true, "userType": "CUSTOMER" }`
- [ ] Teste negativo com e-mail inexistente: `{ "exists": false, "userType": null }`

---

## 4. Fase 2 — Perfil do Cliente

### 2.1 — Listar Clientes

```http
GET /api/customers
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `200` com array de clientes

---

### 2.2 — Buscar Cliente por ID

```http
GET /api/customers/<<customer_id>>
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `200` com dados do cliente

---

### 2.3 — Atualizar Perfil Cliente

```http
PUT /api/customers/me
Authorization: Bearer <<customer_token>>
```

**Body:** `{ "name": "Gabriel Atualizado", "tell": "11988887777" }`

- [ ] Resposta `200`
- [ ] Confirmar atualização via `GET /api/auth/me`

---

### 2.4 — Upload Foto Cliente

```http
POST /api/customers/me/upload-photo
Authorization: Bearer <<customer_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL do Cloudinary

---

## 5. Fase 3 — Owner cria a Barbearia

> **Pré-requisito:** Owner registrado (1.3) e `owner_token` disponível.

### 3.1 — Registrar Barbearia (Owner vira ROLE_OWNER aqui)

```http
POST /api/barbershops/register-my-shop
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

> ⚠️ Enviar como **multipart/form-data**. O campo `shop` é JSON em texto, `file` é a imagem (opcional).

| Part | Content-Type | Valor |
|------|-------------|-------|
| `shop` | `application/json` | JSON abaixo |
| `file` | `image/jpeg` ou `image/png` | arquivo opcional |

**Conteúdo do part `shop`:**
```json
{
  "name": "Barbearia Teste",
  "cnpj": "12345678000199",
  "address": "Rua das Flores, 123"
}
```

> ⚠️ `cnpj` deve ter **exatamente 14 dígitos numéricos** sem pontuação.

**Resposta `201`:**
```json
{
  "id": "uuid-da-barbearia",
  "name": "Barbearia Teste",
  "cnpj": "12345678000199",
  "address": "Rua das Flores, 123",
  "ownerId": "uuid-do-owner"
}
```

- [ ] Salvar `id` em `barbershop_id`
- [ ] O serviço chama internamente o `user-service` para setar `isOwner = true` no barbeiro

---

### 3.2 — Confirmar que virou OWNER

> Pode ser necessário novo login para token com claims atualizados.

```http
GET /api/auth/me
Authorization: Bearer <<owner_token>>
```

- [ ] `isOwner: true`
- [ ] `barbershopId: "<<barbershop_id>>"`
- [ ] `role: "ROLE_OWNER"`

---

### 3.3 — Listar Barbearias

```http
GET /api/barbershops
```

> 🔓 Público.

- [ ] Barbearia criada aparece na lista ✅

---

### 3.4 — Buscar Barbearia por ID

```http
GET /api/barbershops/<<barbershop_id>>
```

> 🔓 Público.

- [ ] Dados corretos ✅

---

### 3.5 — Atualizar Barbearia

```http
PUT /api/barbershops/my-shop
Authorization: Bearer <<owner_token>>
```

**Body:** `{ "name": "Barbearia Atualizada", "address": "Av. Paulista, 1000" }`

- [ ] Resposta `200`

---

## 6. Fase 4 — Gestão de Barbeiros

### 4.1 — Listar Barbeiros

```http
GET /api/barbers
```

> 🔓 Público.

- [ ] Resposta `200` com lista

---

### 4.2 — Buscar Barbeiro por ID

```http
GET /api/barbers/<<barber_id>>
```

> 🔓 Público.

- [ ] Resposta `200`

---

### 4.3 — Barbeiros por Barbearia

```http
GET /api/barbers/barbershop/<<barbershop_id>>
```

> 🔓 Público. Pode estar vazio antes do barbeiro ser aprovado.

- [ ] Resposta `200`

---

### 4.4 — Atualizar Barbeiro

```http
PUT /api/barbers/<<barber_id>>
Authorization: Bearer <<barber_token>>
```

**Body:** `{ "name": "Barbeiro Editado", "tell": "11977776666" }`

- [ ] Resposta `200`

---

## 7. Fase 5 — Atividades e Serviços

### 5.1 — Criar Atividade

```http
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
- [ ] Salvar `id` em `activity_id`

---

### 5.2 — Listar Atividades da Barbearia

```http
GET /api/barbershops/<<barbershop_id>>/activities
```

> 🔓 Público.

- [ ] Atividade criada aparece ✅

---

### 5.3 — Atualizar Atividade

```http
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

## 8. Fase 6 — Barbeiro entra na Barbearia

### 6.1 — Barbeiro Solicita Entrada (pelo CNPJ)

```http
POST /api/barbershops/join-request
Authorization: Bearer <<barber_token>>
```

**Body:** `{ "cnpj": "12345678000199" }`

- [ ] Resposta `200` ou `202`

---

### 6.2 — Owner Lista Solicitações Pendentes

```http
GET /api/barbershops/my-shop/pending-requests
Authorization: Bearer <<owner_token>>
```

- [ ] Solicitação do barbeiro aparece
- [ ] Salvar `id` da solicitação em `join_request_id`

---

### 6.3 — Owner Aprova a Solicitação

```http
POST /api/barbershops/my-shop/approve-request/<<join_request_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `200` ou `204`

---

### 6.4 — Confirmar que Barbeiro está na Barbearia

```http
GET /api/barbers/barbershop/<<barbershop_id>>
```

> 🔓 Público.

- [ ] Barbeiro aparece na lista ✅
- [ ] `barbershopId` do barbeiro = `<<barbershop_id>>`

---

## 9. Fase 7 — Barbeiro vincula Atividades

### 7.1 — Barbeiro Vincula Atividades a Si Mesmo

```http
POST /api/barbers/me/assign-activities
Authorization: Bearer <<barber_token>>
```

**Body:** `{ "activityIds": ["<<activity_id>>"] }`

> ℹ️ Esta operação **substitui** todas as atividades anteriores — envie a lista completa.

- [ ] Resposta `200`

---

### 7.2 — Consultar Atividades Vinculadas

```http
GET /api/barbers/me/my-activities
Authorization: Bearer <<barber_token>>
```

- [ ] `activity_id` aparece no set retornado

---

## 10. Fase 8 — Agendamento Completo

> **Pré-requisitos:** cliente registrado ✅, barbeiro aprovado na barbearia ✅, atividade criada ✅, barbeiro com atividades vinculadas ✅.

### 8.1 — Verificar Disponibilidade

```http
GET /api/appointments/availability?barberId=<<barber_id>>&date=2026-05-20
```

> 🔓 Público.

- [ ] Resposta `200` com array de slots (ex: `["09:00", "09:40", "10:20", ...]`)
- [ ] Horários dentro de `workStartTime`–`workEndTime` do barbeiro
- [ ] Duração dos slots = `durationMinutes` da atividade vinculada

---

### 8.2 — Criar Agendamento

```http
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
  "startTime": "2026-05-20T10:00:00"
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `appointment_id`
- [ ] Status = `PENDING` ou `SCHEDULED`
- [ ] Efeito colateral: evento publicado no RabbitMQ → notificação gerada para cliente e barbeiro

---

### 8.3 — Buscar Agendamento por ID

```http
GET /api/appointments/<<appointment_id>>
Authorization: Bearer <<customer_token>>
```

- [ ] Status: `PENDING` ou `SCHEDULED`

---

### 8.4 — Meus Agendamentos (cliente)

```http
GET /api/appointments/my-appointments
Authorization: Bearer <<customer_token>>
```

- [ ] Agendamento criado aparece na lista

---

### 8.5 — Agendamentos do Barbeiro por Data

```http
GET /api/appointments/barber/<<barber_id>>?date=2026-05-20
Authorization: Bearer <<barber_token>>
```

- [ ] Agendamento aparece

---

### 8.6 — Agendamentos da Barbearia por Data

```http
GET /api/appointments/barbershop/<<barbershop_id>>?date=2026-05-20
Authorization: Bearer <<owner_token>>
```

- [ ] Agendamento aparece

---

### 8.7 — Barbeiro Confirma Agendamento

```http
PUT /api/appointments/<<appointment_id>>/confirm
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200`
- [ ] Status muda para `CONFIRMED`
- [ ] Efeito colateral: e-mail de confirmação enviado ao cliente

---

### 8.8 — Criar Bloqueio de Horário

```http
POST /api/appointments/barber-blocks
Authorization: Bearer <<barber_token>>
```

**Body:**
```json
{
  "barberId": "<<barber_id>>",
  "startTime": "2026-05-20T12:00:00",
  "endTime": "2026-05-20T13:00:00",
  "reason": "Almoço"
}
```

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `block_id`
- [ ] Intervalo bloqueado não aparece mais na disponibilidade (verificar 8.1 com essa data)

---

### 8.9 — Listar Bloqueios do Barbeiro

```http
GET /api/appointments/barber-blocks?barberId=<<barber_id>>&date=2026-05-20
Authorization: Bearer <<barber_token>>
```

- [ ] Bloqueio criado aparece

---

### 8.10 — Teste Negativo: Agendar em horário bloqueado

```http
POST /api/appointments
Authorization: Bearer <<customer_token>>
```

**Body:** igual ao 8.2, mas com `"startTime": "2026-05-20T12:00:00"` (horário do bloqueio)

- [ ] Esperado: `400` ou `409` — conflito de horário

---

### 8.11 — Deletar Bloqueio

```http
DELETE /api/appointments/barber-blocks/<<block_id>>
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200` ou `204`

---

## 11. Fase 9 — Pagamentos

> **Pré-requisito:** agendamento com status `CONFIRMED` e `appointment_id` disponível.

### 9.1 — Criar Pagamento (Checkout Pro Mercado Pago)

```http
POST /api/payments/create
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

**Body:**
```json
{
  "appointmentId": "<<appointment_id>>",
  "paymentMethod": "CREDIT_CARD"
}
```

**Resposta `200`:**
```json
{
  "id": "uuid-do-payment",
  "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=..."
}
```

- [ ] Salvar `id` em `payment_id`
- [ ] Abrir `checkoutUrl` no browser para testar o checkout real
- [ ] Cartão de teste MP: `4509 9535 6623 3704` | CVV: `123` | Vencimento: qualquer data futura

---

### 9.2 — Buscar Pagamento por ID

```http
GET /api/payments/<<payment_id>>
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `200` com status (ex: `PENDING`, `APPROVED`)

---

### 9.3 — Meus Pagamentos

```http
GET /api/payments/my-payments
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Lista com o pagamento criado

---

### 9.4 — Simular Webhook do Mercado Pago

```http
POST /api/payments/webhook
```

> 🔓 Público. Em produção, é chamado automaticamente pelo MP.

**Body:**
```json
{
  "type": "payment",
  "data": { "id": "12345678" }
}
```

- [ ] Resposta `200`
- [ ] Após webhook real: status atualiza de `PENDING` para `APPROVED`

> 💡 Para testar localmente, exponha o serviço via `ngrok http 8080` e configure a URL no painel MP → IPN.

---

### 9.5 — MP OAuth — Iniciar Vinculação (Marketplace, feature futura)

```http
GET /api/payments/mp-connect?state=<<barber_id>>
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200` com `{ "authorizationUrl": "https://auth.mercadopago.com.br/..." }`

> Nota: Funcionalidade de **marketplace** — barbeiro conecta sua conta MP para receber pagamentos diretamente. Requer credenciais de marketplace configuradas (`MP_CLIENT_ID` + `MP_CLIENT_SECRET`).

---

### 9.6 — MP OAuth — Callback *(não testável manualmente)*

```http
GET /api/payments/mp-callback?code=TG-XXXX&state=<<barber_id>>
```

> Chamado automaticamente pelo MP após autorização. Redireciona `302` para `/barberHome?mpLinked=true`.

---

## 12. Fase 10 — Produtos e Pedidos

> **Pré-requisito:** barbearia criada (3.1) e owner logado.

### 10.1 — Criar Produto

```http
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

- [ ] Resposta `200` ou `201`
- [ ] Salvar `id` em `product_id`

---

### 10.2 — Listar Produtos da Barbearia

```http
GET /api/products?barbershopId=<<barbershop_id>>
```

> 🔓 Público.

- [ ] Produto criado aparece ✅

---

### 10.3 — Buscar Produto por ID

```http
GET /api/products/<<product_id>>
```

> 🔓 Público.

- [ ] Resposta `200`

---

### 10.4 — Atualizar Produto

```http
PUT /api/products/<<product_id>>
Authorization: Bearer <<owner_token>>
```

**Body:** `{ "name": "Pomada Pro Max", "price": 34.90 }`

- [ ] Resposta `200`

---

### 10.5 — Criar Pedido (cliente compra produto)

```http
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
- [ ] Verificar redução do estoque: `stockQuantity` do produto caiu 2

---

### 10.6 — Meus Pedidos (cliente)

```http
GET /api/orders/my-orders
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Pedido criado aparece

---

### 10.7 — Pedidos da Barbearia (owner)

```http
GET /api/orders/shop-orders?barbershopId=<<barbershop_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Pedido aparece

---

### 10.8 — Atualizar Status do Pedido

```http
PUT /api/orders/<<order_id>>/status
Authorization: Bearer <<owner_token>>
Content-Type: text/plain
```

**Body (texto puro):** `DELIVERED`

> Ciclo de status: `PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `DELIVERED`
> Cancelamento: `CANCELLED`

- [ ] Resposta `200`

---

## 13. Fase 11 — Notificações

> As notificações são geradas **automaticamente** pelos eventos dos outros fluxos (agendamento criado, confirmado, concluído, pagamento processado, pedido atualizado). Execute os fluxos anteriores antes de testar este.

### 11.1 — Minhas Notificações

```http
GET /api/notifications/my-notifications
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200` com lista de notificações
- [ ] Verificar notificações de: agendamento criado, confirmado, etc.
- [ ] Salvar `id` de uma notificação em `notification_id`

---

### 11.2 — Total de Não Lidas

```http
GET /api/notifications/unread-count
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Retorna `{ "count": N }` onde N > 0

---

### 11.3 — Marcar como Lida

```http
PUT /api/notifications/<<notification_id>>/read
Authorization: Bearer <<customer_token>>
X-User-Id: <<customer_id>>
```

- [ ] Resposta `200`
- [ ] Repetir 11.2 → count diminuiu

---

## 14. Fase 12 — Uploads de Mídia

### 12.1 — Upload Logo da Barbearia

```http
POST /api/barbershops/my-shop/upload-logo
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL do Cloudinary

---

### 12.2 — Upload Banner da Barbearia

```http
POST /api/barbershops/my-shop/upload-banner
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL

---

### 12.3 — Adicionar Highlight

```http
POST /api/barbershops/my-shop/highlights
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` ou `201` com URL
- [ ] Salvar `id` em `highlight_id`

---

### 12.4 — Deletar Highlight

```http
DELETE /api/barbershops/my-shop/highlights/<<highlight_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `200` ou `204`

---

### 12.5 — Upload Foto de Atividade

```http
POST /api/barbershops/my-shop/activities/<<activity_id>>/upload-photo
Authorization: Bearer <<owner_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL

---

### 12.6 — Upload Foto do Cliente

```http
POST /api/customers/me/upload-photo
Authorization: Bearer <<customer_token>>
Content-Type: multipart/form-data
```

**Form field:** `file` → `.jpg` ou `.png`

- [ ] Resposta `200` com URL do Cloudinary

---

## 15. Fase 13 — Testes Negativos e Segurança

### 13.1 — Acesso sem token

```http
GET /api/appointments/my-appointments
```
*(sem header Authorization)*

- [ ] Esperado: `401 Unauthorized`

---

### 13.2 — Token inválido

```http
GET /api/appointments/my-appointments
Authorization: Bearer token_invalido_aqui
```

- [ ] Esperado: `401` ou `403`

---

### 13.3 — Token expirado

> Aguardar 1h após o login e tentar qualquer rota protegida com o token antigo.

- [ ] Esperado: `401`

---

### 13.4 — CUSTOMER tentando criar barbearia

```http
POST /api/barbershops/register-my-shop
Authorization: Bearer <<customer_token>>
```

- [ ] Esperado: `403 Forbidden` (apenas ROLE_BARBER pode criar barbearia)

---

### 13.5 — BARBER funcionário tentando gerenciar a barbearia

```http
POST /api/barbershops/my-shop/activities
Authorization: Bearer <<barber_token>>
```

- [ ] Esperado: `403 Forbidden` (apenas ROLE_OWNER)

---

### 13.6 — Agendar com horário no passado

```http
POST /api/appointments
Authorization: Bearer <<customer_token>>
```

**Body:** igual ao 8.2, mas com `"startTime": "2020-01-01T10:00:00"`

- [ ] Esperado: `400 Bad Request`

---

### 13.7 — Conflito de agendamento (mesmo horário)

- Criar agendamento às 10:00 (passo 8.2)
- Criar segundo agendamento às 10:00 com o **mesmo barbeiro**

- [ ] Esperado: `409 Conflict` ou `400`

---

### 13.8 — Buscar recurso inexistente

```http
GET /api/appointments/00000000-0000-0000-0000-000000000000
Authorization: Bearer <<customer_token>>
```

- [ ] Esperado: `404 Not Found`

---

### 13.9 — Barbeiro confirmando agendamento de outro barbeiro

> Use `<<barber_token>>` para confirmar um agendamento que pertence a outro barbeiro.

- [ ] Esperado: `403 Forbidden`

---

### 13.10 — CNPJ duplicado ao criar barbearia

> Tentar criar uma segunda barbearia com o mesmo CNPJ.

- [ ] Esperado: `409 Conflict`

---

## 16. Fase 14 — Operações Destrutivas

> ⚠️ Execute estas etapas **por último**. Todas são irreversíveis ou quebram os fluxos anteriores.

### 14.1 — Cancelar Agendamento (cliente)

```http
PUT /api/appointments/<<appointment_id>>/cancel
Authorization: Bearer <<customer_token>>
```

- [ ] Status muda para `CANCELLED`
- [ ] Efeito colateral: e-mail de cancelamento enviado

---

### 14.2 — Concluir Agendamento (barbeiro)

```http
PUT /api/appointments/<<appointment_id>>/conclude
Authorization: Bearer <<barber_token>>
```

> Pré-requisito: status `CONFIRMED`.

- [ ] Status muda para `COMPLETED` ou `CONCLUDED`
- [ ] Efeito colateral: e-mail de conclusão enviado ao cliente

---

### 14.3 — Deletar Atividade

```http
DELETE /api/barbershops/my-shop/activities/<<activity_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `200` ou `204`

---

### 14.4 — Deletar Produto

```http
DELETE /api/products/<<product_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `200` ou `204`

---

### 14.5 — Barbeiro Sai da Barbearia (voluntariamente)

```http
POST /api/barbershops/leave-shop
Authorization: Bearer <<barber_token>>
```

- [ ] Resposta `200` ou `204`
- [ ] `GET /api/barbers/barbershop/<<barbershop_id>>` não lista mais o barbeiro

---

### 14.6 — Owner Remove Barbeiro Manualmente

```http
DELETE /api/barbershops/my-shop/remove-barber/<<barber_id>>
Authorization: Bearer <<owner_token>>
```

- [ ] Resposta `200` ou `204`

---

### 14.7 — Deletar Conta do Cliente

```http
DELETE /api/customers/me
Authorization: Bearer <<customer_token>>
```

- [ ] Resposta `200` ou `204`
- [ ] Conta Firebase removida — token inválido após este ponto

---

### 14.8 — Fechar Barbearia ⛔

> **IRREVERSÍVEL** — remove a barbearia, desvincula todos os barbeiros, cancela agendamentos pendentes.

```http
DELETE /api/barbershops/my-shop/close
Authorization: Bearer <<owner_token>>
```

**Body:**
```json
{ "password": "<<owner_password>>" }
```

- [ ] Resposta `204`
- [ ] `GET /api/barbershops/<<barbershop_id>>` → `404`

---

## 17. Tabela de Todos os Endpoints

| # | Método | Endpoint | Auth | Módulo |
|---|--------|----------|------|--------|
| 1 | POST | `/api/auth/email/register` | 🔓 Público | Auth |
| 2 | POST | `/api/auth/email/login` | 🔓 Público | Auth |
| 3 | POST | `/api/auth/verify` | 🔓 Público | Auth Social |
| 4 | GET | `/api/auth/me` | 🔐 Bearer | Auth |
| 5 | POST | `/api/auth/customers/complete-profile` | 🔐 Bearer | Auth Social |
| 6 | POST | `/api/auth/barbers/complete-profile` | 🔐 Bearer | Auth Social |
| 7 | POST | `/api/auth/email/forgot-password` | 🔓 Público | Auth |
| 8 | POST | `/api/auth/email/change-password` | 🔓 Público | Auth |
| 9 | GET | `/api/auth/email/exists` | 🔓 Público | Auth |
| 10 | GET | `/api/customers` | 🔐 Bearer | Clientes |
| 11 | GET | `/api/customers/{id}` | 🔐 Bearer | Clientes |
| 12 | PUT | `/api/customers/me` | 🔐 Bearer | Clientes |
| 13 | DELETE | `/api/customers/me` | 🔐 Bearer | Clientes |
| 14 | POST | `/api/customers/me/upload-photo` | 🔐 Bearer | Clientes |
| 15 | GET | `/api/barbers` | 🔓 Público | Barbeiros |
| 16 | GET | `/api/barbers/{id}` | 🔓 Público | Barbeiros |
| 17 | GET | `/api/barbers/barbershop/{id}` | 🔓 Público | Barbeiros |
| 18 | PUT | `/api/barbers/{id}` | 🔐 Bearer | Barbeiros |
| 19 | GET | `/api/barbers/me/my-activities` | 🔐 Bearer | Barbeiros |
| 20 | POST | `/api/barbers/me/assign-activities` | 🔐 Bearer | Barbeiros |
| 21 | GET | `/api/barbershops` | 🔓 Público | Barbearia |
| 22 | GET | `/api/barbershops/{id}` | 🔓 Público | Barbearia |
| 23 | GET | `/api/barbershops/{id}/activities` | 🔓 Público | Barbearia |
| 24 | POST | `/api/barbershops/register-my-shop` | 🔐 BARBER | Barbearia |
| 25 | PUT | `/api/barbershops/my-shop` | 🔐 OWNER | Barbearia |
| 26 | POST | `/api/barbershops/my-shop/activities` | 🔐 OWNER | Barbearia |
| 27 | PUT | `/api/barbershops/my-shop/activities/{id}` | 🔐 OWNER | Barbearia |
| 28 | DELETE | `/api/barbershops/my-shop/activities/{id}` | 🔐 OWNER | Barbearia |
| 29 | DELETE | `/api/barbershops/my-shop/remove-barber/{id}` | 🔐 OWNER | Barbearia |
| 30 | DELETE | `/api/barbershops/my-shop/close` | 🔐 OWNER | Barbearia |
| 31 | POST | `/api/barbershops/join-request` | 🔐 BARBER | Barbearia |
| 32 | GET | `/api/barbershops/my-shop/pending-requests` | 🔐 OWNER | Barbearia |
| 33 | POST | `/api/barbershops/my-shop/approve-request/{id}` | 🔐 OWNER | Barbearia |
| 34 | POST | `/api/barbershops/leave-shop` | 🔐 BARBER | Barbearia |
| 35 | POST | `/api/barbershops/my-shop/upload-logo` | 🔐 OWNER | Barbearia |
| 36 | POST | `/api/barbershops/my-shop/upload-banner` | 🔐 OWNER | Barbearia |
| 37 | POST | `/api/barbershops/my-shop/activities/{id}/upload-photo` | 🔐 OWNER | Barbearia |
| 38 | POST | `/api/barbershops/my-shop/highlights` | 🔐 OWNER | Barbearia |
| 39 | DELETE | `/api/barbershops/my-shop/highlights/{id}` | 🔐 OWNER | Barbearia |
| 40 | POST | `/api/appointments` | 🔐 Bearer | Agendamentos |
| 41 | GET | `/api/appointments/{id}` | 🔐 Bearer | Agendamentos |
| 42 | GET | `/api/appointments/my-appointments` | 🔐 Bearer | Agendamentos |
| 43 | GET | `/api/appointments/barber/{id}` | 🔐 Bearer | Agendamentos |
| 44 | GET | `/api/appointments/barbershop/{id}` | 🔐 Bearer | Agendamentos |
| 45 | GET | `/api/appointments/availability` | 🔓 Público | Agendamentos |
| 46 | PUT | `/api/appointments/{id}/cancel` | 🔐 Bearer | Agendamentos |
| 47 | PUT | `/api/appointments/{id}/confirm` | 🔐 BARBER | Agendamentos |
| 48 | PUT | `/api/appointments/{id}/conclude` | 🔐 BARBER | Agendamentos |
| 49 | POST | `/api/appointments/barber-blocks` | 🔐 BARBER | Agendamentos |
| 50 | GET | `/api/appointments/barber-blocks` | 🔓 Público | Agendamentos |
| 51 | DELETE | `/api/appointments/barber-blocks/{id}` | 🔐 BARBER | Agendamentos |
| 52 | POST | `/api/payments/create` | 🔐 Bearer | Pagamentos |
| 53 | GET | `/api/payments/{id}` | 🔐 Bearer | Pagamentos |
| 54 | GET | `/api/payments/my-payments` | 🔐 Bearer | Pagamentos |
| 55 | POST | `/api/payments/webhook` | 🔓 Público | Pagamentos |
| 56 | GET | `/api/payments/mp-connect` | 🔐 BARBER | Pagamentos |
| 57 | GET | `/api/payments/mp-callback` | 🔓 Público | Pagamentos |
| 58 | GET | `/api/products` | 🔓 Público | Produtos |
| 59 | GET | `/api/products/{id}` | 🔓 Público | Produtos |
| 60 | POST | `/api/products` | 🔐 OWNER | Produtos |
| 61 | PUT | `/api/products/{id}` | 🔐 OWNER | Produtos |
| 62 | DELETE | `/api/products/{id}` | 🔐 OWNER | Produtos |
| 63 | POST | `/api/orders` | 🔐 Bearer | Pedidos |
| 64 | GET | `/api/orders/my-orders` | 🔐 Bearer | Pedidos |
| 65 | GET | `/api/orders/shop-orders` | 🔐 Bearer | Pedidos |
| 66 | PUT | `/api/orders/{id}/status` | 🔐 OWNER | Pedidos |
| 67 | GET | `/api/notifications/my-notifications` | 🔐 Bearer | Notificações |
| 68 | GET | `/api/notifications/unread-count` | 🔐 Bearer | Notificações |
| 69 | PUT | `/api/notifications/{id}/read` | 🔐 Bearer | Notificações |

**Total: 69 endpoints** (19 públicos + 50 autenticados)

---

## 18. Sequência Mínima (30 min)

Execute essa sequência para validar o fluxo core completo do sistema:

```
1.1  → Registrar CUSTOMER      (salva customer_token, customer_id)
1.2  → Registrar BARBER        (salva barber_token, barber_id)
1.3  → Registrar OWNER         (salva owner_token)

─── login explícito se necessário ───
1.4  → Login CUSTOMER
1.5  → Login BARBER
1.6  → Login OWNER

3.1  → Owner cria barbearia    (salva barbershop_id; owner vira ROLE_OWNER)
3.2  → Confirmar role OWNER

5.1  → Owner cria atividade    (salva activity_id)

6.1  → Barbeiro solicita entrada (CNPJ)
6.2  → Owner lista pendentes   (salva join_request_id)
6.3  → Owner aprova

7.1  → Barbeiro vincula atividade

8.1  → Verificar disponibilidade
8.2  → Cliente cria agendamento  (salva appointment_id)
8.7  → Barbeiro confirma

9.1  → Cliente cria pagamento    (abre checkout URL no browser)

14.2 → Barbeiro conclui agendamento

11.1 → Verificar notificações geradas
11.3 → Marcar notificação como lida
```

> ⏱️ Tempo estimado: **~30 minutos**

---

## 19. Checklist Final

### 🔓 Endpoints Públicos

| Endpoint | Status |
|----------|--------|
| `POST /api/auth/email/register` | ☐ |
| `POST /api/auth/email/login` | ☐ |
| `POST /api/auth/verify` *(social)* | ☐ |
| `GET /api/auth/email/exists` | ☐ |
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

### 🔐 Endpoints Protegidos — Cobertura por Módulo

| Módulo | Endpoints | Testados |
|--------|-----------|---------|
| Auth — perfil e social | 3 | ☐ ☐ ☐ |
| Clientes — CRUD + foto | 4 | ☐ ☐ ☐ ☐ |
| Barbeiros — update, atividades | 3 | ☐ ☐ ☐ |
| Barbearia — criação, gestão, join | 16 | ☐×16 |
| Agendamentos | 9 | ☐×9 |
| Pagamentos — criar, consultar | 3 | ☐ ☐ ☐ |
| Produtos / Pedidos — escrita | 5 | ☐×5 |
| Notificações | 3 | ☐ ☐ ☐ |

### 🚨 Testes de Segurança

- [ ] Rota protegida **sem token** → `401`
- [ ] **Token inválido** → `401` ou `403`
- [ ] **Token expirado** → `401`
- [ ] CUSTOMER criando barbearia → `403`
- [ ] BARBER funcionário gerenciando barbearia → `403`
- [ ] Agendamento em horário **no passado** → `400`
- [ ] Agendamento em horário **já ocupado** → `409`
- [ ] Agendamento **fora do horário de trabalho** do barbeiro → `400`
- [ ] CNPJ **duplicado** ao criar barbearia → `409`
- [ ] Barbeiro confirmando agendamento de **outro barbeiro** → `403`
- [ ] Buscar recurso inexistente → `404`

---

*Atualizado em: Abril/2026 | Branch: `feature/migracao-microservicos` | 69 endpoints*
