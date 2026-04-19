# PR-F6-02 — Execução de Canário Controlado da Sessão

> Projeto: CortaAi  
> Data: 19/04/2026  
> Fase: 6 (operacional)  
> Objetivo: executar rollout progressivo da sessão por cookie com fallback controlado e decisão por evidência.

---

## 1) Escopo desta entrega

Este PR consolida o **playbook operacional** para execução do canário de sessão.

Inclui:

- matriz de flags por estágio;
- critérios objetivos de GO/NO-GO;
- janela de observação e coleta de evidências;
- procedimento de rollback imediato.

> Observação: a execução em ambiente depende de acesso operacional (pipeline/deploy/observabilidade).

---

## 2) Pré-requisitos

- `PR-F6-01` concluído (instrumentação no gateway + guideline/checklist base).
- Flags de sessão disponíveis no backend/frontend.
- Time alinhado com janela de monitoramento de 30–60 min por estágio.

---

## 3) Matriz de flags por estágio

| Estágio | SESSION_COOKIE_ENABLED | SESSION_BEARER_FALLBACK_ENABLED | VITE_SESSION_COOKIE_MODE | VITE_SESSION_COOKIE_CANARY_PERCENT | VITE_SESSION_BEARER_FALLBACK |
|---|---|---|---|---:|---|
| Baseline | false | true | false | 0 | true |
| Canary 5% | true | true | true | 5 | true |
| Canary 10% | true | true | true | 10 | true |
| Canary 25% | true | true | true | 25 | true |
| Canary 50% | true | true | true | 50 | true |
| Canary 100% | true | true | true | 100 | true |

> Corte de fallback Bearer **não** faz parte deste PR (fica para `PR-F6-03`).

---

## 4) Procedimento operacional por estágio

1. Aplicar flags do estágio.
2. Publicar versão e confirmar tráfego ativo.
3. Monitorar janela mínima de 30–60 min.
4. Registrar evidência no relatório operacional.
5. Decidir GO/NO-GO antes de avançar.

---

## 5) Indicadores mínimos para decisão

- taxa de `session-auth-success` por `authSource`;
- volume de `session-cookie-token-fallback-bearer`;
- volume de `session-auth-unauthorized` por rota crítica;
- volume de `session-auth-invalid-token` e `session-auth-processing-error`.

---

## 6) Critérios GO

Avança estágio quando:

1. sem aumento sustentado de 401 em rotas críticas;
2. fallback Bearer estável/esperado para o estágio;
3. sem regressão funcional de login por perfil (`CUSTOMER`, `BARBER`, `OWNER`);
4. sem pico relevante de erro interno de autenticação.

## 7) Critérios NO-GO

Interromper avanço e executar rollback quando:

1. aumento sustentado de 401;
2. falhas de autenticação acima do baseline esperado;
3. regressão funcional em qualquer fluxo crítico de autenticação;
4. indício de inconsistência operacional em multi-aba/logout.

---

## 8) Rollback imediato

Aplicar:

- `SESSION_COOKIE_ENABLED=false`
- `SESSION_BEARER_FALLBACK_ENABLED=true`
- `VITE_SESSION_COOKIE_MODE=false`
- `VITE_SESSION_COOKIE_CANARY_PERCENT=0`
- `VITE_SESSION_BEARER_FALLBACK=true`

Em seguida:

1. validar redução de erro;
2. registrar incidente + janela de impacto;
3. bloquear avanço até análise técnica.

---

## 9) DoD do PR-F6-02

- [x] Playbook de canário documentado por estágio.
- [x] Matriz de flags definida.
- [x] Critérios GO/NO-GO objetivos registrados.
- [x] Procedimento de rollback imediato definido.
- [x] Template de evidência referenciado.

---

## 10) Artefatos

- `analises/PR_F6_02_EXECUCAO_CANARIO_SESSAO_2026-04-19.md`
- `analises/RELATORIO_EVIDENCIAS_CANARIO_SESSAO_FASE6.md`
