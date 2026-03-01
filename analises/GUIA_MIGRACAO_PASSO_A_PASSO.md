# 🚀 CortaAí — Guia de Migração Passo a Passo

> **Versão:** 1.0  
> **Data:** 28 de fevereiro de 2026  
> **Objetivo:** Guia prático e sequencial para desmembrar o monólito em microserviços, na ordem ideal de execução.

---

## 📌 Resumo da Estratégia

A migração segue o padrão **Strangler Fig** — cada módulo do monólito é extraído gradualmente para seu microserviço, enquanto o monólito ainda funciona como referência. A ordem foi definida por **análise de dependências**, **risco técnico** e **valor de negócio**.

### Ordem Final de Migração

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     ORDEM DE DESMEMBRAMENTO                             │
│                                                                         │
│  ETAPA 0 ──► Fundação (infra + correções)              [2 semanas]     │
│  ETAPA 1 ──► user-service (AUTH é a BASE de tudo)       [2 semanas]     │
│  ETAPA 2 ──► barbershop-service (depende do user)       [3 semanas]     │
│  ETAPA 3 ──► schedule-service (depende de 1 + 2)        [3 semanas]     │
│  ETAPA 4 ──► Limpeza, testes e validação geral           [1 semana]     │
│  ETAPA 5 ──► notification-service (consome eventos)      [2 semanas]     │
│  ETAPA 6 ──► payment-service (depende de 3)              [3 semanas]     │
│  ETAPA 7 ──► product-service (depende de 6)              [3 semanas]     │
│  ETAPA 8 ──► Frontend, Infra Produção e Go-Live          [3 semanas]     │
│                                                                         │
│  TOTAL ESTIMADO: ~22 semanas (5,5 meses)                                │
└─────────────────────────────────────────────────────────────────────────┘
```

### Por quê esta ordem?

```
Dependência entre serviços:

user-service ◄── barbershop-service ◄── schedule-service
      ▲                  ▲                    │
      │                  │                    ▼
      │                  │              payment-service
      │                  │                    │
      │                  │                    ▼
      │                  └──────────── product-service
      │
      └─── notification-service (consome eventos de TODOS via RabbitMQ)

Regra: SEMPRE migrar o serviço que NÃO depende de outros primeiro,
       depois os que dependem dos já migrados.
```

| # | Serviço | Justificativa da Posição |
|---|---|---|
| 0 | Fundação | Sem infra funcional, nada funciona |
| 1 | `user-service` | **Não depende de nenhum outro serviço.** Auth/JWT é a base para todos. Já está parcialmente migrado. |
| 2 | `barbershop-service` | Depende apenas do `user-service` (via Feign). Segundo domínio mais independente. |
| 3 | `schedule-service` | Depende do `user-service` + `barbershop-service`. É o core do negócio. |
| 4 | Limpeza | Validar que os 3 serviços core funcionam juntos antes de evoluir. |
| 5 | `notification-service` | Não bloqueia nenhum fluxo. Consome eventos de forma assíncrona. Pode ser adicionado a qualquer momento. |
| 6 | `payment-service` | Depende do `schedule-service` (busca dados do agendamento). É feature nova. |
| 7 | `product-service` | Depende do `payment-service`. É feature 100% nova (e-commerce). Menor prioridade. |
| 8 | Infra/Deploy | Só faz sentido após todos os serviços estarem funcionando. |

---

## 🔧 ETAPA 0 — Fundação e Correções Críticas

> **Duração:** 2 semanas  
> **Pré-requisito:** Nenhum  
> **Resultado:** Infra funcional, todos os módulos compilando, proxy configurado

### 0.1 — Corrigir POM do user-service

**Problema:** `pom.xml` com XML inválido (dependência `cloudinary-http44` fora da tag `<dependencies>`) e versão do Spring Boot desalinhada (`3.2.3` vs `3.3.4`).

- [ ] Abrir `backend/user-service/pom.xml`
- [ ] Mover `cloudinary-http44` para dentro de `<dependencies>`
- [ ] Alterar o parent para herdar do POM pai (`cortaai:0.1`) em vez de `spring-boot-starter-parent:3.2.3`
- [ ] Validar: `cd backend && ./mvnw validate -pl user-service`

### 0.2 — Padronizar POM pai (Multi-Module Maven)

**Problema:** Cada módulo usa versões diferentes de Spring Boot.

- [ ] Abrir `backend/pom.xml` (POM pai)
- [ ] Garantir que todos os módulos estão listados em `<modules>`
- [ ] Validar versões centralizadas:
  - Spring Boot: `3.3.4`
  - Spring Cloud: `2023.0.1`
  - Java: `17`
  - MapStruct: `1.5.5.Final`
  - JJWT: `0.11.5`
- [ ] Garantir que `user-service`, `barbershop-service` e `schedule-service` usam `<parent>` apontando para `cortaai:0.1`
- [ ] Validar: `cd backend && ./mvnw validate`

### 0.3 — Completar docker-compose.yml

**Problema:** Faltam containers e healthchecks.

- [ ] Adicionar container `cortaai-mysql` com healthcheck (`mysqladmin ping`)
- [ ] Adicionar container `cortaai-rabbitmq` com healthcheck (`rabbitmqctl status`)
- [ ] Adicionar container `cortaai-redis` com healthcheck (`redis-cli ping`)
- [ ] Adicionar `discovery-service` (sem depends_on de infra)
- [ ] Adicionar `api-gateway` (depends_on: discovery)
- [ ] Adicionar `user-service` (depends_on: mysql ✅, discovery)
- [ ] Adicionar `barbershop-service` (depends_on: mysql ✅, discovery, rabbitmq ✅)
- [ ] Adicionar `schedule-service` (depends_on: mysql ✅, discovery, rabbitmq ✅, redis ✅)
- [ ] Adicionar volume `./init.sql:/docker-entrypoint-initdb.d/init.sql`
- [ ] Validar: `docker compose config`

### 0.4 — Atualizar init.sql

- [ ] Garantir criação dos 5 schemas:
  ```sql
  CREATE DATABASE IF NOT EXISTS user_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS barbershop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS schedule_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS payment_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS product_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### 0.5 — Configurar proxy no frontend (Vite)

