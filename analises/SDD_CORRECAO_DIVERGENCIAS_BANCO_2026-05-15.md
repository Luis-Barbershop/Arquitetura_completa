# SDD — Correção de Divergências Código × Banco de Dados

> **Data:** 2026-05-15  
> **Branch:** `feature/migracao-microservicos`  
> **Commit de fechamento:** `ea47c27`  
> **Origem:** Análise `ANALISE_BANCO_DADOS.md` — achados críticos levantados em inspeção estática + live do MySQL de produção.  
> **Status geral: ✅ TODOS OS ACHADOS RESOLVIDOS**

| # | Severidade | Problema | Status |
|---|---|---|---|
| 1 | 🔴 CRÍTICO | `categories.barbershop_id` = `binary(16)` no DB, Java sem `@JdbcTypeCode` | ✅ **Resolvido** — `Category.java` + `ALTER TABLE` em produção + migração dos 2 registros existentes (`ea47c27`) |
| 2 | 🟡 MÉDIO | Views `v_customer_retention` e `v_barber_financial_performance` — JOIN cross-DB | ✅ **Documentado** — views marcadas `[CROSS-DB] ANALYTICS-ONLY` em `views.sql`; decisão ADR registrada neste SDD (`ea47c27`) |
| 3 | 🟡 MÉDIO | `PushPlatform` Java declarava ANDROID/IOS; DB só tem WEB | ✅ Corrigido (commit anterior) |
| 4 | 🟢 INFO | `PaymentStatus` sem `IN_PROCESS` | ✅ Corrigido (commit anterior) |
| 5 | 🟢 INFO | `NotificationChannel` tinha PUSH; DB não tem | ✅ Corrigido (commit anterior) |
| — | 🟡 EXTRA | `FixedExpenseService.resolveOwner` usava e-mail em vez de Firebase UID | ✅ Corrigido por outro agente (`aed477d`) |

---

## Problema 1 — `Category.barbershopId` sem mapeamento UUID correto

### Contexto

`ddl-auto: update` + Hibernate 6 (Spring Boot 3.x) mapeiam `UUID` como `binary(16)` por padrão no MySQL quando `@JdbcTypeCode(Types.VARCHAR)` e `columnDefinition = "VARCHAR(36)"` estão ausentes.

O campo `id` da entidade `Category` foi declarado corretamente com essas anotações. O campo `barbershopId` não.

**Efeito em produção:**  
- Gravações de `barbershopId` serializam o UUID como 16 bytes binários.  
- Leituras retornam um `UUID` incorreto / `IllegalArgumentException` de parsing.  
- Toda query `WHERE barbershop_id = ?` passando um UUID `VARCHAR(36)` falha silenciosamente — retorna 0 linhas.  
- Isso afeta: listagem de categorias por barbearia, criação de produto com categoria, unicidade `(name, barbershop_id)`.

### Arquivo afetado

```
backend/product-service/src/main/java/ifsp/edu/projeto/cortaai/productservice/model/Category.java
```

### Fix Java

Adicionar `@JdbcTypeCode(Types.VARCHAR)` no campo `barbershopId`:

```java
@JdbcTypeCode(Types.VARCHAR)
@Column(name = "barbershop_id", nullable = false, columnDefinition = "VARCHAR(36)")
private UUID barbershopId;
```

### Fix DB (executar no servidor após deploy)

Como `ddl-auto: update` não converte `binary(16)` → `VARCHAR(36)` em dados existentes, é necessário a DDL manual:

```sql
-- Verificar estado atual
USE product_db;
DESCRIBE categories;

-- Migrar (seguro para tabela com pouco volume — 2 registros em produção)
ALTER TABLE product_db.categories
    MODIFY COLUMN barbershop_id VARCHAR(36) NOT NULL;
```

> **Atenção:** se houver dados `binary(16)` existentes, eles precisam ser reescrito como UUID string.  
> Com apenas 2 registros, a abordagem é: backup → drop → recreate. Ver script abaixo.

