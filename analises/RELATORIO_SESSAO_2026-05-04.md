# CortaAi — Relatório de Sessão
**Data:** 04/05/2026  
**Branch:** `feature/migracao-microservicos`  
**Período coberto:** últimos 7 dias

---

## 1. Resumo Executivo

| Área | Commits | Arquivos | Linhas |
|---|---|---|---|
| LGPD / Criptografia | `7d20d03` | 66 | +1.250 |
| Analytics / Dashboard | `c958ab1` | 51 | +1.598 |
| Testes Unitários | `7955149` | ~20 | +450 |
| Fix produção | `329082e` | 1 | +1 / -1 |
| **Total** | **4 commits** | **~138** | **~+3.300** |

---

## 2. LGPD — Criptografia de Dados Sensíveis

### Commit: `7d20d03 lgpd e validadores`

#### 2.1 Infraestrutura de criptografia (replicada em 3 serviços)

| Classe | Serviços | Descrição |
|---|---|---|
| `DataCrypto.java` | user, barbershop, schedule | AES-GCM 256-bit. Chave via env `CORTAAI_DATA_CRYPTO_KEY`. Formato de saída: `enc:v1:<base64>` |
| `SensitiveStringConverter.java` | user, barbershop, schedule | `@Converter(autoApply=false)` JPA — criptografia transparente no `@Column` |
| `SensitiveLocalDateConverter.java` | user | Converter para campos `LocalDate` sensíveis (ex: data de nascimento) |
| `PrivacyHash.java` | user | SHA-256 HMAC para campos de busca indexáveis sem expor dado real |
| `SensitiveDataBackfillRunner.java` | user, barbershop, schedule | `ApplicationRunner` — na startup, detecta e migra registros legados sem prefixo `enc:v1:` |
| `DataCryptoConfiguration.java` | user, barbershop, schedule | `@Configuration` que lê a chave do env e instancia `DataCrypto` como bean |

#### 2.2 Alterações por serviço

**`user-service`**
- `Barber.java` / `Customer.java` — campos `cpf`, `phone`, `email`, `birthDate` anotados com `@Convert(converter = SensitiveStringConverter.class)`
- `BarberRepository.java` / `CustomerRepository.java` — expandidos com queries por `cpfHash` (busca sem descriptografar)
- Validators refatorados para trabalhar com dados criptografados: `CPFValidator`, `BarberDocumentCPFUnique`, `CustomerDocumentCPFUnique`, `BarberEmailUnique`, `CustomerEmailUnique`
- DTOs atualizados: `CompleteProfileBarberDTO`, `CompleteProfileCustomerDTO`, `CreateBarberDTO`, `CustomerCreateDTO`, `FirebaseEmailRegisterRequestDTO`
- `patch_lgpd_encryption.sql` — colunas `cpf_hash`, `email_hash` adicionadas; dados existentes migrados

**`barbershop-service`**
- `Barbershop.java` — campo `cnpj` criptografado
- `BarbershopRepository.java` — `findWithLegacyPlainCnpj()` para backfill
- `validator/CNPJ.java` + `CNPJValidator.java` — anotação de validação com algoritmo completo
- `validator/CPF.java` + `CPFValidator.java` — adicionados
- `patch_lgpd_encryption.sql` — coluna `cnpj_hash` adicionada

**`schedule-service`**
- `Appointment.java` — campos de contato criptografados
- `DeduplicationService.java` — chave Redis agora usa SHA-256 do payload (não dado bruto)
- `patch_lgpd_encryption.sql` — script de migração

#### 2.3 Infraestrutura Docker
- Variável `CORTAAI_DATA_CRYPTO_KEY` adicionada em `docker-compose.yml` e `docker-compose.server.yml`
- `.env.example` atualizado com documentação das novas vars

---

## 3. Analytics / Dashboard

### Commit: `c958ab1 feat(analytics): implementa endpoints de views e dashboard com painel Dash/Relatório`

#### 3.1 Backend — Endpoints por serviço

Cada endpoint segue o padrão: `@Entity @Immutable` (view read-only) → `Repository` → `DTO record` → `Service @Transactional(readOnly=true)` → `Controller`.