**Problema:** `baseURL: '/api'` sem proxy causa CORS em dev.

- [ ] Abrir `frontend/vite.config.js`
- [ ] Adicionar configuração de proxy:
  ```javascript
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
  ```
- [ ] Validar: `cd frontend && npm run dev`

### 0.6 — Validar Discovery + Gateway sobem

- [ ] Subir MySQL + Discovery + Gateway via Docker Compose
- [ ] Acessar `http://localhost:8761` (Eureka Dashboard)
- [ ] Verificar que o Gateway aparece registrado no Eureka
- [ ] Validar rotas do Gateway no `application.yml`

### ✅ Checkpoint da Etapa 0

| Critério | Verificação |
|---|---|
| `./mvnw validate` sem erros | `cd backend && ./mvnw validate` |
| Docker Compose sobe infra | `docker compose up -d mysql rabbitmq redis discovery gateway` |
| Eureka Dashboard acessível | `http://localhost:8761` |
| Schemas MySQL criados | `mysql -h localhost -u root -e "SHOW DATABASES;"` |
| Frontend dev funciona | `cd frontend && npm run dev` → acessar `http://localhost:5173` |

---

## 🔐 ETAPA 1 — user-service (Autenticação e Usuários)

> **Duração:** 2 semanas  
> **Pré-requisito:** Etapa 0 concluída  
> **Resultado:** Cadastro, login, JWT e perfil funcionando independentemente  
> **Banco:** `user_db`

### Por que primeiro?

O `user-service` é a **raiz de toda a árvore de dependências**. Todos os outros serviços precisam validar usuários e tokens JWT. Ele não depende de nenhum outro microserviço.

### 1.1 — Eliminar imports cruzados do monólito

**Problema Crítico:** `CustomerController` importa `ifsp.edu.projeto.cortaai.dto.*` e `ifsp.edu.projeto.cortaai.service.*` do monólito. **O serviço não compila sozinho.**

- [ ] Copiar/recriar no pacote `userservice`:
  - `exception/NotFoundException.java`
  - `exception/ReferenceException.java`
  - `service/storage/StorageService.java` (interface)
  - `service/storage/CloudinaryStorageServiceImpl.java`
  - `event/BeforeDeleteCustomer.java`
  - `event/BeforeDeleteBarber.java`
  - `dto/UploadResultDTO.java`
- [ ] Atualizar TODOS os imports para `ifsp.edu.projeto.cortaai.userservice.*`
- [ ] **Validar:** `cd backend && ./mvnw compile -pl user-service`

### 1.2 — Completar model Barber

**Problema:** Campos ausentes no model Barber.

- [ ] Adicionar ao `Barber.java`:
  ```java
  private LocalTime workStartTime;
  private LocalTime workEndTime;
  private String imageUrl;
  private String imageUrlPublicId;
  ```
- [ ] Atualizar `BarberMapper` e `BarberDTO` com os novos campos
- [ ] **Validar:** compilar novamente

### 1.3 — Configurar SecurityConfig + JWT

- [ ] Garantir que `SecurityConfig.java` está configurado:
  - Endpoints públicos: `/api/auth/**`, `/api/customers/register`, `/api/barbers/register`
  - Endpoints protegidos: todo o resto
  - `JwtAuthorizationFilter` como filtro antes de `UsernamePasswordAuthenticationFilter`
- [ ] Garantir que `JwtTokenService` gera tokens com claims: `sub` (email), `role`, `userId`
- [ ] Configurar `application.yml`:
  ```yaml
  app:
    security:
      jwt:
        secret-key: ${JWT_SECRET_KEY:SecretKeySuperSeguraParaDesenvolvimento12345}
        expiration-ms: 3600000
  ```

### 1.4 — Criar endpoints internos (inter-serviço)

Os outros serviços precisarão consultar dados de usuários. Criar endpoints dedicados:

- [ ] Criar `InternalUserController.java`:
  ```
  GET  /api/internal/users/{id}          → UserInfoDTO
  GET  /api/internal/users/by-email/{email} → UserInfoDTO
  PUT  /api/internal/users/{id}/barbershop → void (atualiza barbershopId)
  ```
- [ ] Criar `UserInfoDTO.java`:
  ```java
  public record UserInfoDTO(
      UUID id, String name, String email, String userType, String role,
      UUID barbershopId, LocalTime workStartTime, LocalTime workEndTime,
      String imageUrl, List<String> skills
  ) {}
  ```
- [ ] Proteger com header `X-Internal-Token` (não expor no Gateway)
- [ ] **No Gateway:** garantir que rotas `/api/internal/**` NÃO estão mapeadas

### 1.5 — Configurar application.yml do user-service

