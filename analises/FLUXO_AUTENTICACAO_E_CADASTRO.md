# Fluxo de Autenticação e Cadastro — CortaAí

> **Base URL:** `https://api.cortaai.shop`  
> **Versão:** pós-commit `69ba5ec` (23/03/2026)  
> **Autenticação:** Firebase ID Token via `Authorization: Bearer <token>`

---

## Sumário

1. [Barbeiro Owner — Login + Cadastro de Barbearia](#1-barbeiro-owner)
2. [Usuário Comum (Customer) — Cadastro e Login](#2-customer)
3. [Barbeiro Comum — Cadastro e Login](#3-barbeiro-comum)

---

## Como funciona internamente

```
Cliente (App/Postman)
    │
    ├─ Firebase SDK (signInWithEmailAndPassword / signInWithPopup)
    │       └─ retorna: idToken (JWT Firebase)
    │
    ▼
POST /api/auth/verify  ←── rota PÚBLICA (token vai no BODY)
    │
    ├─ user-service valida token com Firebase Admin SDK
    ├─ busca usuário por firebase_uid no banco
    └─ retorna: { profileComplete, userType, id, ... }
            │
            ├─ profileComplete = false → chamar complete-profile
            └─ profileComplete = true  → pode usar a API normalmente

Requisições protegidas:
    Authorization: Bearer <idToken>
        │
        ▼
    API Gateway (porta 8082)
        ├─ valida token Firebase
        ├─ injeta X-User-UID: "59GSw6dVmiMn6rN4Lp857lzTsv13"
        ├─ injeta X-User-Email: "email@exemplo.com" (pode ser vazio)
        └─ repassa para o serviço destino

Serviços downstream:
    principal.getName() = X-User-UID (Firebase UID — sempre presente)
    resolveUserByUid(uid) → GET /api/internal/users/by-firebase-uid/{uid}
```

---

## 1. Barbeiro Owner

> Fluxo completo: criar conta → completar perfil → cadastrar barbearia

### Passo 1 — Criar conta no Firebase (e-mail/senha)

**Requisição direta ao Firebase REST API:**

```http
POST https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=YOUR_FIREBASE_WEB_API_KEY
Content-Type: application/json

{
  "email": "dono@barbearia.com",
  "password": "SenhaForte123!",
  "returnSecureToken": true
}
```

**Resposta:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiJ9...",
  "localId": "59GSw6dVmiMn6rN4Lp857lzTsv13",
  "email": "dono@barbearia.com",
  "expiresIn": "3600"
}
```

> 💡 Em produção, o SDK do Firebase no app mobile/web faz isso automaticamente.

---

### Passo 2 — Verificar/Provisionar no CortaAí

```http
POST https://api.cortaai.shop/api/auth/verify
Content-Type: application/json

{
  "idToken": "eyJhbGciOiJSUzI1NiJ9...",
  "userType": "BARBER"
}
```

**Resposta esperada (primeiro acesso):**
```json
{
  "id": null,
  "name": "Usuário",
  "email": "dono@barbearia.com",
  "phone": null,
  "photoUrl": null,
  "userType": "BARBER",
  "authProvider": "EMAIL",
  "profileComplete": false,
  "role": "ROLE_BARBER"
}
```

> ⚠️ `profileComplete: false` → precisa completar o perfil antes de usar a API.

---

### Passo 3 — Completar perfil do barbeiro

```http
POST https://api.cortaai.shop/api/auth/barbers/complete-profile
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json

{
  "name": "João Silva",
  "tell": "11999999999",
  "documentCPF": "12345678901",
  "workStartTime": "08:00",
  "workEndTime": "18:00",
  "isOwner": true
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `name` | String | ❌ | Substitui nome do Firebase se informado |
| `tell` | String | ✅ | 10–15 dígitos, aceita `+` |
| `documentCPF` | String | ✅ | 11–14 caracteres |
| `workStartTime` | String | ✅ | Formato `HH:mm` |
| `workEndTime` | String | ✅ | Formato `HH:mm` |
| `isOwner` | boolean | ✅ | `true` para dono de barbearia |

**Resposta:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "João Silva",
  "email": "dono@barbearia.com",
  "phone": "11999999999",
  "photoUrl": null,
  "userType": "BARBER",
  "authProvider": "EMAIL",
  "profileComplete": true,
  "role": "ROLE_BARBER"
}
```

---

### Passo 4 — Cadastrar a Barbearia

```http
POST https://api.cortaai.shop/api/barbershops/register-my-shop
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json

{
  "name": "Barbearia do João",
  "cnpj": "12345678000190",
  "address": "Rua das Flores, 123 - Centro, São Paulo/SP"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `name` | String | ✅ | Máx. 255 chars, sem caracteres especiais |
| `cnpj` | String | ✅ | Exatamente 14 dígitos numéricos (sem máscara) |
| `address` | String | ❌ | Máx. 255 chars |

**Resposta (201 Created):**
```json
{
  "id": "d5face7e-4b81-4681-b80f-673b2c59a312",
  "name": "Barbearia do João",
  "cnpj": "12345678000190",
  "address": "Rua das Flores, 123 - Centro, São Paulo/SP",
  "logoUrl": null,
  "bannerUrl": null,
  "ownerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### Passo 5 (opcional) — Upload do logo

```http
POST https://api.cortaai.shop/api/barbershops/my-shop/upload-logo
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: multipart/form-data

file: [arquivo de imagem]
```

---

### Fluxo resumido — Barbeiro Owner

```
[Firebase] signUp(email, senha)
    └─▶ idToken

[CortaAí] POST /api/auth/verify  { idToken, userType: "BARBER" }
    └─▶ profileComplete: false

[CortaAí] POST /api/auth/barbers/complete-profile  { name, tell, cpf, horários, isOwner: true }
    └─▶ profileComplete: true, id: "uuid-do-barber"

[CortaAí] POST /api/barbershops/register-my-shop  { name, cnpj, address }
    └─▶ 201 Created — barbearia criada ✅
```

---

## 2. Customer

> Fluxo completo: criar conta → completar perfil → uso da plataforma

### Passo 1 — Criar conta no Firebase (e-mail/senha)

```http
POST https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=YOUR_FIREBASE_WEB_API_KEY
Content-Type: application/json

{
  "email": "cliente@email.com",
  "password": "SenhaForte123!",
  "returnSecureToken": true
}
```

**Resposta:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiJ9...",
  "localId": "xYzAbC123defGHI456jkl",
  "email": "cliente@email.com",
  "expiresIn": "3600"
}
```

---

### Passo 2 — Verificar/Provisionar no CortaAí

```http
POST https://api.cortaai.shop/api/auth/verify
Content-Type: application/json

{
  "idToken": "eyJhbGciOiJSUzI1NiJ9...",
  "userType": "CUSTOMER"
}
```

**Resposta:**
```json
{
  "id": null,
  "name": "Usuário",
  "email": "cliente@email.com",
  "phone": null,
  "photoUrl": null,
  "userType": "CUSTOMER",
  "authProvider": "EMAIL",
  "profileComplete": false,
  "role": "ROLE_CUSTOMER"
}
```

---

### Passo 3 — Completar perfil do cliente

```http
POST https://api.cortaai.shop/api/auth/customers/complete-profile
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json

{
  "name": "Maria Souza",
  "tell": "11988887777",
  "documentCPF": "98765432100"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `name` | String | ❌ | Substitui nome do Firebase se informado |
| `tell` | String | ✅ | 10–15 dígitos, aceita `+` |
| `documentCPF` | String | ✅ | 11–14 caracteres |

**Resposta:**
```json
{
  "id": "f7e8d9c0-b1a2-3456-7890-abcdef123456",
  "name": "Maria Souza",
  "email": "cliente@email.com",
  "phone": "11988887777",
  "photoUrl": null,
  "userType": "CUSTOMER",
  "authProvider": "EMAIL",
  "profileComplete": true,
  "role": "ROLE_CUSTOMER"
}
```

---

### Login subsequente (Customer)

```http
POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=YOUR_FIREBASE_WEB_API_KEY
Content-Type: application/json

{
  "email": "cliente@email.com",
  "password": "SenhaForte123!",
  "returnSecureToken": true
}
```
→ Obtém novo `idToken` (válido por 1 hora) e usa em todas as requisições.

---

### Verificar dados do próprio perfil

```http
GET https://api.cortaai.shop/api/auth/me
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

---

### Fluxo resumido — Customer

```
[Firebase] signUp(email, senha)
    └─▶ idToken

[CortaAí] POST /api/auth/verify  { idToken, userType: "CUSTOMER" }
    └─▶ profileComplete: false

[CortaAí] POST /api/auth/customers/complete-profile  { name, tell, cpf }
    └─▶ profileComplete: true ✅

[Uso normal] GET/POST com Authorization: Bearer <idToken>
```

---

## 3. Barbeiro Comum

> Barbeiro que trabalha em uma barbearia existente (não é dono).  
> Após o cadastro, ele envia uma solicitação para entrar em uma barbearia.

### Passos 1 e 2 — Idênticos ao Owner

```http
POST https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=YOUR_FIREBASE_WEB_API_KEY
Content-Type: application/json

{
  "email": "barbeiro@email.com",
  "password": "SenhaForte123!",
  "returnSecureToken": true
}
```

```http
POST https://api.cortaai.shop/api/auth/verify
Content-Type: application/json

{
  "idToken": "eyJhbGciOiJSUzI1NiJ9...",
  "userType": "BARBER"
}
```

---

### Passo 3 — Completar perfil do barbeiro

```http
POST https://api.cortaai.shop/api/auth/barbers/complete-profile
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json

{
  "name": "Carlos Pereira",
  "tell": "11977776666",
  "documentCPF": "11122233344",
  "workStartTime": "09:00",
  "workEndTime": "17:00",
  "isOwner": false
}
```

> ⚠️ `"isOwner": false` — diferença em relação ao Owner.

**Resposta:**
```json
{
  "id": "c3d4e5f6-a7b8-9012-cdef-345678901234",
  "name": "Carlos Pereira",
  "email": "barbeiro@email.com",
  "phone": "11977776666",
  "photoUrl": null,
  "userType": "BARBER",
  "authProvider": "EMAIL",
  "profileComplete": true,
  "role": "ROLE_BARBER"
}
```

---

### Passo 4 — Solicitar entrada em uma barbearia

```http
POST https://api.cortaai.shop/api/barbershops/join-request
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json

{
  "cnpj": "12345678000190"
}
```

**Resposta (200 OK):**
```json
{
  "message": "Solicitação enviada com sucesso. Aguardando aprovação do dono."
}
```

---

### Passo 5 — Owner aprova a solicitação

O dono da barbearia lista as solicitações pendentes:

```http
GET https://api.cortaai.shop/api/barbershops/join-requests/pending
Authorization: Bearer <token-do-owner>
```

E aprova:

```http
POST https://api.cortaai.shop/api/barbershops/join-requests/{requestId}/approve
Authorization: Bearer <token-do-owner>
```

→ O `barbershopId` do barbeiro comum é atualizado automaticamente no user-service.

---

### Login subsequente (Barbeiro Comum)

```http
POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=YOUR_FIREBASE_WEB_API_KEY
Content-Type: application/json

{
  "email": "barbeiro@email.com",
  "password": "SenhaForte123!",
  "returnSecureToken": true
}
```

---

### Fluxo resumido — Barbeiro Comum

```
[Firebase] signUp(email, senha)
    └─▶ idToken

[CortaAí] POST /api/auth/verify  { idToken, userType: "BARBER" }
    └─▶ profileComplete: false

[CortaAí] POST /api/auth/barbers/complete-profile  { name, tell, cpf, horários, isOwner: false }
    └─▶ profileComplete: true ✅

[CortaAí] POST /api/barbershops/join-request  { cnpj: "cnpj-da-barbearia" }
    └─▶ Solicitação criada — status: PENDING

[Owner] POST /api/barbershops/join-requests/{id}/approve
    └─▶ Barbeiro vinculado à barbearia ✅
```

---

## Tabela comparativa dos 3 perfis

| Etapa | Customer | Barbeiro Comum | Barbeiro Owner |
|---|---|---|---|
| Criar conta Firebase | ✅ `signUp` | ✅ `signUp` | ✅ `signUp` |
| `POST /api/auth/verify` | `userType: CUSTOMER` | `userType: BARBER` | `userType: BARBER` |
| `complete-profile` | `/customers/complete-profile` | `/barbers/complete-profile` | `/barbers/complete-profile` |
| `isOwner` no perfil | — | `false` | `true` |
| Registrar barbearia | ❌ | ❌ | ✅ `/register-my-shop` |
| Solicitar entrada | ❌ | ✅ `/join-request` | ❌ |
| Login | `signInWithPassword` | `signInWithPassword` | `signInWithPassword` |

---

## Erros comuns

| Erro | Causa | Solução |
|---|---|---|
| `401 Token Firebase inválido` | Token expirado (>1h) | Obter novo token com `signInWithPassword` |
| `404 Usuário não encontrado` | `complete-profile` não foi chamado | Chamar `/complete-profile` antes |
| `409 Você já possui uma barbearia` | `register-my-shop` chamado 2x | Usar `PUT /my-shop` para atualizar |
| `409 CNPJ já cadastrado` | CNPJ já em uso | Verificar CNPJ informado |
| `403 Apenas barbeiros podem gerenciar barbearias` | Customer tentou registrar barbearia | Usar conta `BARBER` |

---

*Documento gerado automaticamente com base no código-fonte — commit `69ba5ec` — 23/03/2026*