**`payment-service`**
| Artefato | Arquivo |
|---|---|
| Entity | `model/analytics/VBarberFinancialPerformance.java` |
| Repository | `repository/analytics/VBarberFinancialPerformanceRepository.java` |
| DTO | `dto/BarberFinancialPerformanceResponseDTO.java` |
| Endpoint | `GET /api/payments/my-shop/barber-performance?barbershopId={uuid}` |
| Segurança | Valida `X-User-Id` como owner da barbearia (retorna 403 caso contrário) |

**`product-service`**
| Artefato | Arquivo |
|---|---|
| Entity | `model/analytics/VStockHealthAlert.java` |
| Repository | `repository/analytics/VStockHealthAlertRepository.java` (query nativa com `UNHEX`) |
| DTO | `dto/StockHealthAlertResponseDTO.java` |
| Endpoint | `GET /api/products/analytics/stock-health?barbershopId={uuid}` |

**`schedule-service`**
| Artefato | Arquivo |
|---|---|
| Entity | `model/analytics/VAgendaThermometer.java` (`@IdClass` composto: `agendaDate + barbershopId`) |
| Entity | `model/analytics/VBarberSkillMatrix.java` (`@IdClass` composto: `barberId + activityName`) |
| Repository | `repository/analytics/VAgendaThermometerRepository.java` |
| Repository | `repository/analytics/VBarberSkillMatrixRepository.java` |
| DTOs | `AgendaThermometerResponseDTO.java`, `BarberSkillMatrixResponseDTO.java` |
| Service | `service/AnalyticsService.java` (novo serviço dedicado) |
| Endpoints | `GET /api/appointments/analytics/agenda-thermometer` |
| | `GET /api/appointments/analytics/barber-skill-matrix` |

**`user-service`**
| Artefato | Arquivo |
|---|---|
| Entity | `model/analytics/VCustomerAcquisition.java` |
| Entity | `model/analytics/VCustomerRetention.java` |
| Repository | `VCustomerAcquisitionRepository.java`, `VCustomerRetentionRepository.java` |
| DTOs | `CustomerAcquisitionResponseDTO.java`, `CustomerRetentionResponseDTO.java` |
| Service | `service/UserAnalyticsService.java` |
| Endpoints | `GET /api/users/analytics/customer-acquisition` |
| | `GET /api/users/analytics/customer-retention` |

#### 3.2 Frontend

**`src/services/analyticsService.js`**
```
getBarberPerformance(barbershopId)  →  GET /payments/my-shop/barber-performance
getStockHealthAlert(barbershopId)   →  GET /products/analytics/stock-health
getAgendaThermometer(barbershopId)  →  GET /appointments/analytics/agenda-thermometer
getBarberSkillMatrix(barbershopId)  →  GET /appointments/analytics/barber-skill-matrix
getCustomerAcquisition()            →  GET /users/analytics/customer-acquisition
getCustomerRetention()              →  GET /users/analytics/customer-retention
```

**`src/components/Dashboard/DashReportPanel.jsx`**
- Props: `title`, `dashContent`, `reportContent`, `onRefresh`, `refreshInterval` (padrão 30s)
- Dois painéis lado a lado: **Dash** (gráfico/visual) e **Relatório** (tabela detalhada)
- Cada painel tem botão "Ocultar" — ao ocultar um, o outro expande para 100%
- Auto-refresh via `setInterval` no `useEffect`

**Painéis criados em `src/components/Dashboard/panels/`**

| Componente | Visualização Dash | Visualização Relatório |
|---|---|---|
| `BarberPerformancePanel` | Barras verticais por receita | Tabela com % de contribuição |
| `StockHealthPanel` | Cards OK / Em Alerta | Tabela com produtos críticos |
| `AgendaThermometerPanel` | Barras empilhadas ativo/perdido (14 dias) | Tabela dia a dia |
| `BarberSkillMatrixPanel` | Cards agrupados por barbeiro | Tabela serviço × execuções |
| `CustomerAcquisitionPanel` | Barras por mês (12 meses) | Tabela mensal |
| `CustomerRetentionPanel` | Barras azuis por mês | Tabela mensal |