- [ ] DataSource apontando para `user_db`:
  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://${DB_HOST:localhost}:3306/user_db?useSSL=false&allowPublicKeyRetrieval=true
      username: ${DB_USERNAME:root}
      password: ${DB_PASSWORD:root}
    jpa:
      hibernate:
        ddl-auto: update
      show-sql: true
    application:
      name: user-service
  server:
    port: 8081
  eureka:
    client:
      service-url:
        defaultZone: http://${EUREKA_HOST:localhost}:8761/eureka/
  ```

### 1.6 — Testar fluxo completo (manual)

- [ ] Subir: MySQL → Discovery → Gateway → user-service
- [ ] Registrar Customer: `POST /api/customers/register`
- [ ] Login: `POST /api/auth/login` → receber JWT
- [ ] Listar perfil: `GET /api/customers/me` (com header `Authorization: Bearer {token}`)
- [ ] Registrar Barber: `POST /api/barbers/register`
- [ ] Login como Barber
- [ ] Endpoint interno: `GET /api/internal/users/{id}` (com header `X-Internal-Token`)

### 1.7 — Testes unitários

- [ ] Testar `JwtTokenServiceImpl` (gerar/validar token)
- [ ] Testar `CustomerServiceImpl` (cadastro, login, exceções)
- [ ] Testar `BarberServiceImpl`
- [ ] Testar validadores (CPF, email único)
- [ ] **Meta:** >80% cobertura de service/impl

### ✅ Checkpoint da Etapa 1

| Critério | Verificação |
|---|---|
| Compila sozinho | `./mvnw compile -pl user-service` ✅ |
| Sobe sem erros | Logs limpos, registra no Eureka |
| Cadastro Customer funciona | `POST /api/customers/register` → 201 |
| Login retorna JWT | `POST /api/auth/login` → 200 + token |
| JWT valida em requests | `GET /api/customers/me` com Bearer → 200 |
| Endpoint interno funciona | `GET /api/internal/users/{id}` → 200 |
| Testes unitários passam | `./mvnw test -pl user-service` ✅ |

---

## 🏪 ETAPA 2 — barbershop-service (Barbearias, Serviços e Equipe)

> **Duração:** 3 semanas  
> **Pré-requisito:** Etapa 1 concluída (user-service funcional)  
> **Resultado:** CRUD de barbearia, atividades, equipe, highlights, tudo funcionando via Feign  
> **Banco:** `barbershop_db`

### Por que segundo?

O `barbershop-service` depende **apenas** do `user-service` (para validar barbeiros/owners). Ele é pré-requisito para o `schedule-service`.

### Semana 1 — Entidades, Repositórios e Configuração

#### 2.1 — Criar entidades JPA

- [ ] `Barbershop.java` — com novos campos (city, state, latitude, longitude, average_rating, total_reviews)
- [ ] `Activity.java` — com campo `category` (CORTE, BARBA, COMBO, TRATAMENTO)
- [ ] `BarberActivity.java` — tabela pivô com `barberId` como `UUID` simples (sem `@ManyToOne`)
- [ ] `BarbershopJoinRequest.java` — com `barberId` como UUID externo
- [ ] `BarbershopHighlight.java`
- [ ] `BarbershopWorkingHours.java` (**NOVO** — horário de funcionamento)
- [ ] `Review.java` (**NOVO** — avaliações)
- [ ] Enums: `JoinRequestStatus`, `ActivityCategory`

> **⚠️ ATENÇÃO:** `barber_id` em `BarberActivity` e `BarbershopJoinRequest` é um UUID **simples** (sem FK), pois o barbeiro vive no `user_db`. A validação é feita via Feign.

#### 2.2 — Criar repositórios

- [ ] `BarbershopRepository`
- [ ] `ActivityRepository`
- [ ] `BarberActivityRepository`
- [ ] `BarbershopJoinRequestRepository`
- [ ] `BarbershopHighlightRepository`
- [ ] `BarbershopWorkingHoursRepository`
- [ ] `ReviewRepository`

#### 2.3 — Configurar application.yml

```yaml
spring:
  application:
    name: barbershop-service
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/barbershop_db
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
  jpa:
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
server:
  port: 8082
eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_HOST:localhost}:8761/eureka/

cloudinary:
  cloud_name: ${CLOUDINARY_CLOUD_NAME}
  api_key: ${CLOUDINARY_API_KEY}
  api_secret: ${CLOUDINARY_API_SECRET}
```

#### 2.4 — Configurar Feign Client para user-service

- [ ] Adicionar dependências ao `pom.xml`:
  - `spring-cloud-starter-openfeign`
  - `resilience4j-spring-boot3`
- [ ] Adicionar `@EnableFeignClients` na classe `BarbershopServiceApplication`
- [ ] Criar `feign/UserServiceClient.java`:
  ```java
  @FeignClient(name = "user-service")
  public interface UserServiceClient {
      @GetMapping("/api/internal/users/{id}")
      UserInfoDTO getUserById(@PathVariable UUID id);

      @PutMapping("/api/internal/users/{id}/barbershop")
      void updateUserBarbershopId(@PathVariable UUID id, @RequestBody UUID barbershopId);
  }
  ```

#### 2.5 — Configurar Resilience4j

```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
  retry:
    instances:
      userService:
        max-attempts: 3
        wait-duration: 500ms
```

- [ ] Validar: `./mvnw compile -pl barbershop-service`

### Semana 2 — Services e Controllers (migrar do monólito)

#### 2.6 — Criar DTOs

- [ ] `BarbershopDTO`, `CreateBarbershopDTO`, `UpdateBarbershopDTO`
- [ ] `ActivityDTO`, `CreateActivityDTO`
- [ ] `JoinRequestDTO`, `JoinRequestResponseDTO`
- [ ] `HighlightDTO`
- [ ] `ReviewDTO`, `CreateReviewDTO`
- [ ] DTOs internos (inter-serviço): `BarbershopInfoDTO`, `ActivityInfoDTO`

#### 2.7 — Criar Mappers (MapStruct)

- [ ] `BarbershopMapper`
- [ ] `ActivityMapper`
- [ ] `JoinRequestMapper`
- [ ] `ReviewMapper`

#### 2.8 — Migrar BarbershopServiceImpl

Migrar toda a lógica do `BarbershopController` do monólito, substituindo acessos diretos ao banco de users por chamadas Feign:

| Operação no Monólito | Substituição no Microserviço |
|---|---|
| `barberRepository.findById(id)` | `userServiceClient.getUserById(id)` |
| `barberRepository.findByEmail(email)` | `userServiceClient.getUserByEmail(email)` |
| `barber.setBarbershopId(shopId)` | `userServiceClient.updateUserBarbershopId(id, shopId)` |

- [ ] Implementar CRUD de Barbershop (criar, editar, deletar, listar)
- [ ] Implementar CRUD de Activity (criar, editar, deletar, listar por barbearia)
- [ ] Implementar gestão de BarberActivity (vincular barbeiro a serviço)
- [ ] Implementar JoinRequest (solicitar, aprovar, rejeitar)
- [ ] Implementar Highlights (upload de imagens para Cloudinary)
- [ ] Implementar WorkingHours (definir horários de funcionamento)

#### 2.9 — Criar endpoints internos (inter-serviço)

O `schedule-service` precisará consultar dados de barbearia:

- [ ] Criar `InternalBarbershopController.java`:
  ```
  GET  /api/internal/barbershops/{id}                        → BarbershopInfoDTO
  GET  /api/internal/barbershops/{shopId}/activities          → List<ActivityInfoDTO>
  GET  /api/internal/barbershops/{shopId}/activities?ids=...  → List<ActivityInfoDTO> (por IDs)
  GET  /api/internal/barbershops/{shopId}/barber-activities/{barberId} → List<UUID>
  ```
- [ ] Proteger com header `X-Internal-Token`

#### 2.10 — Configurar publicação de eventos (RabbitMQ)

- [ ] Criar `RabbitConfig.java` com exchange e queues:
  ```java
  public static final String EXCHANGE = "cortaai.events";
  public static final String BARBER_APPROVED_QUEUE = "barber.approved";
  public static final String BARBERSHOP_DELETED_QUEUE = "barbershop.deleted";
  ```
- [ ] Publicar evento `BarberApprovedEvent` quando JoinRequest é aprovado
- [ ] Publicar evento `BarbershopDeletedEvent` quando barbearia é removida

### Semana 3 — Controller, Testes e Integração

#### 2.11 — Criar BarbershopController

Migrar os 18 endpoints do controller do monólito:

```
POST   /api/barbershops                           → Criar barbearia
GET    /api/barbershops                           → Listar todas (público)
GET    /api/barbershops/{id}                      → Detalhar (público)
PUT    /api/barbershops/{id}                      → Atualizar
DELETE /api/barbershops/{id}                      → Deletar

