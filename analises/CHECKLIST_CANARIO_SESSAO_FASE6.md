# Checklist de Execução do Canário — Sessão Fase 6

> Projeto: CortaAi  
> Data: 19/04/2026  
> Escopo: PR-F6-01/PR-F6-02

---

## 1) Pré-rollout

- [ ] Flags revisadas no gateway (`SESSION_COOKIE_ENABLED`, `SESSION_BEARER_FALLBACK_ENABLED`)
- [ ] Flags revisadas no frontend (`VITE_SESSION_COOKIE_MODE`, `VITE_SESSION_COOKIE_CANARY_PERCENT`, `VITE_SESSION_BEARER_FALLBACK`)
- [ ] Janela de monitoramento combinada (30–60 min por estágio)
- [ ] Plano de rollback validado com time

---

## 2) Execução por estágio

## Estágio 5%

- [ ] Ativar canário em 5%
- [ ] Registrar início/fim da janela
- [ ] Coletar evidências dos eventos:
  - [ ] `session-auth-success`
  - [ ] `session-cookie-token-fallback-bearer`
  - [ ] `session-auth-unauthorized`

## Estágio 10/25/50/100%

- [ ] Repetir coleta para cada estágio
- [ ] Comparar com baseline anterior
- [ ] Validar ausência de regressão funcional

---

## 3) Smoke funcional por perfil

- [ ] Login `CUSTOMER`
- [ ] Login `BARBER`
- [ ] Fluxo de cadastro/completar perfil
- [ ] Acesso a rota protegida crítica (`appointments`/`payments`)
- [ ] Logout e relogin em múltiplas abas

---

## 4) Critérios de GO

- [ ] 401 estável (sem aumento sustentado)
- [ ] fallback Bearer em tendência controlada/decrescente
- [ ] sem erro crítico de autenticação em produção

---

## 5) Critérios de NO-GO

- [ ] aumento relevante e sustentado de 401
- [ ] pico de `session-auth-processing-error`
- [ ] regressão de login em qualquer perfil

Ação:

- [ ] executar rollback por flags imediatamente
- [ ] registrar incidente + evidência

---

## 6) Registro de evidência (template)

- Data/hora:
- Estágio (%):
- Janela observada:
- Volume de `session-auth-success`:
- Volume de `session-cookie-token-fallback-bearer`:
- Volume de `session-auth-unauthorized`:
- Decisão: GO / NO-GO
- Responsável:
