# Checklist Tecnico por Squad

- **Escopo:** Agendamento, Faturamento, Produtos/Estoque, Catalogo Global de Servicos
- **Data-base:** 2026-04-04
- **Objetivo:** remover inconsistencias front/back e habilitar arquitetura de catalogo global com governanca

## Prioridades globais

- [ ] P0: Fonte de verdade de status no backend (`schedule-service`).
- [ ] P0: Alinhar contratos front/back de agendamento (rotas, metodos, payload).
- [ ] P1: Substituir faturamento mock por endpoints agregados reais.
- [ ] P1: Tirar estoque de `localStorage` como fonte primaria.
- [ ] P2: Implementar catalogo global + oferta local + moderacao de propostas.

---

## Squad Frontend

### Tarefas
- [ ] Ajustar `frontend/src/services/appointmentService.js` para usar `GET /appointments/my-appointments`.
- [ ] Trocar cancelamento para `PUT /appointments/{id}/cancel`.
- [ ] Ajustar leitura/renderizacao de status para compatibilidade `CONCLUDED`/`COMPLETED`.
- [ ] Revisar `frontend/src/pages/MeusAgendamentosPage.jsx` para filtros por status canonico.
- [ ] Integrar `frontend/src/components/BarberPage/Invoicing.jsx` com endpoint de overview de faturamento.
- [ ] Integrar `frontend/src/pages/BarberStockPage.jsx` com API de produtos/estoque (CRUD real).
- [ ] Remover hardcodes de `frontend/src/components/BarberPage/NextScheduling.jsx`, `frontend/src/components/BarberPage/Stock.jsx`, `frontend/src/components/BarberPage/DailyInsights.jsx` (consumir APIs reais ou esconder bloco por feature flag).

### Dependencias
- [ ] Endpoints de faturamento agregados publicados.
- [ ] Contrato de produtos/estoque finalizado.
- [ ] Definicao oficial de status de agendamento publicada.

### Definicao de pronto
- [ ] Nenhum card de dashboard critico com dados fixos.
- [ ] Fluxos de agendamento e cancelamento funcionando com contrato unico.
- [ ] Tela de estoque persistindo dados no backend.

---

## Squad Schedule Service

### Tarefas
- [ ] Publicar politica oficial de status e transicoes permitidas.
- [ ] Padronizar status de conclusao para contrato publico (`COMPLETED`) com compatibilidade para `CONCLUDED`.
- [ ] Garantir endpoints explicitos de transicao (`confirm`, `conclude/complete`, `cancel`).
- [ ] Revisar `CreateAppointmentDTO`: decidir se `customerId` vem do token (preferencial) e remover dependencia do front.
- [ ] Documentar OpenAPI atualizado dos endpoints de agendamento.

### Dependencias
- [ ] Alinhamento com API Gateway para injecao de identidade.
- [ ] Alinhamento com frontend sobre status exibidos.

### Definicao de pronto
- [ ] Nenhum status decidido no frontend.
- [ ] OpenAPI refletindo contratos usados em producao.
- [ ] Testes de transicao de status cobrindo cenarios invalidos.

---

## Squad Payment Service

### Tarefas
- [ ] Criar endpoint `GET /payments/my-shop/overview` com periodo e KPIs.
- [ ] Criar endpoint `GET /payments/my-shop/series` para grafico (DAY/WEEK/MONTH).
- [ ] Garantir filtro por `barbershopId` via contexto autenticado (owner/barber autorizado).
- [ ] Expor status de transacoes e agregacoes consistentes com dashboard.
- [ ] Revisar integracao `updatePaymentStatus` com `schedule-service` para nao usar status inexistente.

### Dependencias
- [ ] Identidade/autorizacao da loja disponivel no gateway.
- [ ] Contrato comum de status entre payment e schedule.

### Definicao de pronto
- [ ] `Invoicing` sem mock e com valores reais do periodo.
- [ ] Endpoints de faturamento cobertos por testes de integracao.

---

## Squad Product Service

### Tarefas
- [ ] Evoluir DTO de produto para suportar `costPrice` e `minStockQuantity`.
- [ ] Implementar listagem paginada com filtros (`search`, `category`, `lowStock`).
- [ ] Expor operacoes de ajuste de estoque (entrada/saida) com historico.
- [ ] Garantir validacoes de dominio (nao negativo, arredondamento monetario, categorias).