POST   /api/barbershops/{id}/activities           → Criar atividade
GET    /api/barbershops/{id}/activities           → Listar atividades
PUT    /api/barbershops/{id}/activities/{actId}   → Atualizar atividade
DELETE /api/barbershops/{id}/activities/{actId}   → Deletar atividade

POST   /api/barbershops/join-request              → Solicitar entrada (barbeiro)
GET    /api/barbershops/{id}/pending-requests     → Listar pendentes (dono)
PUT    /api/barbershops/join-request/{id}/approve → Aprovar (dono)
PUT    /api/barbershops/join-request/{id}/reject  → Rejeitar (dono)

POST   /api/barbershops/{id}/highlights           → Upload destaque
DELETE /api/barbershops/{id}/highlights/{hlId}    → Remover destaque
GET    /api/barbershops/{id}/highlights           → Listar destaques

GET    /api/barbershops/{id}/barbers              → Listar barbeiros da loja
```

#### 2.12 — Testar integração user ↔ barbershop

- [ ] Subir: MySQL → Discovery → Gateway → user-service → barbershop-service
- [ ] Cadastrar barbeiro via `user-service`
- [ ] Criar barbearia via `barbershop-service` (deve chamar Feign para validar owner)
- [ ] Solicitar entrada via `barbershop-service` (JoinRequest)
- [ ] Aprovar via `barbershop-service` → deve atualizar `barbershopId` no `user-service`
- [ ] Listar barbeiros da barbearia

#### 2.13 — Testes unitários

- [ ] Testar `BarbershopServiceImpl` com Feign mockado
- [ ] Testar validador CNPJ
- [ ] Testar fluxo de JoinRequest (solicitar → aprovar/rejeitar)
- [ ] Testar criação de Activity

### ✅ Checkpoint da Etapa 2

| Critério | Verificação |
|---|---|
| Compila sozinho | `./mvnw compile -pl barbershop-service` ✅ |
| Sobe e registra no Eureka | Visível em `http://localhost:8761` |
| CRUD barbearia funciona | POST/GET/PUT/DELETE via Postman |
| CRUD atividade funciona | POST/GET/PUT/DELETE via Postman |
| JoinRequest funciona | Solicitar → Aprovar → barbershopId atualizado no user-service |
| Feign chamando user-service | Logs mostram chamada HTTP |
| Resilience4j ativo | Desligar user-service → circuit breaker atua |
| Testes passam | `./mvnw test -pl barbershop-service` ✅ |

---

## 📅 ETAPA 3 — schedule-service (Agendamentos)

> **Duração:** 3 semanas  
> **Pré-requisito:** Etapas 1 e 2 concluídas  
> **Resultado:** Agendamento completo com validação via Feign e cache Redis  
> **Banco:** `schedule_db` + Redis (cache)

### Por que terceiro?

É o **core do negócio** mas depende dos dois serviços anteriores: precisa validar Customer/Barber no `user-service` e Activities/Barbershop no `barbershop-service`.

### Semana 1 — Entidades, Feign e Configuração

#### 3.1 — Criar entidades JPA

- [ ] `Appointment.java` — com dados desnormalizados:
  ```java
  private String barbershopName;  // Snapshot — evita Feign em toda listagem
  private String barberName;
  private String customerName;
  ```
- [ ] `AppointmentActivity.java` — snapshot do serviço:
  ```java
  private String activityName;    // Snapshot
  private BigDecimal price;       // Snapshot
  private Integer durationMinutes; // Snapshot
  ```
- [ ] `BarberBlock.java` (**NOVO** — férias, folgas, bloqueios)
- [ ] Enum: `AppointmentStatus` (SCHEDULED, CONFIRMED, IN_PROGRESS, CONCLUDED, CANCELLED, NO_SHOW)

> **⚠️ DECISÃO ARQUITETURAL:** Dados desnormalizados (nomes, preços) são copiados no momento da criação. Evita chamadas REST em toda listagem. Atualizados via eventos RabbitMQ quando originais mudam.

#### 3.2 — Criar Feign Clients

- [ ] `UserServiceClient.java`:
  ```
  GET /api/internal/users/{id}          → UserInfoDTO
  GET /api/internal/users/by-email/{email} → UserInfoDTO
  ```
- [ ] `BarbershopServiceClient.java`:
  ```
  GET /api/internal/barbershops/{id}                                → BarbershopInfoDTO
  GET /api/internal/barbershops/{shopId}/activities?ids=...          → List<ActivityInfoDTO>
  GET /api/internal/barbershops/{shopId}/barber-activities/{barberId} → List<UUID>
  ```

