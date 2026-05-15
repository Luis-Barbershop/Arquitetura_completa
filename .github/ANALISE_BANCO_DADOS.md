# CortaAi — Análise Completa do Banco de Dados

> Gerado em: 2026-05-15 | Atualizado em: 2026-05-15 (pós-correções)  
> Branch: `feature/migracao-microservicos`  
> Fonte: análise estática de todas as entidades JPA + `init.sql` + configurações RabbitMQ/Docker + **inspeção ao vivo do MySQL de produção** (`ssh Edu@10.147.19.1`)

---

## ✅ Status dos Achados Críticos

> Todos os 5 achados críticos identificados na análise inicial foram resolvidos.  
> Ver detalhes em [`analises/SDD_CORRECAO_DIVERGENCIAS_BANCO_2026-05-15.md`](../analises/SDD_CORRECAO_DIVERGENCIAS_BANCO_2026-05-15.md)

| # | Severidade | Tabela | Problema | Resolução |
|---|---|---|---|---|
| 1 | 🔴 **CRÍTICO** | `product_db.categories` | `barbershop_id` era `binary(16)` no banco, entidade JPA sem `@JdbcTypeCode` | ✅ **Corrigido** — `Category.java` recebeu `@JdbcTypeCode(Types.VARCHAR)` + `columnDefinition="VARCHAR(36)"`; DDL `ALTER TABLE` executada em produção com migração de dados |
| 2 | 🟡 **MÉDIO** | `notification_db.device_tokens` | `PushPlatform` Java declarava ANDROID/IOS; banco só tem WEB | ✅ **Corrigido** — `PushPlatform.java` agora declara apenas `WEB` (consistente com DB) |
| 3 | 🟡 **MÉDIO** | Views cross-database | `v_customer_retention` e `v_barber_financial_performance` fazem JOIN cross-schema | ✅ **Documentado** — views marcadas como `[CROSS-DB] ANALYTICS-ONLY` em `views.sql`; decisão arquitetural registrada no SDD |
| 4 | 🟢 **INFO** | `payment_db.transactions` | `IN_PROCESS` existia no banco mas não na enum Java | ✅ **Corrigido** — `PaymentStatus` inclui `IN_PROCESS` |
| 5 | 🟢 **INFO** | `notification_db.notifications` | `NotificationChannel` Java tinha `PUSH`; banco não tem | ✅ **Corrigido** — `NotificationChannel` contém apenas `IN_APP, EMAIL` |
| — | 🟡 **EXTRA** | `barbershop_db` (FixedExpense) | `FixedExpenseService.resolveOwner` buscava por e-mail; Principal era Firebase UID | ✅ **Corrigido por outro agente** (`aed477d`) — troca para `getUserByFirebaseUid` |

---

## 2. Visão Geral da Topologia de Bancos (dev local via Docker / produção via `docker-compose.server.yml`).  
Cada microsserviço possui seu banco isolado — **nenhuma FK cross-service existe no banco**.  
Referências entre serviços são mantidas via **UUIDs armazenados como VARCHAR(36)**.

| Banco | Serviço dono | Tabelas |
|---|---|---|
| `user_db` | `user-service` | `barbers`, `customers`, `barber_work_blocks`, `barber_assigned_activities`, `customer_favorite_barbershops` |
| `barbershop_db` | `barbershop-service` | `barbershops`, `activities`, `barbershop_join_requests`, `barbershop_highlights`, `barbershop_reviews`, `barber_commission_rules`, `fixed_expenses` |
| `schedule_db` | `schedule-service` | `appointments`, `appointment_activities`, `barber_blocks` |
| `payment_db` | `payment-service` | `transactions`, `webhook_logs`, `dashboard_kpi_daily` |
| `product_db` | `product-service` | `products`, `stock_movements`, `categories` |
| `notification_db` | `notification-service` | `notifications`, `device_tokens` |

---

## 2. Schema Detalhado por Banco

---

### 2.1 `user_db` — user-service

#### Tabela: `barbers`

Barbeiro/dono de barbearia. Implementa `UserDetails` do Spring Security.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL, unique | UUID gerado automaticamente |
| `name` | VARCHAR(70) | NOT NULL | Nome completo |
| `tell` | VARCHAR(128) | UNIQUE, nullable | Telefone (criptografado AES) |
| `email` | VARCHAR(256) | NOT NULL, UNIQUE | E-mail (criptografado AES) |
| `email_hash` | VARCHAR(64) | UNIQUE | SHA-256 do email — usado para busca sem descriptografar |
| `document_cpf` | VARCHAR(128) | UNIQUE, nullable | CPF (criptografado AES) |
| `password` | VARCHAR(255) | nullable | Senha bcrypt (null para login social) |
| `firebase_uid` | VARCHAR(128) | UNIQUE, nullable | UID do Firebase Authentication |
| `auth_provider` | VARCHAR(30) | default='EMAIL' | EMAIL, GOOGLE, FACEBOOK, APPLE, GITHUB, TWITTER |
| `is_owner` | BOOLEAN | default=false | Indica se é dono de barbearia |
| `act_as_barber` | BOOLEAN | default=true | Owner também atende como barbeiro |
| `role` | VARCHAR(20) | default='ROLE_BARBER' | Papel no sistema |
| `barbershop_id` | VARCHAR(36) | nullable | FK lógica → `barbershop_db.barbershops.id` |
| `work_start_time` | TIME | nullable | Horário de início do expediente (legado, substituído por `barber_work_blocks`) |
| `work_end_time` | TIME | nullable | Horário de fim do expediente (legado) |
| `birth_date` | VARCHAR(128) | nullable | Data de nascimento (criptografada AES) |
| `image_url` | VARCHAR(255) | nullable | URL Cloudinary foto de perfil |
| `image_url_public_id` | VARCHAR(255) | nullable | Public ID Cloudinary (para delete) |
| `mp_access_token` | TEXT | nullable | OAuth token Mercado Pago (para split) |
| `mp_refresh_token` | TEXT | nullable | Refresh token MP (criptografado AES) |
| `mp_user_id` | VARCHAR(256) | nullable | collector_id no Mercado Pago (criptografado) |
| `mp_public_key` | VARCHAR(100) | nullable | Public key MP para tokenização de cartão |
| `date_created` | DATETIME | NOT NULL, updatable=false | Auditoria |
| `last_updated` | DATETIME | nullable | Auditoria |

