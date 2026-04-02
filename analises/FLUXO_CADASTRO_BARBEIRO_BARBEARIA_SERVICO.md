# Fluxo Completo: Cadastro do Barbeiro, Barbearia e Serviços

> **Data:** Abril de 2026  
> **Branch:** `feature/migracao-microservicos`  
> **Arquitetura:** Microserviços (API Gateway → user-service → barbershop-service)

---

## Índice

1. [Visão Geral da Arquitetura](#1-visão-geral-da-arquitetura)
2. [Componentes Envolvidos](#2-componentes-envolvidos)
3. [Fase 1 — Cadastro do Barbeiro](#3-fase-1--cadastro-do-barbeiro)
4. [Fase 2 — Verificação de E-mail e Primeiro Login](#4-fase-2--verificação-de-e-mail-e-primeiro-login)
5. [Fase 3 — Completar Perfil do Barbeiro](#5-fase-3--completar-perfil-do-barbeiro)
6. [Fase 4 — Cadastro da Barbearia](#6-fase-4--cadastro-da-barbearia)
7. [Fase 5 — Cadastro de Serviços](#7-fase-5--cadastro-de-serviços)
8. [Fase 6 — Atribuição de Habilidades ao Barbeiro](#8-fase-6--atribuição-de-habilidades-ao-barbeiro)
9. [Tabela de Endpoints](#9-tabela-de-endpoints)
10. [Tabela de Dados Trafegados](#10-tabela-de-dados-trafegados)
11. [Diagrama de Sequência Completo](#11-diagrama-de-sequência-completo)

---

## 1. Visão Geral da Arquitetura

```
[Navegador / App React]
        │
        │ HTTPS  (Authorization: Bearer <Firebase ID Token>)
        ▼
[API Gateway — porta 8080]          ← Spring Cloud Gateway
        │  valida token Firebase
        │  injeta headers internos:
        │    X-User-UID    = uid do Firebase
        │    X-User-Email  = email do usuário
        │    X-User-Type   = BARBER | CUSTOMER
        │
        ├──► [user-service — porta 8081]        banco: user_db (MySQL)
        │         └── tabelas: customers, barbers, barber_assigned_activities
        │
        └──► [barbershop-service — porta 8082]  banco: barbershop_db (MySQL)
                  └── tabelas: barbershops, activities, barbershop_highlights,
                               barbershop_join_requests
```

**Regra de roteamento do Gateway (application.yml):**

| Prefixo de URL             | Destino              |
|----------------------------|----------------------|
| `/api/auth/**`             | user-service         |
| `/api/customers/**`        | user-service         |
| `/api/barbers/**`          | user-service         |
| `/api/barbershops/**`      | barbershop-service   |
| `/api/internal/**`         | interno (não exposto)|

---

## 2. Componentes Envolvidos

### Frontend (React + Vite)

| Arquivo                                     | Responsabilidade                                                                 |
|---------------------------------------------|----------------------------------------------------------------------------------|
| `pages/IdentificacaoPage.jsx`               | Tela de seleção: cliente ou barbeiro. Guarda `userType` no estado.               |
| `pages/CadastroPage.jsx`                    | Formulário de cadastro por e-mail/senha. Chama `authService.registerWithEmail()`. |
| `services/authService.js`                   | Funções de autenticação: `registerWithEmail`, `loginWithEmail`, `verifyToken`.   |
| `services/api.js`                           | Instância Axios com `baseURL` e interceptor que injeta `Authorization: Bearer`. |
| `pages/BarberHomePage.jsx`                  | Home do barbeiro. Chama `GET /auth/me` ao montar. Exibe painel conforme `barbershopId` e `isOwner`. |
| `pages/CreateBarbershopPage.jsx`            | Formulário de criação de barbearia. Envia multipart com `shop` (JSON) + `file`.  |
| `pages/BarberServicesPage.jsx`              | Página de serviços. Carrega dados do barbeiro via `GET /auth/me`.                |
| `services/barbershopService.js`             | `createBarbershop`, `createService`, `deleteService`, `getMyServices`, etc.      |
| `components/BarberPage/ManageMySkills.jsx`  | Gerenciamento de habilidades: lista serviços da shop e salva seleção do barbeiro.|

### Backend — user-service

| Classe                        | Responsabilidade                                                                         |
|-------------------------------|------------------------------------------------------------------------------------------|
| `FirebaseTokenGatewayFilter`  | (no Gateway) Valida Firebase ID Token; injeta `X-User-UID`, `X-User-Email`, `X-User-Type`. |
| `AuthController`              | Endpoints de auth: `/verify`, `/me`, `/customers/complete-profile`, `/barbers/complete-profile`. |
| `BarberController`            | CRUD de barbeiros + `GET /barbers/me/my-activities` + `POST /barbers/me/assign-activities`. |
| `FirebaseAuthServiceImpl`     | Lógica central: valida token, provisiona usuário, retorna `AuthResponseDTO`.             |
| `BarberServiceImpl`           | Atualização de perfil, busca, e gerenciamento de `assignedActivityIds`.                  |
| `Barber` (entidade JPA)       | Tabela `barbers` + coleção `barber_assigned_activities`.                                 |
| `AuthResponseDTO` (record)    | DTO de resposta completo (13 campos, incluindo `barbershopId` e `isOwner`).              |

### Backend — barbershop-service

| Classe                 | Responsabilidade                                                                        |
|------------------------|-----------------------------------------------------------------------------------------|
| `BarbershopController` | Endpoints: criar barbearia, listar, criar/editar/deletar serviços, join requests.       |
| `BarbershopService`    | Lógica de negócio: cria barbearia, valida dono, vincula barbeiro, promove a owner.       |
| `UserServiceClient`    | Feign Client: chama user-service internamente para ler/atualizar dados do barbeiro.     |
| `Barbershop` (entidade)| Tabela `barbershops`.                                                                   |
| `Activity` (entidade)  | Tabela `activities` — serviços oferecidos pela barbearia.                               |

---

## 3. Fase 1 — Cadastro do Barbeiro

### O que acontece

O usuário escolhe "Sou barbeiro" na tela de identificação e preenche nome, e-mail e senha.

### Responsabilidade do Frontend

- `IdentificacaoPage.jsx` salva `userType = "barber"` no estado da navegação.
- `CadastroPage.jsx` coleta `{ name, email, password }` e chama `authService.registerWithEmail(email, password, name, "BARBER")`.

### Chamada HTTP

```
POST /api/auth/email/register
Content-Type: application/json

{
  "email": "joao@barbearia.com",
  "password": "Senha@123",
  "name": "João Silva",
  "userType": "BARBER"
}
```

**Por que esses dados?**
- `email` + `password` → necessários para criar a conta no Firebase Authentication via REST API (`signUp`).
- `name` → armazenado no perfil Firebase e retornado no token.
- `userType: "BARBER"` → informa ao backend qual tabela usar (`barbers`) ao provisionar.

### Responsabilidade do Backend (user-service)

**`FirebaseDebugServiceImpl.registerWithEmailPassword()`:**

1. Chama a Firebase Auth REST API (`identitytoolkit.googleapis.com/v1/accounts:signUp`) com `email + password`.
2. Firebase retorna `{ idToken, localId (uid), refreshToken }`.
3. Atualiza o `displayName` no Firebase (segunda chamada REST: `accounts:update`).
4. Envia e-mail de verificação (terceira chamada REST: `accounts:sendOobCode`, type=`VERIFY_EMAIL`).
5. Chama internamente `verifyAndProvision(idToken, userType)` para criar o registro no banco.
6. Como o e-mail ainda **não está verificado**, retorna `verificationRequired: true`.

### O que é retornado ao Frontend

```json
{
  "idToken": "<firebase-id-token>",
  "localId": "<firebase-uid>",
  "profile": {
    "id": null,
    "name": "João Silva",
    "email": "joao@barbearia.com",
    "phone": null,
    "photoUrl": null,
    "userType": "BARBER",
    "authProvider": "EMAIL",
    "profileComplete": false,
    "role": "ROLE_BARBER",
    "emailVerified": false,
    "verificationRequired": true,
    "barbershopId": null,
    "isOwner": null
  }
}
```

**Frontend:** Exibe mensagem "Verifique seu e-mail antes de continuar" e **não salva** o token. Aguarda o usuário clicar no link de verificação.

---

## 4. Fase 2 — Verificação de E-mail e Primeiro Login

### O que acontece

O usuário recebe o e-mail do Firebase, clica no link de verificação, depois volta ao app e faz login.

### Responsabilidade do Frontend

1. `authService.loginWithEmail(email, password)` → chama Firebase REST API `accounts:signInWithPassword`.
2. Recebe novo `idToken` (agora com `email_verified: true`).
3. Chama `authService.verifyToken(idToken, null)`.

### Chamada HTTP

```
POST /api/auth/verify
Content-Type: application/json

{
  "idToken": "<firebase-id-token-verificado>",
  "userType": null
}
```

**Por que `userType: null`?** O usuário já existe no banco. O backend busca primeiro por UID na tabela `barbers` — se achar, ignora o `userType` do request e retorna os dados do barbeiro.

### Responsabilidade do Backend (user-service)

**`FirebaseAuthServiceImpl.verifyAndProvision()`:**

1. Valida o `idToken` com `FirebaseAuth.verifyIdToken()`.
2. Extrai `uid`, `email`, `provider`, `emailVerified`.
3. Verifica se e-mail está verificado → `verificationRequired = false` (passou!).
4. Busca `barberRepository.findByFirebaseUid(uid)` → **encontra** o barbeiro cadastrado.
5. Verifica `profileComplete`: `tell == null || documentCPF == null || workStartTime == null || workEndTime == null` → **false** (perfil incompleto).

### O que é retornado ao Frontend

```json
{
  "id": "<uuid-do-barbeiro>",
  "name": "João Silva",
  "email": "joao@barbearia.com",
  "phone": null,
  "photoUrl": null,
  "userType": "BARBER",
  "authProvider": "EMAIL",
  "profileComplete": false,
  "role": "ROLE_BARBER",
  "emailVerified": true,
  "verificationRequired": false,
  "barbershopId": null,
  "isOwner": false
}
```

**Frontend:** Salva `idToken` no `localStorage` como `token`. Como `profileComplete: false`, redireciona para a tela de completar perfil.

---

## 5. Fase 3 — Completar Perfil do Barbeiro

### O que acontece

O barbeiro preenche CPF, telefone e horários de trabalho.

### Responsabilidade do Frontend

- Tela coleta `{ tell, documentCPF, workStartTime, workEndTime }`.
- `workStartTime` e `workEndTime` são strings no formato `"HH:mm"`.
- Chama `api.post('/auth/barbers/complete-profile', dados)`.
- Token de autorização é injetado automaticamente pelo interceptor do `api.js`.

### Chamada HTTP

```
POST /api/auth/barbers/complete-profile
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

{
  "tell": "+5511987654321",
  "documentCPF": "123.456.789-09",
  "name": "João Silva",
  "workStartTime": "09:00",
  "workEndTime": "18:00",
  "isOwner": false
}
```

**Por que esses dados?**
- `tell` → telefone para contato com clientes; validado como `^\\+?[0-9]{10,15}$`.
- `documentCPF` → CPF para identificação; aceita com ou sem formatação (11–14 chars).
- `workStartTime / workEndTime` → necessários para o sistema de agendamentos calcular disponibilidade.
- `name` → opcional, atualiza o nome se informado.
- `isOwner: false` → barbeiro novo ainda não é dono de nenhuma barbearia.

**Como o Gateway processa:**  
O `FirebaseTokenGatewayFilter` valida o `Bearer token`, extrai o `uid` e injeta o header `X-User-UID` para o user-service.

### Responsabilidade do Backend (user-service)

**`FirebaseAuthServiceImpl.completeBarberProfile(firebaseUid, dto)`:**

1. Busca o barbeiro por `firebaseUid` na tabela `barbers`.
2. Atualiza os campos: `tell`, `documentCPF`, `workStartTime`, `workEndTime`, `isOwner`.
3. Garante que `email` e `name` estejam preenchidos (fallback).
4. Chama `barberRepository.saveAndFlush(barber)`.
5. Retorna `AuthResponseDTO` com `profileComplete: true`.

### O que é retornado ao Frontend

```json
{
  "id": "<uuid-do-barbeiro>",
  "name": "João Silva",
  "email": "joao@barbearia.com",
  "phone": "+5511987654321",
  "userType": "BARBER",
  "profileComplete": true,
  "role": "ROLE_BARBER",
  "emailVerified": true,
  "verificationRequired": false,
  "barbershopId": null,
  "isOwner": false
}
```

**Frontend:** Navega para `/barberHome`. O `BarberHomePage` monta e chama `GET /auth/me`. Como `barbershopId: null`, exibe o painel **"Cadastrar Barbearia"**.

---

## 6. Fase 4 — Cadastro da Barbearia

### O que acontece

O barbeiro preenche o formulário de criação da barbearia (nome, CNPJ, endereço e foto opcional).

### Responsabilidade do Frontend

- `CreateBarbershopPage.jsx` coleta `{ name, cnpj, address }` e o arquivo de imagem (opcional).
- **Limpa o CNPJ** antes de enviar: `cnpj.replace(/\D/g, '')` → remove traços e pontos, envia apenas os 14 dígitos.
- Monta um `FormData` com:
  - `shop` → Blob JSON com `Content-Type: application/json`
  - `file` → arquivo de imagem (opcional)
- Chama `barbershopService.createBarbershop(dadosDaShop, file)`.

### Chamada HTTP

```
POST /api/barbershops/register-my-shop
Authorization: Bearer <firebase-id-token>
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="shop"
Content-Type: application/json

{
  "name": "Barbearia do João",
  "cnpj": "12345678000190",
  "address": "Rua das Tesouras, 42 - São Paulo/SP"
}
--boundary
Content-Disposition: form-data; name="file"; filename="logo.jpg"
Content-Type: image/jpeg

<bytes da imagem>
--boundary--
```

**Por que multipart?**  
A barbearia pode ter uma logo. Misturar JSON com arquivo binário exige `multipart/form-data`. O JSON vai na parte `shop` e a imagem em `file`.

**Por que CNPJ sem formatação?**  
O DTO valida `@Pattern(regexp = "^[0-9]{14}$")` — aceita apenas 14 dígitos numéricos.

**Como o Gateway processa:**  
O `FirebaseTokenGatewayFilter` valida o token → injeta `X-User-UID`.  
O `SecurityConfig` do barbershop-service lê `X-User-UID` e seta como `principal.getName()`.

### Responsabilidade do Backend (barbershop-service)

**`BarbershopService.createBarbershop(ownerUid, dto, logoFile)`:**

1. **Resolve o usuário:** `resolveUserByUid(ownerUid)` → Feign `GET /api/internal/users/by-firebase-uid/{uid}` → retorna `UserInfoDTO` com `id` (UUID interno), `userType`, `barbershopId`.
2. **Valida:** `assertOwner(owner)` → verifica que `userType == "BARBER"`.
3. **Verifica duplicidade:** checa se já existe barbearia com aquele `ownerId` ou aquele CNPJ.
4. **Cria a entidade:** `Barbershop { name, cnpj, address, ownerId = owner.getId() }`.
5. **Salva no banco** (`barbershop_db`).
6. **Upload de logo** (se enviado): Cloudinary → salva `logoUrl` e `logoUrlPublicId`.
7. **Vincula ao barbeiro:** Feign `PUT /api/internal/users/{id}/barbershop` com `{ barbershopId: "<uuid>" }` → user-service atualiza `barber.barbershopId`.
8. **Promove a Owner:** Feign `PUT /api/internal/users/make-owner/{uid}` → user-service chama `FirebaseAuth.setCustomUserClaims(uid, { role: "BARBER", isOwner: true })`.

**Por que duas chamadas Feign?**
- A primeira (`updateUserBarbershopId`) salva o UUID da barbearia no registro do barbeiro — essencial para que `GET /auth/me` retorne `barbershopId` preenchido.
- A segunda (`makeBarberOwner`) atualiza as **custom claims** do Firebase — garante que o próximo token obtido pelo app já contenha `isOwner: true`, para que o Gateway passe a autorizar rotas restritas a owners.

### O que é retornado ao Frontend

```json
{
  "id": "<uuid-da-barbearia>",
  "name": "Barbearia do João",
  "cnpj": "12345678000190",
  "address": "Rua das Tesouras, 42",
  "logoUrl": "https://res.cloudinary.com/.../logo.jpg",
  "ownerId": "<uuid-do-barbeiro>"
}
```

**Frontend após sucesso:**  
`CreateBarbershopPage` executa `localStorage.clear()` e navega para `/identificacao` (tela de login).  

**Por que forçar novo login?**  
As custom claims do Firebase são embutidas no JWT no momento em que ele é emitido. O token atual **não** tem `isOwner: true`. O barbeiro precisa fazer login novamente para obter um token novo com a claim atualizada. Só então o `GET /auth/me` retornará `isOwner: true` e `barbershopId` preenchido.

---

## 7. Fase 5 — Cadastro de Serviços

### O que acontece

Após o re-login, o barbeiro acessa o painel e cadastra os serviços que sua barbearia oferece.

### Responsabilidade do Frontend

- `BarberHomePage.jsx` chama `GET /auth/me` ao montar → agora recebe `barbershopId: "<uuid>"` e `isOwner: true`.
- Exibe o painel de gestão da barbearia (aba "Serviços").
- `BarberServicesPage.jsx` usa `barbershopService.createService(dadosDoServico)`.

### Chamada HTTP

```
POST /api/barbershops/my-shop/activities
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

{
  "activityName": "Corte Degradê",
  "price": 45.00,
  "durationMinutes": 45
}
```

**Por que esses dados?**
- `activityName` → nome do serviço exibido para o cliente no agendamento.
- `price` → valor cobrado; validado como `BigDecimal` positivo.
- `durationMinutes` → duração usada pelo sistema de agendamentos para bloquear a agenda.

### Responsabilidade do Backend (barbershop-service)

**`BarbershopService.createActivity(ownerUid, dto)`:**

1. `resolveUserByUid(ownerUid)` → Feign para user-service → obtém `UserInfoDTO` com `id`.
2. `findOwnerShop(owner.getId())` → busca `barbershops` onde `owner_id = owner.getId()` → retorna a `Barbershop`.
3. Cria `Activity { activityName, price, durationMinutes, barbershop }`.
4. Salva em `activities`.
5. Retorna `ActivityDTO`.

**Segurança implícita:** O barbeiro só consegue criar serviços na **sua própria** barbearia. O `ownerUid` vem do header `X-User-UID` (injetado pelo Gateway a partir do token Firebase), nunca do corpo da requisição — impossível forjar.

### O que é retornado ao Frontend

```json
{
  "id": "<uuid-da-atividade>",
  "activityName": "Corte Degradê",
  "price": 45.00,
  "durationMinutes": 45,
  "barbershopId": "<uuid-da-barbearia>",
  "imageUrl": null
}
```

**Frontend:** Adiciona o serviço à lista exibida na tela.

---

## 8. Fase 6 — Atribuição de Habilidades ao Barbeiro

### O que acontece

Um barbeiro (que trabalha em uma barbearia mas não é o dono, ou o próprio dono) marca quais serviços da barbearia ele sabe executar. Isso é necessário para o sistema de agendamentos saber qual barbeiro atender cada tipo de serviço.

### Responsabilidade do Frontend

- `ManageMySkills.jsx` (renderizado dentro de `BarberServicesPage`) recebe `shopId` como prop.
- Ao montar, carrega em paralelo:
  - `getShopServices(shopId)` → `GET /api/barbershops/{shopId}/activities` (lista pública da barbearia)
  - `getMyAssignedActivities()` → `GET /api/barbers/me/my-activities`
- Exibe botões para selecionar/deselecionar cada serviço.
- Ao salvar: `assignActivities(myServicesIds)` → `POST /api/barbers/me/assign-activities`.

### Chamada HTTP — Buscar habilidades

```
GET /api/barbers/me/my-activities
Authorization: Bearer <firebase-id-token>
```

**Resposta:**
```json
["<uuid-atividade-1>", "<uuid-atividade-2>"]
```

### Chamada HTTP — Salvar habilidades

```
POST /api/barbers/me/assign-activities
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

{
  "activityIds": [
    "<uuid-atividade-1>",
    "<uuid-atividade-2>"
  ]
}
```

**Por que essa operação substitui ao invés de adicionar?**  
A operação é idempotente: enviar a lista completa evita estados inconsistentes (atividades deletadas que ainda estariam vinculadas). O backend limpa o `Set` e insere os novos IDs em uma única transação.

### Responsabilidade do Backend (user-service)

**`BarberServiceImpl.assignActivities(firebaseUid, dto)`:**

1. Busca o `Barber` por `firebaseUid`.
2. `barber.getAssignedActivityIds().clear()` → limpa a tabela `barber_assigned_activities` para este barbeiro.
3. `barber.getAssignedActivityIds().addAll(dto.activityIds())` → insere os novos vínculos.
4. `barberRepository.save(barber)` → persiste.
5. Retorna o `Set<UUID>` atualizado.

**Tabela gerada (JPA `@ElementCollection`):**

```sql
CREATE TABLE barber_assigned_activities (
    barber_id   VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    FOREIGN KEY (barber_id) REFERENCES barbers(id)
);
```

---

## 9. Tabela de Endpoints

| Fase | Método | URL                                       | Serviço          | Auth exigida |
|------|--------|-------------------------------------------|------------------|--------------|
| 1    | POST   | `/api/auth/email/register`               | user-service     | ❌ Pública   |
| 2    | POST   | `/api/auth/email/login`                  | user-service     | ❌ Pública   |
| 2    | POST   | `/api/auth/verify`                       | user-service     | ❌ Pública (token no corpo) |
| 3    | POST   | `/api/auth/barbers/complete-profile`     | user-service     | ✅ Bearer Token |
| 4    | GET    | `/api/auth/me`                           | user-service     | ✅ Bearer Token |
| 4    | POST   | `/api/barbershops/register-my-shop`      | barbershop-service | ✅ Bearer Token |
| 5    | POST   | `/api/barbershops/my-shop/activities`    | barbershop-service | ✅ Bearer Token |
| 5    | GET    | `/api/barbershops/{shopId}/activities`   | barbershop-service | ❌ Pública   |
| 6    | GET    | `/api/barbers/me/my-activities`          | user-service     | ✅ Bearer Token |
| 6    | POST   | `/api/barbers/me/assign-activities`      | user-service     | ✅ Bearer Token |

---

## 10. Tabela de Dados Trafegados

### AuthResponseDTO — Retornado por `/auth/verify` e `/auth/me`

| Campo               | Tipo      | Preenchido quando                                  | Usado pelo Frontend para                          |
|---------------------|-----------|----------------------------------------------------|---------------------------------------------------|
| `id`                | UUID      | Após `complete-profile`                           | Identificar o usuário localmente                  |
| `name`              | String    | Sempre                                            | Exibir nome na interface                          |
| `email`             | String    | Sempre                                            | Exibir / confirmar e-mail                         |
| `phone`             | String    | Após `complete-profile`                           | Exibir telefone no perfil                         |
| `photoUrl`          | String    | Login social (Google, etc.)                       | Exibir avatar                                     |
| `userType`          | String    | Sempre                                            | Redirecionar para a home correta (barber/customer)|
| `authProvider`      | String    | Sempre                                            | Saber se pode mudar senha (EMAIL) ou não          |
| `profileComplete`   | boolean   | Sempre                                            | Decidir se vai para complete-profile ou para home |
| `role`              | String    | Sempre                                            | Controle de permissões no frontend               |
| `emailVerified`     | boolean   | Sempre                                            | Mostrar aviso de verificação pendente             |
| `verificationRequired` | boolean | E-mail/senha com e-mail não verificado          | Bloquear acesso e pedir verificação               |
| `barbershopId`      | UUID      | Após cadastrar barbearia + re-login               | Saber se barbeiro já tem barbearia; buscar serviços |
| `isOwner`           | Boolean   | Após `makeBarberOwner` + re-login                 | Mostrar/ocultar painel de gestão da barbearia    |

### Fluxo de autorização no Gateway

O header `Authorization: Bearer <token>` é processado pelo `FirebaseTokenGatewayFilter`:

```
Token recebido
    │
    ├─► FirebaseAuth.verifyIdToken(token)  ← valida assinatura + expiração
    │
    ├─► Extrai: uid, email, userType (custom claim)
    │
    └─► Injeta no request:
          X-User-UID    → uid do Firebase (identifica o usuário em todos os serviços)
          X-User-Email  → email
          X-User-Type   → BARBER | CUSTOMER
```

**Nenhum serviço downstream vê o token Firebase diretamente** — apenas os headers já validados. Isso centraliza a validação no Gateway.

---

## 11. Diagrama de Sequência Completo

```
Frontend          API Gateway        user-service        Firebase Auth      barbershop-service
   │                   │                  │                    │                    │
   │─── POST /auth/email/register ───────►│                   │                    │
   │                   │────────────────►│                    │                    │
   │                   │                 │── signUp ─────────►│                    │
   │                   │                 │◄── {idToken,uid} ──│                    │
   │                   │                 │── sendVerifyEmail─►│                    │
   │                   │                 │── verifyAndProvision()                  │
   │◄── {verificationRequired:true} ─────│                    │                    │
   │                                     │                    │                    │
   │  [usuário clica no link de verificação no e-mail]        │                    │
   │                                     │                    │                    │
   │─── POST /auth/email/login ──────────►│                   │                    │
   │                   │────────────────►│                    │                    │
   │                   │                 │── signIn ─────────►│                    │
   │◄── {idToken} ───────────────────────│                    │                    │
   │                                     │                    │                    │
   │─── POST /auth/verify ───────────────►│                   │                    │
   │                   │── (pública, sem validar token no header)                  │
   │                   │────────────────►│                    │                    │
   │                   │                 │── verifyIdToken ──►│                    │
   │                   │                 │◄── {uid, email_verified:true}           │
   │                   │                 │── findByFirebaseUid(uid) → Barber       │
   │◄── AuthResponseDTO(profileComplete:false) ──────────────│                    │
   │                                     │                    │                    │
   │─── POST /auth/barbers/complete-profile (Bearer token) ──►│                   │
   │                   │── valida token, injeta X-User-UID   │                    │
   │                   │────────────────►│                    │                    │
   │                   │                 │── findByFirebaseUid → atualiza Barber   │
   │◄── AuthResponseDTO(profileComplete:true) ───────────────│                    │
   │                                     │                    │                    │
   │─── GET /auth/me (Bearer token) ─────►│                  │                    │
   │                   │────────────────►│                    │                    │
   │◄── AuthResponseDTO(barbershopId:null, isOwner:false) ───│                    │
   │                                     │                    │                    │
   │─── POST /barbershops/register-my-shop (multipart) ──────────────────────────►│
   │                   │── valida token, injeta X-User-UID                        │
   │                   │──────────────────────────────────────────────────────────►│
   │                   │                 │◄── GET /api/internal/users/by-firebase-uid/{uid}
   │                   │                 │── retorna UserInfoDTO ─────────────────►│
   │                   │                 │                                         │── save(Barbershop)
   │                   │                 │◄── PUT /api/internal/users/{id}/barbershop
   │                   │                 │── barber.barbershopId = <uuid>          │
   │                   │                 │◄── PUT /api/internal/users/make-owner/{uid}
   │                   │                 │── setCustomUserClaims(isOwner:true)───►│ (Firebase)
   │◄── BarbershopDTO ───────────────────────────────────────────────────────────│
   │                                     │                    │                    │
   │  [Frontend: localStorage.clear(), redireciona para login]                    │
   │                                     │                    │                    │
   │─── [login novamente, token novo com isOwner:true]        │                    │
   │─── GET /auth/me ────────────────────►│                  │                    │
   │◄── AuthResponseDTO(barbershopId:"<uuid>", isOwner:true) │                    │
   │                                     │                    │                    │
   │─── POST /barbershops/my-shop/activities ────────────────────────────────────►│
   │◄── ActivityDTO ─────────────────────────────────────────────────────────────│
   │                                     │                    │                    │
   │─── GET /barbers/me/my-activities ───►│                  │                    │
   │                   │────────────────►│                    │                    │
   │◄── [uuid, uuid, ...] ───────────────│                    │                    │
   │                                     │                    │                    │
   │─── POST /barbers/me/assign-activities ──────────────────►│                   │
   │                   │────────────────►│                    │                    │
   │◄── [uuid, uuid, ...] ───────────────│                    │                    │
```

---

## Pontos de Atenção

### 1. Re-login obrigatório após criar barbearia
O Firebase JWT é emitido com as claims vigentes no momento do `signIn`. O `setCustomUserClaims` atualiza as claims no Firebase, mas **o token atual não é invalidado imediatamente**. Por isso, o frontend força `localStorage.clear()` e redireciona para o login, garantindo que o próximo token já contenha `isOwner: true` e que `GET /auth/me` retorne `barbershopId` preenchido.

### 2. Identificação sempre por Firebase UID
Nenhum endpoint protegido recebe `userId` no corpo ou na URL para identificar "quem está agindo". A identidade vem **exclusivamente** do header `X-User-UID`, injetado pelo Gateway após validar o token. Isso impede que um usuário aja em nome de outro.

### 3. Comunicação inter-serviço (Feign)
O `barbershop-service` nunca acessa o banco do `user-service` diretamente. Toda comunicação passa pelo Feign Client (`UserServiceClient`) chamando endpoints `GET /api/internal/users/...` no user-service. Se o user-service estiver indisponível, o `UserServiceClientFallbackFactory` lança `UserServiceUnavailableException` (HTTP 503).

### 4. Tabela `barber_assigned_activities`
Criada automaticamente pelo Hibernate (`ddl-auto: update`) a partir do `@ElementCollection` em `Barber`. Não há `Activity` entity no user-service — apenas o UUID é armazenado. A validação de se o UUID realmente existe em `barbershop-service` é responsabilidade do frontend (que já carregou a lista de atividades válidas antes de salvar).
