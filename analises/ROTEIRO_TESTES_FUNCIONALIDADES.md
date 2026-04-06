# Roteiro de Testes — Funcionalidades Críticas

> **Projeto:** CortaAi  
> **Branch:** `feature/migracao-microservicos`  
> **Data:** Abril 2026  
> **Ambiente:** Docker Compose local + Postman Collection (`collection.json`)

---

## TC-01 — Persistência de Barbearias Favoritas

### Objetivo
Garantir que as barbearias marcadas como favoritas por um cliente persistem corretamente no banco de dados e são restauradas após logout/login.

### Pré-condições
- Usuário do tipo `CUSTOMER` cadastrado e autenticado.
- Ao menos 2 barbearias cadastradas no sistema.

### Passos

| # | Ação | Resultado Esperado |
|---|------|--------------------|
| 1 | Faça login como cliente na tela `/homepage`. | Tela de barbearias carregada. |
| 2 | Clique no ícone de coração (★) de **barbearia A**. | Ícone muda para preenchido; toast "Barbearia adicionada aos favoritos". |
| 3 | Clique no ícone de coração de **barbearia B**. | Ícone muda para preenchido; toast de confirmação. |
| 4 | Faça **logout** (clique no avatar → Sair). | Redireciona para `/identificacao`. |
| 5 | Faça **login** novamente com o mesmo usuário. | Tela inicial carregada. |
| 6 | Verifique os ícones de coração nas barbearias A e B. | **Ambos devem aparecer preenchidos** (favoritos persistidos). |
| 7 | Clique no coração de **barbearia A** para remover. | Ícone muda para vazio; toast de remoção. |
| 8 | Atualize a página (F5). | Barbearia A → coração vazio; Barbearia B → coração preenchido. |

### Verificação via API (Postman)

```
GET /api/barbershops/favorites
Authorization: Bearer <token_do_cliente>
```

Resposta esperada:
```json
["<uuid-barbearia-B>"]
```

### Verificação no banco

```sql
SELECT * FROM customer_favorite_barbershops
WHERE customer_id = '<uuid_do_cliente>';
```

### Critério de aceite
- ✅ Favoritos persistem após logout/login.
- ✅ Remoção de favorito reflete imediatamente na UI e no banco.
- ✅ Nenhum favorito duplicado no banco.

---

## TC-02 — Webhook do Mercado Pago (Pagamento de Agendamento)

### Objetivo
Validar que o `payment-service` processa corretamente a notificação de pagamento aprovado do Mercado Pago, atualizando o status do agendamento de `SCHEDULED` para `CONFIRMED`.

