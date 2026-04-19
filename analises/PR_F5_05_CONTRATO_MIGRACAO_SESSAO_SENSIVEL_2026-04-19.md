# PR-F5-05 — Desenho técnico e contrato de transição da sessão sensível

> Projeto: CortaAi  
> Data: 19/04/2026  
> Fase: F5-E3-H1 (passo 11/12)  
> Objetivo: preparar migração incremental de sessão sensível para cookie HttpOnly sem quebrar autenticação atual.

---

## 1) Escopo e motivação

Estado atual validado no código:

- Frontend injeta `Authorization: Bearer <token>` via `frontend/src/services/api.js` lendo `localStorage.token`.
- Fluxos de login/verify persistem `token` e metadados em `localStorage` (`frontend/src/services/authService.js`).
- Gateway valida token Firebase no `Authorization` e injeta headers internos `X-User-*` para downstream (`backend/api-gateway/.../FirebaseTokenGatewayFilter.java`).
- Gateway remove `Authorization` para rotas internas não explicitamente allowlistadas.

Risco atual:

- Exposição potencial do token em cenários de XSS por armazenamento em `localStorage`.

Meta desta trilha:

- Migrar sessão sensível de forma progressiva para cookie HttpOnly + `SameSite` + `Secure`, mantendo compatibilidade temporária com Bearer atual.

---

## 2) Contrato de transição (compatibilidade dupla)

### 2.1 Princípios

1. **Sem big-bang**: aceitar simultaneamente sessão por cookie e por Bearer durante janela de transição.
2. **Prioridade controlada**: quando ambos existirem, o gateway prioriza cookie (novo) e mantém Bearer como fallback.
3. **Sem quebra de contrato dos microsserviços**: downstream continua recebendo apenas `X-User-*` injetados pelo gateway.
4. **Rollback instantâneo por flag**: desativação do caminho novo sem rollback estrutural de código.

### 2.2 Contrato de entrada no gateway (transicional)

O gateway deve aceitar, em ordem:

1. Cookie de sessão HttpOnly (novo caminho).
2. Header `Authorization: Bearer` (legado/fallback).

Regras:

- Rotas públicas continuam sem obrigatoriedade de autenticação.
- Em rotas protegidas, ausência de cookie e de Bearer => `401` (com `correlationId`).
- Em caso de cookie inválido e Bearer válido durante fase de migração, seguir com Bearer e registrar métrica de fallback.

### 2.3 Contrato de saída do gateway (inalterado)

Independente da origem da sessão (cookie ou Bearer), o gateway mantém o contrato downstream:

- `X-User-UID`
- `X-User-Email`
- `X-User-Type`
- `X-User-Owner`
- `X-Correlation-Id`

`Authorization` não é propagado para serviços internos (regra atual preservada).

---

## 3) Modelo alvo de sessão

### 3.1 Artefatos de sessão

- **Cookie sensível (HttpOnly)**: token de sessão/refresh (não acessível por JS).
- **Estado de UI não sensível**: `userRole`, `userName`, `isOwner`, `barbershopId` podem permanecer em storage para UX, desde que não sejam usados como fonte de autorização.

### 3.2 Atributos mínimos do cookie

- `HttpOnly=true`
- `Secure=true` (ambientes HTTPS)
- `SameSite=Lax` (ou `Strict` após validação de fluxos externos)
- `Path=/`
- TTL compatível com política de sessão (definida no PR-F5-06)

---

## 4) Feature flags e estratégia de ativação

## 4.1 Flags propostas

Backend (gateway):

- `SESSION_COOKIE_ENABLED` — habilita leitura/validação do cookie.
- `SESSION_BEARER_FALLBACK_ENABLED` — mantém fallback Bearer enquanto `true`.

Frontend:

- `VITE_SESSION_COOKIE_MODE` — ativa chamadas com `withCredentials` e fluxo de sessão por cookie.
- `VITE_SESSION_COOKIE_CANARY_PERCENT` — percentual de ativação controlada (0-100).

> Observação: nomes seguem convenção existente de flags (`VITE_ENABLE_*`). Podem ser refinados no PR-F5-06, mantendo semântica equivalente.

## 4.2 Etapas de rollout

1. **Etapa A (0%)**: código novo deployado com flag desligada (comportamento atual).
2. **Etapa B (canary 5-10%)**: habilitar cookie mode para fração controlada.
3. **Etapa C (25-50%)**: ampliar após estabilidade de autenticação/latência.
4. **Etapa D (100% com fallback ligado)**: cookie principal, Bearer ainda de segurança.
5. **Etapa E (cutover final)**: desligar `SESSION_BEARER_FALLBACK_ENABLED` após janela estável.

---

## 5) Observabilidade e critérios de decisão

## 5.1 Métricas mínimas

- Taxa de `401` por rota protegida (antes/depois por etapa).
- Taxa de fallback para Bearer quando cookie falhar.
- Latência P95/P99 de autenticação no gateway.
- Taxa de login concluído por tipo de usuário (`CUSTOMER`/`BARBER`).

## 5.2 Eventos de log recomendados

- `event=session-cookie-auth-success`
- `event=session-cookie-auth-fallback-bearer`
- `event=session-cookie-auth-invalid`
- `event=session-bearer-auth-success`

Todos com:

- `correlationId`
- `outcome`
- IDs mascarados (sem token/secret/cookie bruto)

## 5.3 SLO de corte para próxima etapa

Avança etapa apenas se, por janela mínima de observação:

- Sem aumento relevante de `401` em rotas críticas.
- Sem regressão de login/verify/reportes operacionais.
- Latência dentro do baseline aceitável definido no deploy.

---

## 6) Plano de rollback

Rollback operacional imediato (sem redeploy completo):

1. Desligar `SESSION_COOKIE_ENABLED`.
2. Manter `SESSION_BEARER_FALLBACK_ENABLED=true`.
3. Forçar frontend para modo legado (`VITE_SESSION_COOKIE_MODE=false`).

Rollback de código (se necessário):

- Reverter PR do piloto (`PR-F5-06`) mantendo este documento como referência histórica de decisão.

---

## 7) Edge cases obrigatórios para o piloto F5-06

1. Usuário com aba antiga (Bearer) durante rollout parcial.
2. Cookie expirado + Bearer válido (deve autenticar por fallback).
3. Cookie inválido sem Bearer (deve retornar `401` consistente).
4. Fluxo OAuth/login social com redirecionamento e `SameSite`.
5. Logout em multi-abas limpando sessão de forma consistente.
6. Ambiente local HTTP (comportamento controlado para `Secure`).

---

## 8) Critérios de pronto (DoD) do PR-F5-05

- [x] Contrato de compatibilidade dupla documentado.
- [x] Estratégia de flags e rollout faseado documentada.
- [x] Plano de rollback imediato definido.
- [x] Métricas/eventos para decisão de avanço definidos.
- [x] Edge cases críticos mapeados para execução no PR-F5-06.

---

## 9) Limites desta entrega

Este PR-F5-05 **não implementa** a migração em código. Entrega apenas o desenho técnico executável e rastreável para o piloto controlado (`PR-F5-06`).
