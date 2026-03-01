# 📋 RELATÓRIO DE TAREFAS — DEV 1

**Data:** 01/03/2026  
**Dev:** Dev 1  
**Branch:** `feat/schedule-service`  
**Escopo:** schedule-service (EXCLUSIVO) + 3 arquivos NOVOS no barbershop-service

---

## 📊 STATUS ATUAL

### O que JÁ FOI FEITO (neste chat)

| # | Item | Arquivo | Status |
|---|------|---------|--------|
| 1 | Etapa 0 — Infraestrutura | POMs, docker-compose, init.sql, application.yml, proxy Vite | ✅ |
| 2 | Etapa 1 — user-service completo | 30+ arquivos | ✅ |
| 3 | Etapa 2 — barbershop-service completo | 25+ arquivos | ✅ |
| 4 | Enum AppointmentStatus | `schedule-service/.../model/enums/AppointmentStatus.java` | ✅ |
| 5 | Model Appointment | `schedule-service/.../model/Appointment.java` | ✅ |
| 6 | Model AppointmentActivity | `schedule-service/.../model/AppointmentActivity.java` | ✅ |
| 7 | Model BarberBlock | `schedule-service/.../model/BarberBlock.java` | ✅ |
| 8 | POM atualizado (mapstruct, lombok) | `schedule-service/pom.xml` | ✅ |
| 9 | Application (@EnableJpaAuditing, @EnableCaching) | `ScheduleServiceApplication.java` | ✅ |
| 10 | RedisConfig | `schedule-service/.../config/RedisConfig.java` | ✅ |
| 11 | RabbitConfig | `schedule-service/.../config/RabbitConfig.java` | ✅ |
| 12 | 9 DTOs | `schedule-service/.../dto/` | ✅ |
| 13 | 3 Repositórios | `schedule-service/.../repository/` | ✅ |
| 14 | 2 Feign Clients | `schedule-service/.../feign/` | ✅ |
| 15 | 3 Events (records RabbitMQ) | `schedule-service/.../event/` | ✅ |
| 16 | 2 Exceptions | `schedule-service/.../exception/` | ✅ |
| 17 | AppointmentMapper | `schedule-service/.../mapper/AppointmentMapper.java` | ✅ |
| 18 | AppointmentService (lógica core) | `schedule-service/.../service/AppointmentService.java` | ✅ |
| 19 | BarberBlockService | `schedule-service/.../service/BarberBlockService.java` | ✅ |
| 20 | AppointmentController (9 endpoints) | `schedule-service/.../controller/AppointmentController.java` | ✅ |
| 21 | BarberBlockController (3 endpoints) | `schedule-service/.../controller/BarberBlockController.java` | ✅ |
| 22 | InternalAppointmentController | `schedule-service/.../controller/InternalAppointmentController.java` | ✅ |
| 23 | InternalBarbershopController | `barbershop-service/.../controller/InternalBarbershopController.java` | ✅ |
| 24 | BarbershopInfoDTO (record) | `barbershop-service/.../dto/BarbershopInfoDTO.java` | ✅ |
| 25 | ActivityInfoDTO (record) | `barbershop-service/.../dto/ActivityInfoDTO.java` | ✅ |

### O que JÁ EXISTE (scaffolding pronto)

| Item | Status |
|------|--------|
| `ScheduleServiceApplication.java` | ✅ Existe (`@SpringBootApplication` + `@EnableFeignClients`) |
| `application.yml` | ✅ Completo (MySQL, RabbitMQ, Redis, Eureka, Resilience4j) |
| `pom.xml` | ⚠️ Faltam: mapstruct, lombok, annotation processors |
| Diretórios scaffolded | ✅ config/, controller/, dto/, event/, exception/, feign/, mapper/, model/, repository/, service/ |
| Conteúdo dos diretórios | ❌ Apenas `.gitkeep` (exceto model/ e model/enums/) |

---

## 📝 O QUE FALTA FAZER — CHECKLIST COMPLETO

### BLOCO 1 — Configurações e POM (pré-requisito)

- [ ] **1.1** — Atualizar `pom.xml` do schedule-service
  - Adicionar: `mapstruct`, `lombok`
  - Adicionar: `maven-compiler-plugin` com annotation processors (Lombok + MapStruct)
  - **Arquivo:** `backend/schedule-service/pom.xml`

- [ ] **1.2** — Atualizar `ScheduleServiceApplication.java`
  - Adicionar: `@EnableJpaAuditing`, `@EnableCaching`
  - **Arquivo:** `backend/schedule-service/.../ScheduleServiceApplication.java`

- [ ] **1.3** — Criar `RedisConfig.java`
  - CacheManager com TTL padrão de 5 minutos
  - Serializers Jackson para Redis
  - **Arquivo:** `backend/schedule-service/.../config/RedisConfig.java`