#### 3.3 — Configurar Redis (cache de disponibilidade)

- [ ] Adicionar dependência `spring-boot-starter-data-redis`
- [ ] Criar `RedisConfig.java` com serializers
- [ ] Cache TTL: 5 minutos para dados de horário do barbeiro
- [ ] `application.yml`:
  ```yaml
  spring:
    data:
      redis:
        host: ${REDIS_HOST:localhost}
        port: 6379
  ```

#### 3.4 — Criar repositórios

- [ ] `AppointmentRepository` com queries otimizadas:
  ```java
  // Verificação de conflito de horário
  @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.barberId = :barberId " +
         "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
         "AND a.startTime < :endTime AND a.endTime > :startTime")
  boolean hasConflict(@Param("barberId") UUID barberId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);
  ```
- [ ] `BarberBlockRepository`

### Semana 2 — Service (lógica core de agendamento)

#### 3.5 — Migrar AppointmentsServiceImpl

Este é o passo mais complexo. O fluxo de criação de agendamento no monólito acessa 4 repositórios diferentes. No microserviço, usa Feign:

```
FLUXO DE CRIAÇÃO DE AGENDAMENTO (NOVO):

1. Recebe AppointmentRequestDTO (customerId, barberId, barbershopId, activityIds, startTime)
2. [Feign → user-service]     Validar que customerId existe e é CUSTOMER
3. [Feign → user-service]     Validar que barberId existe e é BARBER
4. [Feign → barbershop-svc]   Validar que barbershopId existe e está ativo
5. [Feign → barbershop-svc]   Buscar activities pelos IDs → obter nomes, preços, durações
6. [Feign → barbershop-svc]   Verificar que barbeiro tem skills para os serviços selecionados
7. [LOCAL → schedule_db]      Calcular endTime = startTime + soma(durações)
8. [LOCAL → schedule_db]      Verificar conflito de horário (query otimizada)
9. [LOCAL → schedule_db]      Verificar BarberBlock (barbeiro não está em folga?)
10. [LOCAL → schedule_db]     Criar Appointment com dados desnormalizados
11. [LOCAL → schedule_db]     Criar AppointmentActivities (snapshots)
12. [RabbitMQ]                 Publicar AppointmentCreatedEvent
13. Retornar AppointmentResponseDTO
```

- [ ] Implementar o fluxo completo acima
- [ ] Implementar cancelamento (com regras de quem pode cancelar)
- [ ] Implementar conclusão (barbeiro marca como concluído)
- [ ] Implementar atualização de status
- [ ] Implementar listagem (por customer, por barber, por barbershop)

#### 3.6 — Implementar endpoint de disponibilidade

```
GET /api/appointments/availability?barberId={id}&date={date}

FLUXO:
1. [Feign → user-service] Buscar workStartTime, workEndTime do barbeiro (com cache Redis 5min)
2. [LOCAL] Buscar agendamentos do barbeiro naquele dia
3. [LOCAL] Buscar bloqueios do barbeiro naquele dia
4. [LOCAL] Calcular slots disponíveis (slots de 30min entre workStart e workEnd, excluindo ocupados)
5. Retornar List<TimeSlotDTO>
```

- [ ] Implementar com cache Redis
- [ ] Invalidar cache quando agendamento é criado/cancelado

#### 3.7 — Publicação de eventos (RabbitMQ)

- [ ] `AppointmentCreatedEvent` → será consumido por notification-service e payment-service
- [ ] `AppointmentCancelledEvent` → será consumido por notification-service
- [ ] `AppointmentConcludedEvent` → será consumido por notification-service

### Semana 3 — Controller, Testes e Integração

#### 3.8 — Criar AppointmentsController

```
POST   /api/appointments                          → Criar agendamento
GET    /api/appointments/{id}                     → Detalhar agendamento
GET    /api/appointments/my-appointments          → Meus agendamentos (customer)
GET    /api/appointments/barber/{barberId}        → Agenda do barbeiro
GET    /api/appointments/barbershop/{shopId}      → Agenda da barbearia
PUT    /api/appointments/{id}/cancel              → Cancelar
PUT    /api/appointments/{id}/conclude            → Concluir (barbeiro)
PUT    /api/appointments/{id}/confirm             → Confirmar
GET    /api/appointments/availability             → Slots disponíveis
POST   /api/appointments/barber-blocks            → Criar bloqueio de agenda
DELETE /api/appointments/barber-blocks/{id}       → Remover bloqueio
```

#### 3.9 — Testar fluxo completo (3 serviços)

- [ ] Subir: MySQL → RabbitMQ → Redis → Discovery → Gateway → user-service → barbershop-service → schedule-service
- [ ] Cadastrar customer + barbeiro + barbearia + atividade + aprovar JoinRequest
- [ ] Criar agendamento → deve validar via Feign e salvar com dados desnormalizados
- [ ] Verificar conflito (agendar no mesmo horário → erro 409)
- [ ] Cancelar agendamento → verificar evento no RabbitMQ
- [ ] Consultar disponibilidade → verificar slots corretos

#### 3.10 — Testes unitários

- [ ] Testar `AppointmentsServiceImpl` com Feign clients mockados
- [ ] Testar verificação de conflito de horário
- [ ] Testar cálculo de disponibilidade
- [ ] Testar validação de BarberBlock

### ✅ Checkpoint da Etapa 3

| Critério | Verificação |
|---|---|
| Compila sozinho | `./mvnw compile -pl schedule-service` ✅ |
| Agendamento cria com sucesso | POST → 201 + dados desnormalizados corretos |
| Conflito detectado | Segundo agendamento no mesmo horário → 409 |
| Disponibilidade funciona | GET → slots calculados corretamente |
| BarberBlock impede agendamento | Criar bloqueio → agendar no período → erro |
| Feign chama user + barbershop | Logs mostram chamadas HTTP |
| Eventos publicados no RabbitMQ | RabbitMQ Management mostra mensagens |
| Redis cacheando | Logs mostram cache hit/miss |
| Testes passam | `./mvnw test -pl schedule-service` ✅ |

