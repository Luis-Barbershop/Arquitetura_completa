# Relatório de Evidências — Canário de Sessão (Fase 6)

> Projeto: CortaAi  
> Data-base: 19/04/2026  
> Uso: preencher durante execução dos estágios do PR-F6-02.

---

## 1) Identificação da execução

- Ambiente:
- Responsável técnico:
- Janela global de execução:
- Versão/backend deploy:
- Versão/frontend deploy:

---

## 2) Baseline (antes do canário)

- Janela observada:
- Taxa de 401 (rotas críticas):
- Volume `session-auth-success` (`COOKIE`/`BEARER`):
- Volume `session-auth-unauthorized`:
- Volume `session-auth-invalid-token`:
- Volume `session-auth-processing-error`:
- Observações:

---

## 3) Estágio 5%

- Início/fim da janela:
- Flags aplicadas (backend/frontend):
- Taxa de 401 (rotas críticas):
- `session-auth-success` por origem:
- `session-cookie-token-fallback-bearer`:
- Erros relevantes:
- Smoke por perfil: PASS / FAIL
- Decisão: GO / NO-GO
- Responsável da decisão:
- Observações:

---

## 4) Estágio 10%

- Início/fim da janela:
- Flags aplicadas:
- Taxa de 401:
- `session-auth-success` por origem:
- `session-cookie-token-fallback-bearer`:
- Erros relevantes:
- Smoke por perfil: PASS / FAIL
- Decisão: GO / NO-GO
- Responsável:
- Observações:

---

## 5) Estágio 25%

- Início/fim da janela:
- Flags aplicadas:
- Taxa de 401:
- `session-auth-success` por origem:
- `session-cookie-token-fallback-bearer`:
- Erros relevantes:
- Smoke por perfil: PASS / FAIL
- Decisão: GO / NO-GO
- Responsável:
- Observações:

---

## 6) Estágio 50%

- Início/fim da janela:
- Flags aplicadas:
- Taxa de 401:
- `session-auth-success` por origem:
- `session-cookie-token-fallback-bearer`:
- Erros relevantes:
- Smoke por perfil: PASS / FAIL
- Decisão: GO / NO-GO
- Responsável:
- Observações:

---

## 7) Estágio 100% (com fallback ativo)

- Início/fim da janela:
- Flags aplicadas:
- Taxa de 401:
- `session-auth-success` por origem:
- `session-cookie-token-fallback-bearer`:
- Erros relevantes:
- Smoke por perfil: PASS / FAIL
- Decisão: GO / NO-GO
- Responsável:
- Observações:

---

## 8) Registro de incidentes (se houver)

| Data/hora | Estágio | Sintoma | Impacto | Ação | Resultado |
|---|---|---|---|---|---|
| | | | | | |

---

## 9) Conclusão operacional

- Resultado final da execução: GO / NO-GO
- Recomendação para PR-F6-03 (corte de fallback):
- Pendências abertas:
- Aprovação final (nome/funcão/data):