- [ ] **1.4** — Criar `RabbitConfig.java`
  - Exchange: `cortaai.events` (tipo topic)
  - Routing keys: `appointment.created`, `appointment.cancelled`, `appointment.concluded`
  - Declarar TopicExchange + Jackson MessageConverter
  - **Arquivo:** `backend/schedule-service/.../config/RabbitConfig.java`

### BLOCO 2 — DTOs (9 arquivos)

Todos no diretório `backend/schedule-service/.../dto/`

- [ ] **2.1** — `CreateAppointmentDTO.java`
  - Campos: customerId (UUID), barberId (UUID), barbershopId (UUID), activityIds (List\<UUID\>), startTime (LocalDateTime)
  - Validações: @NotNull em todos

- [ ] **2.2** — `AppointmentDTO.java`
  - Response completo: id, customerId, barberId, barbershopId, customerName, barberName, barbershopName, startTime, endTime, totalPrice, status, activities (List\<AppointmentActivityDTO\>), dateCreated

- [ ] **2.3** — `AppointmentActivityDTO.java`
  - Campos: id, activityId, activityName, price, durationMinutes

- [ ] **2.4** — `TimeSlotDTO.java`
  - Campos: startTime (LocalDateTime), endTime (LocalDateTime), available (boolean)

- [ ] **2.5** — `CreateBarberBlockDTO.java`
  - Campos: barberId (UUID), startTime (LocalDateTime), endTime (LocalDateTime), reason (String)

- [ ] **2.6** — `BarberBlockDTO.java`
  - Response: id, barberId, startTime, endTime, reason, dateCreated

- [ ] **2.7** — `UserInfoDTO.java`
  - Recebido via Feign do user-service: id, name, email, userType, role, barbershopId, workStartTime, workEndTime, imageUrl

- [ ] **2.8** — `BarbershopInfoDTO.java`
  - Recebido via Feign do barbershop-service: id, ownerId, name, cnpj, address

- [ ] **2.9** — `ActivityInfoDTO.java`
  - Recebido via Feign do barbershop-service: id, activityName, price, durationMinutes, barbershopId

### BLOCO 3 — Repositórios (3 arquivos)

Todos no diretório `backend/schedule-service/.../repository/`

- [ ] **3.1** — `AppointmentRepository.java`
  - `hasConflict(barberId, startTime, endTime)` — @Query JPQL para verificar sobreposição
  - `findByCustomerIdOrderByStartTimeDesc(customerId)`
  - `findByBarberIdAndStartTimeBetween(barberId, start, end)`
  - `findByBarbershopIdAndStartTimeBetween(shopId, start, end)`

- [ ] **3.2** — `AppointmentActivityRepository.java`
  - JpaRepository simples (UUID)

- [ ] **3.3** — `BarberBlockRepository.java`
  - `findByBarberIdAndStartTimeBetween(barberId, start, end)`
  - `existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(barberId, end, start)` — verifica sobreposição

### BLOCO 4 — Feign Clients (2 arquivos)

Todos no diretório `backend/schedule-service/.../feign/`

- [ ] **4.1** — `UserServiceClient.java`
  - `@FeignClient(name = "user-service", path = "/api/internal/users")`
  - `GET /{id}` → UserInfoDTO
  - `GET /by-email/{email}` → UserInfoDTO

- [ ] **4.2** — `BarbershopServiceClient.java`
  - `@FeignClient(name = "barbershop-service", path = "/api/internal/barbershops")`
  - `GET /{id}` → BarbershopInfoDTO
  - `GET /{shopId}/activities?ids=` → List\<ActivityInfoDTO\>

### BLOCO 5 — Events (3 arquivos)

Todos no diretório `backend/schedule-service/.../event/`

- [ ] **5.1** — `AppointmentCreatedEvent.java`
  - record: appointmentId, customerId, barberId, barbershopId, customerName, barberName, barbershopName, startTime, totalPrice

- [ ] **5.2** — `AppointmentCancelledEvent.java`
  - record: appointmentId, customerId, barberId, cancelledBy

- [ ] **5.3** — `AppointmentConcludedEvent.java`
  - record: appointmentId, customerId, barberId, barbershopId

### BLOCO 6 — Exception (2 arquivos)

Todos no diretório `backend/schedule-service/.../exception/`

- [ ] **6.1** — `NotFoundException.java` — @ResponseStatus(404)
- [ ] **6.2** — `ConflictException.java` — @ResponseStatus(409) — para conflito de horário

### BLOCO 7 — Mapper (1 arquivo)