**Índices implícitos:** `email_hash` (UNIQUE), `firebase_uid` (UNIQUE), `tell` (UNIQUE), `document_cpf` (UNIQUE)

**Regras de negócio:**
- Um barbeiro pode ser `is_owner=true` (dono) e ainda `act_as_barber=true` (atende clientes)
- O campo `barbershop_id` é preenchido quando o barbeiro entra em uma barbearia (aprovação da `BarbershopJoinRequest`)
- Campos sensíveis (email, CPF, telefone, tokens MP) são **criptografados em repouso** via `SensitiveStringConverter` (AES/GCM)
- `email_hash` é calculado via `@PrePersist/@PreUpdate` — SHA-256 do email antes da criptografia — para permitir busca

---

#### Tabela: `barber_assigned_activities` (tabela de coleção — `@ElementCollection`)

Atividades/serviços que um barbeiro sabe executar. Referência lógica para `barbershop_db.activities`.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `barber_id` | VARCHAR(36) | NOT NULL, FK → `barbers.id` | Barbeiro |
| `activity_id` | VARCHAR(36) | NOT NULL | FK lógica → `barbershop_db.activities.id` |

**Regra:** Ao atribuir atividades ao barbeiro, o `barbershop-service` publica o UUID da atividade; o `user-service` armazena nesta tabela.

---

#### Tabela: `customers`

Cliente final que agenda serviços.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | UUID |
| `name` | VARCHAR(70) | NOT NULL | Nome |
| `tell` | VARCHAR(128) | UNIQUE, nullable | Telefone (criptografado) |
| `email` | VARCHAR(256) | NOT NULL, UNIQUE | E-mail (criptografado) |
| `email_hash` | VARCHAR(64) | UNIQUE | Hash SHA-256 para busca |
| `document_cpf` | VARCHAR(128) | UNIQUE, nullable | CPF (criptografado) |
| `password` | VARCHAR(255) | nullable | Senha bcrypt |
| `firebase_uid` | VARCHAR(128) | UNIQUE, nullable | UID Firebase |
| `auth_provider` | VARCHAR(30) | default='EMAIL' | Provedor de auth |
| `role` | VARCHAR(20) | default='ROLE_CUSTOMER' | Papel |
| `image_url` | VARCHAR(255) | nullable | Foto de perfil Cloudinary |
| `image_url_public_id` | VARCHAR(255) | nullable | Public ID Cloudinary |
| `birth_date` | VARCHAR(128) | nullable | Data nascimento (criptografada) |
| `date_created` | DATETIME | NOT NULL | Criação |
| `last_updated` | DATETIME | NOT NULL | Atualização |

---

#### Tabela: `customer_favorite_barbershops` (tabela de coleção — `@ElementCollection`)

Barbearias favoritas do cliente.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `customer_id` | VARCHAR(36) | NOT NULL, FK → `customers.id` | Cliente |
| `barbershop_id` | VARCHAR(36) | NOT NULL | FK lógica → `barbershop_db.barbershops.id` |

**Constraint:** `uk_customer_favorite_shop` — (customer_id, barbershop_id) UNIQUE

---

#### Tabela: `barber_work_blocks`

Blocos de horário de trabalho do barbeiro (substituiu `work_start_time`/`work_end_time`).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barber_id` | VARCHAR(36) | NOT NULL | FK lógica → `barbers.id` |
| `day_of_week` | VARCHAR(10) | NOT NULL | MONDAY, TUESDAY, ..., SUNDAY |
| `start_time` | TIME | NOT NULL | Início do bloco |
| `end_time` | TIME | NOT NULL | Fim do bloco |

**Índice:** `idx_bwb_barber_day (barber_id, day_of_week)`

**Regra:** Um barbeiro pode ter múltiplos blocos por dia (ex: 09:00–12:00 e 13:00–18:00 = 2 blocos na segunda).

---

### 2.2 `barbershop_db` — barbershop-service

#### Tabela: `barbershops`

Entidade central do negócio.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `owner_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.barbers.id` (dono) |
| `name` | VARCHAR(255) | NOT NULL | Nome fantasia |
| `cnpj` | VARCHAR(128) | NOT NULL, UNIQUE | CNPJ (criptografado AES) |
| `address` | VARCHAR(255) | nullable | Endereço |
| `logo_url` | VARCHAR(255) | nullable | URL logo Cloudinary |
| `logo_url_public_id` | VARCHAR(255) | nullable | Public ID logo |
| `banner_url` | VARCHAR(255) | nullable | URL banner Cloudinary |
| `banner_url_public_id` | VARCHAR(255) | nullable | Public ID banner |
| `latitude` | DOUBLE | nullable | Geolocalização |
| `longitude` | DOUBLE | nullable | Geolocalização |
| `average_rating` | DOUBLE | calculado (Formula) | Média de `barbershop_reviews.rating` |
| `reviews_count` | BIGINT | calculado (Formula) | Contagem de avaliações |
| `date_created` | DATETIME | NOT NULL | Criação |
| `last_updated` | DATETIME | NOT NULL | Atualização |

**`average_rating` e `reviews_count`** são calculados por `@Formula` SQL — não são colunas físicas, são `SELECT` inline do Hibernate:
```sql
select avg(br.rating) from barbershop_reviews br where br.barbershop_id = id
select count(*) from barbershop_reviews br where br.barbershop_id = id
```

---

#### Tabela: `activities`

Serviços oferecidos pela barbearia (corte, barba, hidratação, etc).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barbershop_id` | VARCHAR(36) | NOT NULL, FK → `barbershops.id` | Barbearia dona |
| `activity_name` | VARCHAR(255) | NOT NULL | Nome do serviço |
| `price` | DECIMAL(10,2) | NOT NULL | Preço em BRL |
| `duration_minutes` | INT | NOT NULL | Duração em minutos |
| `image_url` | VARCHAR(255) | nullable | Imagem Cloudinary |
| `image_url_public_id` | VARCHAR(255) | nullable | Public ID |
| `date_created` | DATETIME | NOT NULL | Criação |
| `last_updated` | DATETIME | NOT NULL | Atualização |

