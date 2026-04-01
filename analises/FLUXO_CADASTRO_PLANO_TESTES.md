# CortaAi — Fluxo de Cadastro/Login e Plano de Testes

> **Branch:** `feature/migracao-microservicos`  
> **Última atualização:** commit `83a523d`

---

## 1. Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE (Browser)                              │
│                           React 19 + Vite 7 + Nginx                         │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ HTTPS  (api.cortaai.shop)
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Cloudflare Tunnel                                   │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     API Gateway  (:8080)                                    │
│                  Spring Cloud Gateway (WebFlux)                             │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  FirebaseTokenGatewayFilter                                          │   │
│  │  • Lê Authorization: Bearer <idToken>                                │   │
│  │  • Valida token com Firebase Admin SDK                               │   │
│  │  • Bloqueia se emailVerified = false  (→ 401)                        │   │
│  │  • Injeta headers: X-User-UID, X-User-Email, X-User-Type            │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└──────┬──────────────┬──────────────┬──────────────┬────────────────┬────────┘
       │              │              │              │                │
       ▼              ▼              ▼              ▼                ▼
 user-service   barbershop-     schedule-    payment-service   notification-
    (:8081)      service          service       (:8084)          service
                  (:8082)          (:8083)                        (:8085)
       │
       ▼
 ┌─────────────┐     ┌────────────────────────────┐
 │  PostgreSQL  │     │  Firebase Auth             │
 │  (database) │     │  (Identity Toolkit REST +  │
 └─────────────┘     │   Firebase Admin SDK)       │
                     └────────────────────────────┘
```

**Eureka Discovery** (`:8761`) registra todos os serviços. O Gateway resolve rotas via `lb://nome-servico`.

---

## 2. Fluxo de Cadastro — Email/Password (Novo Fluxo)

### 2.1 Cadastro de Customer

```
Frontend                        API Gateway              user-service           Firebase
   │                                 │                        │                     │
   │  POST /api/auth/firebase-test   │                        │                     │
   │    /register-email              │                        │                     │
   │  Body (JSON):                   │                        │                     │
   │  { email, password, userType,   │                        │                     │
   │    name, tell, documentCPF }    │                        │                     │
   │────────────────────────────────►│                        │                     │
   │                                 │  (rota pública —       │                     │
   │                                 │   sem auth filter)     │                     │
   │                                 │───────────────────────►│                     │
   │                                 │                        │                     │
   │                                 │                        │  1. POST signUp     │
   │                                 │                        │     (REST Identity) │
   │                                 │                        │────────────────────►│
   │                                 │                        │◄────────────────────│
   │                                 │                        │  { idToken,         │
   │                                 │                        │    localId,         │
   │                                 │                        │    refreshToken }   │
   │                                 │                        │                     │
   │                                 │                        │  2. POST sendOobCode│
   │                                 │                        │     VERIFY_EMAIL    │
   │                                 │                        │────────────────────►│
   │                                 │                        │  (Firebase envia    │
   │                                 │                        │   e-mail ao user)   │
   │                                 │                        │                     │
   │                                 │                        │  3. verifyAndProvision
   │                                 │                        │  (Admin SDK valida  │
   │                                 │                        │   token, cria       │
   │                                 │                        │   Customer no DB)   │
   │                                 │                        │                     │
   │                                 │                        │  4. completeCustomerProfile
   │                                 │                        │  (salva name, tell, │
   │                                 │                        │   CPF, email        │
   │                                 │                        │   EXPLÍCITO)        │
   │                                 │                        │                     │
   │◄────────────────────────────────│◄───────────────────────│                     │
   │  200 OK                         │                        │                     │
   │  { idToken, localId,            │                        │                     │
   │    refreshToken, expiresIn,     │                        │                     │
   │    profile: { ... } }           │                        │                     │
   │                                 │                        │                     │
   │  [Frontend exibe tela           │                        │                     │
   │   "Verifique seu e-mail"]       │                        │                     │
```

### 2.2 Rollback em caso de falha