- [ ] **7.1** — `AppointmentMapper.java`
  - `Appointment` → `AppointmentDTO` (com lista de activities)
  - **Arquivo:** `backend/schedule-service/.../mapper/AppointmentMapper.java`

### BLOCO 8 — Services (2 arquivos)

Todos no diretório `backend/schedule-service/.../service/`

- [ ] **8.1** — `AppointmentService.java`
  - `createAppointment(String callerEmail, CreateAppointmentDTO dto)` → AppointmentDTO
    1. Feign → user-service: validar customerId (CUSTOMER)
    2. Feign → user-service: validar barberId (BARBER)
    3. Feign → barbershop-service: validar barbershopId + buscar activities por IDs
    4. Calcular totalDuration = soma(durationMinutes)
    5. Calcular endTime = startTime + totalDuration
    6. Verificar conflito (`hasConflict`)
    7. Verificar BarberBlock (barbeiro em folga?)
    8. Criar Appointment com snapshots desnormalizados
    9. Criar AppointmentActivities (snapshots)
    10. Publicar AppointmentCreatedEvent no RabbitMQ
    11. Retornar AppointmentDTO
  - `cancelAppointment(String callerEmail, UUID id)` → void
    1. Verificar que caller é customer, barber ou owner
    2. Status → CANCELLED
    3. Publicar AppointmentCancelledEvent
  - `concludeAppointment(String callerEmail, UUID id)` → void
    1. Verificar que caller é o barber
    2. Status → CONCLUDED
    3. Publicar AppointmentConcludedEvent
  - `confirmAppointment(String callerEmail, UUID id)` → void
  - `getAvailability(UUID barberId, LocalDate date)` → List\<TimeSlotDTO\>
    1. Feign → user-service: buscar workStartTime/workEndTime (@Cacheable Redis 5min)
    2. Buscar agendamentos do barber naquele dia
    3. Buscar bloqueios do barber naquele dia
    4. Calcular slots de 30min entre workStart e workEnd
    5. Marcar ocupados
  - `getAppointmentById(UUID id)` → AppointmentDTO
  - `getMyAppointments(String email)` → List\<AppointmentDTO\>
  - `getBarberSchedule(UUID barberId, LocalDate date)` → List\<AppointmentDTO\>
  - `getBarbershopSchedule(UUID shopId, LocalDate date)` → List\<AppointmentDTO\>

- [ ] **8.2** — `BarberBlockService.java`
  - `createBlock(String callerEmail, CreateBarberBlockDTO dto)` → BarberBlockDTO
  - `getBlocks(UUID barberId, LocalDate date)` → List\<BarberBlockDTO\>
  - `deleteBlock(String callerEmail, UUID blockId)` → void

### BLOCO 9 — Controllers (3 arquivos)

Todos no diretório `backend/schedule-service/.../controller/`

- [ ] **9.1** — `AppointmentController.java`
  ```
  POST   /api/appointments                              → Criar
  GET    /api/appointments/{id}                          → Detalhar
  GET    /api/appointments/my-appointments               → Meus (customer)
  GET    /api/appointments/barber/{barberId}?date=       → Agenda barbeiro
  GET    /api/appointments/barbershop/{shopId}?date=     → Agenda loja
  PUT    /api/appointments/{id}/cancel                   → Cancelar
  PUT    /api/appointments/{id}/conclude                 → Concluir
  PUT    /api/appointments/{id}/confirm                  → Confirmar
  GET    /api/appointments/availability?barberId=&date=  → Slots
  ```

- [ ] **9.2** — `BarberBlockController.java`
  ```
  POST   /api/appointments/barber-blocks                 → Criar
  GET    /api/appointments/barber-blocks?barberId=&date= → Listar
  DELETE /api/appointments/barber-blocks/{id}             → Remover
  ```

- [ ] **9.3** — `InternalAppointmentController.java` (para payment-service do Dev 2)
  ```
  GET /api/internal/appointments/{id}                → AppointmentDTO
  PUT /api/internal/appointments/{id}/payment-status → void (atualiza status)
  ```

### BLOCO 10 — InternalBarbershopController (3 arquivos NOVOS no barbershop-service)

> ⚠️ Estes são os ÚNICOS arquivos que o Dev 1 cria FORA do schedule-service.
> São arquivos NOVOS — não edita nenhum existente.

- [ ] **10.1** — `InternalBarbershopController.java`
  ```
  GET /api/internal/barbershops/{id}                    → BarbershopInfoDTO
  GET /api/internal/barbershops/{shopId}/activities?ids= → List<ActivityInfoDTO>
  ```
  - **Arquivo:** `backend/barbershop-service/.../controller/InternalBarbershopController.java`

- [ ] **10.2** — `BarbershopInfoDTO.java`
  - record: id (UUID), ownerId (UUID), name, cnpj, address
  - **Arquivo:** `backend/barbershop-service/.../dto/BarbershopInfoDTO.java`

