# ✂️ CortaAi Backend — Arquitetura de Microserviços

Backend do **CortaAi**, um sistema de marketplace para barbearias. A aplicação foi migrada de um monolito para uma **arquitetura de microserviços** com Spring Boot, Spring Cloud e Docker.

---

## 🏗️ Arquitetura

```
                    ┌────────────────┐
                    │   API Gateway  │ :8080
                    │ (Spring Cloud) │
                    └───────┬────────┘
                            │
                    ┌───────┴────────┐
                    │   Discovery    │ :8761
                    │   (Eureka)     │
                    └───────┬────────┘
          ┌─────────────────┼─────────────────────────┐
          │         │         │         │         │    │
    ┌─────┴──┐ ┌───┴────┐ ┌──┴───┐ ┌───┴──┐ ┌───┴──┐ ┌──┴───┐
    │ User   │ │Barber- │ │Sched-│ │Pay-  │ │Notif-│ │Prod- │
    │Service │ │shop    │ │ule   │ │ment  │ │ication│ │uct   │
    │ :8081  │ │Service │ │Svc   │ │Svc   │ │Svc   │ │Svc   │
    │        │ │ :8082  │ │:8083 │ │:8084 │ │:8085 │ │:8086 │
    └────────┘ └────────┘ └──────┘ └──────┘ └──────┘ └──────┘
```

---

## 📦 Microserviços

| Serviço | Porta | Banco | Responsabilidade |
|---------|-------|-------|-----------------|
| **discovery-service** | 8761 | — | Service Discovery (Eureka Server) |
| **api-gateway** | 8080 | — | Roteamento, ponto de entrada único |
| **user-service** | 8081 | `user_db` | Autenticação (JWT), cadastro de clientes e barbeiros, perfis, fotos (Cloudinary) |
| **barbershop-service** | 8082 | `barbershop_db` | CRUD de barbearias, atividades, equipe, join requests, fotos (Cloudinary) |
| **schedule-service** | 8083 | `schedule_db` | Agendamentos, disponibilidade, bloqueios de horário |
| **payment-service** | 8084 | `payment_db` | Integração Mercado Pago (Checkout Pro), webhooks, transações |
| **notification-service** | 8085 | `notification_db` | Notificações in-app e email, listeners de eventos RabbitMQ |
| **product-service** | 8086 | `product_db` | Produtos, pedidos, controle de estoque |

---

## 🛠️ Tecnologias

| Categoria | Tecnologia |
|-----------|-----------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.4, Spring Cloud 2023.0.1 |
| Service Discovery | Eureka |
| API Gateway | Spring Cloud Gateway |
| Mensageria | RabbitMQ (eventos assíncronos) |
| Cache | Redis (deduplicação, cache de slots) |
| Banco de Dados | MySQL 8.0 (database-per-service) |
| Pagamento | Mercado Pago SDK 2.1.24 |
| Upload de Imagens | Cloudinary |
| Mapeamento | MapStruct 1.5.5 |
| Utilitários | Lombok |
| Resiliência | Resilience4j (circuit breaker, retry) |
| Containers | Docker Compose |

---

## 🚀 Como Executar

### Pré-requisitos
- **Docker** e **Docker Compose** instalados

### 1. Clone e configure
```bash
git clone https://github.com/AppCortaAi/Arquitetura_completa.git
cd Arquitetura_completa
cp .env.example .env
# Preencha os valores no .env
```

### 2. Suba tudo com Docker
```bash
docker compose up -d
```

### 3. Acesse
- **API Gateway:** http://localhost:8080
- **Eureka Dashboard:** http://localhost:8761
- **RabbitMQ Management:** http://localhost:15672 (guest/guest)
- **Frontend:** http://localhost:5173

### Para desenvolvimento local (sem Docker)
```bash
cd backend
./mvnw spring-boot:run -pl discovery-service
./mvnw spring-boot:run -pl api-gateway
./mvnw spring-boot:run -pl user-service
# ... demais serviços
```

---

## 📂 Estrutura de Cada Microserviço

```
<service>/
├── pom.xml
└── src/main/java/ifsp/edu/projeto/cortaai/<service>/
    ├── <Service>Application.java
    ├── config/          # Configurações (RabbitMQ, Redis, Security, Cloudinary)
    ├── controller/      # REST Controllers
    ├── dto/             # Data Transfer Objects
    ├── event/           # Eventos RabbitMQ (publicação/consumo)
    ├── feign/           # Feign Clients (comunicação inter-serviço)
    ├── listener/        # Listeners de eventos
    ├── mapper/          # MapStruct mappers
    ├── model/           # Entidades JPA
    ├── repository/      # Spring Data JPA repositories
    └── service/         # Lógica de negócio
```

---

## � Comunicação entre Serviços

| Tipo | Tecnologia | Exemplo |
|------|-----------|---------|
| **Síncrona** | Feign Client + Eureka | payment → schedule (buscar info do agendamento) |
| **Assíncrona** | RabbitMQ (Topic Exchange) | schedule → notification (agendamento criado) |

### Eventos RabbitMQ (Exchange: `cortaai.events`)
| Routing Key | Produtor | Consumidor |
|-------------|----------|-----------|
| `appointment.created` | schedule-service | notification-service |
| `appointment.cancelled` | schedule-service | notification-service |
| `appointment.concluded` | schedule-service | notification-service |
| `appointment.rescheduled` | schedule-service | notification-service |
| `payment.approved` | payment-service | notification-service |
