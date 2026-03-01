# 📦 CortaAí — Documento de Componentes Criados

> **Data:** 28 de fevereiro de 2026  
> **Objetivo:** Documentar cada componente da nova arquitetura de microsserviços, detalhando arquivos criados, dependências, configurações e estrutura de pacotes.

---

## Sumário

1. [POM Pai (Multi-Module Maven)](#1-pom-pai-multi-module-maven)
2. [Infraestrutura (Docker Compose)](#2-infraestrutura-docker-compose)
3. [Banco de Dados (init.sql)](#3-banco-de-dados-initsql)
4. [discovery-service](#4-discovery-service)
5. [api-gateway](#5-api-gateway)
6. [user-service](#6-user-service)
7. [barbershop-service](#7-barbershop-service)
8. [schedule-service](#8-schedule-service)
9. [payment-service](#9-payment-service)
10. [notification-service](#10-notification-service)
11. [product-service](#11-product-service)

---

## 1. POM Pai (Multi-Module Maven)

**Arquivo:** `backend/pom.xml`

O POM pai orquestra todos os módulos e centraliza versões de dependências.

| Propriedade | Valor |
|---|---|
| **GroupId** | `ifsp.edu.projeto` |
| **ArtifactId** | `cortaai` |
| **Versão** | `0.1` |
| **Packaging** | `pom` |
| **Java** | 17 |
| **Spring Boot** | 3.3.4 |
| **Spring Cloud** | 2023.0.1 |
| **MapStruct** | 1.5.5.Final |

### Módulos Registrados
```xml
<modules>
    <module>discovery-service</module>
    <module>api-gateway</module>
    <module>user-service</module>
    <module>barbershop-service</module>
    <module>schedule-service</module>
    <module>payment-service</module>
    <module>notification-service</module>
    <module>product-service</module>
</modules>
```

### Dependências Globais (herdadas por todos os módulos)
| Dependência | Escopo |
|---|---|
| `lombok` | compile (optional) |
| `mapstruct` | compile |
| `spring-boot-starter-test` | test |

### Annotation Processors (maven-compiler-plugin)
- `lombok`
- `mapstruct-processor`
- `lombok-mapstruct-binding` (permite Lombok + MapStruct coexistirem)

---

## 2. Infraestrutura (Docker Compose)

**Arquivo:** `docker-compose.yml`

### Containers de Infraestrutura

| Container | Imagem | Porta(s) | Healthcheck | Papel |
|---|---|---|---|---|
| `cortaai-mysql` | `mysql:8.0` | 3306 | `mysqladmin ping` | Banco de dados (5 schemas) |
| `cortaai-rabbitmq` | `rabbitmq:3-management` | 5672, 15672 | `rabbitmqctl status` | Message broker (eventos assíncronos) |
| `cortaai-redis` | `redis:7-alpine` | 6379 | `redis-cli ping` | Cache + pub/sub |

### Containers de Serviço

| Container | Porta | Depende de | Env Vars |
|---|---|---|---|
| `discovery-service` | 8761 | — | — |
| `api-gateway` | 8080 | discovery | — |
| `user-service` | — | db ✅, discovery | DATASOURCE (user_db) |
| `barbershop-service` | — | db ✅, discovery, rabbitmq ✅ | DATASOURCE (barbershop_db) |
| `schedule-service` | — | db ✅, discovery, rabbitmq ✅, redis ✅ | DATASOURCE (schedule_db) |
| `payment-service` | — | db ✅, discovery, rabbitmq ✅ | DATASOURCE (payment_db) |
| `notification-service` | — | discovery, rabbitmq ✅, redis ✅ | — |
| `product-service` | — | db ✅, discovery, rabbitmq ✅ | DATASOURCE (product_db) |
| `cortaai-web` | 5173 | — | — |

> ✅ = `condition: service_healthy` (aguarda healthcheck passar antes de subir)

### Grafo de Dependências
```
                    ┌──────────┐
                    │  MySQL   │ ← healthcheck
                    └────┬─────┘
                         │
         ┌───────────────┼───────────────────────────────┐
         │               │                               │
         ▼               ▼                               ▼
   ┌───────────┐  ┌────────────┐  ┌───────────┐  ┌───────────┐
   │ RabbitMQ  │  │ Discovery  │  │   Redis   │  │ Frontend  │
   │ (healthy) │  │  (Eureka)  │  │ (healthy) │  │  (React)  │
   └─────┬─────┘  └──────┬─────┘  └─────┬─────┘  └───────────┘
         │               │              │
         │    ┌──────────┼──────────────┤
         │    │          │              │
         ▼    ▼          ▼              ▼
   ┌─────────────┐ ┌──────────┐ ┌──────────────┐
   │ API Gateway │ │user-svc  │ │schedule-svc  │ ← depende dos 3
   └─────────────┘ └──────────┘ └──────────────┘
                   ┌──────────┐ ┌──────────────┐
                   │barber-svc│ │payment-svc   │ ← db + rabbit + discovery
                   └──────────┘ └──────────────┘
                   ┌──────────┐ ┌──────────────┐
                   │notif-svc │ │product-svc   │
                   └──────────┘ └──────────────┘
```

---

## 3. Banco de Dados (init.sql)

**Arquivo:** `init.sql`

Executado automaticamente na inicialização do container MySQL:

```sql
CREATE DATABASE IF NOT EXISTS user_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS barbershop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS schedule_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- notification-service usa Redis (sem schema MySQL)
```

| Schema | Serviço Responsável | DDL |
|---|---|---|
| `user_db` | user-service | Hibernate `ddl-auto: update` |
| `barbershop_db` | barbershop-service | Hibernate `ddl-auto: update` |
| `schedule_db` | schedule-service | Hibernate `ddl-auto: update` |
| `payment_db` | payment-service | Hibernate `ddl-auto: update` |
| `product_db` | product-service | Hibernate `ddl-auto: update` |

---

## 4. discovery-service

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8761 |
| **Spring Name** | `discovery-service` |
| **Parent POM** | `cortaai:0.1` (herda do pai) |
| **Pacote base** | `ifsp.edu.projeto.cortaai.discoveryservice` |
| **Classe main** | `DiscoveryServiceApplication` |
| **Anotações** | `@SpringBootApplication`, `@EnableEurekaServer` |
| **Banco** | Nenhum |

### Dependências
| Dependência | Função |
|---|---|
| `spring-cloud-starter-netflix-eureka-server` | Eureka Server para registro de serviços |
| *(herdadas do pai)* | lombok, mapstruct, test |

### Configuração (application.yml)
```yaml
eureka:
  client:
    register-with-eureka: false   # Não se registra em si mesmo
    fetch-registry: false         # Não busca registro de outros
  server:
    enable-self-preservation: false  # Desliga self-preservation (dev)
```

### Estrutura de Diretórios
```
discovery-service/
├── pom.xml
├── src/main/java/.../discoveryservice/
│   └── DiscoveryServiceApplication.java
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/.../discoveryservice/
    └── resources/
```

---

## 5. api-gateway

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8080 |
| **Spring Name** | `api-gateway` |
| **Parent POM** | `cortaai:0.1` |
| **Pacote base** | `ifsp.edu.projeto.cortaai.apigateway` |
| **Classe main** | `ApiGatewayApplication` |
| **Anotações** | `@SpringBootApplication` |
| **Banco** | Nenhum |

### Dependências
| Dependência | Função |
|---|---|
| `spring-cloud-starter-gateway` | Spring Cloud Gateway (roteamento reativo) |
| `spring-cloud-starter-netflix-eureka-client` | Registra-se no Eureka para descoberta |

### Tabela de Rotas (application.yml)

| Route ID | Path Predicate | Destino (lb://) |
|---|---|---|
| `user-service` | `/api/customers/**`, `/api/barbers/**`, `/api/auth/**` | `lb://user-service` |
| `barbershop-service` | `/api/barbershops/**` | `lb://barbershop-service` |
| `schedule-service` | `/api/appointments/**` | `lb://schedule-service` |
| `payment-service` | `/api/payments/**` | `lb://payment-service` |
| `product-service` | `/api/products/**`, `/api/orders/**` | `lb://product-service` |
| `notification-service` | `/api/notifications/**` | `lb://notification-service` |

> `lb://` = Load Balanced — resolve o nome via Eureka e faz client-side load balancing.

### Configuração Eureka
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://discovery:8761/eureka/
```

### Estrutura de Diretórios
```
api-gateway/
├── pom.xml
├── src/main/java/.../apigateway/
│   ├── ApiGatewayApplication.java
│   ├── config/          # Configs do Gateway (CORS, Rate Limiting, etc.)
│   ├── filter/          # Filtros customizados (JWT validation, logging)
│   └── exception/       # GlobalExceptionHandler
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/.../apigateway/
    └── resources/
```

### Para que serve cada subpacote

| Pacote | Responsabilidade |
|---|---|
| `config/` | Configuração de CORS global, Rate Limiting, Security do Gateway |
| `filter/` | `JwtValidationFilter` — valida o token JWT antes de rotear. `RequestLoggingFilter` — logging de requests |
| `exception/` | Handler global para erros de roteamento (503 Service Unavailable, etc.) |

---

## 6. user-service

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8081 |
| **Spring Name** | `user-service` |
| **Parent POM** | `spring-boot-starter-parent:3.2.3` ⚠️ (será alinhado para 3.3.4 na Fase 0) |
| **Pacote base** | `ifsp.edu.projeto.cortaai.userservice` |
| **Classe main** | `UserServiceApplication` |
| **Banco** | `user_db` (MySQL) |

### Dependências
| Dependência | Função |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-validation` | Bean Validation (Jakarta) |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `spring-boot-starter-security` | Spring Security |
| `mysql-connector-j` | Driver MySQL |
| `spring-cloud-starter-netflix-eureka-client` | Registro no Eureka |
| `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.11.5) | Geração e validação de JWT |
| `lombok`, `mapstruct` | *(herdado)* |

### Configuração Específica (application.yml)
```yaml
app:
  security:
    jwt:
      secret-key: ${JWT_SECRET_KEY:SecretKeySuperSeguraParaDesenvolvimento12345}
      expiration-ms: 3600000  # 1 hora
```

### Estrutura de Diretórios
```
user-service/
├── pom.xml
├── src/main/java/.../userservice/
│   ├── UserServiceApplication.java
│   ├── config/           # SecurityConfig, JwtAuthorizationFilter, WebConfig
│   ├── controller/       # CustomerController, BarberController, HomeController
│   ├── dto/              # LoginDTO, LoginResponseDTO, CustomerCreateDTO, BarberDTO, etc.
│   ├── exception/        # NotFoundException, etc.
│   ├── event/            # UserDeletedEvent (RabbitMQ publisher)
│   ├── mapper/           # CustomerMapper, BarberMapper (MapStruct)
│   ├── model/
│   │   ├── Customer.java
│   │   ├── Barber.java
│   │   └── enums/        # BarberSkills, JoinRequestStatus, AppointmentStatus
│   ├── repository/       # CustomerRepository, BarberRepository
│   ├── service/
│   │   ├── CustomerService.java (interface)
│   │   ├── BarberService.java (interface)
│   │   ├── JwtTokenService.java (interface)
│   │   ├── impl/         # CustomerServiceImpl, BarberServiceImpl, JwtTokenServiceImpl, CustomUserDetailsService
│   │   └── storage/      # StorageService, CloudinaryStorageServiceImpl
│   └── validador/        # CPF, CPFValidator, CustomerEmailUnique, BarberEmailUnique, etc.
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/.../userservice/
    └── resources/
```

### Para que serve cada subpacote

| Pacote | Responsabilidade |
|---|---|
| `config/` | `SecurityConfig` — configura filtros, endpoints públicos/privados. `JwtAuthorizationFilter` — intercepta requests e valida JWT. `WebConfig` — CORS |
| `controller/` | Endpoints REST: login, register, perfil, listagem |
| `dto/` | Objetos de transporte (request/response). Desacoplam a API do model JPA |
| `exception/` | Exceções de domínio (`NotFoundException`, `ReferenceException`) |
| `event/` | Eventos publicados no RabbitMQ quando um usuário é deletado/atualizado |
| `mapper/` | MapStruct: converte `Customer ↔ CustomerDTO`, `Barber ↔ BarberDTO` |
| `model/` | Entidades JPA mapeadas para tabelas no `user_db` |
| `model/enums/` | Enums de domínio (skills, status) |
| `repository/` | Interfaces Spring Data JPA (queries automáticas) |
| `service/impl/` | Lógica de negócio: cadastro, login, geração JWT |
| `service/storage/` | Upload de imagens (avatar) via Cloudinary |
| `validador/` | Anotações e validators customizados: `@CPF`, `@CustomerEmailUnique`, etc. |

---

## 7. barbershop-service

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8082 |
| **Spring Name** | `barbershop-service` |
| **Parent POM** | `cortaai:0.1` (herda do pai) |
| **Pacote base** | `ifsp.edu.projeto.cortaai.barbershopservice` |
| **Classe main** | `BarbershopServiceApplication` |
| **Anotações** | `@SpringBootApplication`, `@EnableFeignClients` |
| **Banco** | `barbershop_db` (MySQL) |

### Dependências
| Dependência | Função |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `mysql-connector-j` | Driver MySQL |
| `spring-cloud-starter-netflix-eureka-client` | Registro no Eureka |
| `spring-cloud-starter-openfeign` | Chamadas REST declarativas para outros serviços |
| `spring-boot-starter-amqp` | RabbitMQ (publicar eventos) |
| `resilience4j-spring-boot3` | Circuit Breaker + Retry |

### Configuração Específica (application.yml)
```yaml
# Cloudinary (upload de imagens)
cloudinary:
  cloud_name: ${CLOUDINARY_CLOUD_NAME}
  api_key: ${CLOUDINARY_API_KEY}
  api_secret: ${CLOUDINARY_API_SECRET}

# Resilience4j
resilience4j:
  circuitbreaker:
    instances:
      userService:
        sliding-window-size: 10
        failure-rate-threshold: 50         # Abre circuito se 50% falhar
        wait-duration-in-open-state: 10s   # Espera 10s antes de tentar novamente
  retry:
    instances:
      userService:
        max-attempts: 3
        wait-duration: 500ms
```

### Estrutura de Diretórios
```
barbershop-service/
├── pom.xml
├── src/main/java/.../barbershopservice/
│   ├── BarbershopServiceApplication.java
│   ├── config/           # CloudinaryConfig, SwaggerConfig, CorsConfig
│   ├── controller/       # BarbershopController (CRUD + endpoints de gestão)
│   ├── dto/              # BarbershopDTO, CreateActivityDTO, JoinRequestDTO, etc.
│   ├── exception/        # NotFoundException, BusinessException
│   ├── event/            # BarbershopDeletedEvent, BarberApprovedEvent (RabbitMQ)
│   ├── feign/            # UserServiceClient (chamadas REST ao user-service)
│   ├── mapper/           # BarbershopMapper, ActivityMapper (MapStruct)
│   ├── model/
│   │   ├── Barbershop.java
│   │   ├── Activity.java
│   │   ├── BarberActivity.java        # Tabela pivô (barber_id como UUID externo)
│   │   ├── BarbershopJoinRequest.java
│   │   ├── BarbershopHighlight.java
│   │   ├── BarbershopWorkingHours.java # NOVO
│   │   ├── Review.java                # NOVO
│   │   └── enums/
│   │       ├── JoinRequestStatus.java  # PENDING, APPROVED, REJECTED
│   │       └── ActivityCategory.java   # CORTE, BARBA, COMBO, TRATAMENTO
│   ├── repository/       # BarbershopRepository, ActivityRepository, etc.
│   ├── service/
│   │   ├── impl/         # BarbershopServiceImpl
│   │   └── storage/      # CloudinaryStorageService (upload de logo/banner/highlights)
│   └── validator/        # CNPJ, CNPJValidator
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/.../barbershopservice/
    └── resources/
```

### Para que serve cada subpacote

| Pacote | Responsabilidade |
|---|---|
| `config/` | Configuração do Cloudinary, Swagger/OpenAPI, CORS |
| `controller/` | CRUD barbearia, gerenciamento de atividades, aprovação de barbeiros, destaques |
| `dto/` | DTOs de request/response para todos os endpoints |
| `exception/` | Exceções de domínio |
| `event/` | Eventos publicados no RabbitMQ (ex: barbeiro aprovado → notifica) |
| `feign/` | `UserServiceClient` — chama `GET /api/internal/users/{id}` e `PUT /api/internal/users/{id}/barbershop` |
| `mapper/` | MapStruct mappers |
| `model/` | 7 entidades JPA → tabelas no `barbershop_db` |
| `model/enums/` | Enums de status e categorias |
| `repository/` | Spring Data JPA Repositories |
| `service/impl/` | Lógica de negócio: CRUD, join requests, vinculação de barbeiros |
| `service/storage/` | Upload de imagens para Cloudinary |
| `validator/` | Validação customizada de CNPJ |

---

## 8. schedule-service

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8083 |
| **Spring Name** | `schedule-service` |
| **Parent POM** | `cortaai:0.1` |
| **Pacote base** | `ifsp.edu.projeto.cortaai.scheduleservice` |
| **Classe main** | `ScheduleServiceApplication` |
| **Anotações** | `@SpringBootApplication`, `@EnableFeignClients` |
| **Banco** | `schedule_db` (MySQL) + Redis (cache) |

### Dependências
| Dependência | Função |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `mysql-connector-j` | Driver MySQL |
| `spring-cloud-starter-netflix-eureka-client` | Eureka |
| `spring-cloud-starter-openfeign` | Chamadas ao user-service e barbershop-service |
| `spring-boot-starter-amqp` | RabbitMQ (publicar/consumir eventos) |
| `spring-boot-starter-data-redis` | Cache de disponibilidade |
| `resilience4j-spring-boot3` | Circuit Breaker + Retry |

### Configuração Específica (application.yml)
```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379

resilience4j:
  circuitbreaker:
    instances:
      userService:           # Circuit breaker para chamadas ao user-service
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
      barbershopService:     # Circuit breaker para chamadas ao barbershop-service
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

### Estrutura de Diretórios
```
schedule-service/
├── pom.xml
├── src/main/java/.../scheduleservice/
│   ├── ScheduleServiceApplication.java
│   ├── config/           # RedisConfig, RabbitMQConfig
│   ├── controller/       # AppointmentsController
│   ├── dto/              # AppointmentRequestDTO, AppointmentResponseDTO, etc.
│   ├── exception/        # ConflictException, NotFoundException
│   ├── event/            # AppointmentCreatedEvent, AppointmentCancelledEvent
│   ├── feign/            # UserServiceClient, BarbershopServiceClient
│   ├── mapper/           # AppointmentMapper (MapStruct)
│   ├── model/
│   │   ├── Appointment.java            # Dados desnormalizados (nomes)
│   │   ├── AppointmentActivity.java    # Snapshot: nome, preço, duração
│   │   ├── BarberBlock.java            # NOVO: bloqueios de agenda
│   │   └── enums/
│   │       └── AppointmentStatus.java  # SCHEDULED, CONFIRMED, IN_PROGRESS, CONCLUDED, CANCELLED, NO_SHOW
│   ├── repository/       # AppointmentsRepository, BarberBlockRepository
│   └── service/
│       └── impl/         # AppointmentsServiceImpl (lógica core)
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/.../scheduleservice/
    └── resources/
```

### Para que serve cada subpacote

| Pacote | Responsabilidade |
|---|---|
| `config/` | Configuração do Redis (serializer, TTL) e RabbitMQ (exchanges, queues) |
| `controller/` | Endpoints: criar, cancelar, listar, disponibilidade |
| `dto/` | DTOs de request/response |
| `exception/` | Conflito de horário, entidade não encontrada |
| `event/` | Publica `AppointmentCreatedEvent` → RabbitMQ → notification-service + payment-service |
| `feign/` | `UserServiceClient` — valida barbeiro/cliente. `BarbershopServiceClient` — valida serviços e skills |
| `mapper/` | MapStruct mapper |
| `model/` | 3 entidades JPA com dados desnormalizados |
| `repository/` | Queries otimizadas para verificação de conflito de horários |
| `service/impl/` | Lógica core: validação via Feign → verificação de conflito local → salvar → publicar evento |

---

## 9. payment-service

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8084 |
| **Spring Name** | `payment-service` |
| **Parent POM** | `cortaai:0.1` |
| **Pacote base** | `ifsp.edu.projeto.cortaai.paymentservice` |
| **Classe main** | `PaymentServiceApplication` |
| **Anotações** | `@SpringBootApplication`, `@EnableFeignClients` |
| **Banco** | `payment_db` (MySQL) |

### Dependências
| Dependência | Função |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `mysql-connector-j` | Driver MySQL |
| `spring-cloud-starter-netflix-eureka-client` | Eureka |
| `spring-cloud-starter-openfeign` | Chamadas ao schedule-service |
| `spring-boot-starter-amqp` | RabbitMQ (consumir/publicar eventos) |
| **`com.mercadopago:sdk-java:2.1.24`** | SDK oficial do Mercado Pago |

### Configuração Específica (application.yml)
```yaml
mercadopago:
  access-token: ${MP_ACCESS_TOKEN}
  notification-url: ${MP_NOTIFICATION_URL:http://localhost:8080/api/payments/webhook}
  marketplace-fee-percent: 5.0    # Taxa da plataforma CortaAí
```

### Estrutura de Diretórios
```
payment-service/
├── pom.xml
├── src/main/java/.../paymentservice/
│   ├── PaymentServiceApplication.java
│   ├── config/           # MercadoPagoConfig (inicializa SDK com access-token)
│   ├── controller/       # PaymentController (criar pagamento), WebhookController (receber callbacks do MP)
│   ├── dto/              # CreatePaymentDTO, PaymentResponseDTO, WebhookPayloadDTO
│   ├── exception/        # PaymentException, WebhookValidationException
│   ├── event/            # PaymentApprovedEvent, PaymentRefundedEvent (RabbitMQ)
│   ├── feign/            # ScheduleServiceClient (buscar dados do agendamento)
│   ├── mapper/           # TransactionMapper (MapStruct)
│   ├── model/
│   │   ├── Transaction.java           # Dados da transação + campos do MP
│   │   ├── PaymentWebhookLog.java     # Log de webhooks (idempotência)
│   │   └── enums/
│   │       ├── PaymentStatus.java     # PENDING, APPROVED, REJECTED, REFUNDED, CANCELLED
│   │       └── PaymentMethod.java     # PIX, CREDIT_CARD, DEBIT
│   ├── repository/       # TransactionRepository, WebhookLogRepository
│   └── service/
│       └── impl/         # PaymentServiceImpl (cria Preference no MP, processa webhooks)
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/.../paymentservice/
    └── resources/
```

### Para que serve cada subpacote

| Pacote | Responsabilidade |
|---|---|
| `config/` | Inicializa o SDK do Mercado Pago com o access token |
| `controller/` | `PaymentController` — cria pagamento. `WebhookController` — recebe notificações do MP |
| `dto/` | Payloads de criação, resposta e webhook |
| `exception/` | Erros de pagamento e validação de webhook |
| `event/` | Publica `PaymentApprovedEvent` → schedule-service atualiza status → notification-service notifica |
| `feign/` | `ScheduleServiceClient` — busca dados do agendamento para montar a Preference do MP |
| `model/` | 2 entidades: transação e log de webhooks |
| `model/enums/` | Status e métodos de pagamento |
| `service/impl/` | Lógica: criar Preference MP, processar webhook, calcular split payment |

---

## 10. notification-service

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8085 |
| **Spring Name** | `notification-service` |
| **Parent POM** | `cortaai:0.1` |
| **Pacote base** | `ifsp.edu.projeto.cortaai.notificationservice` |
| **Classe main** | `NotificationServiceApplication` |
| **Anotações** | `@SpringBootApplication` |
| **Banco** | Redis (sem MySQL) |

### Dependências
| Dependência | Função |
|---|---|
| `spring-boot-starter-web` | API REST (+ endpoints de preferências) |
| `spring-cloud-starter-netflix-eureka-client` | Eureka |
| `spring-boot-starter-amqp` | RabbitMQ (consumir eventos) |
| `spring-boot-starter-data-redis` | Cache + deduplicação de notificações |
| `spring-boot-starter-websocket` | STOMP/WebSocket (notificações in-app em tempo real) |
| `spring-boot-starter-thymeleaf` | Templates HTML para emails |
| `spring-boot-starter-mail` | Envio de emails (Resend/SMTP) |

### Configuração Específica (application.yml)
```yaml
resend:
  api-key: ${RESEND_API_KEY}
  from-email: noreply@cortaai.com.br

fcm:
  credentials-path: ${FCM_CREDENTIALS_PATH:firebase-credentials.json}
```

### Estrutura de Diretórios
```
notification-service/
├── pom.xml
├── src/main/java/.../notificationservice/
│   ├── NotificationServiceApplication.java
│   ├── config/           # RabbitMQConfig (queues/exchanges), WebSocketConfig (STOMP), RedisConfig
│   ├── controller/       # NotificationPreferencesController, WebSocketController
│   ├── dto/              # NotificationDTO, UserPreferencesDTO
│   ├── event/            # Classes de eventos consumidos (AppointmentCreatedEvent, PaymentApprovedEvent, etc.)
│   ├── listener/         # RabbitMQ @RabbitListener — consome filas de eventos
│   ├── model/
│   │   └── enums/
│   │       ├── NotificationType.java  # APPOINTMENT_CREATED, PAYMENT_APPROVED, REMINDER, etc.
│   │       └── NotificationChannel.java # EMAIL, PUSH, WHATSAPP, IN_APP
│   ├── repository/       # Acesso ao Redis (preferências de notificação)
│   ├── service/
│   │   ├── impl/         # NotificationRouterImpl (decide canal + template baseado no evento)
│   │   └── channel/      # EmailChannel, PushChannel, WhatsAppChannel, InAppChannel
│   └── template/         # Lógica de montagem de mensagens por tipo de evento
├── src/main/resources/
│   ├── application.yml
│   └── templates/        # Templates Thymeleaf para emails HTML
└── src/test/
    ├── java/.../notificationservice/
    └── resources/
```

### Para que serve cada subpacote

| Pacote | Responsabilidade |
|---|---|
| `config/` | Configuração de RabbitMQ (exchanges, queues, bindings), WebSocket STOMP, Redis |
| `controller/` | Gerenciamento de preferências do usuário (quais canais ativar) |
| `dto/` | DTOs de notificação e preferências |
| `event/` | Classes que representam os eventos consumidos do RabbitMQ |
| `listener/` | `@RabbitListener` — escuta filas: `appointment.created`, `payment.approved`, `reminder.schedule`, etc. |
| `model/enums/` | Tipos de notificação e canais disponíveis |
| `repository/` | Acesso ao Redis para preferências e deduplicação |
| `service/impl/` | `NotificationRouter` — decide qual canal usar baseado no tipo de evento + preferências do usuário |
| `service/channel/` | Implementações por canal: `EmailChannel` (Resend), `PushChannel` (FCM), `WhatsAppChannel`, `InAppChannel` (WebSocket) |
| `template/` | Montagem de corpo da mensagem por tipo de evento |
| `resources/templates/` | Templates Thymeleaf para gerar HTML dos emails |

---

## 11. product-service

### Identificação

| Item | Valor |
|---|---|
| **Porta** | 8086 |
| **Spring Name** | `product-service` |
| **Parent POM** | `cortaai:0.1` |
| **Pacote base** | `ifsp.edu.projeto.cortaai.productservice` |
| **Classe main** | `ProductServiceApplication` |
| **Anotações** | `@SpringBootApplication`, `@EnableFeignClients` |
| **Banco** | `product_db` (MySQL) |

### Dependências
| Dependência | Função |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `mysql-connector-j` | Driver MySQL |
| `spring-cloud-starter-netflix-eureka-client` | Eureka |
| `spring-cloud-starter-openfeign` | Chamadas ao payment-service |
| `spring-boot-starter-amqp` | RabbitMQ (publicar eventos) |

### Estrutura de Diretórios
```
product-service/
├── pom.xml
├── src/main/java/.../productservice/
│   ├── ProductServiceApplication.java
│   ├── config/           # CorsConfig, SwaggerConfig
│   ├── controller/       # ProductController (CRUD), OrderController (pedidos)
│   ├── dto/              # ProductDTO, OrderDTO, CartItemDTO
│   ├── exception/        # OutOfStockException, NotFoundException
│   ├── event/            # OrderCreatedEvent, StockLowEvent (RabbitMQ)
│   ├── feign/            # PaymentServiceClient (integra pagamento de pedidos)
│   ├── mapper/           # ProductMapper, OrderMapper (MapStruct)
│   ├── model/
│   │   ├── Product.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── StockMovement.java
│   │   └── enums/
│   │       ├── OrderStatus.java       # PENDING, PAID, PREPARING, READY, DELIVERED, CANCELLED
│   │       └── ProductCategory.java   # POMADA, SHAMPOO, CERA, TRATAMENTO, etc.
│   ├── repository/       # ProductRepository, OrderRepository, StockMovementRepository
│   └── service/
│       └── impl/         # ProductServiceImpl, OrderServiceImpl, StockServiceImpl
├── src/main/resources/
│   └── application.yml
└── src/test/
    ├── java/.../productservice/
    └── resources/
```

### Para que serve cada subpacote

| Pacote | Responsabilidade |
|---|---|
| `config/` | CORS e documentação Swagger/OpenAPI |
| `controller/` | `ProductController` — CRUD de produtos (dono). `OrderController` — criar pedido, atualizar status |
| `dto/` | DTOs de produtos, pedidos e itens de carrinho |
| `exception/` | Produto sem estoque, entidade não encontrada |
| `event/` | `OrderCreatedEvent` → payment-service. `StockLowEvent` → notification-service (alerta ao dono) |
| `feign/` | `PaymentServiceClient` — cria pagamento para o pedido |
| `mapper/` | MapStruct mappers |
| `model/` | 4 entidades JPA no `product_db` |
| `model/enums/` | Status de pedido e categorias de produto |
| `repository/` | Repositories + queries customizadas (estoque, produtos por barbearia) |
| `service/impl/` | CRUD, criação de pedido com baixa automática de estoque, alertas |

---

## Resumo Geral — Mapa de Componentes

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        BACKEND (Multi-Module Maven)                     │
│                         pom.xml (cortaai:0.1)                           │
│  Spring Boot 3.3.4 · Spring Cloud 2023.0.1 · Java 17                   │
│  Lombok · MapStruct 1.5.5 · JUnit 5                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─ INFRA ──────────────────────────────────────────────────────────┐   │
│  │  discovery-service (:8761)  → Eureka Server                     │   │
│  │  api-gateway (:8080)        → Spring Cloud Gateway + Rotas      │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─ CORE ──────────────────────────────────────────────────────────┐   │
│  │  user-service (:8081)       → Auth/JWT, Cadastro, Perfil        │   │
│  │     Deps: Security, JPA, JJWT, Eureka                          │   │
│  │     DB: user_db                                                  │   │
│  │                                                                  │   │
│  │  barbershop-service (:8082) → Barbearias, Serviços, Equipe     │   │
│  │     Deps: JPA, Feign, RabbitMQ, Resilience4j, Eureka           │   │
│  │     DB: barbershop_db                                            │   │
│  │                                                                  │   │
│  │  schedule-service (:8083)   → Agendamentos, Disponibilidade    │   │
│  │     Deps: JPA, Feign, RabbitMQ, Redis, Resilience4j, Eureka    │   │
│  │     DB: schedule_db + Redis (cache)                              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─ EVOLUÇÃO ──────────────────────────────────────────────────────┐   │
│  │  payment-service (:8084)    → Mercado Pago, Webhooks, Split    │   │
│  │     Deps: JPA, Feign, RabbitMQ, MercadoPago SDK, Eureka        │   │
│  │     DB: payment_db                                               │   │
│  │                                                                  │   │
│  │  notification-service (:8085) → Email, Push, WebSocket         │   │
│  │     Deps: RabbitMQ, Redis, WebSocket, Thymeleaf, Mail, Eureka  │   │
│  │     DB: Redis (sem MySQL)                                        │   │
│  │                                                                  │   │
│  │  product-service (:8086)    → e-Commerce, Estoque, Pedidos     │   │
│  │     Deps: JPA, Feign, RabbitMQ, Eureka                         │   │
│  │     DB: product_db                                               │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─ LEGADO ────────────────────────────────────────────────────────┐   │
│  │  src/ (monólito) → Referência para migração. Não compilado.     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                        INFRAESTRUTURA (Docker)                          │
│  MySQL 8.0 (:3306) · RabbitMQ 3 (:5672/:15672) · Redis 7 (:6379)      │
├─────────────────────────────────────────────────────────────────────────┤
│                        FRONTEND (React 19 + Vite 7)                     │
│  cortaai-web (:5173) · Axios · React Router DOM 7                       │
└─────────────────────────────────────────────────────────────────────────┘
```

