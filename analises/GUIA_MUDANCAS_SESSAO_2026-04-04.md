# Guia de mudancas da sessao (2026-04-04)

Este documento consolida o que foi implementado nesta sessao e como usar no frontend, backend e Postman.

## 1) Resumo dos commits da sessao

Commits aplicados nesta ordem:

1. `8571c63` - docs: ADR, checklist por squad e plano tatico
2. `a93b1f3` - schedule: status canonico e contratos front-back
3. `04fe1ca` - finance: overview/series com gastos e bens de estoque
4. `eceb344` - stock: estoque interno com `minStockQuantity` e categorias padronizadas
5. `1d5e428` - finance: serie no dashboard e autorizacao owner
6. `de99c36` - front: remove hardcodes da home do barbeiro
7. `8a6d98f` - p0: alinhamento schedule-payment e customer por token
8. `a3863c5` - product: inventario paginado, filtros e historico de movimentacoes

## 2) O que mudou por modulo

## 2.1 Schedule Service

Arquivos principais:
- `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/model/enums/AppointmentStatus.java`
- `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/mapper/AppointmentMapper.java`
- `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/controller/AppointmentController.java`
- `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/service/AppointmentService.java`
- `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/repository/AppointmentRepository.java`
- `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/dto/CreateAppointmentDTO.java`

Mudancas:
- Status canonico com `COMPLETED` (compatibilidade de leitura para `CONCLUDED`).
- Endpoint alias adicionado: `PUT /api/appointments/{id}/complete`.
- `GET /api/appointments/my-appointments` agora suporta customer e barber.
- Criacao de agendamento usa usuario autenticado como customer (payload com `customerId` ficou opcional para compatibilidade).
- Atualizacao interna de status por pagamento agora aceita alias:
  - `PAID -> CONFIRMED`
  - `CONCLUDED -> COMPLETED`

## 2.2 Payment Service

Arquivos principais:
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/controller/PaymentController.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/service/PaymentService.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/model/Transaction.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/repository/TransactionRepository.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/dto/TransactionDTO.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/dto/FinancialOverviewDTO.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/dto/FinancialSeriesDTO.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/dto/FinancialSeriesPointDTO.java`
- `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/feign/ProductServiceClient.java`

Mudancas:
- `Transaction` passou a armazenar `barbershopId`.
- Novo endpoint de resumo financeiro:
  - `GET /api/payments/my-shop/overview`
- Novo endpoint de serie financeira:
  - `GET /api/payments/my-shop/series`
- Autorizacao diferenciada:
  - `overview`: barber vinculado a barbearia pode acessar.
  - `series`: owner da barbearia (role contendo `OWNER`) e vinculado.
- Integracao payment -> schedule ajustada para usar `CONFIRMED` (em vez de `PAID`).

## 2.3 Product Service (estoque interno)

Arquivos principais:
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/controller/ProductController.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/controller/InternalProductController.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/service/ProductService.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/repository/ProductRepository.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/repository/StockMovementRepository.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/model/Product.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/dto/CreateProductDTO.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/dto/UpdateProductDTO.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/dto/ProductDTO.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/dto/InventoryPageDTO.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/dto/InventoryProductItemDTO.java`
- `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/dto/StockMovementDTO.java`

Mudancas:
- Produto definido como estoque interno (nao e fluxo de venda ao cliente).
- Inclusao de `minStockQuantity` no modelo e DTOs.
- Endpoint interno para financeiro:
  - `GET /api/internal/products/barbershops/{barbershopId}/financial-summary`
- Inventario paginado com filtros:
  - `GET /api/products/inventory`
- Historico de movimentacoes por produto:
  - `GET /api/products/{id}/movements`
- Filtros suportados no inventario:
  - `search` (nome/descricao)
  - `category`
  - `lowStock`
  - `page` e `size`

## 2.4 Frontend

Arquivos principais:
- `frontend/src/services/appointmentService.js`
- `frontend/src/pages/AgendamentoPage.jsx`
- `frontend/src/components/BarberPage/Invoicing.jsx`
- `frontend/src/components/BarberPage/NextScheduling.jsx`
- `frontend/src/components/BarberPage/Stock.jsx`
- `frontend/src/components/BarberPage/DailyInsights.jsx`
- `frontend/src/components/BarberPage/ActionsBarber.jsx`
- `frontend/src/pages/BarberHomePage.jsx`
- `frontend/src/pages/BarberStockPage.jsx`

Mudancas:
- Agendamento front alinhado ao backend:
  - usa `/appointments/my-appointments`
  - cancelamento via `PUT /appointments/{id}/cancel`
  - disponibilidade via `/appointments/availability`
- Tela de agendamento envia `customerId` por compatibilidade (backend ja resolve por token).
- Card de faturamento (`Invoicing`) agora usa dados reais de API.
- Serie financeira exibida em grafico simples para owner.
- Home do barbeiro sem dados fixos:
  - proximos agendamentos reais
  - estoque baixo real
  - insights dinamicos
- `BarberStockPage` integrada ao backend de produtos (sem `localStorage` como fonte principal).
- Feature flag para insights:
  - `VITE_ENABLE_BARBER_INSIGHTS=true`

## 3) Como usar no backend

