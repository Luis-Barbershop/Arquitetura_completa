# Guideline de Observabilidade da Sessão — Fase 6

> Projeto: CortaAi  
> Data: 19/04/2026  
> Escopo: PR-F6-01 (instrumentação operacional do canário de sessão)

---

## 1) Objetivo

Padronizar leitura operacional da autenticação durante o rollout de sessão por cookie, mantendo diagnóstico rápido de:

- sucesso por origem de autenticação;
- fallback para Bearer;
- falhas e 401 por rota.

---

## 2) Catálogo mínimo de eventos (gateway)

### Eventos de sucesso

- `event=session-auth-success`
  - campos: `correlationId`, `path`, `authSource`, `uid`, `role`

### Eventos de origem/fallback

- `event=session-cookie-token-used`
  - identifica tráfego autenticado via cookie.
- `event=session-cookie-token-fallback-bearer`
  - identifica fallback para Bearer durante piloto.

### Eventos de falha

- `event=session-auth-invalid-token`
  - token inválido/expirado.
- `event=session-auth-processing-error`
  - erro inesperado no processamento de autenticação.
- `event=session-auth-unauthorized`
  - resposta final 401 emitida pelo gateway.

### Eventos auxiliares

- `event=session-cookie-auth-missing`
  - cenário com cookie exigido e ausente.
- `event=gateway-email-not-verified`
  - bloqueio por e-mail não verificado em provider `password`.

---

## 3) Campos obrigatórios para correlação

Para leitura operacional consistente, usar sempre:

- `correlationId`
- `path`
- `authSource` (`COOKIE`, `BEARER`, `NONE`)
- `reason` (quando 401)

---

## 4) Indicadores de acompanhamento (canário)

1. **Taxa de 401 por rota protegida**
2. **Percentual de fallback Bearer** (`session-cookie-token-fallback-bearer`)
3. **Taxa de sucesso por origem** (`session-auth-success` por `authSource`)
4. **Erros de token inválido** (`session-auth-invalid-token`)

---

## 5) Regras de decisão operacional

Avança estágio do canário somente quando:

- 401 não aumenta de forma sustentada nas rotas críticas;
- fallback Bearer não cresce de forma anômala;
- autenticação por cookie apresenta estabilidade na janela observada.

Retrocede estágio (ou rollback por flag) quando:

- aumento relevante e sustentado de 401;
- queda abrupta de sucesso em `authSource=COOKIE`;
- pico de `session-auth-processing-error`.

---

## 6) Rollback imediato (flags)

- `SESSION_COOKIE_ENABLED=false`
- `SESSION_BEARER_FALLBACK_ENABLED=true`
- `VITE_SESSION_COOKIE_MODE=false`
- `VITE_SESSION_COOKIE_CANARY_PERCENT=0`
- `VITE_SESSION_BEARER_FALLBACK=true`

---

## 7) Observação de segurança

Logs devem manter mascaramento de identificadores e **nunca** incluir token/cookie bruto em texto claro.
