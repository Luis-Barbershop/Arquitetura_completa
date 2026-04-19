# Plano do Próximo Ciclo — Fase 6 Operacional (Pós Fase 5)

> Projeto: CortaAi  
> Data: 19/04/2026  
> Objetivo: operacionalizar a migração de sessão sensível em produção controlada, com observabilidade e critérios de corte objetivos.

---

## 1) Escopo da Fase 6

## Em escopo

1. ativação canário da sessão por cookie em ambiente controlado;
2. instrumentação operacional de autenticação no gateway;
3. checklist de smoke focado em fluxos críticos por perfil;
4. definição de critérios de avanço e corte do fallback Bearer.

## Fora de escopo

1. remoção definitiva do Bearer em um único PR;
2. reescrita de autenticação end-to-end;
3. mudança de contratos públicos de API.

---

## 2) Plano executável (PRs curtos)

## PR-F6-01 — Instrumentação operacional da sessão

### Objetivo

Adicionar telemetria/observabilidade mínima para acompanhar:

- autenticação por cookie;
- fallback para Bearer;
- falhas de autenticação por rota.

### Entregas

- guideline de métricas e eventos para sessão no gateway;
- painel/checklist operacional inicial para canário.

### Artefatos desta etapa

- `analises/GUIDELINE_OBSERVABILIDADE_SESSAO_FASE6.md`
- `analises/CHECKLIST_CANARIO_SESSAO_FASE6.md`

### DoD

- eventos mínimos documentados;
- forma de leitura operacional definida (logs + consulta padronizada).

## PR-F6-02 — Execução de canário controlado

### Objetivo

Rodar canário progressivo com flags já implementadas no F5-06.

### Etapas sugeridas

1. 5%
2. 10%
3. 25%
4. 50%
5. 100% (com fallback ainda ativo)

### DoD

- cada etapa com janela de observação e evidência registrada;
- sem degradação sustentada de 401/latência.

### Artefatos desta etapa

- `analises/PR_F6_02_EXECUCAO_CANARIO_SESSAO_2026-04-19.md`
- `analises/RELATORIO_EVIDENCIAS_CANARIO_SESSAO_FASE6.md`

## PR-F6-03 — Proposta de corte de fallback

### Objetivo

Tomar decisão técnica de desligar fallback Bearer com base em evidência.

### Entregas

- relatório de decisão (go/no-go);
- plano de corte gradual do fallback;
- rollback validado.

### DoD

- critérios quantitativos atendidos;
- plano de contingência testado.

---

## 3) Critérios objetivos de avanço

Avançar estágio apenas quando:

1. sem aumento relevante de `401` em rotas críticas;
2. sem regressão de login/cadastro/completar perfil por tipo de usuário;
3. latência de autenticação dentro do baseline aceito;
4. fallback Bearer em tendência decrescente conforme aumento do canário.

---

## 4) Edge cases obrigatórios da Fase 6

1. usuário com aba antiga durante aumento de canário;
2. cookie expirado com Bearer válido;
3. cookie inválido sem Bearer;
4. login social com redirecionamento e política `SameSite`;
5. logout multi-abas com consistência de sessão.

---

## 5) Quality gates por PR (Fase 6)

1. build frontend = PASS
2. lint frontend = PASS
3. build/test backend módulos tocados = PASS
4. smoke de autenticação por perfil = PASS
5. checklist de rollback = PASS

---

## 6) Rollout e rollback

## Rollout

- usar flags do F5-06;
- subir percentual apenas após janela de observação.

## Rollback

- retorno imediato para modo legado via flags:
  - `SESSION_COOKIE_ENABLED=false`
  - `SESSION_BEARER_FALLBACK_ENABLED=true`
  - `VITE_SESSION_COOKIE_MODE=false`
  - `VITE_SESSION_COOKIE_CANARY_PERCENT=0`
  - `VITE_SESSION_BEARER_FALLBACK=true`

---

## 7) Próximo passo imediato

Avançar para **PR-F6-03** com decisão técnica de corte gradual do fallback Bearer baseada nas evidências coletadas no canário.