## 3.1 Compilar servicos alterados

```zsh
cd "/Users/SPEBR3977/Desktop/pessoal/Arquitetura_completa/backend"
./mvnw -q -pl schedule-service,payment-service,product-service -DskipTests compile
```

## 3.2 Rodar (opcao local por servico)

Use o perfil/porta que seu time ja utiliza. Exemplo de execucao individual:

```zsh
cd "/Users/SPEBR3977/Desktop/pessoal/Arquitetura_completa/backend"
./mvnw -pl schedule-service spring-boot:run
./mvnw -pl payment-service spring-boot:run
./mvnw -pl product-service spring-boot:run
```

## 3.3 Rodar via compose (opcional)

Se o fluxo do time usa Docker Compose, usar os arquivos do repo:
- `docker-compose.yml`
- `docker-compose.server.yml`

## 4) Como usar no frontend

## 4.1 Build

```zsh
cd "/Users/SPEBR3977/Desktop/pessoal/Arquitetura_completa/frontend"
npm run build
```

## 4.2 Desenvolvimento

```zsh
cd "/Users/SPEBR3977/Desktop/pessoal/Arquitetura_completa/frontend"
npm install
npm run dev
```

## 4.3 Variaveis recomendadas

Criar/ajustar `.env` no frontend (exemplo):

```env
VITE_ENABLE_BARBER_INSIGHTS=true
```

Se deixar `false`, o card de insights pode ficar oculto no bloco de acoes.

## 5) Como testar no Postman

Observacao:
- Quando passar pelo Gateway, normalmente basta `Authorization: Bearer <token>`.
- Em endpoints que dependem de identidade no backend, o Gateway injeta headers como `X-User-Id`.

## 5.1 Agendamentos

### Listar meus agendamentos
- `GET /api/appointments/my-appointments`

### Cancelar agendamento
- `PUT /api/appointments/{id}/cancel`

### Concluir (alias canonico)
- `PUT /api/appointments/{id}/complete`

### Criar agendamento
- `POST /api/appointments`

Body exemplo:

```json
{
  "barberId": "11111111-1111-1111-1111-111111111111",
  "barbershopId": "22222222-2222-2222-2222-222222222222",
  "activityIds": [
    "33333333-3333-3333-3333-333333333333"
  ],
  "startTime": "2026-04-05T14:00:00"
}
```

`customerId` e opcional por compatibilidade; backend usa usuario autenticado.

## 5.2 Financeiro da barbearia

### Overview
- `GET /api/payments/my-shop/overview?barbershopId=<uuid>&from=2026-04-01&to=2026-04-04`

Retorno esperado (exemplo):

```json
{
  "barbershopId": "22222222-2222-2222-2222-222222222222",
  "currency": "BRL",
  "serviceRevenue": 800.0,
  "productExpenses": 220.0,
  "inventoryAssetValue": 1450.0,
  "operationalResult": 580.0,
  "transactionsCount": 12,
  "approvedCount": 10,
  "pendingCount": 1,
  "cancelledCount": 1
}
```

### Serie
- `GET /api/payments/my-shop/series?barbershopId=<uuid>&from=2026-03-29&to=2026-04-04&groupBy=DAY`
- `groupBy`: `DAY` ou `WEEK`

Retorno esperado (exemplo):

```json
{
  "barbershopId": "22222222-2222-2222-2222-222222222222",
  "groupBy": "DAY",
  "points": [
    {
      "date": "2026-04-01",
      "serviceRevenue": 220.0,
      "approvedTransactions": 3
    }
  ]
}
```

Regra de acesso:
- overview: barber vinculado pode acessar
- series: owner vinculado

## 5.3 Produtos/estoque interno

### Criar produto
- `POST /api/products`

```json
{
  "barbershopId": "22222222-2222-2222-2222-222222222222",
  "name": "Pomada X",
  "description": "Uso interno",
  "price": 19.9,
  "category": "POMADE",
  "stockQuantity": 10,
  "minStockQuantity": 3,
  "imageUrl": null
}
```

### Inventario com filtros/paginacao
- `GET /api/products/inventory?barbershopId=<uuid>&search=pomada&category=POMADE&lowStock=true&page=0&size=20`

### Atualizar estoque
- `PUT /api/products/{id}`

```json
{
  "stockQuantity": 7,
  "minStockQuantity": 3
}
```

### Historico de movimentacoes
- `GET /api/products/{id}/movements?page=0&size=20`

## 6) Checklist rapido de validacao

- [ ] `my-appointments` retorna para customer e barber
- [ ] cancelamento funciona com `PUT`
- [ ] `Invoicing` mostra overview real
- [ ] `series` aparece para owner no dashboard
- [ ] `Stock` mostra quantidade real de itens com estoque baixo
- [ ] `NextScheduling` exibe proximos agendamentos reais
- [ ] `products/inventory` responde com filtros e paginacao
- [ ] `products/{id}/movements` retorna historico paginado

## 7) Documentos gerados na sessao

- `analises/ADR_SERVICO_BASE_CATALOGO_GLOBAL.md`
- `analises/CHECKLIST_TECNICO_POR_SQUAD.md`
- `analises/PLANO_TATICO_2_SEMANAS_ADR_SERVICOS.md`

