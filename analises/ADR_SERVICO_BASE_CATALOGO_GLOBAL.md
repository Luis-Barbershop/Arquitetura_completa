# ADR: Catalogo Global de Servicos + Oferta por Barbearia

- **Status:** Proposto (pronto para aprovacao)
- **Data:** 2026-04-04
- **Responsaveis:** Arquitetura CortaAI
- **Relacionados:** `frontend/src/pages/BarberServicesPage.jsx`, `frontend/src/services/appointmentService.js`, `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/model/Activity.java`, `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/model/enums/AppointmentStatus.java`

## Contexto

Hoje o sistema possui servicos por barbearia (modelo `Activity`) e alguns fluxos do frontend ainda estao com inconsistencias de contrato com o backend. Em paralelo, existe a necessidade de evoluir para um modelo onde:

1. Existe um **catalogo global de servicos**.
2. Cada barbearia define sua **oferta local** (preco e duracao) para servicos do catalogo.
3. Barbeiros podem sugerir servicos novos, que podem entrar no catalogo global para uso por outras barbearias.

Tambem foi identificado que o ecossistema precisa de padronizacao de estados e contratos:
- **Agendamento:** backend deve ser a fonte de verdade de status.
- **Faturamento:** dashboard do front ainda usa dados fixos, faltam endpoints agregados.
- **Produtos/estoque:** tela principal usa `localStorage` e mocks, sem integracao completa com backend.

## Problema

O modelo atual de servicos esta acoplado por barbearia e dificulta:
- reutilizacao de servicos entre lojas,
- descoberta e padronizacao de nomenclaturas,
- governanca de qualidade para evitar duplicidade/lixo,
- comparabilidade de indicadores por tipo de servico.

Ao mesmo tempo, contratos front/back inconsistentes geram risco de regressao funcional (agendamento, cancelamento e renderizacao de status).

## Decisao

Adotar arquitetura em dois niveis:

1. **Servico de Catalogo Global (conceitual):**
   - entidade canonicamente normalizada de servico,
   - sem preco/duracao obrigatorios globais,
   - com governanca e moderacao.

2. **Oferta de Servico por Barbearia:**
   - referencia um item do catalogo,
   - define `price` e `durationMinutes` por loja,
   - e a base usada no agendamento.

### Modelo de dominio proposto

## `service_catalog_item` (global)
- `id: UUID`
- `canonicalName: string` (normalizado)
- `displayName: string`
- `description: string|null`
- `category: enum`
- `tags: string[]`
- `status: ACTIVE | INACTIVE | MERGED`
- `mergedIntoId: UUID|null`
- `createdByUserId: UUID`
- `createdAt`, `updatedAt`

## `service_catalog_alias`
- `id: UUID`
- `catalogItemId: UUID`
- `aliasName: string`
- `normalizedAlias: string`

## `barbershop_service_offer` (local)
- `id: UUID`
- `barbershopId: UUID`
- `catalogItemId: UUID`
- `displayNameOverride: string|null`
- `price: decimal(10,2)`
- `durationMinutes: int`
- `active: boolean`
- `createdAt`, `updatedAt`

## `service_catalog_proposal`
- `id: UUID`
- `proposedByUserId: UUID`
- `sourceBarbershopId: UUID|null`
- `proposedName: string`
- `proposedCategory: enum|null`
- `notes: string|null`
- `status: PENDING | APPROVED | REJECTED | MERGED`
- `reviewedByUserId: UUID|null`
- `reviewReason: string|null`
- `createdAt`, `updatedAt`

## Regras de negocio

1. Agendamento referencia **oferta local** (`barbershop_service_offer.id`), nunca apenas nome livre.
2. Preco e duracao usados no agendamento vem da oferta local no momento da marcacao (snapshot).
3. Propostas de barbeiros entram em fila de moderacao.
4. Itens duplicados sao consolidados por `MERGED` + alias.
5. `assignedActivityIds` (habilidades do barbeiro) migra para IDs de oferta local (ou mapeamento equivalente).

## Fonte de verdade para status de agendamento

Padronizar regra oficial:
- **Backend (`schedule-service`) e a fonte de verdade.**
- Frontend apenas renderiza e aciona transicoes por endpoint explicito.

Canon de status publico recomendado:
- `SCHEDULED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`

Compatibilidade:
- Enquanto existir `CONCLUDED` internamente, mapear `CONCLUDED -> COMPLETED` no contrato de saida (ou no front temporariamente).

## Contratos minimos (JSON)

### Faturamento (dashboard barbeiro)