**Regra:** `CascadeType.ALL` + `orphanRemoval=true` — ao deletar a barbearia, todos os serviços são deletados.

---

#### Tabela: `barbershop_join_requests`

Solicitações de vinculação entre barbeiro e barbearia.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barber_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.barbers.id` |
| `barbershop_id` | VARCHAR(36) | NOT NULL, FK → `barbershops.id` | Barbearia |
| `status` | VARCHAR(50) | NOT NULL | PENDING, APPROVED, REJECTED |
| `request_type` | VARCHAR(20) | NOT NULL | JOIN (barbeiro pede) ou INVITE (owner convida) |
| `date_created` | DATETIME | NOT NULL | Criação |

**Constraint UNIQUE:** `(barber_id, barbershop_id)` — um barbeiro não pode ter duas solicitações abertas para a mesma barbearia

**Regra de negócio:**
- `JOIN`: barbeiro busca barbearia no marketplace e solicita entrada
- `INVITE`: owner convida barbeiro pelo e-mail/ID
- Aprovação (`APPROVED`) → `user-service` atualiza `barbers.barbershop_id` via Feign call
- Evento `barbershop.join-request.created` publicado no RabbitMQ ao criar → `notification-service` notifica o owner

---

#### Tabela: `barbershop_highlights`

Imagens de destaque/portfólio da barbearia.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `image_url` | VARCHAR(255) | NOT NULL | URL Cloudinary |
| `image_url_public_id` | VARCHAR(255) | nullable | Public ID para deleção |
| `barbershop_id` | VARCHAR(36) | NOT NULL, FK → `barbershops.id` | Barbearia |

---

#### Tabela: `barbershop_reviews`

Avaliações dos clientes sobre a barbearia.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `customer_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.customers.id` |
| `barbershop_id` | VARCHAR(36) | NOT NULL, FK → `barbershops.id` | Barbearia avaliada |
| `rating` | INT | NOT NULL | Nota (implícito 1–5) |
| `comment` | VARCHAR(500) | nullable | Comentário |
| `created_at` | DATETIME | NOT NULL | Preenchido por `@PrePersist` |

**Constraint UNIQUE:** `uk_review_customer_shop (customer_id, barbershop_id)` — cliente avalia cada barbearia apenas 1 vez

---

#### Tabela: `barber_commission_rules`

Regras de comissão por barbeiro × serviço × barbearia.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barbershop_id` | VARCHAR(36) | NOT NULL | FK lógica → `barbershops.id` |
| `barber_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.barbers.id` |
| `activity_id` | VARCHAR(36) | NOT NULL, FK → `activities.id` | Serviço |
| `percentage` | DECIMAL(5,2) | NOT NULL | % de comissão (ex: 40.00 = 40%) |
| `created_at` | DATETIME | NOT NULL | Criação |

**Constraint UNIQUE:** `uq_barber_activity (barbershop_id, barber_id, activity_id)` — uma regra por combinação

---

#### Tabela: `fixed_expenses`

Despesas fixas mensais da barbearia.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barbershop_id` | VARCHAR(36) | NOT NULL | Barbearia dona |
| `category` | VARCHAR(30) | NOT NULL | AGUA, LUZ, ALUGUEL, INTERNET, ENERGIA, FUNCIONARIOS, MATERIAL, SISTEMA, CONTABILIDADE, MARKETING, MANUTENCAO, OUTROS |
| `custom_name` | VARCHAR(80) | nullable | Nome personalizado (quando category = OUTROS) |
| `amount` | DECIMAL(10,2) | NOT NULL | Valor em BRL |
| `month` | INT | NOT NULL | Mês (1–12) |
| `year` | INT | NOT NULL | Ano |
| `recurring_monthly` | BOOLEAN | NOT NULL, default=false | Se repete mensalmente |
| `created_at` | DATETIME | NOT NULL | Criação |

**Índices:**  
- `idx_fe_barbershop_month_year (barbershop_id, month, year)`  
- `idx_fe_barbershop_recurring (barbershop_id, recurring_monthly, year, month)`

---

### 2.3 `schedule_db` — schedule-service

#### Tabela: `appointments`

Agendamento de serviços pelo cliente.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `customer_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.customers.id` |
| `barber_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.barbers.id` |
| `barbershop_id` | VARCHAR(36) | NOT NULL | FK lógica → `barbershop_db.barbershops.id` |
| `customer_name` | VARCHAR(256) | NOT NULL | **Snapshot** — nome no momento do agendamento (criptografado) |
| `barber_name` | VARCHAR(70) | NOT NULL | **Snapshot** |
| `barbershop_name` | VARCHAR(255) | NOT NULL | **Snapshot** |
| `start_time` | DATETIME | NOT NULL | Início do atendimento |
| `end_time` | DATETIME | NOT NULL | Fim do atendimento |
| `total_price` | DECIMAL(10,2) | NOT NULL | Soma dos preços das atividades |
| `status` | VARCHAR(50) | NOT NULL | Ver enum abaixo |
| `date_created` | DATETIME | NOT NULL | Criação |
| `last_updated` | DATETIME | NOT NULL | Atualização |

**Enum `AppointmentStatus`:**

| Valor | Descrição |
|---|---|
| `SCHEDULED` | Agendado (pagamento local ou pendente) |
| `PAYMENT_PENDING` | Aguardando pagamento online (Mercado Pago) |
| `EXPIRED` | Projeção lazy: `PAYMENT_PENDING` + `startTime + 1h < now` |
| `CONFIRMED` | Barbeiro confirmou a presença |
| `IN_PROGRESS` | Atendimento em andamento |
| `COMPLETED` | Atendimento concluído |
| `WALK_IN` | Atendimento sem agendamento prévio (walk-in) |
| `CONCLUDED` | ⚠️ @Deprecated — use COMPLETED |
| `CANCELLED` | Cancelado (cliente ou barbeiro) |
| `NO_SHOW` | Cliente não compareceu |

**Regras de negócio críticas:**
- `end_time = start_time + soma(activity.durationMinutes)` — calculado no `AppointmentService`
- Conflito de horário: bloqueia novo agendamento se `(startTime < existente.endTime AND endTime > existente.startTime)` para o mesmo `barberId`, excluindo status `CANCELLED` e `NO_SHOW`
- `customer_name` é **desnormalizado** (snapshot) para preservar histórico mesmo se o cliente alterar o nome
- Ao criar, se `paymentMethod` for online (MP), status inicial = `PAYMENT_PENDING`; caso contrário = `SCHEDULED`

---

#### Tabela: `appointment_activities`

Serviços incluídos em um agendamento (snapshot no momento do agendamento).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `appointment_id` | VARCHAR(36) | NOT NULL, FK → `appointments.id` | Agendamento pai |
| `activity_id` | VARCHAR(36) | NOT NULL | FK lógica → `barbershop_db.activities.id` |
| `activity_name` | VARCHAR(255) | NOT NULL | **Snapshot** do nome |
| `price` | DECIMAL(10,2) | NOT NULL | **Snapshot** do preço |
| `duration_minutes` | INT | NOT NULL | **Snapshot** da duração |

**Regra:** Snapshots garantem que alterações futuras no serviço não afetam agendamentos passados. `CascadeType.ALL + orphanRemoval`.

---

#### Tabela: `barber_blocks`

Bloqueios de agenda criados manualmente pelo barbeiro (folga, compromisso externo, etc).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barber_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.barbers.id` |
| `start_time` | DATETIME | NOT NULL | Início do bloqueio |
| `end_time` | DATETIME | NOT NULL | Fim do bloqueio |
| `reason` | VARCHAR(255) | nullable | Motivo |
| `date_created` | DATETIME | NOT NULL | Criação |

