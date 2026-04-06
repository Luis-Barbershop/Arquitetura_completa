# 📋 PROGRESSO DA MIGRAÇÃO — CortaAí (Monólito → Microserviços)

**Última atualização:** 28/02/2026  
**Responsável:** Dev 1  
**Status Geral:** ETAPAS 0, 1 e 2 concluídas | ETAPAS 3, 4, 5 pendentes

---

## 📊 VISÃO GERAL

| Etapa | Serviço | Status | Descrição |
|-------|---------|--------|-----------|
| **0** | Infraestrutura | ✅ CONCLUÍDO | POMs, docker-compose, application.yml, proxy Vite |
| **1** | user-service | ✅ CONCLUÍDO | Auth, JWT, CRUD Customer/Barber, endpoints internos |
| **2** | barbershop-service | ✅ CONCLUÍDO | CRUD Barbershop, Activities, JoinRequests, Highlights |
| **3** | schedule-service | ⏳ PENDENTE | Agendamentos, slots, Redis cache |
| **4** | payment-service | ⏳ PENDENTE | Mercado Pago, webhooks |
| **5** | notification-service | ⏳ PENDENTE | Email (Resend), Push (FCM) |
| **6** | product-service | ⏳ PENDENTE | Catálogo de produtos |
| **7** | Frontend | ⏳ PENDENTE | Adaptar chamadas para novo Gateway |

---

## ✅ ETAPA 0 — INFRAESTRUTURA

### 0.1 — POM pai (`backend/pom.xml`)
- Versões centralizadas: Spring Boot `3.3.4`, Spring Cloud `2023.0.1`, Java `17`, MapStruct `1.5.5.Final`
- Todos os 7 módulos listados em `<modules>`
- `dependencyManagement` com Spring Cloud BOM

### 0.2 — POMs dos módulos padronizados
- Todos herdam de `<parent>cortaai:0.1</parent>`
- **user-service** teve `<parent>` corrigido (antes apontava para `spring-boot-starter-parent`)

### 0.3 — `docker-compose.yml`
- Serviços: `cortaai-mysql`, `cortaai-rabbitmq`, `cortaai-redis`
- Todos com `healthcheck` configurado
- Environment variables padronizadas: `SPRING_DATASOURCE_URL`, `EUREKA_HOST`, `RABBITMQ_HOST`, `REDIS_HOST`
- Volume `./init.sql` mapeado

### 0.4 — `init.sql`
- 5 schemas criados: `user_db`, `barbershop_db`, `schedule_db`, `payment_db`, `product_db`

### 0.5 — Proxy Vite (`frontend/vite.config.js`)
- Proxy `/api` → `http://localhost:8080` (Gateway)

### 0.6 — `application.yml` de todos os serviços parametrizados
Todos os 7 serviços agora usam variáveis de ambiente com defaults para dev local:
- `${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/xxx_db?...}`
- `${EUREKA_HOST:localhost}`
- `${RABBITMQ_HOST:localhost}`
- `${REDIS_HOST:localhost}`

**Serviços afetados:** user-service, barbershop-service, schedule-service, payment-service, notification-service, product-service, api-gateway

---

## ✅ ETAPA 1 — user-service (porta 8081)

### 1.1 — Imports cruzados eliminados
**Problema:** O user-service importava classes do pacote do monólito (`ifsp.edu.projeto.cortaai.dto.*`, `ifsp.edu.projeto.cortaai.service.*`, `ifsp.edu.projeto.cortaai.events.*`, etc.)

**Solução:** Criadas as seguintes classes DENTRO do pacote `ifsp.edu.projeto.cortaai.userservice`:

| Classe criada | Pacote | Origem |
|---|---|---|
| `NotFoundException.java` | `exception` | Copiada do monólito |
| `ReferenceException.java` | `exception` | Copiada do monólito |
| `BeforeDeleteCustomer.java` | `event` | Copiada do monólito |
| `BeforeDeleteBarber.java` | `event` | Copiada do monólito |
| `UploadResultDTO.java` | `dto` | Copiada do monólito |
| `StorageService.java` (interface) | `service.storage` | Copiada do monólito |
| `CloudinaryStorageServiceImpl.java` | `service.storage` | Copiada do monólito |
| `CloudinaryConfig.java` | `config` | Copiada do monólito |