### Pré-condições
- Docker Compose rodando localmente (`docker compose up`).
- [ngrok](https://ngrok.com) instalado para expor a porta local.
- Conta no [Mercado Pago Developers](https://www.mercadopago.com.br/developers) com credenciais de **sandbox**.
- Agendamento criado com status `SCHEDULED` e ID conhecido.

### Preparação

#### 1. Expor o payment-service via ngrok
```bash
# O payment-service escuta na porta 8084 (via gateway 8080)
ngrok http 8080
# Copie a URL gerada, ex: https://abc123.ngrok.io
```

#### 2. Configurar webhook no painel MP
1. Acesse [Mercado Pago Developers → Suas integrações](https://www.mercadopago.com.br/developers/panel/app).
2. Selecione a aplicação CortaAi.
3. Em **Webhooks → Configurar notificações**, adicione:
   - URL: `https://abc123.ngrok.io/api/payments/webhook`
   - Eventos: `payment`
4. Salve.

### Passos

| # | Ação | Resultado Esperado |
|---|------|--------------------|
| 1 | Crie um agendamento via `POST /api/appointments`. Anote o `appointmentId`. | Status `SCHEDULED`. |
| 2 | Inicie o fluxo de pagamento via `POST /api/payments/preference` com o `appointmentId`. | Retorna `preferenceId` e `initPoint`. |
| 3 | Acesse o `initPoint` no navegador e complete o pagamento com cartão de teste do MP sandbox. | Pagamento aprovado na tela do MP. |
| 4 | Aguarde a notificação webhook chegar (5–30s). | Log do `payment-service` exibe: `"Webhook recebido: payment"`. |
| 5 | Consulte o agendamento: `GET /api/appointments/<appointmentId>`. | Status deve ser `CONFIRMED`. |
| 6 | Verifique notificação por e-mail (se SMTP configurado). | E-mail de confirmação enviado ao cliente. |

### Simulação manual do webhook (sem ngrok)

Caso ngrok não esteja disponível, simule a notificação diretamente:

```bash
curl -X POST http://localhost:8080/api/payments/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "type": "payment",
    "data": { "id": "<payment_id_do_mp_sandbox>" }
  }'
```

> Obtenha o `payment_id` na aba **Atividade** do painel MP sandbox após criar uma preferência de teste.

### Cartões de teste MP Sandbox

| Situação | Número | CVV | Validade |
|----------|--------|-----|----------|
| Aprovado | `5031 4332 1540 6351` | `123` | `11/25` |
| Recusado | `4235 6477 2802 5682` | `123` | `11/25` |
| Pendente | `3743 781877 55283` | `1234` | `11/25` |

### Verificação no banco

```sql
SELECT id, status FROM appointments WHERE id = '<appointmentId>';
-- Esperado: status = 'CONFIRMED'

SELECT * FROM payments WHERE appointment_id = '<appointmentId>';
-- Esperado: status = 'PAID', mercado_pago_payment_id preenchido
```

### Critério de aceite
- ✅ Webhook recebido e processado sem erro 500.
- ✅ Status do agendamento atualizado para `CONFIRMED`.
- ✅ Registro de pagamento criado no banco com `status = PAID`.
- ✅ Evento `payment.confirmed` publicado no RabbitMQ (verificar via `rabbitmq-management` em `http://localhost:15672`).

---

## TC-03 — Logs de Entrega de E-mail (Notification Service)

### Objetivo
Confirmar que o `notification-service` recebe eventos do RabbitMQ e registra (ou envia) e-mails corretamente para agendamentos criados, confirmados e cancelados.

### Pré-condições
- Docker Compose rodando com `notification-service`, `rabbitmq`.
- SMTP configurado no `.env` (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`).

### Opção A — Verificação via logs do container

```bash
# Verificar logs em tempo real
docker compose logs -f notification-service

# Filtrar apenas envios de e-mail
docker compose logs notification-service | grep -i "email\|mail\|enviado\|notificação"
```

#### Eventos esperados nos logs

| Evento RabbitMQ | Log esperado |
|-----------------|--------------|
| `appointment.created` | `"Enviando e-mail de confirmação para <email>"` |
| `appointment.cancelled` | `"Enviando e-mail de cancelamento para <email>"` |
| `appointment.concluded` | `"Enviando e-mail de conclusão para <email>"` |

### Opção B — Usando Mailtrap (SMTP fake para desenvolvimento)

1. Crie uma conta gratuita em [mailtrap.io](https://mailtrap.io).
2. Copie as credenciais SMTP da caixa de entrada de teste.
3. Configure no `.env`:
   ```env
   MAIL_HOST=sandbox.smtp.mailtrap.io
   MAIL_PORT=2525
   MAIL_USERNAME=<seu_usuario_mailtrap>
   MAIL_PASSWORD=<sua_senha_mailtrap>
   ```
4. Reinicie o `notification-service`:
   ```bash
   docker compose restart notification-service
   ```
5. Crie ou cancele um agendamento.
6. Acesse o painel do Mailtrap — o e-mail deve aparecer em segundos.

### Opção C — Verificação via RabbitMQ Management UI

1. Acesse `http://localhost:15672` (usuário: `guest`, senha: `guest`).
2. Vá em **Queues**.
3. Verifique a fila `notification.queue` (ou equivalente configurada).
4. Clique em **Get messages** para ver mensagens pendentes.
5. Se a fila estiver vazia, os eventos foram consumidos com sucesso.

### Passos de teste integrado

| # | Ação | Verificação |
|---|------|-------------|
| 1 | Crie um agendamento via Postman (`POST /api/appointments`). | Log: e-mail de criação enviado. |
| 2 | Cancele o agendamento (`PUT /api/appointments/<id>/cancel`). | Log: e-mail de cancelamento enviado. |
| 3 | Crie e conclua um agendamento (`PUT /api/appointments/<id>/conclude`). | Log: e-mail de conclusão enviado. |
| 4 | Cheque a caixa de entrada no Mailtrap (ou SMTP real). | E-mails recebidos com dados corretos. |

### Verificação de conteúdo do e-mail

Cada e-mail deve conter:
- Nome do cliente
- Nome do barbeiro
- Nome da barbearia
- Data e horário do agendamento
- Serviços contratados
- Valor total

### Critério de aceite
- ✅ `notification-service` consome eventos do RabbitMQ sem falhas.
- ✅ E-mail enviado em até 30s após o evento.
- ✅ Conteúdo do e-mail contém todos os dados do agendamento.
- ✅ Sem mensagens de erro nos logs do container.
- ✅ Fila RabbitMQ esvaziada após processamento.

---

## TC-04 — Agendamento Manual (Walk-in) pelo Barbeiro

### Objetivo
Validar o fluxo completo do agendamento manual (`WALK_IN`) criado pelo barbeiro sem necessidade de conta do cliente.

### Pré-condições
- Usuário do tipo `BARBER` autenticado e vinculado a uma barbearia com serviços cadastrados.

### Passos (Frontend)

| # | Ação | Resultado Esperado |
|---|------|--------------------|
| 1 | Acesse `/barberHome/dashboard`. | Botão "✂️ Novo Agendamento (Walk-in)" visível. |
| 2 | Clique no botão. | Redireciona para `/barberHome/novo-agendamento`. |
| 3 | Preencha nome do cliente: "João Walk-in". | Campo aceito. |
| 4 | Preencha telefone: "(11) 99999-9999". | Campo aceito. |
| 5 | Selecione data e horário livres. | Campos preenchidos. |
| 6 | Clique em ao menos 1 serviço na lista. | Card do serviço destaca em dourado; resumo mostra duração e valor. |
| 7 | Clique em "Registrar atendimento". | Toast: "Agendamento registrado com sucesso!"; formulário limpo. |

### Verificação via API (Postman)

```
POST /api/appointments/barber-booking
Authorization: Bearer <token_do_barbeiro>
Content-Type: application/json

{
  "barbershopId": "<uuid-da-barbearia>",
  "activityIds": ["<uuid-do-servico>"],
  "startTime": "2026-04-10T10:00:00",
  "clientName": "João Walk-in",
  "clientPhone": "(11) 99999-9999"
}
```

Resposta esperada `201 Created`:
```json
{
  "id": "<uuid>",
  "customerName": "João Walk-in",
  "status": "WALK_IN",
  ...
}
```

### Critério de aceite
- ✅ Agendamento criado com `status = WALK_IN`.
- ✅ Nenhum evento de pagamento gerado no RabbitMQ.
- ✅ Conflito de horário detectado corretamente (tente criar outro agendamento no mesmo horário).
- ✅ Campo `clientName` é obrigatório — erro 400 sem ele.

---

## Comandos úteis para execução local

```bash
# Subir todos os serviços
docker compose up -d

# Ver logs de um serviço específico
docker compose logs -f schedule-service
docker compose logs -f payment-service
docker compose logs -f notification-service

# Reiniciar um serviço após mudança de .env
docker compose restart notification-service

# Acessar RabbitMQ Management
# http://localhost:15672 — guest/guest

# Executar query no banco via Docker
docker exec -it cortaai-postgres psql -U cortaai -d cortaai_db
```