---

## 🧹 ETAPA 4 — Limpeza, Testes e Validação Geral

> **Duração:** 1 semana  
> **Pré-requisito:** Etapas 1, 2 e 3 concluídas  
> **Resultado:** 3 serviços core validados e estáveis

### 4.1 — Testes de integração com Testcontainers

- [ ] Criar teste de integração para cada serviço usando Testcontainers (MySQL real + RabbitMQ real)
- [ ] Testar jornada completa: registro → login → criar barbearia → criar atividade → aprovar barbeiro → agendar → cancelar

### 4.2 — Desativar módulo monólito do build

- [ ] Remover o módulo `src/` (monólito legado) da lista de `<modules>` no POM pai
- [ ] Manter a pasta como referência histórica, mas **não compilar** mais
- [ ] Adicionar um `README.md` em `backend/src/` explicando que é legado

### 4.3 — Atualizar rotas do API Gateway

- [ ] Verificar que todas as rotas do Gateway estão corretas:
  ```yaml
  routes:
    - id: user-service
      uri: lb://user-service
      predicates:
        - Path=/api/customers/**, /api/barbers/**, /api/auth/**
    - id: barbershop-service
      uri: lb://barbershop-service
      predicates:
        - Path=/api/barbershops/**
    - id: schedule-service
      uri: lb://schedule-service
      predicates:
        - Path=/api/appointments/**
  ```
- [ ] Verificar que `/api/internal/**` **NÃO** está roteado (segurança)

### 4.4 — Atualizar frontend (apontar para novos endpoints)

- [ ] Verificar `frontend/src/services/api.js` → `baseURL: '/api'`
- [ ] Verificar `appointmentService.js` → endpoints corretos
- [ ] Verificar `barbershopService.js` → endpoints corretos
- [ ] Verificar `authService.js` → endpoints corretos
- [ ] Testar fluxo completo no frontend

### 4.5 — Documentação Swagger/OpenAPI

- [ ] Adicionar `springdoc-openapi-starter-webmvc-ui` em cada serviço
- [ ] Acessar Swagger UI de cada serviço:
  - `http://localhost:8081/swagger-ui.html`
  - `http://localhost:8082/swagger-ui.html`
  - `http://localhost:8083/swagger-ui.html`

### ✅ Checkpoint da Etapa 4

| Critério | Verificação |
|---|---|
| 3 serviços funcionando juntos | Docker Compose up → tudo verde |
| Frontend funciona E2E | Login → buscar barbearia → agendar → ver agenda |
| Monólito não compila mais | Removido do build |
| Swagger acessível | 3 URLs de documentação |
| Sem imports cruzados | `grep -r "ifsp.edu.projeto.cortaai.dto" backend/user-service/` → vazio |

---

## 🔔 ETAPA 5 — notification-service (Notificações)

> **Duração:** 2 semanas  
> **Pré-requisito:** Etapa 4 concluída (eventos fluindo no RabbitMQ)  
> **Resultado:** Notificações por email e in-app funcionando  
> **Banco:** Redis (sem MySQL)

### Por que quinto?

É um serviço **puramente consumidor de eventos**. Não bloqueia nenhum outro fluxo. Pode ser adicionado a qualquer momento pois apenas escuta filas do RabbitMQ.

### 5.1 — Criar estrutura base

- [ ] Entidade main: `NotificationServiceApplication.java`
- [ ] Configurações: `RabbitConfig`, `WebSocketConfig`, `RedisConfig`
- [ ] Enums: `NotificationType`, `NotificationChannel`

### 5.2 — Configurar consumidores RabbitMQ

- [ ] Criar `@RabbitListener` para cada fila:
  - `appointment.created` → Notificar cliente + barbeiro
  - `appointment.cancelled` → Notificar contraparte
  - `appointment.concluded` → Pedir avaliação ao cliente
  - `payment.approved` → Confirmar pagamento ao cliente
  - `barber.approved` → Notificar barbeiro

### 5.3 — Implementar canal Email (Resend)

- [ ] Integrar com API do Resend (HTTP REST)
- [ ] Criar templates Thymeleaf para cada tipo de notificação
- [ ] Templates: confirmação de agendamento, cancelamento, recibo de pagamento

### 5.4 — Implementar canal In-App (WebSocket)

- [ ] Configurar STOMP via Spring WebSocket
- [ ] Enviar notificações em tempo real para o frontend
- [ ] Frontend: conectar via SockJS + STOMP

### 5.5 — Implementar deduplicação (Redis)

- [ ] Gerar chave única para cada notificação (`eventType:entityId:timestamp`)
- [ ] Verificar no Redis antes de enviar (TTL 24h)
- [ ] Evitar envio duplicado se mensagem for reprocessada

### ✅ Checkpoint da Etapa 5

| Critério | Verificação |
|---|---|
| Sobe sem MySQL | Depende apenas de RabbitMQ + Redis |
| Recebe eventos do RabbitMQ | Criar agendamento → log de notificação |
| Email enviado | Receber email de confirmação via Resend |
| WebSocket funciona | Notificação aparece no frontend em tempo real |
| Deduplicação funciona | Mesmo evento 2x → 1 notificação |

---

## 💳 ETAPA 6 — payment-service (Pagamentos)

> **Duração:** 3 semanas  
> **Pré-requisito:** Etapa 3 concluída (schedule-service funcional)  
> **Resultado:** Pagamento online via Mercado Pago funcional em sandbox  
> **Banco:** `payment_db`

### 6.1 — Setup Mercado Pago

- [ ] Criar conta de Marketplace no Mercado Pago (modo Sandbox)
- [ ] Obter `ACCESS_TOKEN` de teste
- [ ] Adicionar SDK ao `pom.xml`:
  ```xml
  <dependency>
      <groupId>com.mercadopago</groupId>
      <artifactId>sdk-java</artifactId>
      <version>2.1.24</version>
  </dependency>
  ```

### 6.2 — Criar entidades