**Imports corrigidos em:**
- `CustomerController.java` → agora importa de `userservice.dto.*` e `userservice.service.*`
- `JwtAuthorizationFilter.java` → `userservice.service.JwtTokenService`
- `JwtTokenServiceImpl.java` → `userservice.service.JwtTokenService`
- `CustomerServiceImpl.java` → `userservice.event.*`, `userservice.exception.*`, `userservice.service.storage.*`
- `BarberEmailUnique.java` → `userservice.service.impl.BarberServiceImpl`
- `CustomerDocumentCPFUnique.java` → `userservice.service.impl.CustomerServiceImpl`
- `CustomerEmailUnique.java` → `userservice.service.impl.CustomerServiceImpl`

**Resultado:** `grep -rn "cortaai\." | grep -v "cortaai\.userservice"` → ZERO resultados

### 1.2 — Diretórios com nomes errados corrigidos
- **` dto/` (com espaço)** → Conteúdo migrado para `dto/` (sem espaço). Diretório antigo deletado.
- **`validador/`** → Conteúdo migrado para `validator/` (sem acento, alinhado com `package` declaration). Diretório antigo deletado.
- **`target/`** (dentro de userservice) → Deletado (não deveria existir ali).

### 1.3 — Model Barber completado
Novos campos adicionados à entidade `Barber.java`:
```java
@Column(name = "work_start_time")
private LocalTime workStartTime;

@Column(name = "work_end_time")
private LocalTime workEndTime;

@Column(name = "image_url")
private String imageUrl;

@Column(name = "image_url_public_id")
private String imageUrlPublicId;
```

### 1.4 — BarberDTO e CreateBarberDTO atualizados
- `BarberDTO` (record) → adicionados `workStartTime`, `workEndTime`, `imageUrl`
- `CreateBarberDTO` (record) → adicionados `workStartTime`, `workEndTime`
- `LoginResponseDTO` (record) → `@Builder`, campo `userData` (Object) para CustomerService

### 1.5 — BarberMapper corrigido
- Removida referência a `barbershop` aninhado (não existe mais, Barber tem `barbershopId` direto como UUID)
- Removido método `toEntity()` (não necessário)

### 1.6 — BarberService/BarberServiceImpl ampliados
Novos métodos adicionados:
```java
BarberDTO get(UUID id);
boolean emailExists(String email);
boolean documentCPFExists(String documentCPF);
boolean tellExists(String tell);
```

### 1.7 — BarberRepository ampliado
Novos métodos:
```java
boolean existsByEmailIgnoreCase(String email);
boolean existsByDocumentCPFIgnoreCase(String documentCPF);
boolean existsByTellIgnoreCase(String tell);
```

### 1.8 — CustomerRepository ampliado
Novos métodos:
```java
boolean existsByEmailIgnoreCase(String email);
boolean existsByDocumentCPFIgnoreCase(String documentCPF);
boolean existsByTellIgnoreCase(String tell);
```

### 1.9 — BarberController corrigido
- `@PathVariable Long id` → `@PathVariable UUID id`
- `/signup` → `/register`
- Adicionado `GET /api/barbers/barbershop/{barbershopId}`

### 1.10 — SecurityConfig atualizado
Endpoints públicos:
```
POST /api/customers/register
POST /api/customers/login
GET  /api/barbers/**
POST /api/barbers/register
POST /api/barbers/login
/api/internal/**  (inter-serviço)
```

### 1.11 — CustomUserDetailsService RECRIADO
**Problema:** Arquivo estava com conteúdo duplicado do BarberServiceImpl.
**Solução:** Recriado como `UserDetailsService` correto que busca Customer e Barber por email.

### 1.12 — UserServiceApplication.java RECRIADO
**Problema:** Arquivo estava corrompido com conteúdo YAML.
**Solução:** Recriado com `@SpringBootApplication` + `@EnableJpaAuditing`.

### 1.13 — InternalUserController criado (NOVO)
Endpoints inter-serviço (NÃO expostos pelo Gateway):
```
GET  /api/internal/users/{id}            → UserInfoDTO
GET  /api/internal/users/by-email/{email} → UserInfoDTO  
PUT  /api/internal/users/{id}/barbershop  → atualiza barbershopId do Barber
```

### 1.14 — UserInfoDTO criado (NOVO)
```java
public record UserInfoDTO(
    UUID id, String name, String email, String userType, 
    String role, UUID barbershopId, LocalTime workStartTime, 
    LocalTime workEndTime, String imageUrl
) {}
```