**Regra:** Verificado junto com `appointments` ao calcular disponibilidade — um bloqueio impede agendamentos naquele intervalo.

---

### 2.4 `payment_db` — payment-service

#### Tabela: `transactions`

Transação de pagamento vinculada a um agendamento.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `appointment_id` | VARCHAR(36) | NOT NULL | FK lógica → `schedule_db.appointments.id` |
| `customer_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.customers.id` |
| `barbershop_id` | VARCHAR(36) | nullable | FK lógica → `barbershop_db.barbershops.id` |
| `amount` | DECIMAL | NOT NULL | Valor total cobrado |
| `gross_amount` | DECIMAL(10,2) | nullable | Valor bruto pago (preenchido pelo webhook MP) |
| `net_amount` | DECIMAL(10,2) | nullable | Valor líquido repassado ao barbeiro |
| `mp_fee_amount` | DECIMAL(10,2) | nullable | Taxa Mercado Pago |
| `platform_fee_amount` | DECIMAL(10,2) | nullable | Taxa CortaAi (application fee) |
| `payment_method` | VARCHAR(30) | nullable | PIX, CREDIT_CARD, DEBIT_CARD, LOCAL |
| `status` | VARCHAR(? ) | NOT NULL, default=PENDING | PENDING, APPROVED, REJECTED, REFUNDED, CANCELLED |
| `mp_preference_id` | VARCHAR | UNIQUE, nullable | ID de preferência no Mercado Pago |
| `mp_payment_id` | VARCHAR | nullable | ID do pagamento no webhook |
| `checkout_url` | TEXT | nullable | URL de checkout gerado pelo MP |
| `created_at` | DATETIME | NOT NULL | Criação |
| `updated_at` | DATETIME | nullable | Atualização |

**Fluxo de pagamento:**
1. `schedule-service` cria agendamento → publica `appointment.created`
2. `payment-service` cria `Transaction(status=PENDING)` + gera preferência no MP → retorna `checkoutUrl`
3. MP notifica via webhook → `payment-service` atualiza status + preenche campos financeiros → publica `payment.approved`
4. `notification-service` consome `payment.approved` → notifica cliente

---

#### Tabela: `webhook_logs`

Log de idempotência para webhooks do Mercado Pago.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `mp_resource_id` | VARCHAR | NOT NULL, UNIQUE | ID do recurso MP (ex: payment_id) — garante idempotência |
| `event_type` | VARCHAR | NOT NULL | Tipo do evento (ex: `payment`) |
| `raw_payload` | TEXT | nullable | JSON bruto recebido |
| `processed` | BOOLEAN | NOT NULL, default=false | Se já foi processado |
| `received_at` | DATETIME | NOT NULL | Data de recebimento |

**Regra:** Antes de processar qualquer webhook, verifica `webhook_logs.mp_resource_id` — se já existir, descarta (idempotência).

---

#### Tabela: `dashboard_kpi_daily`

