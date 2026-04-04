# Tutorial: Favoritos de Barbearias (Cliente)

Este guia explica como o sistema de favoritos funciona no projeto CortaAI, cobrindo backend, frontend e teste via Postman.

## Objetivo

Permitir que clientes:
- favoritem barbearias
- removam barbearias dos favoritos
- vejam a lista de favoritas na aba de favoritos da Home

## 1) Fluxo funcional

1. Cliente abre a Home.
2. O frontend busca favoritos do cliente autenticado (`GET /api/customers/me/favorites`).
3. Cada card de barbearia recebe o estado `isFavorite` com base nesses IDs.
4. Ao clicar no icone de coracao:
   - se nao for favorita: `POST /api/customers/me/favorites/{barbershopId}`
   - se ja for favorita: `DELETE /api/customers/me/favorites/{barbershopId}`
5. O estado local da Home e atualizado e a secao "Minhas barbearias favoritas" reflete imediatamente.

## 2) Backend

## 2.1 Persistencia

Arquivo:
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/model/Customer.java`

Foi adicionado no `Customer`:
- `favoriteBarbershopIds: Set<UUID>`
- mapeado com `@ElementCollection`
- tabela: `customer_favorite_barbershops`
- constraint unica por cliente + barbearia para evitar duplicidade

## 2.2 Service

Arquivos:
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/service/CustomerService.java`
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/service/impl/CustomerServiceImpl.java`

Metodos implementados:
- `listFavoriteBarbershopIdsByFirebaseUid(firebaseUid)`
- `addFavoriteBarbershopByFirebaseUid(firebaseUid, barbershopId)`
- `removeFavoriteBarbershopByFirebaseUid(firebaseUid, barbershopId)`

## 2.3 Controller / Endpoints

Arquivo:
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/controller/CustomerController.java`

Endpoints:
- `GET /api/customers/me/favorites`
- `POST /api/customers/me/favorites/{barbershopId}`
- `DELETE /api/customers/me/favorites/{barbershopId}`

Autenticacao:
- os endpoints usam `X-User-UID` (injetado pelo Gateway apos validar token)
- no frontend basta enviar `Authorization: Bearer <token>`

## 3) Frontend

## 3.1 Servico de API

Arquivo:
- `frontend/src/services/barbershopService.js`

Funcoes:
- `getMyFavoriteBarbershopsIds()`
- `addFavoriteBarbershop(shopId)`
- `removeFavoriteBarbershop(shopId)`

## 3.2 Estado centralizado na Home

Arquivo:
- `frontend/src/pages/HomePage.jsx`

Como funciona:
- estado `favoriteIds` guarda os IDs favoritos atuais
- no mount, chama `getMyFavoriteBarbershopsIds()`
- `handleToggleFavorite(shopId, currentlyFavorite)` chama POST/DELETE e atualiza `favoriteIds`
- passa props para componentes filhos:
  - `Favorite_barbershops favoriteIds={favoriteIds}`
  - `Barbershops favoriteIds={favoriteIds} onToggleFavorite={handleToggleFavorite}`

## 3.3 Cards de barbearia

Arquivo:
- `frontend/src/components/HomePage/Barbershops/Container_Barbericons.jsx`

Mudanca principal:
- o componente deixou de ler/escrever `localStorage`
- agora recebe `isFavorite` e `onToggleFavorite` por props

## 3.4 Aba de favoritas

Arquivo:
- `frontend/src/components/HomePage/Favorite_barbershops/Favorite_barbershops.jsx`

Comportamento:
- recebe `favoriteIds`
- busca lista publica de barbearias
- filtra pelas favoritas do cliente
- exibe cards resumidos na secao "Minhas barbearias favoritas"

## 4) Teste rapido via Postman

## 4.1 Preparacao

Variaveis sugeridas:
- `baseUrl = https://api.cortaai.shop/api`
- `customerToken = <token JWT de cliente>`
- `shopId = <uuid de uma barbearia existente>`

## 4.2 Listar favoritas do cliente

Request:
- Method: `GET`
- URL: `{{baseUrl}}/customers/me/favorites`
- Header: `Authorization: Bearer {{customerToken}}`

Esperado:
- `200 OK`
- body: array de UUIDs (pode ser vazio)

## 4.3 Favoritar barbearia

Request:
- Method: `POST`
- URL: `{{baseUrl}}/customers/me/favorites/{{shopId}}`
- Header: `Authorization: Bearer {{customerToken}}`

Esperado:
- `201 Created`

## 4.4 Confirmar favorita adicionada

Repita o GET de favoritas.

Esperado:
- `shopId` presente no array

## 4.5 Remover favorita

Request:
- Method: `DELETE`
- URL: `{{baseUrl}}/customers/me/favorites/{{shopId}}`
- Header: `Authorization: Bearer {{customerToken}}`

Esperado:
- `204 No Content`

## 4.6 Confirmar remocao

Repita o GET de favoritas.

Esperado:
- `shopId` nao aparece mais no array

## 5) Cenarios esperados de erro

- `401 Unauthorized`
  - token ausente/invalido
- `404 Not Found`
  - cliente nao encontrado para o `firebaseUid`
- `400 Bad Request`
  - `barbershopId` invalido (nao UUID)

## 6) Observacoes de projeto

- No momento, o backend de favoritos persiste apenas o UUID da barbearia no `user-service`.
- A validacao de existencia da barbearia no momento do POST nao e obrigatoria para salvar favorito.
- A aba de favoritas no frontend resolve os dados completos cruzando os IDs com `GET /api/barbershops`.

## 7) Arquivos impactados

Backend:
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/model/Customer.java`
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/service/CustomerService.java`
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/service/impl/CustomerServiceImpl.java`
- `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/controller/CustomerController.java`

Frontend:
- `frontend/src/services/barbershopService.js`
- `frontend/src/pages/HomePage.jsx`
- `frontend/src/components/HomePage/Barbershops/Barbershops.jsx`
- `frontend/src/components/HomePage/Barbershops/Container_Barbericons.jsx`
- `frontend/src/components/HomePage/Favorite_barbershops/Favorite_barbershops.jsx`