### Dependencias
- [ ] Acordo de UX da tela de estoque.
- [ ] Definicao se produto representa estoque interno, venda ao cliente ou ambos.

### Definicao de pronto
- [ ] Front usa backend como fonte primaria de estoque.
- [ ] Alertas de estoque baixo calculados no backend.
- [ ] Historico minimo de movimentacoes disponivel.

---

## Squad Barbershop Service

### Tarefas
- [ ] Introduzir dominio de catalogo global e oferta local (ou novo servico dedicado, conforme decisao final).
- [ ] Criar CRUD de `barbershop_service_offer` (preco/duracao/ativo).
- [ ] Criar fluxo de proposta de novo servico por barbeiro.
- [ ] Implementar moderacao (`PENDING`, `APPROVED`, `REJECTED`, `MERGED`).
- [ ] Implementar busca com normalizacao para evitar duplicidade.

### Dependencias
- [ ] Definicao de ownership da moderacao (admin central vs owners).
- [ ] Definicao de migracao dos `Activity` atuais.

### Definicao de pronto
- [ ] Mesma entrada de catalogo pode ser ofertada por multiplas barbearias.
- [ ] Proposta aprovada fica reutilizavel no catalogo global.
- [ ] Fluxo de merge/alias preserva historico.

---

## Squad User Service

### Tarefas
- [ ] Migrar `assignedActivityIds` para referenciar ofertas locais (ou camada de compatibilidade).
- [ ] Garantir endpoints de habilidades do barbeiro compatveis com novo modelo.
- [ ] Revisar autorizacoes de owner/barber para criacao de ofertas/propostas.

### Dependencias
- [ ] Modelo final de IDs (catalogo vs oferta).

### Definicao de pronto
- [ ] Vinculo de habilidades funcional no novo modelo sem quebrar agendamento.

---

## QA e Observabilidade

### Tarefas
- [ ] Criar matriz de testes e2e para agendamento: criar, listar, confirmar, concluir, cancelar.
- [ ] Testar compatibilidade de status (`CONCLUDED` legado vs `COMPLETED` canonico).
- [ ] Criar testes de contrato para endpoints de faturamento e estoque.
- [ ] Monitorar erros 4xx/5xx por endpoint no gateway.
- [ ] Criar dashboards de negocio: taxa de aprovacao de pagamentos, receita diaria, estoque baixo, propostas aprovadas.

### Definicao de pronto
- [ ] Suite e2e verde em pipeline para fluxos criticos.
- [ ] Alertas de regressao de contrato configurados.

---

## Contratos JSON de referencia rapida

### Faturamento overview

```json
{
  "barbershopId": "uuid",
  "currency": "BRL",
  "grossRevenue": 800.00,
  "netRevenue": 650.00,
  "platformFee": 40.00,
  "mpFee": 110.00,
  "transactionsCount": 14,
  "approvedCount": 12,
  "pendingCount": 1,
  "cancelledCount": 1,
  "deltaVsPreviousPeriodPct": 15.0
}
```

### Produto/estoque

```json
{
  "id": "uuid",
  "barbershopId": "uuid",
  "name": "Pomada Modeladora",
  "category": "POMADE",
  "salePrice": 39.90,
  "costPrice": 22.50,
  "stockQuantity": 12,
  "minStockQuantity": 5,
  "lowStock": false,
  "active": true
}
```

---

## Sequencia recomendada de entrega

- [ ] Semana 1: P0 agendamento (contrato/status).
- [ ] Semana 2: P1 faturamento (overview + serie + front).
- [ ] Semana 3: P1 estoque/produtos (backend + front).
- [ ] Semana 4+: P2 catalogo global e moderacao.

## Gate final de release

- [ ] Nenhum endpoint legado critico sendo usado no frontend.
- [ ] Nenhum status ambiguo em producao.
- [ ] Nenhum modulo principal dependente de mock/localStorage para dado de negocio.
- [ ] Telemetria e alertas ativos para fluxos de agendamento e pagamento.