```sql
-- Script seguro para produção com dados existentes
USE product_db;

-- 1. Backup dos dados atuais (UUIDs lidos como hex para preservar valor)
CREATE TABLE categories_backup AS
    SELECT id, name, HEX(barbershop_id) AS barbershop_id_hex, created_at
    FROM categories;

-- 2. Limpar e recriar com tipo correto
TRUNCATE TABLE categories;
ALTER TABLE categories MODIFY COLUMN barbershop_id VARCHAR(36) NOT NULL;

-- 3. Restaurar dados com UUID no formato correto (ajustar conforme valores reais)
-- INSERT INTO categories (id, name, barbershop_id, created_at)
-- SELECT id, name, LOWER(CONCAT(
--     SUBSTR(barbershop_id_hex, 1, 8), '-',
--     SUBSTR(barbershop_id_hex, 9, 4), '-',
--     SUBSTR(barbershop_id_hex, 13, 4), '-',
--     SUBSTR(barbershop_id_hex, 17, 4), '-',
--     SUBSTR(barbershop_id_hex, 21)
-- )), created_at FROM categories_backup;
```

---

## Problema 2 — Views SQL com JOIN cross-database

### Contexto

`views.sql` define 7 views analíticas. Duas delas fazem JOIN entre bancos diferentes rodando no **mesmo container MySQL**:

| View | Bancos cruzados | Violação |
|---|---|---|
| `payment_db.v_barber_financial_performance` | `payment_db` × `schedule_db` × `barbershop_db` | Owner: payment-service lê dados do schedule-service e barbershop-service diretamente |
| `user_db.v_customer_retention` | `user_db` × `schedule_db` | Owner: user-service lê dados do schedule-service diretamente |

### Impacto

- **Runtime:** funciona em produção (MySQL single-instance). Não há erro em runtime.
- **Arquitetural:** viola o princípio de isolamento de banco por microsserviço. Em uma eventual migração para bancos separados (PostgreSQL por serviço), essas views quebrariam.
- **Manutenção:** mudanças de schema em `schedule_db.appointments` exigem atualizar views que "pertencem" a outros serviços.

### Fix adotado

Adicionar bloco de aviso explícito no `views.sql` e mover as views para um schema neutro de analytics (`analytics_db`), ou documentar o compromisso explícito de que essas views são exclusivas do ambiente **single-MySQL**.

**Decisão arquitetural:**  
Como o projeto usa MySQL single-container (tanto local quanto produção via `docker-compose.server.yml`), e não há planos documentados de migração para bancos separados, a correção adotada é:
1. Adicionar comentários explícitos de "analytics cross-DB" em cada view que faz JOIN cross-schema.
2. Registrar no `ANALISE_BANCO_DADOS.md` que essas views são `ANALYTICS-ONLY` e dependem de single-MySQL.
3. Separar essas views em um bloco isolado do `views.sql` com aviso de dependência.

> Esta decisão é reversível: quando o projeto migrar para bancos isolados, as views devem ser substituídas por endpoints dedicados de analytics (ex: `GET /analytics/barber-performance` no `payment-service` consultando dados via Feign ou eventos materializados).

---

## Resumo das Ações Executadas

| Ação | Arquivo | Tipo | Status |
|---|---|---|---|
| Adicionar `@JdbcTypeCode` + `columnDefinition` em `Category.barbershopId` | `Category.java` | Código Java | ✅ Executado |
| `ALTER TABLE categories MODIFY barbershop_id VARCHAR(36)` + restauração dos 2 registros com UUID correto | MySQL produção (`ssh Edu@10.147.19.1`) | DDL manual | ✅ Executado |
| Adicionar aviso `[CROSS-DB] ANALYTICS-ONLY` nas views cross-DB | `views.sql` | Documentação SQL | ✅ Executado |
| Atualizar `ANALISE_BANCO_DADOS.md` — todos os achados marcados ✅ | `.github/ANALISE_BANCO_DADOS.md` | Documentação | ✅ Executado |
