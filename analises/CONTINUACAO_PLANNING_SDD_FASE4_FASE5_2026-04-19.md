# Continuação do Planning/SDD — Fases 4 e 5 (Pós Fase 3)

> Projeto: CortaAi  
> Branch: `feature/migracao-microservicos`  
> Data: 19/04/2026  
> Base: SDD + Planning anteriores + fechamento da Fase 3.

---

## 1) Status consolidado (checkpoint)

### Fases concluídas
- **Fase 1** (deduplicação frontend): concluída.
- **Fase 1.5** (fundação visual): concluída no escopo planejado.
- **Fase 2** (sistema visual unificado): concluída no escopo crítico.
- **Fase 3** (segurança alta prioridade): concluída com hardening em `api-gateway` e `payment-service` + testes.

### Gates já validados
- Build frontend: PASS
- Lint frontend: PASS
- Build gateway/payment: PASS
- Testes payment-service: PASS

### Ponto de entrada da continuação
Avançar para **Fase 4 (PWA base segura)** e depois **Fase 5 (hardening avançado)**, mantendo entregas pequenas e rollback simples.

---

## 2) Escopo da continuação

## Em escopo
1. PWA base com estratégia de cache segura para domínio transacional.
2. Observabilidade operacional para comportamento offline/atualização.
3. Hardening avançado de segurança (pipeline + política + sessão sensível com trilha de migração).

## Fora de escopo
1. Reescrita de autenticação completa em big-bang.
2. Mudança de stack frontend/backend.
3. Alteração de contratos públicos de API sem versionamento explícito.

---

## 3) Contrato técnico por fase

## Fase 4 — PWA base segura

### Entradas
- Frontend React/Vite estabilizado nas fases anteriores.
- Fluxos críticos existentes (`login`, `agendamento`, `pagamento`) sem regressão.

### Saídas
- App instalável (manifest + ícones + SW).
- Estratégia de cache controlada:
  - App shell: cache-first.
  - APIs transacionais: network-first.
- Banner de atualização e fallback offline mínimo (sem prometer consistência transacional offline).

### Critérios de sucesso
- Instalação PWA funcional em ambiente HTTPS.
- Sem exibir dados transacionais obsoletos como se fossem verdade em tela crítica.
- Rollback por commit/feature-flag de SW.

## Fase 5 — Hardening avançado

### Entradas
- Hardening de gateway/webhook já concluído.

### Saídas
- Segurança automatizada em pipeline (SAST/dependency scan/CVE).
- Política de logs com mascaramento e retenção orientada à LGPD.
- Plano executável de migração de sessão sensível (incremental, sem ruptura).

### Critérios de sucesso
- Pipeline falha em vulnerabilidade crítica nova.
- Checklist de segurança versionado e reutilizável.
- Trilha de migração de sessão aprovada (sem corte abrupto).

---

## 4) Plano executável em épicos, histórias e PRs

## Épico F4-E1 — Base PWA e instalação

### História F4-E1-H1
**Como** usuário recorrente, **quero** instalar o CortaAi no dispositivo, **para** acesso rápido e experiência de app.

**Arquivos alvo (frontend)**
- `frontend/public/manifest.webmanifest` (novo)
- `frontend/public/icons/*` (novos)
- `frontend/src/main.jsx` (registro controlado do SW)
- `frontend/vite.config.js` (plugin/config PWA)

**PRs sugeridos**
1. PR-F4-01: Manifest + ícones + metadados base.
2. PR-F4-02: Registro de SW com toggle por env (`VITE_ENABLE_PWA`).

**DoD**
- App instalável em ambiente HTTPS.
- Sem impacto em fluxo quando PWA desativado por env.

---

## Épico F4-E2 — Cache seguro para domínio transacional

### História F4-E2-H1
**Como** cliente/barbeiro, **quero** comportamento previsível offline/online, **para** não tomar decisão com dado desatualizado crítico.

**Estratégia de cache**
- `index.html`, assets estáticos: cache-first.
- Endpoints críticos (`/appointments`, `/payments`, `/auth`): network-first + timeout curto + fallback visual.
- Nunca confirmar ação transacional em modo offline sem roundtrip.

**PRs sugeridos**
1. PR-F4-03: Regras de runtime caching por rota.
2. PR-F4-04: Tela/mensagem offline contextual (`ca-state--error`/`ca-state--empty`).

**DoD**
- Estados offline explícitos em telas críticas.
- Sem confirmação falsa de agendamento/pagamento offline.

---

## Épico F4-E3 — Update flow e observabilidade PWA

### História F4-E3-H1
**Como** usuário, **quero** ser notificado quando houver nova versão, **para** atualizar sem comportamento inconsistente.

**PRs sugeridos**
1. PR-F4-05: Banner de atualização (nova versão disponível).
2. PR-F4-06: Instrumentação mínima de eventos (`sw_installed`, `sw_updated`, `offline_mode_entered`).

**DoD**
- Atualização controlada sem refresh abrupto.
- Eventos observáveis para diagnóstico.

---