- [ ] `Transaction.java` — com campos do Mercado Pago (mp_payment_id, mp_preference_id, etc.)
- [ ] `PaymentWebhookLog.java` — log de webhooks para idempotência
- [ ] Enums: `PaymentStatus`, `PaymentMethod`

### 6.3 — Criar Feign Client para schedule-service

```java
@FeignClient(name = "schedule-service")
public interface ScheduleServiceClient {
    @GetMapping("/api/internal/appointments/{id}")
    AppointmentInfoDTO getAppointmentById(@PathVariable Long id);

    @PutMapping("/api/internal/appointments/{id}/payment")
    void updateAppointmentPayment(@PathVariable Long id, @RequestBody PaymentUpdateDTO dto);
}
```

### 6.4 — Implementar Checkout Pro

- [ ] Criar `PaymentServiceImpl`:
  1. Receber `CreatePaymentDTO` (appointmentId ou orderId)
  2. Buscar dados do agendamento via Feign
  3. Montar `PreferenceRequest` do Mercado Pago (itens, back_urls, notification_url, marketplace_fee)
  4. Chamar API do MP → receber `init_point` (URL de checkout)
  5. Salvar `Transaction` no banco
  6. Retornar URL de checkout ao frontend

### 6.5 — Implementar Webhook

- [ ] `WebhookController` → `POST /api/payments/webhook`
- [ ] Validar assinatura do MP
- [ ] Log em `payment_webhooks_log` (idempotência)
- [ ] Atualizar status da `Transaction`
- [ ] Publicar `PaymentApprovedEvent` no RabbitMQ
- [ ] `schedule-service` consome evento → atualiza appointment para CONFIRMED
- [ ] `notification-service` consome evento → envia confirmação

### 6.6 — Implementar PIX

- [ ] Criar pagamento PIX via API do MP
- [ ] Retornar QR Code e código copia-e-cola
- [ ] Frontend: exibir QR Code + polling de status

### 6.7 — Frontend: integrar checkout

- [ ] Instalar `@mercadopago/sdk-react`
- [ ] Criar componente de checkout (botão "Pagar")
- [ ] Redirecionar para checkout do MP ou exibir Checkout Bricks inline

### ✅ Checkpoint da Etapa 6

| Critério | Verificação |
|---|---|
| Checkout Pro funciona (sandbox) | Criar pagamento → redirecionar ao MP → pagar → webhook |
| PIX funciona | QR Code gerado → pagar em sandbox → webhook processa |
| Webhook idempotente | Mesmo webhook 2x → 1 processamento |
| Appointment atualizado | Após pagamento → status CONFIRMED |
| Notificação enviada | Email de confirmação de pagamento |

---

## 🛒 ETAPA 7 — product-service (e-Commerce)

> **Duração:** 3 semanas  
> **Pré-requisito:** Etapa 6 concluída (payment-service funcional)  
> **Resultado:** Venda de produtos com estoque e checkout integrado  
> **Banco:** `product_db`

### 7.1 — Criar entidades

- [ ] `Product.java` (nome, preço, foto, categoria, SKU, estoque)
- [ ] `Order.java` (customer, barbershop, status, total)
- [ ] `OrderItem.java` (product, quantidade, preço unitário)
- [ ] `StockMovement.java` (IN, OUT, ADJUSTMENT)
- [ ] Enums: `OrderStatus`, `ProductCategory`

### 7.2 — Implementar CRUD de Produtos

- [ ] Criar, editar, deletar, listar produtos (Dono)
- [ ] Listagem pública por barbearia (Cliente)
- [ ] Upload de foto do produto (Cloudinary)

### 7.3 — Implementar fluxo de pedido

```
1. Cliente monta carrinho (frontend, localStorage)
2. POST /api/orders → product-service cria Order + OrderItems
3. Verificar estoque → se insuficiente, rejeitar
4. Baixar estoque (StockMovement tipo OUT)
5. Feign → payment-service cria pagamento
6. Publicar OrderCreatedEvent no RabbitMQ
7. Após pagamento (webhook) → atualizar status do pedido para PAID
8. Dono atualiza: PREPARING → READY → DELIVERED
```

### 7.4 — Alertas de estoque

- [ ] Se `stock_quantity < min_stock` → publicar `StockLowEvent`
- [ ] `notification-service` consome → alerta ao dono

### 7.5 — Frontend

- [ ] Componente de vitrine de produtos na página da barbearia
- [ ] Carrinho de compras (Context API)
- [ ] Tela de checkout integrada ao Mercado Pago
- [ ] Painel do dono: gestão de produtos e pedidos

### ✅ Checkpoint da Etapa 7

| Critério | Verificação |
|---|---|
| CRUD de produtos funciona | Dono cria/edita/remove produtos |
| Pedido com pagamento funciona | Comprar → pagar → pedido atualizado |
| Estoque atualizado | Compra → estoque diminui |
| Alerta de estoque baixo | Estoque < mínimo → notificação ao dono |

---

## 🚀 ETAPA 8 — Frontend, Infra Produção e Go-Live

> **Duração:** 3 semanas  
> **Pré-requisito:** Todas as etapas anteriores concluídas  
> **Resultado:** Sistema em produção, acessível via domínio com SSL

### Semana 1 — Frontend completo

- [ ] Atualizar todos os componentes React para usar os novos endpoints
- [ ] Implementar telas novas: Dashboard, Produtos, Checkout, Notificações
- [ ] Implementar PWA (Service Worker para push notifications)
- [ ] Modo escuro (opcional, mas recomendado)
- [ ] Testes E2E com Playwright (login → agendar → pagar)

### Semana 2 — Infra de produção