KPIs financeiros diários pré-agregados por barbearia (evita `GROUP BY` pesados em consultas de dashboard).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barbershop_id` | VARCHAR(36) | NOT NULL | Barbearia |
| `reference_date` | DATE | NOT NULL | Data de referência |
| `approved_revenue` | DECIMAL(12,2) | NOT NULL, default=0 | Receita aprovada no dia |
| `approved_transactions_count` | INT | NOT NULL, default=0 | Quantidade de transações aprovadas |
| `updated_at` | DATETIME | NOT NULL | Atualizado via `@PrePersist/@PreUpdate` |

**Constraint UNIQUE:** `uk_dashboard_kpi_daily_shop_date (barbershop_id, reference_date)`  
**Índices:**
- `idx_dashboard_kpi_daily_shop_date (barbershop_id, reference_date)`
- `idx_dashboard_kpi_daily_reference_date (reference_date)`

---

### 2.5 `product_db` — product-service

#### Tabela: `products`

Estoque interno operacional da barbearia (produtos usados no atendimento ou vendidos).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `barbershop_id` | VARCHAR(36) | NOT NULL | FK lógica → `barbershop_db.barbershops.id` |
| `name` | VARCHAR | NOT NULL | Nome do produto |
| `description` | TEXT | nullable | Descrição |
| `price` | DECIMAL | NOT NULL | Preço de venda |
| `category` | VARCHAR | nullable, default=OTHER | Enum estático: SHAMPOO, CONDITIONER, POMADE, RAZOR, SCISSORS, CLIPPERS, TOWEL, OTHER |
| `category_id` | VARCHAR(36) | nullable, FK → `categories.id` | Categoria dinâmica (opcional) |
| `stock_quantity` | INT | NOT NULL, default=0 | Quantidade em estoque |
| `min_stock_quantity` | INT | NOT NULL, default=0 | Quantidade mínima (alerta de reposição) |
| `image_url` | VARCHAR | nullable | Imagem do produto |
| `active` | BOOLEAN | NOT NULL, default=true | Soft delete / desativação |
| `created_at` | DATETIME | NOT NULL | Criação |
| `updated_at` | DATETIME | nullable | Atualização |

**Regra de alerta:** quando `stock_quantity <= min_stock_quantity`, o sistema deve alertar para reposição.

---

#### Tabela: `stock_movements`

Auditoria completa de todas as movimentações de estoque.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `product_id` | VARCHAR(36) | NOT NULL | FK lógica → `products.id` |
| `type` | VARCHAR | NOT NULL | IN, OUT, OUT_CONSUMPTION, OUT_SALE, LOSS, RETURN |
| `quantity` | INT | NOT NULL | Quantidade movimentada |
| `unit_sale_price` | DECIMAL(10,2) | nullable | Preço unitário de venda (para OUT_SALE) |
| `notes` | VARCHAR(500) | nullable | Observação |
| `reason` | VARCHAR | nullable | Motivo (texto livre) |
| `created_at` | DATETIME | NOT NULL | Criação |

**Tipos de movimento:**

| Tipo | Direção | Descrição |
|---|---|---|
| `IN` | Entrada | Compra/reposição de estoque |
| `OUT` | Saída genérica | Saída sem classificação específica |
| `OUT_CONSUMPTION` | Saída | Usado no atendimento (não vendido) |
| `OUT_SALE` | Saída | Vendido ao cliente |
| `LOSS` | Perda | Quebra, vencimento, extravio |
| `RETURN` | Retorno | Devolução ao fornecedor |

---

#### Tabela: `categories`

Categorias dinâmicas de produtos criadas pela barbearia.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `name` | VARCHAR(80) | NOT NULL | Nome da categoria |
| `barbershop_id` | VARCHAR(36) | NOT NULL | Barbearia dona |
| `created_at` | DATETIME | NOT NULL | Criação |

**Constraint UNIQUE:** `uq_category (name, barbershop_id)` — nome único por barbearia

---

### 2.6 `notification_db` — notification-service

#### Tabela: `notifications`

Notificações in-app e transacionais enviadas aos usuários.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `user_id` | VARCHAR(36) | NOT NULL | FK lógica → `user_db.barbers.id` ou `customers.id` |
| `type` | VARCHAR | NOT NULL | Enum NotificationType |
| `title` | VARCHAR | NOT NULL | Título |
| `message` | TEXT | NOT NULL | Corpo da mensagem |
| `channel` | VARCHAR | NOT NULL, default=IN_APP | IN_APP, EMAIL, PUSH |
| `is_read` | BOOLEAN | NOT NULL, default=false | Lida pelo usuário |
| `created_at` | DATETIME | NOT NULL | Criação |

---

#### Tabela: `device_tokens`

Tokens de dispositivo para push notifications (FCM/APNs).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| `id` | VARCHAR(36) | PK | UUID |
| `user_id` | VARCHAR(36) | NOT NULL | FK lógica → user |
| `platform` | VARCHAR | NOT NULL | ANDROID, IOS, WEB |
| `token` | VARCHAR(512) | NOT NULL | Token FCM/APNs |
| `active` | BOOLEAN | NOT NULL, default=true | Se o token ainda é válido |
| `created_at` | DATETIME | NOT NULL | Criação |
| `updated_at` | DATETIME | NOT NULL | Atualização |

**Constraint UNIQUE:** `uk_device_token_value (token)` — um token não pode ser registrado duas vezes

---

## 3. Relacionamentos Cross-Service (FKs Lógicas)

Não existem FKs físicas entre bancos. As referências são mantidas por UUID armazenado como VARCHAR(36).

```
user_db.barbers.id
    ↑ referenciado por:
    barbershop_db.barbershops.owner_id
    barbershop_db.barbershop_join_requests.barber_id
    barbershop_db.barber_commission_rules.barber_id
    schedule_db.appointments.barber_id
    schedule_db.barber_blocks.barber_id
    user_db.barber_assigned_activities.barber_id

user_db.customers.id
    ↑ referenciado por:
    schedule_db.appointments.customer_id
    payment_db.transactions.customer_id
    barbershop_db.barbershop_reviews.customer_id
    user_db.customer_favorite_barbershops.customer_id

barbershop_db.barbershops.id
    ↑ referenciado por:
    user_db.barbers.barbershop_id
    schedule_db.appointments.barbershop_id
    payment_db.transactions.barbershop_id
    payment_db.dashboard_kpi_daily.barbershop_id
    product_db.products.barbershop_id
    product_db.categories.barbershop_id
    barbershop_db.barber_commission_rules.barbershop_id
    barbershop_db.fixed_expenses.barbershop_id

barbershop_db.activities.id
    ↑ referenciado por:
    schedule_db.appointment_activities.activity_id
    user_db.barber_assigned_activities.activity_id
    barbershop_db.barber_commission_rules.activity_id

schedule_db.appointments.id
    ↑ referenciado por:
    payment_db.transactions.appointment_id
    schedule_db.appointment_activities.appointment_id
