# Tutorial: Rating de Barbearias (Front + Back + Postman)

Este documento explica a implementacao do rating de barbearias no projeto CortaAI, cobrindo:
- banco de dados
- backend (API)
- frontend (fluxo de tela)
- testes via Postman

## 1) Visao geral do fluxo

O ciclo de rating implementado funciona assim:

1. Cliente conclui um atendimento.
2. Na tela `MeusAgendamentosPage`, quando o agendamento esta `COMPLETED`, aparece o botao **Avaliar**.
3. O frontend abre um modal e envia `rating` (1 a 5) e `comment` (opcional) para:
   - `POST /api/barbershops/{shopId}/reviews`
4. O backend salva a review em `barbershop_reviews`.
5. A media e a quantidade de reviews sao calculadas e expostas em:
   - `GET /api/barbershops`
   - `GET /api/barbershops/{shopId}`
6. O card da barbearia na Home mostra o rating real.

## 2) Banco de dados

### 2.1 Tabela de reviews

A entidade `BarbershopReview` gera a tabela `barbershop_reviews` (via JPA `ddl-auto: update`).

Campos principais:
- `id` (UUID)
- `customer_id` (UUID)
- `barbershop_id` (FK para `barbershops`)
- `rating` (int 1..5)
- `comment` (varchar ate 500)
- `created_at` (datetime)

Regra importante:
- Constraint unica em `(customer_id, barbershop_id)` para impedir avaliacao duplicada do mesmo cliente para a mesma barbearia.

### 2.2 Media de rating

Na entidade `Barbershop`, os campos abaixo sao calculados por formula SQL:
- `averageRating`
- `reviewsCount`

Assim, o frontend recebe valores agregados sem precisar calcular no cliente.

## 3) Backend

### 3.1 Endpoint de criacao de review

**Rota**
- `POST /api/barbershops/{shopId}/reviews`

**Auth**
- Requer Bearer Token
- Usuario precisa ser `CUSTOMER`

**Body**
```json
{
  "rating": 5,
  "comment": "Atendimento excelente"
}
```

**Validacoes**
- `rating` obrigatorio
- `rating` entre 1 e 5
- `comment` opcional com maximo 500 caracteres
- bloqueia segunda review do mesmo cliente para a mesma barbearia (`409`)

**Respostas esperadas**
- `201 Created`: review criada
- `400 Bad Request`: payload invalido
- `403 Forbidden`: usuario nao e cliente
- `404 Not Found`: barbearia nao existe
- `409 Conflict`: cliente ja avaliou essa barbearia

### 3.2 Endpoints de leitura com rating

Os dados publicos de barbearia agora incluem:
- `averageRating`
- `reviewsCount`

Em:
- `GET /api/barbershops`
- `GET /api/barbershops/{shopId}`

Exemplo de item de resposta:
```json
{
  "id": "7b579e5d-8ad2-4f0a-9a3f-c4f8fd6a9fa2",
  "ownerId": "de4f3e84-cf6f-4f4f-96f3-56f6a120e3ea",
  "name": "Barbearia Centro",
  "cnpj": "12345678000199",
  "address": "Rua A, 100",
  "logoUrl": "https://...",
  "bannerUrl": "https://...",
  "averageRating": 4.7,
  "reviewsCount": 12,
  "highlightUrls": []
}
```

## 4) Frontend

### 4.1 Onde o cliente avalia

Arquivo principal:
- `frontend/src/pages/MeusAgendamentosPage.jsx`

Comportamento:
- Se usuario for cliente e agendamento estiver `COMPLETED`, aparece botao **Avaliar**.
- Ao clicar, abre modal com:
  - nota (1 a 5)
  - comentario (opcional)
- O submit chama `createBarbershopReview(shopId, payload)`.

Servico usado:
- `frontend/src/services/barbershopService.js`
- funcao: `createBarbershopReview(shopId, reviewData)`

### 4.2 Onde o rating aparece

Arquivos:
- `frontend/src/components/HomePage/Barbershops/Barbershops.jsx`
- `frontend/src/components/HomePage/Barbershops/Container_Barbericons.jsx`

Comportamento:
- Card usa `averageRating` e `reviewsCount` vindos do backend.
- Se nao houver reviews, mostra texto **Sem avaliacoes**.

## 5) Tutorial de testes no Postman

## 5.1 Preparacao

1. Defina uma collection variable (exemplo):
   - `baseUrl = https://api.cortaai.shop/api`
2. Tenha 2 tokens:
   - `customerToken` (cliente)
   - `barberToken` (barbeiro, para teste negativo)
3. Descubra um `shopId` valido usando `GET /barbershops`.

## 5.2 Buscar barbearias e pegar shopId

**Request**
- Method: `GET`
- URL: `{{baseUrl}}/barbershops`

**Validar**
- resposta com array de lojas
- cada item pode ter `averageRating` e `reviewsCount`

## 5.3 Criar review com cliente (caminho feliz)

**Request**
- Method: `POST`
- URL: `{{baseUrl}}/barbershops/{{shopId}}/reviews`
- Headers:
  - `Authorization: Bearer {{customerToken}}`
  - `Content-Type: application/json`
- Body:
```json
{
  "rating": 5,
  "comment": "Corte muito bom, atendimento rapido"
}
```

**Esperado**
- `201 Created`

## 5.4 Validar que a media mudou

Repita:
- `GET {{baseUrl}}/barbershops/{{shopId}}`

**Esperado**
- `reviewsCount` incrementado
- `averageRating` recalculado

## 5.5 Teste de duplicidade (mesmo cliente)

Envie novamente a mesma chamada `POST` da etapa 5.3.

**Esperado**
- `409 Conflict`
- mensagem indicando que o cliente ja avaliou a barbearia

## 5.6 Teste de permissao (nao cliente)

Troque o token por `{{barberToken}}` e repita o `POST`.

**Esperado**
- `403 Forbidden`

## 5.7 Teste de validacao de nota

Body invalido:
```json
{
  "rating": 6,
  "comment": "fora do intervalo"
}
```

**Esperado**
- `400 Bad Request`

## 6) Cenarios de erro comuns

- `401 Unauthorized`
  - token ausente, expirado ou invalido
- `404 Not Found`
  - `shopId` inexistente
- `409 Conflict`
  - cliente ja avaliou a mesma barbearia
- `400 Bad Request`
  - `rating` fora do intervalo ou body invalido

## 7) Observacao de regra de negocio

No estado atual:
- O frontend exibe o botao de avaliacao apenas para agendamentos `COMPLETED`.
- O backend ainda nao valida, via `schedule-service`, se houve atendimento concluido para o cliente.

Ou seja:
- Ha validacao de role (cliente) e duplicidade no backend.
- A elegibilidade por atendimento concluido esta no fluxo de tela.

Se desejado, o proximo passo e reforcar essa regra no backend com verificacao inter-servico.

## 8) Arquivos alterados na implementacao

Backend:
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/model/BarbershopReview.java`
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/repository/BarbershopReviewRepository.java`
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/dto/CreateBarbershopReviewDTO.java`
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/model/Barbershop.java`
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/dto/BarbershopDTO.java`
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/mapper/BarbershopMapper.java`
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/service/BarbershopService.java`
- `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/controller/BarbershopController.java`

Frontend:
- `frontend/src/services/barbershopService.js`
- `frontend/src/pages/MeusAgendamentosPage.jsx`
- `frontend/src/pages/CSS/MeusAgendamentos.module.css`
- `frontend/src/components/HomePage/Barbershops/Barbershops.jsx`
- `frontend/src/components/HomePage/Barbershops/Container_Barbericons.jsx`