- [ ] Criar Dockerfiles multi-stage para cada serviço
- [ ] Criar `docker-compose.prod.yml` com todas as configurações de produção
- [ ] Configurar Nginx como reverse proxy + SSL (Let's Encrypt)
- [ ] Configurar variáveis de ambiente (`.env`) com secrets reais
- [ ] Provisionar VPS (Hetzner/DigitalOcean: 4 vCPU, 8GB RAM, 80GB SSD)
- [ ] Configurar firewall (UFW: portas 22, 80, 443 apenas)
- [ ] Configurar backup automático (mysqldump diário)

### Semana 3 — Monitoramento e Go-Live

- [ ] Configurar Prometheus + Grafana
- [ ] Adicionar Spring Actuator + Micrometer em todos os serviços
- [ ] Criar dashboards no Grafana (saúde dos serviços, métricas JVM, latência)
- [ ] Configurar alertas (error rate > 5%, p99 > 2s)
- [ ] Configurar Rate Limiting no API Gateway (100 req/min por IP)
- [ ] Scan de segurança (dependências, headers HTTP)
- [ ] Deploy final + smoke test em produção
- [ ] Go-live! 🎉

### ✅ Checkpoint Final

| Critério | Status |
|---|---|
| Todos os 6 microserviços compilam e sobem | ⬜ |
| Testes unitários >80% cobertura | ⬜ |
| Testes de integração passam | ⬜ |
| Docker Compose produção funciona | ⬜ |
| SSL funcional (HTTPS) | ⬜ |
| Login funcional (email/senha) | ⬜ |
| Login Social funcional (Google) | ⬜ |
| Agendamento funcional E2E | ⬜ |
| Pagamento funcional (sandbox) | ⬜ |
| Notificações por email funcionais | ⬜ |
| E-commerce funcional | ⬜ |
| Dashboards com dados reais | ⬜ |
| Monitoramento (Prometheus + Grafana) | ⬜ |
| Rate limiting ativo | ⬜ |
| Swagger/OpenAPI acessível | ⬜ |
| README.md atualizado | ⬜ |

---

## 📊 Mapa Visual de Progresso

Use este mapa para acompanhar o progresso geral:

```
ETAPA 0 ████████████████████ [Fundação]         ⬜ Não iniciada
ETAPA 1 ████████████████████ [user-service]      ⬜ Não iniciada
ETAPA 2 ████████████████████ [barbershop-svc]    ⬜ Não iniciada
ETAPA 3 ████████████████████ [schedule-svc]      ⬜ Não iniciada
ETAPA 4 ████████████████████ [Limpeza/Testes]    ⬜ Não iniciada
ETAPA 5 ████████████████████ [notification-svc]  ⬜ Não iniciada
ETAPA 6 ████████████████████ [payment-svc]       ⬜ Não iniciada
ETAPA 7 ████████████████████ [product-svc]       ⬜ Não iniciada
ETAPA 8 ████████████████████ [Infra/Go-Live]     ⬜ Não iniciada

Legenda: ⬜ Não iniciada | 🟡 Em progresso | ✅ Concluída
```

---

## 🛡️ Regras de Ouro da Migração

1. **Nunca migre dois serviços em paralelo.** Termine um, valide, depois comece o próximo.
2. **Sempre valide que compila sozinho** (`./mvnw compile -pl {service}`) antes de avançar.
3. **Nunca faça FK entre bancos diferentes.** Use UUID externo + validação via Feign.
4. **Dados desnormalizados são seus amigos.** Copie nomes/preços no momento da criação para evitar Feign em toda listagem.
5. **Teste com o serviço dependente desligado.** O Circuit Breaker deve atuar graciosamente.
6. **Um commit por passo.** Facilita rollback se algo quebrar.
7. **Atualize este documento** a cada etapa concluída com a data e observações.

---

## 📁 Estrutura Final Esperada

```
backend/
├── pom.xml                          ← POM pai (multi-module)
├── mvnw / mvnw.cmd
├── src/                             ← LEGADO (referência, não compila)
│
├── discovery-service/               ← Eureka :8761
├── api-gateway/                     ← Gateway :8080
│
├── user-service/                    ← Auth/JWT :8081 (ETAPA 1)
│   └── src/main/java/.../userservice/
│       ├── config/
│       ├── controller/              ← + InternalUserController
│       ├── dto/
│       ├── exception/
│       ├── event/
│       ├── mapper/
│       ├── model/
│       ├── repository/
│       ├── service/impl/
│       ├── service/storage/
│       └── validador/
│
├── barbershop-service/              ← Barbearias :8082 (ETAPA 2)
│   └── src/main/java/.../barbershopservice/
│       ├── config/
│       ├── controller/              ← + InternalBarbershopController
│       ├── dto/
│       ├── exception/
│       ├── event/
│       ├── feign/                   ← UserServiceClient
│       ├── mapper/
│       ├── model/
│       ├── repository/
│       ├── service/impl/
│       ├── service/storage/
│       └── validator/
│
├── schedule-service/                ← Agendamentos :8083 (ETAPA 3)
│   └── src/main/java/.../scheduleservice/
│       ├── config/                  ← + RedisConfig, RabbitConfig
│       ├── controller/
│       ├── dto/
│       ├── exception/
│       ├── event/
│       ├── feign/                   ← UserServiceClient + BarbershopServiceClient
│       ├── mapper/
│       ├── model/
│       ├── repository/
│       └── service/impl/
│
├── notification-service/            ← Notificações :8085 (ETAPA 5)
│   └── src/main/java/.../notificationservice/
│       ├── config/
│       ├── controller/
│       ├── listener/                ← @RabbitListener
│       ├── service/channel/         ← EmailChannel, PushChannel
│       └── template/
│
├── payment-service/                 ← Pagamentos :8084 (ETAPA 6)
│   └── src/main/java/.../paymentservice/
│       ├── config/                  ← MercadoPagoConfig
│       ├── controller/              ← PaymentController + WebhookController
│       ├── feign/                   ← ScheduleServiceClient
│       └── service/impl/
│
└── product-service/                 ← e-Commerce :8086 (ETAPA 7)
    └── src/main/java/.../productservice/
        ├── controller/
        ├── feign/                   ← PaymentServiceClient
        └── service/impl/
```

---

> **Este guia é vivo.** Atualize o status de cada checkbox (⬜ → ✅) conforme avança.  
> Registre a data de conclusão de cada etapa e quaisquer desvios do plano.  
> Versione no Git: `git add GUIA_MIGRACAO_PASSO_A_PASSO.md && git commit -m "docs: atualizar progresso migração"`