### Estrutura final do user-service:
```
user-service/src/main/java/.../userservice/
├── UserServiceApplication.java          ← RECRIADO
├── config/
│   ├── CloudinaryConfig.java            ← NOVO
│   ├── JwtAuthorizationFilter.java      ← IMPORT CORRIGIDO
│   ├── SecurityConfig.java              ← ATUALIZADO
│   └── WebConfig.java
├── controller/
│   ├── BarberController.java            ← CORRIGIDO (Long→UUID, signup→register)
│   ├── CustomerController.java          ← IMPORTS CORRIGIDOS
│   ├── HomeController.java
│   └── InternalUserController.java      ← NOVO
├── dto/
│   ├── BarberDTO.java                   ← ATUALIZADO (novos campos)
│   ├── BarberInfoDTO.java
│   ├── CreateBarberDTO.java             ← ATUALIZADO (record + novos campos)
│   ├── CustomerCreateDTO.java
│   ├── CustomerDTO.java
│   ├── LoginDTO.java
│   ├── LoginResponseDTO.java            ← ATUALIZADO (@Builder, userData)
│   ├── UpdateBarberDTO.java
│   ├── UploadResultDTO.java             ← NOVO
│   └── UserInfoDTO.java                 ← NOVO
├── event/
│   ├── BeforeDeleteBarber.java          ← NOVO
│   └── BeforeDeleteCustomer.java        ← NOVO
├── exception/
│   ├── NotFoundException.java           ← NOVO
│   └── ReferenceException.java          ← NOVO
├── mapper/
│   ├── BarberMapper.java                ← CORRIGIDO
│   └── CustomerMapper.java
├── model/
│   ├── Barber.java                      ← ATUALIZADO (4 novos campos)
│   ├── Customer.java
│   └── enums/ (AppointmentStatus, BarberSkills, JoinRequestStatus — legado)
├── repository/
│   ├── BarberRepository.java            ← ATUALIZADO (3 novos métodos)
│   └── CustomerRepository.java          ← ATUALIZADO (3 novos métodos)
├── service/
│   ├── BarberService.java               ← ATUALIZADO (4 novos métodos)
│   ├── CustomerService.java
│   ├── JwtTokenService.java
│   ├── impl/
│   │   ├── BarberServiceImpl.java       ← ATUALIZADO
│   │   ├── CustomUserDetailsService.java ← RECRIADO
│   │   ├── CustomerServiceImpl.java     ← IMPORTS CORRIGIDOS
│   │   └── JwtTokenServiceImpl.java     ← IMPORT CORRIGIDO
│   └── storage/
│       ├── CloudinaryStorageServiceImpl.java ← NOVO
│       └── StorageService.java          ← NOVO
└── validator/                           ← RENOMEADO de "validador"
    ├── BarberDocumentCPFUnique.java
    ├── BarberEmailUnique.java
    ├── BarberTellUnique.java
    ├── CPF.java
    ├── CPFValidator.java
    ├── CustomerDocumentCPFUnique.java
    ├── CustomerEmailUnique.java
    └── CustomerTellUnique.java
```

---

## ✅ ETAPA 2 — barbershop-service (porta 8082)

### Mudanças arquiteturais em relação ao monólito:

| Aspecto | Monólito | Microserviço |
|---|---|---|
| Dono da barbearia | `@ManyToOne Barber` (JPA) | `UUID ownerId` (desacoplado) |
| Barbeiros da barbearia | `@OneToMany Set<Barber>` | Removido (vive no user-service) |
| JoinRequest → Barber | `@ManyToOne Barber` | `UUID barberId` (desacoplado) |
| JoinRequest ID | `Long` (auto-increment) | `UUID` (UuidGenerator) |
| Agendamentos | `@OneToMany Set<Appointments>` | Removido (vive no schedule-service) |
| Resolução de usuários | Acesso direto ao DB | Feign Client → user-service |
| Atualizar barbershopId | `barber.setBarbershopId()` local | Feign `PUT /api/internal/users/{id}/barbershop` |

### Arquivos criados:

**Models (4):**
- `Barbershop.java` — com `ownerId` UUID, sem relações cross-boundary
- `Activity.java` — sem `Set<Barber>`, sem `Set<Appointments>`
- `BarbershopJoinRequest.java` — com `barberId` UUID, sem `@ManyToOne Barber`
- `BarbershopHighlight.java`

**Enum (1):**
- `JoinRequestStatus.java` — `PENDING`, `APPROVED`, `REJECTED` (adicionado `APPROVED`)

