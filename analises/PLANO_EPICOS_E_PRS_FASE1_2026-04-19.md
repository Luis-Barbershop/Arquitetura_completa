# Plano Executável — Épicos, Histórias e Checklist de PR (Fase 1)

> Baseado em: `analises/PLANNING_EVOLUCAO_SEM_REGRESSAO_2026-04-19.md` + SDD aprovada em 19/04/2026.
> Objetivo: iniciar Fase 1 (deduplicação segura frontend) com PRs pequenos, reversíveis e sem regressão.

---

## 1) Guardrails da execução

1. Sem big-bang.
2. Novas camadas convivem com legado até estabilização.
3. Toda alteração com rollback explícito por PR.
4. Quality gates obrigatórios antes de merge.
5. Não alterar contratos de API nesta fase.

---

## 2) Escopo fechado da Fase 1

### Entregas obrigatórias
- `frontend/src/services/navigationService.js`
- `frontend/src/hooks/useAuthGuard.js`
- `frontend/src/services/appointmentAvailabilityService.js`
- Migração piloto de **3 páginas**:
  - `frontend/src/pages/BarberHomePage.jsx`
  - `frontend/src/pages/BarberDashboardPage.jsx`
  - `frontend/src/pages/BarberManualBookingPage.jsx`

### Fora do escopo nesta fase
- Redesign visual completo (Fase 1.5+).
- Mudanças de backend/gateway/payment.
- PWA.

---

## 3) Épicos e histórias (implementáveis)

## Épico E1 — Navegação centralizada por perfil

### Objetivo
Eliminar duplicação de mapeamento de tabs/rotas espalhada nas páginas do barbeiro.

### História E1-H1
**Como** barbeiro/owner, **quero** que a troca de abas siga roteamento consistente, **para** evitar comportamentos divergentes entre páginas.

**Arquivos**
- Novo: `frontend/src/services/navigationService.js`
- Alterar:
  - `frontend/src/pages/BarberHomePage.jsx`
  - `frontend/src/pages/BarberDashboardPage.jsx`

**Contrato interno sugerido**
- `resolveBarberRoute(tab, { isOwner }) => string`
- `getAvailableTabs({ isOwner }) => string[]`

**Critérios de aceite**
- Mesmo tab sempre resolve para mesma rota.
- Regras owner x não-owner centralizadas no serviço.
- Sem alteração de URL pública existente.

**Trade-off**
- Prós: previsibilidade + manutenção.
- Contras: coexistência temporária com lógica legada.

**Rollback**
- Reverter import/uso do `navigationService` nas páginas piloto.

---

## Épico E2 — Guarda de autenticação/autorização reutilizável

### Objetivo
Consolidar validações de sessão/role/owner hoje repetidas em várias páginas.

### História E2-H1
**Como** sistema, **quero** validar acesso via hook único, **para** reduzir inconsistência de redirecionamento.

**Arquivos**
- Novo: `frontend/src/hooks/useAuthGuard.js`
- Alterar (piloto):
  - `frontend/src/pages/BarberDashboardPage.jsx`
  - `frontend/src/pages/BarberHomePage.jsx`

**Contrato interno sugerido**
- `useAuthGuard({ requireAuth, requireOwner, redirectTo })`
- Retorno: `{ isAuthorized, isLoadingAuth }`

**Critérios de aceite**
- Página owner-only redireciona corretamente quando não-owner.
- Guardas de autenticação não duplicadas no corpo da página.
- Comportamento final idêntico ao fluxo atual.

**Trade-off**
- Prós: padronização e menor chance de regressão futura.
- Contras: ajuste inicial de efeito/ordem de render.

**Rollback**
- Restaurar validações inline anteriores nas páginas piloto.

---

## Épico E3 — Disponibilidade de agendamento centralizada

### Objetivo
Remover lógica duplicada de busca de slots e normalização de disponibilidade.

### História E3-H1
**Como** barbeiro, **quero** que disponibilidade seja carregada por serviço único, **para** evitar divergência com a página de agendamento principal.

**Arquivos**
- Novo: `frontend/src/services/appointmentAvailabilityService.js`
- Alterar (piloto):
  - `frontend/src/pages/BarberManualBookingPage.jsx`

**Contrato interno sugerido**
- `fetchAvailabilitySlots({ barberId, dateISO, durationMinutes }) => string[]`
- `hydrateDateOptionsWithAvailability({ barberId, dateOptions, durationMinutes }) => DateOptions[]`

**Critérios de aceite**
- Resultado de slots equivalente ao comportamento atual.
- Erro de API tratado sem quebrar tela (fallback previsível).
- Sem chamada HTTP direta duplicada fora de `services`.

**Trade-off**
- Prós: consistência de regra de disponibilidade.
- Contras: etapa de extração exige validação fina de edge cases.