```

---

## 4. Fluxo de Eventos RabbitMQ

Exchange único: **`cortaai.events`** (TopicExchange, durable)

| Routing Key | Publicador | Consumidores | Payload |
|---|---|---|---|
| `appointment.created` | `schedule-service` | `notification-service` | `AppointmentCreatedEvent` |
| `appointment.cancelled` | `schedule-service` | `notification-service` | `AppointmentCancelledEvent` |
| `appointment.concluded` | `schedule-service` | `notification-service` | `AppointmentConcludedEvent` |
| `appointment.rescheduled` | `schedule-service` | `notification-service` | `AppointmentRescheduledEvent` |
| `appointment.reminder` | `schedule-service` (scheduler) | `notification-service` | `AppointmentReminderEvent` |
| `payment.approved` | `payment-service` | `notification-service` | `PaymentApprovedEvent` |
| `barbershop.join-request.created` | `barbershop-service` | `notification-service` | `JoinRequestCreatedEvent` |
| `customer.deleted` | `user-service` | `schedule-service`, `payment-service`, `notification-service` | `CustomerDeletedEvent` |
| `barber.removed` | `barbershop-service` | — (registrado, sem consumer mapeado ainda) | — |

**Filas:**

| Fila | Consumidor |
|---|---|
| `notification.appointment.created` | notification-service |
| `notification.appointment.cancelled` | notification-service |
| `notification.appointment.concluded` | notification-service |
| `notification.appointment.rescheduled` | notification-service |
| `notification.appointment.reminder` | notification-service |
| `notification.payment.approved` | notification-service |
| `notification.barbershop.join-request.created` | notification-service |
| `notification.customer.deleted` | notification-service |
| `schedule.customer.deleted` | schedule-service |
| `payment.customer.deleted` | payment-service |

---

## 5. Regras de Negócio Críticas

### 5.1 Autenticação e Identidade
- Firebase Authentication é a fonte de verdade de identidade
- O `api-gateway` valida o ID Token Firebase e injeta `X-User-Id`, `X-User-Email`, `X-User-Role` em todos os requests downstream
- Nenhum microsserviço revalida o token — confiam nos headers
- Campos PII (CPF, email, telefone, data de nascimento, tokens MP) são criptografados em repouso com AES/GCM via `SensitiveStringConverter`
- O `email_hash` (SHA-256) permite busca por email sem descriptografar — usado em `findByEmail` nos repositórios

### 5.2 Multi-Tenancy
- Toda entidade de negócio carrega `barbershop_id` como discriminador de tenant
- Um barbeiro pertence a exatamente uma barbearia (quando vinculado): `barbers.barbershop_id`
- Produtos, despesas fixas, atividades, regras de comissão, categorias — todos isolados por `barbershop_id`

### 5.3 Agendamento
- Cálculo de `end_time`: soma das `duration_minutes` de todas as atividades selecionadas
- Verificação de conflito: para o mesmo barbeiro, não pode haver dois agendamentos com intervalo de tempo sobreposto (exceto status CANCELLED/NO_SHOW)
- Walk-in: agendamento criado com `startTime = now`, sem verificação antecipada, status = `WALK_IN`
- Reminder automático: `ReminderScheduler` (Spring `@Scheduled`) publica `appointment.reminder` para agendamentos próximos

### 5.4 Pagamentos e Split
- Pagamento online: Mercado Pago com split (marketplace)
- Split: `netAmount` (barbeiro) + `mpFeeAmount` (MP) + `platformFeeAmount` (CortaAi) = `grossAmount`
- `dashboard_kpi_daily` é atualizado via `@PrePersist/@PreUpdate` a cada webhook processado — estratégia de upsert (UPDATE ou INSERT)
- Idempotência de webhook: `webhook_logs.mp_resource_id` UNIQUE garante que o mesmo evento do MP não seja processado duas vezes

### 5.5 Estoque
- `stock_quantity` é atualizado diretamente na tabela `products` a cada movimentação
- O histórico completo fica em `stock_movements` (log imutável)
- Alerta de reposição: `stock_quantity <= min_stock_quantity`
- Produto pode ser desativado (soft delete): `active = false`

### 5.6 Regras de Comissão
- `barber_commission_rules` define a `percentage` que cada barbeiro recebe por serviço específico
- Constraint `uq_barber_activity (barbershop_id, barber_id, activity_id)` — uma regra por tripla
- Usada no split do Mercado Pago para calcular `netAmount` do barbeiro

### 5.7 Deleção de Cliente (LGPD)
- Ao deletar um cliente, `user-service` publica `customer.deleted`
- `schedule-service` consome → anonimiza/deleta agendamentos futuros
- `payment-service` consome → anonimiza transações
- `notification-service` consome → remove notificações
- Estratégia de anonimização, não deleção física (manter histórico financeiro)

---

## 6. Criptografia e LGPD

| Campo | Tabela | Tipo de proteção |
|---|---|---|
| `email` | `barbers`, `customers` | AES/GCM via `SensitiveStringConverter` |
| `email_hash` | `barbers`, `customers` | SHA-256 (hash irreversível) — busca |
| `tell` | `barbers`, `customers` | AES/GCM |
| `document_cpf` | `barbers`, `customers` | AES/GCM |
| `birth_date` | `barbers`, `customers` | AES/GCM via `SensitiveLocalDateConverter` |
| `mp_refresh_token` | `barbers` | AES/GCM |
| `mp_user_id` | `barbers` | AES/GCM |
| `customer_name` | `appointments` | AES/GCM |
| `cnpj` | `barbershops` | AES/GCM |

Chave de criptografia: variável de ambiente `CORTAAI_DATA_CRYPTO_KEY`

---

## 7. Índices Relevantes

| Tabela | Índice | Colunas | Propósito |
|---|---|---|---|
| `barbers` | (implícito UNIQUE) | `email_hash` | Busca por email |
| `barbers` | (implícito UNIQUE) | `firebase_uid` | Login OAuth |
| `barber_work_blocks` | `idx_bwb_barber_day` | `barber_id, day_of_week` | Consulta de agenda |
| `fixed_expenses` | `idx_fe_barbershop_month_year` | `barbershop_id, month, year` | Relatório mensal |
| `fixed_expenses` | `idx_fe_barbershop_recurring` | `barbershop_id, recurring_monthly, year, month` | Renovação automática |
| `dashboard_kpi_daily` | `idx_dashboard_kpi_daily_shop_date` | `barbershop_id, reference_date` | Dashboard |
| `dashboard_kpi_daily` | `idx_dashboard_kpi_daily_reference_date` | `reference_date` | KPIs globais |
| `webhook_logs` | (implícito UNIQUE) | `mp_resource_id` | Idempotência MP |

---

## 8. Frontend — Serviços HTTP Mapeados

Localização: `frontend/src/services/`

| Arquivo | Endpoints consumidos | Domínio |
|---|---|---|
| `authService.js` | `/auth/**`, Firebase SDK | Autenticação |
| `userProfileService.js` | `/users/barbers/**`, `/users/customers/**` | Perfil de usuário |
| `barbershopService.js` | `/barbershops/**` | Barbearias |
| `appointmentService.js` | `/appointments/**` | Agendamentos |
| `appointmentAvailabilityService.js` | `/appointments/availability/**` | Disponibilidade |
| `barberBlockService.js` | `/barber-blocks/**` | Bloqueios de agenda |
| `offlineTransactionalService.js` | `/payments/**` (offline/local) | Pagamentos locais |
| `pushNotificationService.js` | `/notifications/device-tokens` | Push tokens |
| `analyticsService.js` | `/payments/analytics/**` | Dashboard KPIs |
| `gustaveService.js` | IA / chat interno | Assistente |
| `navigationService.js` | Utilitário de navegação | — |

**Regra:** Todos os requests passam por `api.js` que injeta automaticamente o `Authorization: Bearer <firebase_token>` no header.

**localStorage keys usadas pelo frontend:**

| Chave | Descrição |
|---|---|
| `token` | Firebase ID Token |
| `userId` | UUID do usuário (barber ou customer) |
| `userEmail` | Email |
| `userName` | Nome |
| `userRole` | `ROLE_CUSTOMER` ou `ROLE_BARBER` |
| `isOwner` | `true` / `false` |
| `authProvider` | EMAIL, GOOGLE, etc |
| `barbershopId` | UUID da barbearia vinculada (apenas barbers) |

---

## 9. Queries de Inspeção ao Vivo

Execute no servidor após `ssh Edu@10.147.19.1` e acesso ao MySQL:

```sql
-- Conectar ao MySQL
-- mysql -u root -p

-- ==== user_db ====
USE user_db;
SHOW TABLES;
SELECT COUNT(*) as total_barbers, SUM(is_owner) as owners FROM barbers;
SELECT COUNT(*) as total_customers FROM customers;
SELECT COUNT(*) as total_work_blocks FROM barber_work_blocks;

-- ==== barbershop_db ====
USE barbershop_db;
SHOW TABLES;
SELECT COUNT(*) as total_barbershops FROM barbershops;
SELECT COUNT(*) as total_activities FROM activities;
SELECT COUNT(*) as pending_join_requests FROM barbershop_join_requests WHERE status = 'PENDING';
SELECT category, COUNT(*) as cnt, SUM(amount) as total FROM fixed_expenses GROUP BY category;

-- ==== schedule_db ====
USE schedule_db;
SHOW TABLES;
SELECT status, COUNT(*) as cnt FROM appointments GROUP BY status;
SELECT COUNT(*) as total_blocks FROM barber_blocks;

-- ==== payment_db ====
USE payment_db;
SHOW TABLES;
SELECT status, COUNT(*) as cnt, SUM(amount) as volume FROM transactions GROUP BY status;
SELECT COUNT(*) as unprocessed_webhooks FROM webhook_logs WHERE processed = false;
SELECT barbershop_id, SUM(approved_revenue) as revenue_total FROM dashboard_kpi_daily GROUP BY barbershop_id ORDER BY revenue_total DESC LIMIT 10;

-- ==== product_db ====
USE product_db;
SHOW TABLES;
SELECT COUNT(*) as total_products, SUM(stockQuantity) as total_stock FROM products WHERE active = true;
SELECT type, COUNT(*) as cnt FROM stock_movements GROUP BY type;

-- ==== notification_db ====
USE notification_db;
SHOW TABLES;
SELECT channel, COUNT(*) as cnt, SUM(is_read) as read_count FROM notifications GROUP BY channel;
SELECT platform, COUNT(*) as cnt FROM device_tokens WHERE active = true GROUP BY platform;

-- ==== Verificação de integridade cross-service (FKs lógicas) ====
-- Barbeiros com barbershop_id que não existe no barbershop_db:
-- (rodar conectado ao user_db)
-- SELECT b.id, b.name, b.barbershop_id FROM user_db.barbers b
-- WHERE b.barbershop_id IS NOT NULL
-- AND NOT EXISTS (SELECT 1 FROM barbershop_db.barbershops bs WHERE bs.id = b.barbershop_id);
```

---

## 10. Dependências de Infraestrutura

| Serviço | MySQL DB | RabbitMQ | Redis | Cloudinary | Firebase | Mercado Pago |
|---|---|---|---|---|---|---|
| user-service | user_db | Publica | — | ✅ | ✅ | ✅ (OAuth) |
| barbershop-service | barbershop_db | Publica | — | ✅ | — | — |
| schedule-service | schedule_db | Publica + Consome | — | — | — | — |
| payment-service | payment_db | Publica + Consome | — | — | — | ✅ (API) |
| product-service | product_db | — | — | — | — | — |
| notification-service | notification_db | Consome | ✅ (dedup) | — | ✅ (FCM) | — |
| api-gateway | — | — | — | — | ✅ (validação token) | — |

---

## 11. Views SQL (Não Mapeadas como Entidades JPA)

> ⚠️ Existem Views no banco que **não aparecem no código Java**. Foram criadas provavelmente via `views.sql`. Duas delas fazem **JOIN cross-database** (violação do princípio de isolamento de microsserviços).

### `user_db.v_customer_acquisition`
Novos clientes por mês.
```sql
SELECT DATE_FORMAT(date_created, '%Y-%m') AS reference_month,
       COUNT(id) AS new_customers
FROM user_db.customers
GROUP BY DATE_FORMAT(date_created, '%Y-%m')
```

### `user_db.v_customer_retention`
⚠️ **JOIN cross-database** (user_db → schedule_db)
```sql
SELECT DATE_FORMAT(a.start_time, '%Y-%m') AS reference_month,
       COUNT(DISTINCT a.customer_id) AS returning_customers
FROM schedule_db.appointments a
WHERE a.status IN ('COMPLETED', 'CONCLUDED')
GROUP BY DATE_FORMAT(a.start_time, '%Y-%m')
```

### `schedule_db.v_agenda_thermometer`
Termômetro da agenda por dia e barbearia.
```sql
SELECT CAST(start_time AS DATE) AS agenda_date,
       barbershop_id,
       COUNT(id) AS total_appointments,
       SUM(CASE WHEN status IN ('CONFIRMED','IN_PROGRESS','SCHEDULED') THEN 1 ELSE 0 END) AS active_appointments,
       SUM(CASE WHEN status IN ('CANCELLED','NO_SHOW') THEN 1 ELSE 0 END) AS lost_appointments
FROM schedule_db.appointments
GROUP BY CAST(start_time AS DATE), barbershop_id
```

### `schedule_db.v_barber_skill_matrix`
Matrix de habilidades e receita gerada por barbeiro × serviço.
```sql
SELECT a.barber_id, a.barber_name, aa.activity_name,
       COUNT(aa.id) AS times_executed,
       SUM(aa.price) AS total_generated_by_activity
FROM appointments a
JOIN appointment_activities aa ON a.id = aa.appointment_id
WHERE a.status IN ('COMPLETED', 'CONCLUDED')
GROUP BY a.barber_id, a.barber_name, aa.activity_name
```

### `payment_db.v_avg_ticket`
Ticket médio por barbearia por mês.
```sql
SELECT barbershop_id,
       DATE_FORMAT(created_at, '%Y-%m') AS reference_month,
       COUNT(id) AS total_transactions,
       ROUND(SUM(amount), 2) AS total_revenue,
       ROUND(AVG(amount), 2) AS avg_ticket
FROM payment_db.transactions
WHERE status = 'APPROVED'
GROUP BY barbershop_id, DATE_FORMAT(created_at, '%Y-%m')
```

### `payment_db.v_barber_financial_performance`
⚠️ **JOIN cross-database** (payment_db → schedule_db)
```sql
SELECT a.barber_id, a.barber_name, t.barbershop_id,
       SUM(t.amount) AS generated_revenue,
       COUNT(t.id) AS total_appointments,
       ROUND((100.0 * SUM(t.amount)) / NULLIF(SUM(SUM(t.amount)) OVER (PARTITION BY t.barbershop_id), 0), 2) AS contribution_percentage
FROM payment_db.transactions t
JOIN schedule_db.appointments a ON a.id = BIN_TO_UUID(t.appointment_id)
WHERE t.status = 'APPROVED'
GROUP BY a.barber_id, a.barber_name, t.barbershop_id
```

### `product_db.v_stock_health_alert`
Produtos com estoque abaixo do mínimo.
```sql
SELECT id AS product_id, name AS product_name, category,
       stock_quantity AS current_stock,
       min_stock_quantity AS predicted_minimum,
       CASE WHEN stock_quantity <= min_stock_quantity THEN 1 ELSE 0 END AS requires_restock
FROM product_db.products
WHERE active = 1
```

---

## 12. Estado Atual do Banco de Produção (2026-05-15)

| Tabela | Registros |
|---|---|
| `user_db.barbers` | 5 (2 owners, 3 employees) |
| `user_db.customers` | 3 |
| `user_db.barber_work_blocks` | 24 |
| `user_db.barber_assigned_activities` | 6 |
| `user_db.customer_favorite_barbershops` | 2 |
| `barbershop_db.barbershops` | 2 |
| `barbershop_db.activities` | 4 |
| `barbershop_db.barbershop_join_requests` | 1 (APPROVED) |
| `barbershop_db.barbershop_reviews` | 2 |
| `barbershop_db.barber_commission_rules` | 2 |
| `barbershop_db.barbershop_highlights` | 0 |
| `barbershop_db.fixed_expenses` | 0 |
| `schedule_db.appointments` | 21 (8 SCHEDULED, 12 COMPLETED, 1 PAYMENT_PENDING) |
| `schedule_db.appointment_activities` | 22 |
| `schedule_db.barber_blocks` | 1 |
| `payment_db.transactions` | 1 (PENDING, R$ 23,90) |
| `payment_db.webhook_logs` | 0 |
| `payment_db.dashboard_kpi_daily` | 0 |
| `product_db.products` | 2 (todos com estoque saudável) |
| `product_db.categories` | 2 |
| `product_db.stock_movements` | 3 (todos IN) |
| `notification_db.notifications` | 1 (IN_APP, não lida) |
| `notification_db.device_tokens` | 0 |

---

## 13. Enums Reais no Banco (verificado ao vivo)

| Tabela.Coluna | Valores no banco |
|---|---|
| `barbershop_join_requests.request_type` | `INVITE`, `JOIN` |
| `barbershop_join_requests.status` | `APPROVED`, `PENDING`, `REJECTED` |
| `fixed_expenses.category` | `AGUA`, `ALUGUEL`, `CONTABILIDADE`, `ENERGIA`, `FUNCIONARIOS`, `INTERNET`, `LUZ`, `MANUTENCAO`, `MARKETING`, `MATERIAL`, `OUTROS`, `SISTEMA` |
| `appointments.status` | `CANCELLED`, `COMPLETED`, `CONCLUDED`, `CONFIRMED`, `EXPIRED`, `IN_PROGRESS`, `NO_SHOW`, `PAYMENT_PENDING`, `SCHEDULED`, `WALK_IN` |
| `transactions.status` | `APPROVED`, `CANCELLED`, `IN_PROCESS`, `PENDING`, `REFUNDED`, `REJECTED` ⚠️ `IN_PROCESS` não está na enum Java |
| `products.category` | `ACCESSORY`, `AFTERSHAVE`, `BEARD_OIL`, `BRUSH`, `COMB`, `CONDITIONER`, `OIL`, `OTHER`, `POMADE`, `RAZOR`, `SCISSORS`, `SHAMPOO`, `WAX` |
| `stock_movements.type` | `IN`, `LOSS`, `OUT`, `OUT_CONSUMPTION`, `OUT_SALE`, `RETURN` |
| `notifications.type` | `APPOINTMENT_CANCELLED`, `APPOINTMENT_CONCLUDED`, `APPOINTMENT_CREATED`, `APPOINTMENT_RESCHEDULED`, `INVITE_RECEIVED`, `JOIN_REQUEST_RECEIVED`, `PAYMENT_APPROVED` |
| `notifications.channel` | `EMAIL`, `IN_APP` ⚠️ `PUSH` não existe no banco |
| `device_tokens.platform` | `WEB` ⚠️ `ANDROID`, `IOS` não existem no banco |

---

*Documento baseado em análise estática do código + inspeção ao vivo do banco de produção em 2026-05-15.*

