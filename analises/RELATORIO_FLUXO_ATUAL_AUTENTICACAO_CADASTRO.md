# Relatorio Tecnico - Fluxo Atual de Autenticacao e Cadastro (CortaAi)

> Data: 2026-04-02  
> Ambiente alvo: producao (`https://api.cortaai.shop`)  
> Objetivo: descrever o fluxo real atual da aplicacao (customer, barbeiro comum, barbeiro owner), incluindo validacoes de token/e-mail, claims e origem de barbearia/servicos.

---

## 1) Resumo executivo

- O fluxo atual de auth por e-mail/senha usa endpoints canonicos:
  - `POST /api/auth/email/register`
  - `POST /api/auth/email/login`
  - `POST /api/auth/verify`
- A verificacao de e-mail acontece em duas camadas:
  1. `user-service` retorna `verificationRequired=true` no `verifyAndProvision`.
  2. `api-gateway` bloqueia rotas protegidas quando `email_verified=false` para provider `password`.
- O tipo de usuario (`CUSTOMER`/`BARBER`) e owner nao dependem apenas do token: ha resolucao e validacao na base local (`user-service`) via `firebase_uid` e, para loja, via `ownerId`/`barbershopId`.
- O owner e consolidado de fato quando cria a loja (`register-my-shop`), momento em que o `barbershop-service` chama `user-service` interno para atualizar claim `isOwner=true` no Firebase.

---

## 2) Fontes da verdade (codigo)

- Auth e perfil:
  - `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/controller/AuthController.java`
  - `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/controller/FirebaseTestController.java`
  - `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/service/impl/FirebaseDebugServiceImpl.java`
  - `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/service/impl/FirebaseAuthServiceImpl.java`
- Gateway e headers:
  - `backend/api-gateway/src/main/java/ifsp/edu/projeto/cortaai/apigateway/filter/FirebaseTokenGatewayFilter.java`
- Fluxo barbearia:
  - `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/controller/BarbershopController.java`
  - `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/service/BarbershopService.java`
  - `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/controller/InternalBarbershopController.java`
- Fluxo agendamento:
  - `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/service/AppointmentService.java`
- Front atual:
  - `frontend/src/services/authService.js`
  - `frontend/src/services/api.js`

---

## 3) Fluxo completo atual por perfil

## 3.1 Customer

1. Cadastro
   - Front chama `POST /api/auth/email/register` com `userType=CUSTOMER`.
   - Backend (`FirebaseDebugServiceImpl`) executa:
     - Firebase `signUp`
     - Firebase `sendOobCode` (`VERIFY_EMAIL`)
     - `verifyAndProvision`
     - `completeCustomerProfile`
2. Login
   - Front chama `POST /api/auth/email/login`.
   - Salva `idToken` no storage.
   - Chama `POST /api/auth/verify` com `{ idToken, userType: null }`.
   - Se `verificationRequired=true`, o front bloqueia e pede confirmacao de e-mail.
3. Uso de rotas protegidas
   - `Authorization: Bearer <idToken>`.
   - Gateway valida token e e-mail verificado.

## 3.2 Barbeiro comum

1. Cadastro
   - `POST /api/auth/email/register` com `userType=BARBER` e `isOwner=false`.
   - Backend faz signUp + envio de verificacao + provisionamento + complete-profile.
2. Login e verificacao
   - Mesmo fluxo do customer (`/api/auth/email/login` + `/api/auth/verify`).
3. Vinculo com loja
   - Barbeiro solicita entrada: `POST /api/barbershops/join-request` com `cnpj`.
   - Owner lista pendencias: `GET /api/barbershops/my-shop/pending-requests`.
   - Owner aprova: `POST /api/barbershops/my-shop/approve-request/{requestId}`.
   - `barbershop-service` atualiza `barbershopId` do barbeiro no `user-service` interno.

## 3.3 Barbeiro owner

1. Cadastro como barbeiro
   - `POST /api/auth/email/register` com `userType=BARBER`.
2. Login e verificacao
   - Mesmo fluxo (`/api/auth/email/login` + `/api/auth/verify`).
3. Criacao da barbearia
   - `POST /api/barbershops/register-my-shop` (multipart; parte `shop`, opcional `file`).
   - `barbershop-service` valida dono, cria loja e seta `ownerId`.
   - Em seguida chama `user-service` interno (`PUT /api/internal/users/make-owner/{uid}`) para setar claim `isOwner=true` no Firebase.

---

## 4) Como as verificacoes funcionam hoje

## 4.1 Verificacao do token

- No gateway, token e validado via Firebase Admin (`verifyIdToken`).
- Em sucesso, gateway injeta:
  - `X-User-UID`
  - `X-User-Email`
  - `X-User-Type` (claim `role`)
  - `X-User-Owner` (claim `isOwner`)
- O gateway remove headers `X-User-*` recebidos do cliente antes de reinjetar (mitiga spoofing).

