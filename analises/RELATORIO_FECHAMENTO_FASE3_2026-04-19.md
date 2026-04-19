# Relatório de Fechamento — Fase 3 (Segurança) 

> Projeto: CortaAi  
> Branch: `feature/migracao-microservicos`  
> Data: 19/04/2026  
> Escopo: hardening de segurança no `api-gateway` e `payment-service`, com cobertura de testes.

---

## 1) Resumo executivo

A Fase 3 foi concluída no escopo planejado de segurança de alta prioridade, com foco em:

1. **Sanitização e confiança de headers no gateway** (mitigação de spoofing de identidade).
2. **Validação robusta de webhook no payment-service** (assinatura + replay window).
3. **Redução de exposição de dados sensíveis em logs**.
4. **Cobertura de testes de segurança** para o fluxo de webhook.

Não houve mudança de contrato público de API para clientes do frontend. As mudanças são compatíveis com o comportamento atual e seguem o princípio de evolução incremental sem regressão.

---

## 2) Alterações implementadas (por arquivo)

## `backend/api-gateway/src/main/java/ifsp/edu/projeto/cortaai/apigateway/filter/FirebaseTokenGatewayFilter.java`

- Adicionada allowlist explícita de endpoints públicos que podem receber `Authorization` (`PUBLIC_AUTHORIZATION_ALLOWED_PATHS`).
- Para rotas públicas, passou a sanitizar headers de identidade (`X-User-*`) antes de encaminhar downstream.
- Para rotas privadas autenticadas, remove `Authorization` antes do encaminhamento downstream por padrão.
- Resultado: reduz risco de **header spoofing** e propagação indevida de credenciais.

## `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/controller/WebhookController.java`

- Endpoint `/api/payments/webhook` passou a receber `x-signature` e `x-request-id`.
- Inclusão de verificação de confiança via `paymentService.isWebhookTrusted(...)` antes do processamento.
- Logs reduzidos para evitar vazamento de payload bruto.
- Mantido retorno `200 OK` para evitar retentativa infinita da plataforma externa (com rejeição lógica quando inválido).

## `backend/payment-service/src/main/java/ifsp/edu/projeto/cortaai/paymentservice/service/PaymentService.java`

- Implementado `isWebhookTrusted(...)` com:
  - parsing de assinatura,
  - validação de timestamp,
  - controle de replay por janela de tempo,
  - cálculo HMAC SHA-256,
  - comparação segura (`secureEquals`).
- Inclusão de propriedades de configuração para segredo e janela de replay.
- Ajustes de logging para reduzir exposição de mensagens sensíveis.

## `backend/payment-service/src/main/resources/application.yml`

- Novas propriedades:
  - `mercadopago.webhook.secret`
  - `mercadopago.webhook.replay-window-seconds`
- Mantida compatibilidade local quando segredo não configurado (modo permissivo controlado).

## Testes adicionados

### `backend/payment-service/src/test/java/ifsp/edu/projeto/cortaai/paymentservice/service/PaymentServiceWebhookSecurityTest.java`

Cobertura de cenários críticos:
- Aceita webhook quando segredo não configurado (compatibilidade).
- Aceita assinatura válida dentro da janela.
- Rejeita assinatura inválida.
- Rejeita assinatura fora da janela de replay.

### `backend/payment-service/src/test/java/ifsp/edu/projeto/cortaai/paymentservice/controller/WebhookControllerTest.java`

Cobertura de controller (MockMvc standalone):
- Não processa webhook quando assinatura inválida.
- Processa webhook quando assinatura válida.

---

## 3) Quality gates (delta da fase)

- **Build (frontend): PASS**
- **Lint/Typecheck (frontend): PASS**
- **Build (api-gateway): PASS**
- **Build (payment-service): PASS**
- **Testes (payment-service): PASS**

---

## 4) Riscos residuais e mitigação

1. **Segredo de webhook não configurado em ambiente**
   - Risco: validação de assinatura fica desativada por compatibilidade.
   - Mitigação: exigir `MP_WEBHOOK_SECRET` em staging/prod com checklist de release.

2. **Janela de replay mal calibrada**
   - Risco: rejeição de webhooks legítimos ou aceitação excessiva.
   - Mitigação: iniciar em 300s e monitorar rejeições para ajuste controlado.

3. **Observabilidade insuficiente de rejeições**
   - Risco: dificuldade para diagnosticar incidentes de integração.
   - Mitigação: monitorar taxa de rejeição e correlacionar com `x-request-id`.

---

## 5) Rollout e rollback

## Rollout recomendado

1. Aplicar deploy de `api-gateway` e `payment-service`.
2. Configurar variáveis de ambiente:
   - `MP_WEBHOOK_SECRET`
   - `MP_WEBHOOK_REPLAY_WINDOW_SECONDS` (opcional; default 300)
3. Executar smoke de webhook (válido e inválido).
4. Monitorar logs e taxa de rejeição nos primeiros minutos.

## Rollback

- Reverter commit da fase no `api-gateway` e `payment-service`.
- Em emergência operacional, remover `MP_WEBHOOK_SECRET` para retornar ao modo permissivo temporário (somente contingência, não política final).

---

## 6) Pendências operacionais (pós-merge)

1. **Infra/DevOps**
   - Garantir segredo `MP_WEBHOOK_SECRET` em todos os ambientes não-locais.
   - Validar política de rotação de segredo.

2. **Observabilidade**
   - Criar alerta para aumento anormal de rejeições de webhook.
   - Dashboard com volume total x rejeitado x processado.

3. **QA funcional de integração**
   - Rodar roteiro de webhook com payload realista e assinatura válida/inválida.

---

## 7) Conclusão

A Fase 3 está **PR-ready** e fecha os objetivos de segurança priorizados sem quebra de fluxo funcional existente. O próximo passo recomendado é abrir PR com este relatório anexado, checklist de configuração de ambiente e validação de smoke pós-deploy.