**`src/pages/BarberDashboardPage.jsx`** — reescrito:
- `Promise.allSettled` para as 6 chamadas paralelas (falha isolada por painel)
- `useCallback` no `fetchAll` memoizado por `barbershopId`
- 6 `<DashReportPanel>` com `onRefresh={fetchAll}` e auto-refresh individual de 30s
- CTA "Novo Encaixe" preservado

---

## 4. Testes Unitários

### Commit: `7955149 teste unitario 1`

| Arquivo de Teste | Serviço | Cobertura |
|---|---|---|
| `PaymentServiceFinancialsTest.java` | payment | 234 linhas — split de pagamento, cálculo de receita, edge cases de valor zero/negativo |
| `DeduplicationServiceTest.java` | notification + schedule | Desduplicação por Redis, idempotência, TTL |
| `PushNotificationServiceTest.java` | notification | 60 linhas — envio de push, fallback, payload inválido |
| `NotificationServiceTest.java` | notification | Ajustado para novo contexto pós-LGPD |
| `DataCryptoTest.java` | user, barbershop, schedule | 3 arquivos — encrypt/decrypt round-trip, prefixo `enc:v1:`, chave inválida, dado nulo |
| `CPFValidatorTest.java` | user, barbershop | 2 arquivos — CPF válido, inválido, com máscara, todos dígitos iguais |
| `CNPJValidatorTest.java` | barbershop | CNPJ válido, inválido, com máscara |
| `DiscoveryServiceApplicationTest.java` | discovery | Ajuste de carregamento de contexto Spring |

**Infraestrutura adicionada:**
- `resources/META-INF/services/org.mockito.plugins.MockMaker` — habilita inline mock maker para mocks de classes finais (payment, notification)
- `test/resources/application.yml` — configuração de teste isolada (sem conexão real com DB/Redis) nos serviços que receberam crypto

---

## 5. Fix de Produção

### Commit: `329082e fix(barbershop-service)`

**Problema:** `barbershop-service` crashava na startup com `Column 'averageRating' not found`.

**Causa raiz:** `findWithLegacyPlainCnpj()` usava `nativeQuery = true` com `SELECT * FROM barbershops`. O Hibernate 6 tentava mapear o campo `@Formula` como coluna real do ResultSet JDBC — o que não existe em queries nativas.

**Correção:** convertida para JPQL puro (`@Query("SELECT b FROM Barbershop b WHERE ...")`), que o Hibernate processa corretamente resolvendo `@Formula` em memória.

---

## 6. Estado Atual do Servidor (04/05/2026)

| Container | Status |
|---|---|
| `cortaai-mysql` | ✅ healthy |
| `cortaai-rabbitmq` | ✅ healthy |
| `cortaai-redis` | ✅ healthy (senha `cortaai_redis_secret` definida em `.env.prod`) |
| `discovery-service` | ✅ healthy |
| `api-gateway` | ✅ up |
| `user-service` | ✅ up |
| `product-service` | ✅ up |
| `payment-service` | ✅ up |
| `schedule-service` | ✅ up |
| `notification-service` | ✅ up |
| `cortaai-web` | ✅ up |
| `barbershop-service` | ⚠️ **pendente rebuild** com fix do commit `329082e` |

### Próximo passo imediato

No servidor (`ssh Edu@10.147.19.1`, senha `24042004`):

```bash
cd /DATA/cortaai/repo
sudo DOCKER_CONFIG=$HOME/.docker docker compose \
  --env-file .env.prod \
  -f docker-compose.server.yml \
  build barbershop-service && \
sudo DOCKER_CONFIG=$HOME/.docker docker compose \
  --env-file .env.prod \
  -f docker-compose.server.yml \
  up -d barbershop-service
```

---

## 7. Variáveis de Ambiente Adicionadas em `.env.prod`

| Variável | Valor | Motivo |
|---|---|---|
| `CORTAAI_DATA_CRYPTO_KEY` | `NM5z3FAD+vlmhNTg7awklXBoL+08iJGRyY9TEWtLmC4=` | Chave AES-GCM para criptografia LGPD |
| `REDIS_PASSWORD` | `cortaai_redis_secret` | Redis 7.x exige valor não-vazio em `--requirepass` |

> ⚠️ Estas variáveis **não estão no git** (`.env.prod` está em `.gitignore`). Documentar em local seguro da equipe.

---

*Gerado automaticamente em 04/05/2026*