**Rollback**
- Restaurar função local de `fetchSlots` na página piloto.

---

## 4) Sequência de PRs recomendada (pequenos)

1. **PR-1 (infra de deduplicação)**
   - Criar `navigationService.js`.
   - Criar testes unitários do serviço.
   - Sem alteração de páginas ainda.

2. **PR-2 (piloto navegação)**
   - Migrar `BarberHomePage.jsx` e `BarberDashboardPage.jsx` para `navigationService`.
   - Manter fallback local por 1 iteração (se necessário).

3. **PR-3 (auth guard)**
   - Criar `useAuthGuard.js`.
   - Aplicar em `BarberDashboardPage.jsx` e `BarberHomePage.jsx`.

4. **PR-4 (disponibilidade centralizada)**
   - Criar `appointmentAvailabilityService.js`.
   - Migrar `BarberManualBookingPage.jsx`.

5. **PR-5 (limpeza controlada)**
   - Remover código legado não usado nas páginas piloto.
   - Atualizar documentação curta da camada `services/hooks`.

---

## 5) Checklist de PR por arquivo (copiar no template do PR)

## Checklist técnico obrigatório
- [ ] Mudança limitada ao escopo da fase.
- [ ] Sem quebra de rotas públicas existentes.
- [ ] Sem mudança de contrato backend.
- [ ] Sem criação de novo axios (usar `frontend/src/services/api.js`).
- [ ] Sem CSS global novo (somente CSS Modules quando houver ajuste visual).
- [ ] Tratamento explícito de loading/erro/vazio quando aplicável.

## Arquivos críticos a revisar
- [ ] `frontend/src/services/navigationService.js`
- [ ] `frontend/src/hooks/useAuthGuard.js`
- [ ] `frontend/src/services/appointmentAvailabilityService.js`
- [ ] `frontend/src/pages/BarberHomePage.jsx`
- [ ] `frontend/src/pages/BarberDashboardPage.jsx`
- [ ] `frontend/src/pages/BarberManualBookingPage.jsx`

## Checklist de qualidade (gate)
- [ ] Build frontend PASS.
- [ ] Lint frontend PASS (ou sem novos erros).
- [ ] Testes unitários novos PASS.
- [ ] Smoke manual: login, navegação de abas, acesso owner-only, novo agendamento.

## Checklist de regressão por perfil
- [ ] `cliente`: não acessa telas owner-only.
- [ ] `barbeiro`: navega sem acessar funções exclusivas de owner.
- [ ] `barbeiro owner`: mantém acesso completo às abas de gestão.

---

## 6) Casos críticos de teste (Fase 1)

1. Tab `agenda-equipe` com usuário não-owner deve bloquear/redirecionar.
2. Usuário com `isOwner` divergente entre API e `localStorage` deve seguir regra centralizada.
3. Falha temporária de disponibilidade não deve quebrar render de `BarberManualBookingPage`.
4. Agendamento no limite de virada de dia (`23:45`) mantém consistência de slots.
5. Timezone cliente diferente não deve deslocar dia selecionado indevidamente.
6. Recarregar página durante troca de aba preserva rota esperada.
7. Sessão expirada redireciona para login sem loop.

---

## 7) Definição de pronto (DoD) da Fase 1

- Serviços/hook novos criados e usados nas 3 páginas piloto.
- Comportamento funcional equivalente ao atual (sem regressão observável).
- Build/lint/test/smoke verdes.
- Rollback descrito e validado por PR.
- Base preparada para Fase 1.5 (fundação visual premium) sem retrabalho estrutural.

---

## 8) Plano de rollout e rollback por PR

| PR | Rollout | Rollback |
|---|---|---|
| PR-1 | merge em branch de trabalho + validação unitária | revert commit único |
| PR-2 | habilitar uso de `navigationService` nas páginas piloto | remover import/uso e restaurar handlers locais |
| PR-3 | ativar `useAuthGuard` em 2 páginas | restaurar guardas inline anteriores |
| PR-4 | ativar `appointmentAvailabilityService` no manual booking | restaurar `fetchSlots` local |
| PR-5 | limpeza de legado já não utilizado | revert seletivo apenas de remoções |

---

## 9) Métricas rápidas de acompanhamento (Fase 1)

- Taxa de erro de navegação por aba (console/API).
- Incidentes de autorização por role/owner.
- Tempo médio de ajuste após PR (retrabalho).
- Regressões reportadas em fluxo de agendamento manual.

---

## 10) Próximo passo imediato (hoje)

Abrir **PR-1** com:
1. `navigationService.js`
2. testes unitários do mapeamento de tab->rota
3. checklist de PR preenchido

Após PR-1 verde, iniciar PR-2 no mesmo dia.