```
user-service                                    Firebase
     │                                              │
     │  1. signUp → localId = "xyz123"              │
     │─────────────────────────────────────────────►│
     │◄─────────────────────────────────────────────│
     │                                              │
     │  2. verifyAndProvision → FALHA (ex: DB down)│
     │  ┌─────────────────────────────────┐         │
     │  │ catch(Exception ex) {           │         │
     │  │   rollbackFirebaseUser(localId) │         │
     │  │   → firebaseAuth.deleteUser(    │         │
     │  │       "xyz123")                 │         │
     │  │ }                               │         │
     │  └─────────────────────────────────┘         │
     │─────────────────────────────────────────────►│
     │  DELETE user xyz123                          │
     │◄─────────────────────────────────────────────│
     │                                              │
     │  → lança RuntimeException ao frontend        │
     │  → usuário NÃO existe em lugar nenhum        │
     │    (Firebase limpo, DB não foi afetado)       │
```

### 2.3 Cadastro de Barber

Idêntico ao Customer, mas com campos extras no body:
```json
{
  "email": "barbeiro@email.com",
  "password": "Senha@123",
  "userType": "BARBER",
  "name": "João Barbeiro",
  "tell": "(11) 99999-9999",
  "documentCPF": "123.456.789-00",
  "workStartTime": "09:00",
  "workEndTime": "18:00",
  "isOwner": true
}
```

---

## 3. Fluxo de Login — Email/Password

```
Frontend                   API Gateway           user-service           Firebase
   │                            │                     │                     │
   │  POST /api/auth/           │                     │                     │
   │    firebase-test/          │                     │                     │
   │    sign-in-email           │                     │                     │
   │  { email, password }       │                     │                     │
   │───────────────────────────►│                     │                     │
   │                            │  (rota pública)     │                     │
   │                            │────────────────────►│                     │
   │                            │                     │  POST signInWithPassword
   │                            │                     │────────────────────►│
   │                            │                     │◄────────────────────│
   │                            │                     │  { idToken, localId }
   │                            │                     │                     │
   │◄───────────────────────────│◄────────────────────│                     │
   │  200 { idToken, localId }  │                     │                     │
   │                            │                     │                     │
   │  localStorage.setItem(     │                     │                     │
   │    'token', idToken)       │                     │                     │
   │                            │                     │                     │
   │  [Requisiç. autenticadas   │                     │                     │
   │   usam este token]         │                     │                     │
   │                            │                     │                     │
   │  GET /api/auth/me          │                     │                     │
   │  Authorization: Bearer ... │                     │                     │
   │───────────────────────────►│                     │                     │
   │                            │  Gateway valida     │                     │
   │                            │  token + injeta     │                     │
   │                            │  X-User-UID         │                     │
   │                            │────────────────────►│                     │
   │◄───────────────────────────│◄────────────────────│                     │
   │  { name, email, userType,  │                     │                     │
   │    profileComplete, ... }  │                     │                     │
```

**Importante:** Se `emailVerified = false`, o Gateway retorna `401` com body:
```json
{ "error": "E-mail ainda não verificado. Verifique sua caixa de entrada." }
```

---

## 4. Fluxo de Login Social (Google)

```
Frontend → Firebase SDK (popup) → obtém idToken
Frontend → POST /api/auth/complete-profile-customer
           Authorization: Bearer <idToken>
           Body: { name, tell, documentCPF }

Gateway → valida token → injeta X-User-UID, X-User-Email
user-service → completeCustomerProfile(uid, dto)
             → email vem do header X-User-Email
```

---

## 5. Plano de Testes

> **Base URL:** `https://api.cortaai.shop`  
> Para testes locais: `http://localhost:8080`

---

### TC-01 — Cadastro de Customer com sucesso

```bash
curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/register-email" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cliente_teste@gmail.com",
    "password": "Senha@123456",
    "userType": "CUSTOMER",
    "name": "Cliente Teste",
    "tell": "11999999999",
    "documentCPF": "12345678900"
  }' | jq .
```