- [ ] **10.3** — `ActivityInfoDTO.java`
  - record: id (UUID), activityName, price (BigDecimal), durationMinutes (Integer), barbershopId (UUID)
  - **Arquivo:** `backend/barbershop-service/.../dto/ActivityInfoDTO.java`

---

## 📊 RESUMO NUMÉRICO

| Bloco | Arquivos | Diretório |
|-------|----------|-----------|
| Configs e POM | 4 edições/criações | schedule-service: pom.xml, Application, config/ |
| DTOs | 9 arquivos novos | schedule-service/dto/ |
| Repositórios | 3 arquivos novos | schedule-service/repository/ |
| Feign Clients | 2 arquivos novos | schedule-service/feign/ |
| Events | 3 arquivos novos | schedule-service/event/ |
| Exceptions | 2 arquivos novos | schedule-service/exception/ |
| Mapper | 1 arquivo novo | schedule-service/mapper/ |
| Services | 2 arquivos novos | schedule-service/service/ |
| Controllers | 3 arquivos novos | schedule-service/controller/ |
| Barbershop (interno) | 3 arquivos novos | barbershop-service/controller/ e dto/ |
| **TOTAL** | **~32 arquivos** | |

---

## 🔄 ORDEM DE EXECUÇÃO RECOMENDADA

```
FASE 1 — Fundação (não depende de nada)
  1.1  POM (mapstruct, lombok)
  1.2  Application (@EnableJpaAuditing, @EnableCaching)
  1.3  RedisConfig
  1.4  RabbitConfig
  2.*  DTOs (9 arquivos)
  3.*  Repositórios (3 arquivos)
  5.*  Events (3 records)
  6.*  Exceptions (2 classes)

FASE 2 — Comunicação externa
  4.*  Feign Clients (2 arquivos)
  10.* InternalBarbershopController + DTOs no barbershop-service (3 arquivos)

FASE 3 — Lógica de negócio
  7.1  AppointmentMapper
  8.1  AppointmentService (lógica core — o mais complexo)
  8.2  BarberBlockService

FASE 4 — Exposição
  9.1  AppointmentController
  9.2  BarberBlockController
  9.3  InternalAppointmentController

FASE 5 — Validação
  ./mvnw compile -pl schedule-service
  Testes unitários
  Teste integração manual
```

---

## 📋 CONTRATO QUE O DEV 1 DEVE RESPEITAR

### Eventos que o Dev 1 PUBLICA (Dev 2 consome no notification-service)

| Evento | Routing Key | Payload |
|--------|------------|---------|
| AppointmentCreatedEvent | `appointment.created` | appointmentId, customerId, barberId, barbershopId, customerName, barberName, barbershopName, startTime, totalPrice |
| AppointmentCancelledEvent | `appointment.cancelled` | appointmentId, customerId, barberId, cancelledBy |
| AppointmentConcludedEvent | `appointment.concluded` | appointmentId, customerId, barberId, barbershopId |

### Endpoints internos que o Dev 1 CRIA (Dev 2 consome no payment-service)

| Método | Endpoint | Response |
|--------|----------|----------|
| GET | `/api/internal/appointments/{id}` | AppointmentDTO |
| PUT | `/api/internal/appointments/{id}/payment-status` | void |

### Endpoints internos que o Dev 1 USA (já existem)

| Método | Endpoint | Serviço | Existe? |
|--------|----------|---------|---------|
| GET | `/api/internal/users/{id}` | user-service | ✅ Já criado |
| GET | `/api/internal/users/by-email/{email}` | user-service | ✅ Já criado |
| GET | `/api/internal/barbershops/{id}` | barbershop-service | ❌ Dev 1 deve criar |
| GET | `/api/internal/barbershops/{shopId}/activities?ids=` | barbershop-service | ❌ Dev 1 deve criar |

---

## ⚠️ REGRAS PARA O DEV 1

1. **Não toque** em `frontend/`, `notification-service/`, `payment-service/`, `product-service/`, `api-gateway/`
2. No `barbershop-service/`, apenas CRIE os 3 arquivos novos (Bloco 10) — não edite nada existente
3. Use `@Cacheable("barberWorkHours")` para cache Redis de horários do barbeiro
4. Todos os IDs cross-service são UUID simples (sem @ManyToOne)
5. Dados desnormalizados (nomes, preços) são copiados na CRIAÇÃO — nunca buscar via Feign para listar
6. Exchange RabbitMQ: `cortaai.events` (tipo TopicExchange)
7. Commits: `feat(schedule-service): descrição`
8. Antes de push: `./mvnw compile -pl schedule-service` sem erros