## Épico F5-E1 — Segurança no CI/CD

### História F5-E1-H1
**Como** equipe de engenharia, **quero** barrar merge com vulnerabilidade crítica nova, **para** reduzir risco em produção.

**PRs sugeridos**
1. PR-F5-01: scan de dependências frontend/backend no pipeline.
2. PR-F5-02: política de severidade (falha em critical/high nova).

**DoD**
- Pipeline com status de segurança visível.
- Política documentada em `analises/` e aplicada.

---

## Épico F5-E2 — Política de logs e LGPD operacional

### História F5-E2-H1
**Como** operação, **quero** logs úteis sem PII sensível exposta, **para** auditoria segura.

**PRs sugeridos**
1. PR-F5-03: guideline de mascaramento e classificação de campos.
2. PR-F5-04: ajustes de logging em pontos sensíveis restantes.

**DoD**
- Campos sensíveis mascarados por padrão.
- Trilha de auditoria mínima definida.

---

## Épico F5-E3 — Migração incremental de sessão sensível

### História F5-E3-H1
**Como** arquitetura, **quero** reduzir dependência de armazenamento vulnerável no cliente, **para** elevar segurança sem quebrar autenticação atual.

**Estratégia de transição (sem big-bang)**
1. Compatibilidade dupla temporária.
2. Rollout por feature flag e % de usuários.
3. Observação de erro/latência/autenticação.
4. Corte final apenas após janela de estabilidade.

**PRs sugeridos**
1. PR-F5-05: desenho técnico + contrato de transição.
2. PR-F5-06: implementação piloto (ambiente controlado).

**Artefato desta etapa**
- `analises/PR_F5_05_CONTRATO_MIGRACAO_SESSAO_SENSIVEL_2026-04-19.md`
- `analises/PR_F5_06_PILOTO_SESSAO_COOKIE_2026-04-19.md`

**DoD**
- Sem regressão de login nas jornadas críticas.
- Plano de rollback imediato validado.

---

## 5) Matriz impacto x risco x esforço (continuação)

| Item | Impacto | Risco | Esforço | Prioridade |
|---|---:|---:|---:|---:|
| PWA base (manifest + install) | Médio | Baixo | Baixo | P1 |
| Cache seguro para rotas transacionais | Alto | Médio | Médio | P1 |
| Banner de atualização + telemetria SW | Médio | Baixo | Baixo | P1 |
| Security scan no pipeline | Alto | Baixo | Médio | P1 |
| Política de logs/LGPD operacional | Alto | Médio | Médio | P1 |
| Migração de sessão sensível (incremental) | Alto | Alto | Alto | P2 |

---

## 6) Edge cases obrigatórios (Fases 4 e 5)

1. Usuário offline tentando confirmar agendamento.
2. SW antigo servindo bundle após deploy crítico.
3. Inconsistência entre cache local e estado real de pagamento.
4. Upgrade de dependência com CVE e impacto em build pipeline.
5. Usuário autenticado em aba antiga após rollout parcial de sessão.
6. Queda de rede intermitente durante refresh de token.
7. Conteúdo stale em tela de agenda da equipe.

---

## 7) Quality gates por PR (continuação)

1. Build frontend = PASS
2. Lint frontend = PASS (sem novos erros)
3. Build backend módulos tocados = PASS
4. Testes módulos tocados = PASS
5. Smoke manual de fluxo crítico tocado = PASS
6. Checklist de segurança (quando aplicável) = PASS

---

## 8) Rollout e rollback

## Rollout
- Sempre por PR curto, com escopo único.
- Ativação de PWA e mudanças sensíveis por env flag.
- Monitoramento inicial de 30–60 min pós-deploy em fases críticas.

## Rollback
- Reversão por commit.
- Flags para desativar comportamento novo (`VITE_ENABLE_PWA`, rollout de sessão).
- Plano de contingência documentado antes de merge.

---

## 9) Próximos passos (status atualizado)

### Itens executados nesta trilha

1. **PR-F4-01/F4-02/F4-03** concluídos (manifest, SW com toggle e cache inicial).
2. **PR-F4-04/F4-05/F4-06** concluídos (offline contextual, banner de update e telemetria PWA).
3. **PR-F5-01 até PR-F5-06** concluídos (pipeline de segurança, LGPD logs, contrato e piloto de sessão).

### Próximo passo imediato

- Iniciar **PR-F6-01** conforme `analises/PLANO_PROXIMO_CICLO_FASE6_2026-04-19.md`.

---

## 10) Conclusão

A continuidade do planning/SDD está pronta para execução incremental, com foco em evolução de produto (PWA) e maturidade de segurança (hardening avançado) sem regressão funcional. O plano mantém compatibilidade, qualidade verificável por gate e rollback operacional claro.

---

## 11) Fechamento formal e próximo ciclo

- Fechamento da Fase 5: `analises/RELATORIO_FECHAMENTO_FASE5_2026-04-19.md`
- Próximo ciclo (Fase 6 operacional): `analises/PLANO_PROXIMO_CICLO_FASE6_2026-04-19.md`