**Resultado esperado:**
```json
{
  "idToken": "<jwt>",
  "localId": "<uid>",
  "refreshToken": "<token>",
  "expiresIn": "3600",
  "profile": {
    "name": "Cliente Teste",
    "email": "cliente_teste@gmail.com",
    "userType": "CUSTOMER",
    "profileComplete": true
  }
}
```

**Verificar também:** e-mail de verificação recebido na caixa de entrada.

---

### TC-02 — Cadastro de Barbeiro com sucesso

```bash
curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/register-email" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "barbeiro_teste@gmail.com",
    "password": "Senha@123456",
    "userType": "BARBER",
    "name": "Barbeiro Teste",
    "tell": "11988888888",
    "documentCPF": "98765432100",
    "workStartTime": "09:00",
    "workEndTime": "18:00",
    "isOwner": true
  }' | jq .
```

**Resultado esperado:** HTTP 200 com `profile.userType = "BARBER"` e e-mail de verificação.

---

### TC-03 — Login antes de verificar e-mail

```bash
# 1. Cadastrar (TC-01 acima), NÃO verificar o e-mail
# 2. Tentar login:
curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/sign-in-email" \
  -H "Content-Type: application/json" \
  -d '{ "email": "cliente_teste@gmail.com", "password": "Senha@123456" }' | jq .

# 3. Tentar acessar rota protegida com o token retornado:
TOKEN=$(curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/sign-in-email" \
  -H "Content-Type: application/json" \
  -d '{ "email": "cliente_teste@gmail.com", "password": "Senha@123456" }' \
  | jq -r '.idToken')

curl -s -X GET "https://api.cortaai.shop/api/auth/me" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Resultado esperado:** HTTP 401
```json
{ "error": "E-mail ainda não verificado. Verifique sua caixa de entrada." }
```

---

### TC-04 — Login após verificar e-mail

```bash
# 1. Verificar e-mail clicando no link recebido
# 2. Obter novo token (token antigo pode ter emailVerified=false em cache)
TOKEN=$(curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/sign-in-email" \
  -H "Content-Type: application/json" \
  -d '{ "email": "cliente_teste@gmail.com", "password": "Senha@123456" }' \
  | jq -r '.idToken')

curl -s -X GET "https://api.cortaai.shop/api/auth/me" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Resultado esperado:** HTTP 200 com dados do perfil.

---

### TC-05 — Cadastro com e-mail duplicado

```bash
# Rodar TC-01 duas vezes com o mesmo e-mail
curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/register-email" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cliente_teste@gmail.com",
    "password": "Senha@123456",
    "userType": "CUSTOMER",
    "name": "Cliente Duplicado",
    "tell": "11999999999",
    "documentCPF": "12345678900"
  }' | jq .
```

**Resultado esperado:** HTTP 400 ou 409
```json
{ "error": "EMAIL_EXISTS" }
```

---

### TC-06 — Cadastro com senha fraca

```bash
curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/register-email" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "novo_user@gmail.com",
    "password": "123",
    "userType": "CUSTOMER",
    "name": "Teste Fraco",
    "tell": "11999999999",
    "documentCPF": "11122233344"
  }' | jq .
```

**Resultado esperado:** HTTP 400
```json
{ "error": "WEAK_PASSWORD" }
```

---

### TC-07 — Login com senha errada

```bash
curl -s -X POST "https://api.cortaai.shop/api/auth/firebase-test/sign-in-email" \
  -H "Content-Type: application/json" \
  -d '{ "email": "cliente_teste@gmail.com", "password": "SenhaErrada" }' | jq .
```

**Resultado esperado:** HTTP 400
```json
{ "error": "INVALID_PASSWORD" }
```

---

### TC-08 — GET /me com token válido

```bash
TOKEN="<idToken do login>"
curl -s -X GET "https://api.cortaai.shop/api/auth/me" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Resultado esperado:** HTTP 200
```json
{
  "name": "Cliente Teste",
  "email": "cliente_teste@gmail.com",
  "userType": "CUSTOMER",
  "profileComplete": true,
  "firebaseUid": "<uid>"
}
```

---

### TC-09 — GET /me sem token

```bash
curl -s -X GET "https://api.cortaai.shop/api/auth/me" | jq .
```

**Resultado esperado:** HTTP 401
```json
{ "error": "Token de autenticação não fornecido." }
```

---

### TC-10 — Rollback de registro (simular falha de DB)

> **Testar em ambiente de desenvolvimento apenas.**

1. Derrubar o PostgreSQL: `docker stop <nome_container_db>`
2. Tentar cadastro (TC-01)
3. **Resultado esperado:** HTTP 500 ou 503
4. **Verificar no Firebase Console:** usuário NÃO deve existir (foi deletado pelo rollback)
5. Subir DB novamente: `docker start <nome_container_db>`

**Log esperado no user-service:**
```
WARN  event=register-rollback uid=<localId> — Firebase user deleted due to backend failure
```

---

## 6. Tabela Resumo de Cenários

| # | Cenário | Método | Endpoint | Esperado |
|---|---------|--------|----------|----------|
| TC-01 | Cadastro customer válido | POST | `/api/auth/firebase-test/register-email` | 200 + e-mail verificação |
| TC-02 | Cadastro barber válido | POST | `/api/auth/firebase-test/register-email` | 200 + e-mail verificação |
| TC-03 | Login sem verificar e-mail | GET | `/api/auth/me` | 401 emailNotVerified |
| TC-04 | Login após verificar e-mail | GET | `/api/auth/me` | 200 perfil |
| TC-05 | E-mail duplicado | POST | `/api/auth/firebase-test/register-email` | 400/409 EMAIL_EXISTS |
| TC-06 | Senha fraca | POST | `/api/auth/firebase-test/register-email` | 400 WEAK_PASSWORD |
| TC-07 | Senha errada no login | POST | `/api/auth/firebase-test/sign-in-email` | 400 INVALID_PASSWORD |
| TC-08 | GET /me token válido | GET | `/api/auth/me` | 200 perfil completo |
| TC-09 | GET /me sem token | GET | `/api/auth/me` | 401 |
| TC-10 | Rollback falha de DB | POST | `/api/auth/firebase-test/register-email` | 5xx + user removido do Firebase |

---

## 7. Variáveis de Ambiente Necessárias

| Variável | Serviço | Descrição |
|----------|---------|-----------|
| `FIREBASE_WEB_API_KEY` | user-service | Chave Web do projeto Firebase (console → Configurações) |
| `GOOGLE_APPLICATION_CREDENTIALS` | user-service | Path para `firebase-service-account.json` |
| `SPRING_DATASOURCE_URL` | user-service | JDBC URL do PostgreSQL |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | todos | URL do Eureka Discovery |

---

## 8. Observações Importantes

1. **Token de verificação de e-mail:** Após cadastro, o frontend exibe a tela `"Verifique seu e-mail"`. O usuário só consegue usar rotas protegidas após clicar no link recebido e fazer login novamente (para obter um token com `emailVerified=true`).

2. **Rollback automático:** Se qualquer etapa após o `signUp` (provisionamento no DB, `completeProfile`) falhar, o `FirebaseDebugServiceImpl.rollbackFirebaseUser()` invoca `firebaseAuth.deleteUser(uid)` para manter consistência. Logs são gravados com `event=register-rollback`.

3. **Email placeholder eliminado:** O campo `email` do Customer/Barber no banco era preenchido com `uid@firebase.local` quando o SecurityContext não tinha o e-mail disponível (ex.: chamadas internas sem header). Isso foi corrigido — o `registerWithEmailPassword` agora passa o e-mail explicitamente via overload.

4. **Rota `/register-email` é pública:** O `FirebaseTokenGatewayFilter` não intercepta este endpoint para não criar dependência circular (o usuário não tem token antes de se cadastrar).
