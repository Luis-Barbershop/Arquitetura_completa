# Plano Tatico de 2 Semanas - Execucao da ADR de Servicos

- **Data:** 2026-04-04
- **Escopo:** estabilizacao de contratos (agendamento), faturamento real, estoque no backend e MVP de catalogo global
- **Base:** `analises/ADR_SERVICO_BASE_CATALOGO_GLOBAL.md`

## Checklist de execucao

- [ ] Executar P0 de agendamento e status (backend como fonte de verdade)
- [ ] Substituir mock de faturamento por dados reais
- [ ] Migrar estoque de `localStorage` para backend
- [ ] Entregar MVP de catalogo global + oferta local + proposta/moderacao
- [ ] Validar go/no-go de release parcial no D10

## 1) Backlog priorizado por dia util (D1..D10)

| Dia | Prioridade | Entregas do dia | Owners sugeridos |
|---|---|---|---|
| D1 | P0 | Congelar contrato canonico de agendamento/status no `schedule-service`; alinhar gaps do `appointmentService` | Schedule + Frontend + QA |
| D2 | P0 | Publicar compatibilidade `CONCLUDED -> COMPLETED` e politica de transicoes em `AppointmentController` e OpenAPI | Schedule + QA |
| D3 | P0 | Corrigir front para `GET /appointments/my-appointments` e `PUT /appointments/{id}/cancel` em `appointmentService` e `MeusAgendamentosPage` | Frontend + Schedule + QA |
| D4 | P1 | Criar endpoints de faturamento `overview/series` em `PaymentController` com contexto da loja | Payment + QA |
| D5 | P1 | Integrar `Invoicing` aos endpoints reais; remover valor fixo | Frontend + Payment + QA |
| D6 | P1 | Evoluir contrato de produtos (cost/min stock/filtros) em `ProductController` | Product + QA |
| D7 | P1 | Trocar `localStorage` por backend em `BarberStockPage` com fallback por feature flag | Frontend + Product + QA |
| D8 | P2 | Criar MVP de `service_catalog_item` + `barbershop_service_offer` no `barbershop-service` | Barbershop + QA |
| D9 | P2 | Criar fluxo de proposta/moderacao (`PENDING/APPROVED/REJECTED/MERGED`) e compatibilidade com `assignedActivityIds` | Barbershop + User + QA |
| D10 | P0/P1/P2 | Hardening final para release parcial: contratos, observabilidade e gates | QA + todos os squads |

## 2) Owners sugeridos por squad

- **Frontend:** ajustes de contrato em `frontend/src/services/appointmentService.js`, dashboard `frontend/src/components/BarberPage/Invoicing.jsx`, estoque `frontend/src/pages/BarberStockPage.jsx`
- **Schedule:** status/transicoes em `backend/schedule-service/src/main/java/ifsp/edu/projeto/cortaai/scheduleservice/controller/AppointmentController.java`
- **Payment:** agregacoes de receita e serie em `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/controller/PaymentController.java`
- **Product:** contrato e filtros em `backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/controller/ProductController.java`
- **Barbershop:** catalogo/oferta/moderacao em `backend/barbershop-service/src/main/java/ifsp/edu/projeto/cortaai/barbershopservice/controller/BarbershopController.java`
- **User:** compatibilidade de habilidades em `backend/user-service/src/main/java/ifsp/edu/projeto/cortaai/userservice/controller/BarberController.java`
- **QA:** contrato/e2e cross-service e observabilidade

## 3) Ordem recomendada de PRs (com dependencias)

1. **PR-01** `backend/schedule-service`: status canonico + transicoes + compatibilidade de saida (`CONCLUDED -> COMPLETED`)
2. **PR-02** `frontend`: ajuste de rotas/metodos de agendamento (depende de PR-01)
3. **PR-03** `backend/payment-service`: `my-shop/overview` e `my-shop/series` (depende de PR-01)
4. **PR-04** `frontend`: integrar `Invoicing` ao PR-03
5. **PR-05** `backend/product-service`: contrato de produtos com filtros e baixo estoque
6. **PR-06** `frontend`: integrar `BarberStockPage` ao PR-05
7. **PR-07** `backend/barbershop-service`: entidades/CRUD de catalogo global + oferta local
8. **PR-08** `backend/user-service`: compatibilidade de habilidades (depende de PR-07)
9. **PR-09** `backend/barbershop-service`: propostas e moderacao (depende de PR-08)
10. **PR-10** `frontend`: ajustes finais da UX de servicos para novo modelo (depende de PR-09)
11. **PR-11** `qa/observabilidade`: suites de contrato/e2e e alertas (depende de PR-02, PR-04, PR-06, PR-10)

## 4) Criterios de entrada/saida por semana

### Semana 1 (D1-D5)

**Entrada**
- ADR alinhada entre squads
- donos tecnicos por servico definidos
- prioridade P0 aprovada

**Saida**
- contrato de agendamento estabilizado
- front sem endpoints legados criticos
- faturamento com API real disponivel e consumido no dashboard

### Semana 2 (D6-D10)

**Entrada**
- PRs P0/P1 da semana 1 mergeados
- dados minimos de pagamentos e produtos confiaveis

**Saida**
- estoque no backend sem dependencia primaria de `localStorage`
- MVP catalogo global + oferta local + proposta/moderacao ativo
- checklist de go/no-go aprovado

## 5) Riscos e mitigacoes

- **Divergencia de status entre servicos:** contrato unico e mapper de compatibilidade
- **Quebra no front por mudanca de endpoint:** rollout com feature flag e fallback temporario
- **Inconsistencia na migracao de habilidades:** camada de compatibilidade no `user-service`
- **Sobrecarga de moderacao:** deduplicacao basica + fila priorizada por uso
- **Atraso por dependencia cruzada:** sincronizacao diaria e SLA de desbloqueio em 24h

## 6) Go/No-Go (release parcial no fim da semana 2)

- [ ] Front nao usa rotas legadas criticas de agendamento
- [ ] Status exibido no front vem do backend sem regra local decisoria
- [ ] Card de faturamento sem mock em `frontend/src/components/BarberPage/Invoicing.jsx`
- [ ] Estoque usa backend como fonte primaria em `frontend/src/pages/BarberStockPage.jsx`
- [ ] Endpoints de catalogo/oferta/proposta estaveis
- [ ] Compatibilidade de `assignedActivityIds` preserva fluxo atual
- [ ] Erros 4xx/5xx monitorados para agendamento/pagamentos/produtos/catalogo
- [ ] Fluxos criticos sem regressao bloqueante
- [ ] Nenhum bloqueio P0/P1 aberto no D10