**DTOs (11):**
- `BarbershopDTO.java` (com `ownerId`)
- `CreateBarbershopDTO.java`
- `UpdateBarbershopDTO.java`
- `ActivityDTO.java`
- `CreateActivityDTO.java`
- `UpdateActivityDTO.java`
- `BarberJoinRequestDTO.java`
- `JoinRequestDTO.java` (com `barberId`, `barberName`, `barberEmail`)
- `CloseBarbershopRequestDTO.java`
- `UploadResultDTO.java`
- `UserInfoDTO.java` (recebido via Feign do user-service)

**Repositories (4):**
- `BarbershopRepository.java` (`findByCnpj`, `findByOwnerId`, `existsByCnpj`)
- `ActivityRepository.java` (`findByBarbershopId`)
- `BarbershopJoinRequestRepository.java` (`findByBarbershopIdAndStatus`, `findByBarberIdAndBarbershopId`, `findByBarberId`)
- `BarbershopHighlightRepository.java` (`findByBarbershopId`)

**Mappers (2):**
- `BarbershopMapper.java` (com `mapHighlights` para `Set<BarbershopHighlight>` → `List<String>`)
- `ActivityMapper.java` (com `barbershop.id` → `barbershopId`)

**Feign Client (1):**
- `UserServiceClient.java` — `@FeignClient(name = "user-service", path = "/api/internal/users")`
  - `getUserById(UUID id)`
  - `getUserByEmail(String email)`
  - `updateUserBarbershopId(UUID id, UUID barbershopId)`

**Service (1):**
- `BarbershopService.java` — TODA a lógica migrada:
  - Leitura: `listBarbershops()`, `listActivities()`, `getBarbershop()`
  - Dono: `createBarbershop()`, `updateBarbershop()`, `createActivity()`, `updateActivity()`, `deleteActivity()`, `removeBarber()`, `closeBarbershop()`
  - Join: `requestToJoinBarbershop()`, `getPendingJoinRequests()`, `approveJoinRequest()`
  - Sair: `freeBarber()`
  - Imagens: `updateBarbershopLogo()`, `updateBarbershopBanner()`, `updateActivityPhoto()`, `addBarbershopHighlight()`, `deleteBarbershopHighlight()`

**Controller (1):**
- `BarbershopController.java` — todos os endpoints em `/api/barbershops`

**Exceptions (2):**
- `NotFoundException.java`
- `ForbiddenException.java`

**Config (1):**
- `CloudinaryConfig.java`

**Storage (2):**
- `StorageService.java` (interface)
- `CloudinaryStorageServiceImpl.java`

**POM atualizado:**
- Adicionadas dependências: `cloudinary-http44`, `mapstruct`, `lombok`
- Adicionado `maven-compiler-plugin` com annotation processors (Lombok + MapStruct)

**Application atualizada:**
- `@EnableJpaAuditing` adicionado
- `@EnableFeignClients` já existia

### Endpoints do barbershop-service:

```
# Leitura pública
GET    /api/barbershops                              → List<BarbershopDTO>
GET    /api/barbershops/{shopId}                     → BarbershopDTO
GET    /api/barbershops/{shopId}/activities           → List<ActivityDTO>

# Gestão do Dono (requer auth)
POST   /api/barbershops/register-my-shop             → BarbershopDTO (multipart)
PUT    /api/barbershops/my-shop                       → BarbershopDTO
POST   /api/barbershops/my-shop/activities            → ActivityDTO
PUT    /api/barbershops/my-shop/activities/{id}       → ActivityDTO
DELETE /api/barbershops/my-shop/activities/{id}       → 204
DELETE /api/barbershops/my-shop/remove-barber/{id}    → 204
DELETE /api/barbershops/my-shop/close                 → 204

# Join Requests (requer auth)
POST   /api/barbershops/join-request                  → 202
GET    /api/barbershops/my-shop/pending-requests      → List<JoinRequestDTO>
POST   /api/barbershops/my-shop/approve-request/{id}  → 204

# Sair da loja (requer auth)
POST   /api/barbershops/leave-shop                    → 204

# Imagens (requer auth)
POST   /api/barbershops/my-shop/upload-logo           → String (URL)
POST   /api/barbershops/my-shop/upload-banner         → String (URL)
POST   /api/barbershops/my-shop/activities/{id}/upload-photo → String (URL)
POST   /api/barbershops/my-shop/highlights            → String (URL)
DELETE /api/barbershops/my-shop/highlights/{id}       → 204
```