```json
GET /api/payments/my-shop/overview?from=2026-04-01&to=2026-04-30
{
  "barbershopId": "uuid",
  "currency": "BRL",
  "grossRevenue": 12450.00,
  "netRevenue": 9970.00,
  "platformFee": 620.00,
  "mpFee": 1860.00,
  "transactionsCount": 238,
  "approvedCount": 219,
  "pendingCount": 11,
  "cancelledCount": 8,
  "deltaVsPreviousPeriodPct": 12.3
}
```

```json
GET /api/payments/my-shop/series?groupBy=DAY&from=2026-04-01&to=2026-04-30
{
  "points": [
    {
      "date": "2026-04-01",
      "grossRevenue": 540.00,
      "netRevenue": 430.00,
      "transactions": 11
    }
  ]
}
```

### Produtos/estoque (operacao da barbearia)

```json
POST /api/products
{
  "barbershopId": "uuid",
  "name": "Pomada Modeladora",
  "description": "Fixacao forte",
  "category": "POMADE",
  "salePrice": 39.90,
  "costPrice": 22.50,
  "stockQuantity": 12,
  "minStockQuantity": 5,
  "unit": "UN",
  "sku": "POM-001",
  "imageUrl": "https://cdn.../pomada.png"
}
```

```json
GET /api/products?barbershopId=uuid&page=0&size=20&search=pomada&lowStock=true
{
  "items": [
    {
      "id": "uuid",
      "name": "Pomada Modeladora",
      "category": "POMADE",
      "salePrice": 39.90,
      "costPrice": 22.50,
      "stockQuantity": 12,
      "minStockQuantity": 5,
      "lowStock": false,
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

## Gaps atuais mapeados (Front x Back)

1. **Agendamento:** front chama rotas/metodos legados (`/appointments/customer/me`, `/appointments/barber/me`, `PATCH cancel`) enquanto backend atual usa `/appointments/my-appointments` e `PUT cancel`.
2. **Status:** front trabalha com `COMPLETED`; backend atual publica `CONCLUDED`.
3. **Criacao de agendamento:** payload do front sem `customerId` vs DTO backend exigindo `customerId`.
4. **Faturamento:** card `Invoicing` em mock (`R$ 800,00`) sem API agregada para dashboard.
5. **Produtos:** tela de estoque persiste em `localStorage` e nao usa `product-service` como fonte de verdade.

## Alternativas consideradas

1. **Manter servicos apenas por barbearia (status quo):**
   - Pro: menor esforco imediato.
   - Contra: duplicidade, baixa governanca, pouca escalabilidade.

2. **Catalogo global rigido sem ofertas locais:**
   - Pro: padronizacao total.
   - Contra: inviabiliza autonomia de preco/duracao por loja.

3. **Decisao escolhida (catalogo global + oferta local + proposta moderada):**
   - Equilibra padronizacao com autonomia operacional.

## Impactos

### Positivos
- Reuso de servicos entre barbearias.
- Melhor qualidade semantica do catalogo.
- Melhor base para analytics por tipo de servico.

### Negativos
- Maior complexidade de dominio e migracao.
- Necessidade de moderacao e ferramentas de governanca.

## Plano de migracao (fases)

1. **Fase 1 - Estabilizacao de contratos atuais**
   - Corrigir rotas/metodos front de agendamento.
   - Resolver `CONCLUDED` vs `COMPLETED` com compatibilidade.
   - Definir estrategia de `customerId` (preferencia: backend extrai do token).

2. **Fase 2 - Faturamento real no dashboard**
   - Expor endpoints agregados no `payment-service`.
   - Substituir mock de `Invoicing` por dados reais.

3. **Fase 3 - Estoque/produtos no backend**
   - Integrar `BarberStockPage` ao backend.
   - Evoluir contrato de produto para custo e estoque minimo.

4. **Fase 4 - Introducao do catalogo global**
   - Criar entidades de catalogo/oferta/proposta.
   - Migrar atividades atuais para oferta local com referencia ao catalogo.

5. **Fase 5 - Governanca e observabilidade**
   - Moderacao de propostas, deteccao de duplicidade e metricas de qualidade.

## Riscos

- Regressao em clientes que esperam status antigos.
- Divergencia de dados durante migracao parcial.
- Sobrecarga operacional da moderacao sem automacoes.

## Mitigacoes

- Versionamento de contrato e feature flags.
- Migrações idempotentes com rollback.
- Heuristica de duplicidade + fila de revisao por prioridade.

## Criterios de aceite

1. Front nao decide status de agendamento; apenas renderiza retorno do backend.
2. Fluxos de criar/listar/cancelar/concluir funcionam em contrato unico.
3. Dashboard de faturamento sem dados fixos em producao.
4. Estoque/produtos persistidos no backend (sem `localStorage` como fonte primaria).
5. Servico proposto por barbeiro pode ser aprovado e reutilizado por outra barbearia.
6. Duplicidades tratadas por merge/alias sem perda de historico.