## 4.2 Verificacao de e-mail (email/senha)

- `user-service` (`verifyAndProvision`) marca `verificationRequired=true` para provider `EMAIL` sem `email_verified`.
- `api-gateway` bloqueia acesso protegido nessas condicoes (`401`).
- Resultado pratico: usuario pode fazer login, mas nao consegue operar rotas protegidas sem verificar e-mail e obter token atualizado.

## 4.3 Tipo de usuario e ownership

- `userType` de negocio e definido/confirmado na base local (`Customer`/`Barber`) vinculada ao `firebase_uid`.
- `isOwner` de negocio e confirmado pela existencia de loja com `ownerId` correspondente no `barbershop-service`.
- Claims no token (`role`, `isOwner`) sao auxiliares de roteamento/headers, mas a fonte funcional principal para ownership de loja e estado local.

---

## 5) Por que voce pode nao ver `userType`/`isOwner` no token

Isso e esperado no estado atual por dois motivos:

1. Claims customizadas sao setadas em pontos especificos (ex.: `make-owner`) e nao em todo caminho de cadastro Firebase.
2. Mesmo apos setar claim, o token antigo nao muda; precisa novo login/refresh para refletir as claims atualizadas.

Em outras palavras: token sem claim nao significa necessariamente usuario sem perfil local. O sistema ainda resolve por `firebase_uid` no banco.

---

## 6) Origem da barbearia e dos servicos (validacao de dominio)

## 6.1 Barbearia (owner e equipe)

- Owner da loja:
  - `Barbershop.ownerId` e o id local do usuario dono.
  - Operacoes de gestao da loja usam `resolveUserByUid(uid)` + `findOwnerShop(ownerId)`.
- Barbeiro comum:
  - Vinculo e controlado por `barbershopId` no registro do barbeiro no `user-service`.
  - Aprovacao de join-request atualiza esse campo via endpoint interno.

## 6.2 Servicos/atividades no agendamento

- `schedule-service` valida:
  - loja existe (`getBarbershopById`) 
  - atividades existem para aquela loja (`/api/internal/barbershops/{shopId}/activities?ids=...`)
- Com isso, as atividades usadas no agendamento estao amarradas ao `shopId` informado.

## 6.3 Observacao importante

- Em `AppointmentService#createAppointment`, o sistema valida customer, barber, loja e atividades, mas nao ha validacao explicita no mesmo metodo comprovando que o `barberId` pertence ao `barbershopId` informado.

---

## 7) Matriz de endpoints atual (producao)

## 7.1 Auth (publico)

- `POST /api/auth/email/register`
- `POST /api/auth/email/login`
- `POST /api/auth/email/verify-token`
- `POST /api/auth/verify`

## 7.2 Auth (protegido)

- `GET /api/auth/me`
- `POST /api/auth/customers/complete-profile`
- `POST /api/auth/barbers/complete-profile`

## 7.3 Barbearia (protegido)

- `POST /api/barbershops/register-my-shop`
- `POST /api/barbershops/join-request`
- `GET /api/barbershops/my-shop/pending-requests`
- `POST /api/barbershops/my-shop/approve-request/{requestId}`

## 7.4 Legado (compatibilidade)

- `/api/auth/firebase-test/*` ainda existe como alias.

---

## 8) Divergencias do documento antigo vs fluxo atual

- O fluxo atual de front/back ja usa `/api/auth/email/*` como canonico; o sufixo `firebase-test` e legado.
- Aprovacao/listagem de join requests no codigo atual usa:
  - `GET /api/barbershops/my-shop/pending-requests`
  - `POST /api/barbershops/my-shop/approve-request/{requestId}`
  e nao os caminhos antigos `join-requests/*`.
- `register-my-shop` no controller atual esta como multipart (`shop` + `file` opcional), nao JSON puro simples.

---

## 9) Fluxo de teste recomendado (passo a passo rapido)

1. `POST /api/auth/email/register` (perfil desejado)
2. Confirmar e-mail no link recebido
3. `POST /api/auth/email/login` (obter novo `idToken`)
4. `POST /api/auth/verify`
5. `GET /api/auth/me` (deve retornar 200)
6. Se barbeiro comum: `POST /api/barbershops/join-request`
7. Se owner: `POST /api/barbershops/register-my-shop`
8. Se owner aprovando equipe: `GET /pending-requests` + `POST /approve-request/{id}`

---

## 10) Conclusao

- O fluxo esta funcionalmente coerente em autenticacao e bloqueio por e-mail nao verificado.
- A autorizacao de ownership da loja e sustentada por estado de dominio (`ownerId`/`barbershopId`) e nao exclusivamente por claim do token.
- A ausencia de `userType`/`isOwner` no JWT em alguns momentos e esperada pela forma atual de emissao/refresh de claims.
- Para rastreabilidade operacional, o endpoint mais confiavel para estado atual do usuario continua sendo `POST /api/auth/verify` seguido de `GET /api/auth/me`.

