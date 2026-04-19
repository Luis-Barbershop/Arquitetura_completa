# Planning de Evolução Segura (sem quebrar o funcionamento atual)

> **Objetivo:** implementar melhorias de visual/layout, UX, qualidade de código, segurança e PWA em fases pequenas, verificáveis e reversíveis.

---

## 1) Princípios operacionais

1. **Sem big-bang**: entregas pequenas por domínio.
2. **Compatibilidade primeiro**: novos serviços/hooks convivem com legado até estabilizar.
3. **Feature flag para mudanças transversais**.
4. **Uma fase só avança com quality gates verdes**.
5. **Rollback claro** por fase (commit + flag).

---

## 2) Contrato de execução

### Entradas
- Código atual em `feature/migracao-microservicos`
- Fluxos críticos validados (agendamento, login, pagamento)
- Perfis de visualização definidos: `cliente`, `barbeiro`, `barbeiro owner`

### Saídas
- Menos duplicação de lógica no frontend
- Interface visual premium com layout consistente e moderno
- Experiência segmentada por perfil sem perder consistência visual
- Segurança reforçada no gateway e integrações sensíveis
- Base de PWA com cache controlado
- Documentação de decisão e rollout

### Critérios de sucesso
- Zero regressão funcional em fluxos críticos
- Build/lint/testes passando por fase
- Ganho visível de consistência visual entre páginas críticas
- Clareza de navegação e CTA principal para cada perfil (cliente/barbeiro/owner)
- Time apto a manter e evoluir sem retrabalho

---

## 3) Edge cases a cobrir obrigatoriamente

1. Horários no limite de virada de dia (`23:45` → dia seguinte)
2. Timezone divergente entre servidor e cliente
3. Webhook duplicado/reentregue
4. Usuário owner/barber alternando permissões/abas
5. Cliente offline voltando online com dados em cache
6. Conteúdo extenso (nomes longos) quebrando layout em mobile
7. Estados vazios com CTA não visível em telas pequenas

---

## 4) Roadmap faseado

## Fase 0 — Baseline (1-2 dias)

### Entregas
- Congelar baseline de rotas e fluxos críticos.
- Definir checklists automáticos mínimos por PR.

### Qualidade
- Build: obrigatório
- Lint: obrigatório
- Smoke de fluxo crítico: obrigatório

### Rollback
- Reverter PR único da fase.

---

## Fase 1 — Deduplicação segura frontend (3-5 dias)

### Entregas
- `navigationService.js` com mapeamento central de tabs/rotas.
- Hook `useAuthGuard` para guardas de role/token.
- `appointmentAvailabilityService.js` como camada única de disponibilidade.

### Estratégia
- Não remover implementação antiga nesta fase.
- Migrar inicialmente apenas 2-3 páginas piloto.

### DoD
- Nenhuma mudança visual inesperada.
- Páginas piloto com comportamento idêntico ao atual.

---

## Fase 1.5 — Fundação visual/layout premium (3-5 dias)

### Entregas
- Definir grid responsivo oficial (4/8/12 colunas) e containers padrão.
- Consolidar escala tipográfica e espaçamento global.
- Criar padrões de página (listagem, detalhe, formulário, dashboard).
- Aplicar quick wins visuais em 2 fluxos críticos (agendamento + home do barbeiro).
- Definir variações de navegação e prioridade de conteúdo por perfil (`cliente`, `barbeiro`, `barbeiro owner`).

### Estratégia
- Adoção incremental sem quebrar componentes legados.
- Usar tokens e templates para evitar retrabalho visual nas próximas fases.

### DoD
- Coerência visual percebida entre as páginas piloto.
- Layout validado em mobile e desktop sem quebras relevantes.
- Checklist visual por PR adotado no time.
- Diferenças de experiência por perfil validadas sem quebrar o design system base.

---

## Fase 2 — Sistema visual unificado (3-4 dias)

### Entregas
- `styles/tokens.css` (cor, spacing, radius, shadow, z-index).
- Política de breakpoints padrão.
- Política global `prefers-reduced-motion`.

### Estratégia
- Adotar tokens em componentes mais usados primeiro (header, cards, botões).

### DoD
- Sem regressão visual em mobile/desktop das páginas piloto.

---

## Fase 3 — Segurança alta prioridade (4-6 dias)

### Entregas
- Hardening de headers no gateway.
- Revisão de logs para evitar vazamento de segredos.
- Validação robusta de webhook MP (assinatura, replay window).

### Estratégia
- Alterações protegidas por logs de auditoria e métricas.

### DoD
- Testes de integração de webhook aprovados.
- Nenhum segredo exposto em logs coletados por amostragem.

---

## Fase 4 — PWA base (3-5 dias)

### Entregas
- Manifest, ícones e service worker.
- Cache app shell + network-first para APIs transacionais.
- Banner de atualização e fallback offline básico.

### Estratégia
- Inicialmente sem cache agressivo de disponibilidade/pagamento.

### DoD
- App instalável em Android/desktop.
- Fluxo offline básico não compromete dados transacionais.

---

## Fase 5 — Hardening avançado (5-8 dias)

### Entregas
- Plano de migração de sessão sensível para cookie HttpOnly (quando aprovado).
- SAST/DAST/CVE no pipeline.
- Auditoria LGPD (retenção + mascaramento).

### DoD
- Política de segurança documentada e automatizada no CI.

---

## 5) RACI simplificado

- **Tech Lead:** decisão de arquitetura e critérios de corte por fase.
- **Frontend:** deduplicação, design system, layout premium, PWA base.
- **Produto/UX:** direção visual, priorização de melhorias de experiência e validação de usabilidade.
- **Backend:** gateway hardening, webhook security, observabilidade.
- **QA:** cenários críticos e regressão por fase.
- **DevOps:** pipeline e monitoramento.

---

## 6) Quality gates por fase (obrigatórios)

1. Build backend (módulos tocados) = PASS
2. Build frontend = PASS
3. Lint frontend = PASS (ou baseline aprovado sem novos erros)
4. Testes críticos automatizados = PASS
5. Checklist manual curto de UX (mobile/desktop) = PASS
6. Checklist visual/layout (grid, estados, CTA, contraste, foco) = PASS
7. Checklist de consistência por perfil (cliente/barbeiro/owner) = PASS

---

## 7) Métricas de acompanhamento

- Taxa de regressão por release
- Tempo médio de correção após deploy
- Cobertura de fluxos críticos
- Erros de autorização/autenticação por rota
- Latência média de disponibilidade/agendamento
- Taxa de conclusão de agendamento (antes/depois)
- Tempo até primeira ação útil em telas-chave
- Satisfação visual/usabilidade em rodada rápida com usuários

---

## 8) Próximo passo recomendado

Iniciar **Fase 1** com PR pequeno:

1. adicionar `navigationService.js`,
2. migrar 2 páginas piloto,
3. validar regressão,
4. expandir gradualmente.

Em seguida, executar **Fase 1.5** para elevar aparência premium com baixo risco e preparar o terreno das próximas evoluções.