### Estrutura final do barbershop-service:
```
barbershop-service/src/main/java/.../barbershopservice/
├── BarbershopServiceApplication.java
├── config/
│   └── CloudinaryConfig.java
├── controller/
│   └── BarbershopController.java
├── dto/
│   ├── ActivityDTO.java
│   ├── BarberJoinRequestDTO.java
│   ├── BarbershopDTO.java
│   ├── CloseBarbershopRequestDTO.java
│   ├── CreateActivityDTO.java
│   ├── CreateBarbershopDTO.java
│   ├── JoinRequestDTO.java
│   ├── UpdateActivityDTO.java
│   ├── UpdateBarbershopDTO.java
│   ├── UploadResultDTO.java
│   └── UserInfoDTO.java
├── exception/
│   ├── ForbiddenException.java
│   └── NotFoundException.java
├── feign/
│   └── UserServiceClient.java
├── mapper/
│   ├── ActivityMapper.java
│   └── BarbershopMapper.java
├── model/
│   ├── Activity.java
│   ├── Barbershop.java
│   ├── BarbershopHighlight.java
│   ├── BarbershopJoinRequest.java
│   └── enums/
│       └── JoinRequestStatus.java
├── repository/
│   ├── ActivityRepository.java
│   ├── BarbershopHighlightRepository.java
│   ├── BarbershopJoinRequestRepository.java
│   └── BarbershopRepository.java
└── service/
    ├── BarbershopService.java
    └── storage/
        ├── CloudinaryStorageServiceImpl.java
        └── StorageService.java
```

---

## ⏳ ETAPAS PENDENTES

### ETAPA 3 — schedule-service (porta 8083)
- Migrar `Appointments` do monólito
- Criar Feign Clients para user-service e barbershop-service
- Implementar cache de slots com Redis
- Lógica de verificação de conflito de horário

### ETAPA 4 — payment-service (porta 8084)
- Integração Mercado Pago
- Webhooks de pagamento
- Listener RabbitMQ para eventos de agendamento

### ETAPA 5 — notification-service (porta 8085)
- Integração Resend (email transacional)
- Integração Firebase Cloud Messaging (push)
- Listener RabbitMQ para eventos

### ETAPA 6 — product-service (porta 8086)
- Catálogo de produtos da barbearia
- CRUD simples

### ETAPA 7 — Frontend
- Adaptar chamadas `api.js` para novo Gateway
- Verificar rotas e interceptors

---

## ⚙️ PORTAS DOS SERVIÇOS

| Serviço | Porta | Status |
|---------|-------|--------|
| api-gateway | 8080 | ✅ Configurado |
| user-service | 8081 | ✅ Migrado |
| barbershop-service | 8082 | ✅ Migrado |
| schedule-service | 8083 | ⏳ Pendente |
| payment-service | 8084 | ⏳ Pendente |
| notification-service | 8085 | ⏳ Pendente |
| product-service | 8086 | ⏳ Pendente |
| discovery-service | 8761 | ✅ Configurado |
| MySQL | 3306 | ✅ Configurado |
| RabbitMQ | 5672/15672 | ✅ Configurado |
| Redis | 6379 | ✅ Configurado |

---

## 🔗 COMUNICAÇÃO INTER-SERVIÇO

```
barbershop-service  ──Feign──►  user-service (/api/internal/users)
schedule-service    ──Feign──►  user-service (/api/internal/users)     [PENDENTE]
schedule-service    ──Feign──►  barbershop-service (/api/internal/*)   [PENDENTE]
payment-service     ──RabbitMQ──►  schedule-service                    [PENDENTE]
notification-service ──RabbitMQ──►  (escuta eventos)                   [PENDENTE]
```

**Regra:** Rotas `/api/internal/**` NÃO são expostas pelo API Gateway.

---

## 📝 NOTAS PARA O PRÓXIMO DEV

1. **A pasta `backend/src/` é o MONÓLITO antigo.** Não altere ela — é referência para migração.
2. **Cada microserviço é independente.** Sem imports cruzados entre módulos.
3. **Feign Clients usam Eureka** para service discovery (`lb://user-service`).
4. **MapStruct + Lombok** precisam do plugin `maven-compiler-plugin` com annotation processors configurados no POM de cada módulo.
5. **Cloudinary** precisa da env var `CLOUDINARY_URL` — tem um default placeholder para dev.
6. **JWT** é gerado pelo user-service e validado localmente em cada serviço (shared secret via `JWT_SECRET_KEY`).
7. **O barbershop-service ainda não tem SecurityConfig/JWT filter** — precisa ser adicionado se endpoints autenticados forem chamados diretamente (via Gateway, o JWT é propagado no header).

