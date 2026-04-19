# PR-F5-06 — Piloto controlado da migração de sessão sensível

> Projeto: CortaAi  
> Data: 19/04/2026  
> Fase: F5-E3-H1 (passo 12/12)  
> Objetivo: ativar trilha incremental de autenticação por cookie HttpOnly com fallback Bearer, sem quebra de contrato.

---

## 1) Escopo implementado

### Backend (`api-gateway`)

Arquivo alterado:

- `backend/api-gateway/src/main/java/ifsp/edu/projeto/cortaai/apigateway/filter/FirebaseTokenGatewayFilter.java`

Implementação:

- resolução de token por ordem:
  1. cookie (`session.cookie.name`) quando `session.cookie.enabled=true`;
  2. Bearer header quando fallback habilitado.
- logs de trilha para observabilidade do piloto:
  - `event=session-cookie-token-used`
  - `event=session-cookie-token-fallback-bearer`
  - `event=session-cookie-auth-missing`

Contrato downstream preservado:

- headers `X-User-*` continuam sendo a única identidade encaminhada para microsserviços.

Arquivo de configuração alterado:

- `backend/api-gateway/src/main/resources/application.yml`

Novas flags:

- `session.cookie.enabled` ← env `SESSION_COOKIE_ENABLED` (default `false`)
- `session.cookie.name` ← env `SESSION_COOKIE_NAME` (default `cortaai_session`)
- `session.bearer-fallback.enabled` ← env `SESSION_BEARER_FALLBACK_ENABLED` (default `true`)

### Frontend (`axios wrapper`)

Arquivo alterado:

- `frontend/src/services/api.js`

Implementação:

- modo cookie controlado por env (`VITE_SESSION_COOKIE_MODE`);
- canário por percentual (`VITE_SESSION_COOKIE_CANARY_PERCENT`), com seleção determinística por hash de identidade local;
- fallback Bearer configurável (`VITE_SESSION_BEARER_FALLBACK`);
- `withCredentials` ativado apenas para usuários canário quando modo cookie está ligado;
- manutenção de comportamento legado por padrão (flags desligadas = sem mudança de runtime).

---

## 2) Estratégia de ativação recomendada

1. Deploy com defaults (cookie desligado) e observar baseline.
2. Habilitar no gateway:
   - `SESSION_COOKIE_ENABLED=true`
   - `SESSION_BEARER_FALLBACK_ENABLED=true`
3. Habilitar canário no frontend:
   - `VITE_SESSION_COOKIE_MODE=true`
   - `VITE_SESSION_COOKIE_CANARY_PERCENT=5`
   - `VITE_SESSION_BEARER_FALLBACK=true`
4. Subir gradualmente 5 → 10 → 25 → 50 → 100.
5. Após estabilidade, iniciar corte de fallback:
   - `SESSION_BEARER_FALLBACK_ENABLED=false`
   - `VITE_SESSION_BEARER_FALLBACK=false`

---

## 3) Rollback imediato

Para retorno ao comportamento anterior, sem rollback de código:

- `SESSION_COOKIE_ENABLED=false`
- `SESSION_BEARER_FALLBACK_ENABLED=true`
- `VITE_SESSION_COOKIE_MODE=false`
- `VITE_SESSION_COOKIE_CANARY_PERCENT=0`
- `VITE_SESSION_BEARER_FALLBACK=true`

---

## 4) Métricas mínimas de acompanhamento

- taxa de 401 por rota protegida;
- taxa de uso do fallback Bearer durante canário;
- latência P95/P99 de autenticação no gateway;
- taxa de login concluído por tipo (`CUSTOMER`, `BARBER`).

---

## 5) Critérios de sucesso do piloto

- sem regressão dos fluxos críticos de login/autorização;
- sem aumento sustentado de 401;
- fallback acionado apenas em casos esperados;
- rollback operacional validado por flags.

---

## 6) Observação importante

Este PR habilita o **piloto técnico de compatibilidade**. A emissão efetiva do cookie HttpOnly no fluxo de autenticação deve ser ativada conforme estratégia de backend de auth, mantendo os contratos atuais enquanto o rollout estiver em andamento.
