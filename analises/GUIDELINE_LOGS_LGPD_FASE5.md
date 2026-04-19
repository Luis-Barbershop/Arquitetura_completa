# Guideline de Logs e LGPD Operacional — Fase 5

> Projeto: CortaAi  
> Data: 19/04/2026  
> Escopo: `PR-F5-03` e base para `PR-F5-04`

---

## 1) Objetivo

Padronizar logs com rastreabilidade operacional sem exposição de dados pessoais, credenciais ou segredos.

---

## 2) Classificação de dados para logging

### Permitido em texto claro

- `correlationId`
- método HTTP, path e status HTTP
- códigos de erro de domínio
- timestamps técnicos
- nomes de evento (`event=...`)

### Permitido somente mascarado

- `userId`, `barberId`, `customerId`, `mpUserId`
- `resourceId` externo (webhook/payment)
- e-mail
- UUIDs de entidades de negócio

### Proibido em log

- tokens (`Authorization`, Firebase ID token, MP OAuth token)
- segredos (`client_secret`, `webhook secret`, `refresh token`)
- payload bruto de credenciais
- cabeçalhos de autenticação completos

---

## 3) Regras de mascaramento

1. IDs: manter prefixo curto e sufixo curto (ex.: `abc1...9f0d`).
2. E-mail: manter 2 primeiros caracteres do usuário e domínio parcial (ex.: `jo***@g***.com`).
3. Mensagens de exceção externas: preferir classe da exceção e código/status; evitar texto bruto do provedor.
4. `state`/parâmetros de callback: nunca logar valor integral quando inválido.

---

## 4) Retenção e acesso (diretriz operacional)

- Retenção default recomendada para logs de aplicação: **30 dias**.
- Logs de auditoria de segurança: **90 dias** (com acesso restrito).
- Acesso apenas para perfis de operação/segurança com trilha de auditoria.

---

## 5) Padrão mínimo de evento

Formato recomendado:

- `event=<nome> correlationId=<id> outcome=<ok|fail> details=<campo mascarado>`

Exemplos:

- `event=webhook-processed correlationId=... resourceId=ab12...9f00 outcome=ok`
- `event=gateway-firebase-invalid-token correlationId=... outcome=fail`

---

## 6) Checklist para code review

- [ ] Algum log imprime token/secret/cookie/header sensível?
- [ ] IDs de usuário/cliente/provedor estão mascarados?
- [ ] Erros externos evitam mensagem bruta com potencial PII?
- [ ] Existe `correlationId` para rastrear incidentes?
- [ ] Logs de webhook/OAuth seguem padrão de evento?

---

## 7) Observações de rollout

- Aplicar primeiro nos fluxos críticos: gateway auth, webhook pagamentos e callback OAuth.
- Expandir para demais serviços no `PR-F5-04` sem alterar contratos de API.

---

## 8) Cobertura aplicada nesta sessão

- `api-gateway`: filtro Firebase com mascaramento de UID e sanitização de mensagem.
- `payment-service`: webhook/OAuth e logs de integração com IDs mascarados.
- `user-service`: autenticação Firebase e endpoints internos com mascaramento de UID/e-mail/IDs.
- `barbershop-service`: integração com user-service e fluxo de convites/solicitações com IDs mascarados e erro sanitizado.
