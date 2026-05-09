# 🚀 Diagrama de Implantação — CortaAi (UML)

> **Data:** 08 de maio de 2026  
> **Escopo:** topologia de execução em ambiente Docker Compose/servidor

---

## Diagrama de implantação (padrão UML / PlantUML)

```plantuml
@startuml
title CortaAi - Diagrama de Implantação UML
left to right direction

node "Dispositivo do Cliente" as CLIENT {
  artifact "Browser / WebView" as BROWSER
}

node "Host Web" as WEB_HOST {
  node "Frontend Container" as FRONTEND_C {
    artifact "React 18 + Vite" as FRONTEND_APP
  }
}

node "Host Backend (Docker Compose)" as BACKEND_HOST {
  node "Edge" as EDGE {
    node "api-gateway :8080" as GW
    node "discovery-service :8761" as EUREKA
  }

  node "Domínio" as DOMAIN {
    node "user-service :8081" as USER
    node "barbershop-service :8082" as BARBERSHOP
    node "schedule-service :8083" as SCHEDULE
    node "payment-service :8084" as PAYMENT
    node "notification-service :8085" as NOTIFICATION
    node "product-service :8086" as PRODUCT
  }

  node "Infra de Dados/Mensageria" as INFRA {
    database "MySQL" as MYSQL
    node "Redis" as REDIS
    node "RabbitMQ" as RABBIT
  }
}

cloud "Firebase Auth" as FIREBASE
cloud "Mercado Pago" as MP
cloud "Cloudinary" as CLOUDINARY

BROWSER --> FRONTEND_APP : HTTPS
FRONTEND_APP --> GW : REST /api/*

GW --> USER : /api/users
GW --> BARBERSHOP : /api/barbershops
GW --> SCHEDULE : /api/appointments
GW --> PAYMENT : /api/payments
GW --> NOTIFICATION : /api/notifications
GW --> PRODUCT : /api/products

GW --> EUREKA : descoberta
USER --> EUREKA : registro/descoberta
BARBERSHOP --> EUREKA : registro/descoberta
SCHEDULE --> EUREKA : registro/descoberta
PAYMENT --> EUREKA : registro/descoberta
NOTIFICATION --> EUREKA : registro/descoberta
PRODUCT --> EUREKA : registro/descoberta

USER --> MYSQL : JPA
BARBERSHOP --> MYSQL : JPA
SCHEDULE --> MYSQL : JPA
PAYMENT --> MYSQL : JPA
PRODUCT --> MYSQL : JPA

SCHEDULE --> REDIS : cache
NOTIFICATION --> REDIS : deduplicação

USER --> RABBIT : publish/consume
BARBERSHOP --> RABBIT : publish/consume
SCHEDULE --> RABBIT : publish/consume
PAYMENT --> RABBIT : publish/consume
NOTIFICATION --> RABBIT : publish/consume
PRODUCT --> RABBIT : publish/consume

GW --> FIREBASE : validação ID token
PAYMENT --> MP : transações/webhooks
USER --> CLOUDINARY : upload de mídia
BARBERSHOP --> CLOUDINARY : upload de portfólio
@enduml
```

---

## Desenho da arquitetura em caracteres (implantação)

```text
                          [ Cliente ]
                              |
                          HTTPS/SPA
                              v
                    +----------------------+
                    | Frontend React/Vite |
                    +----------+-----------+
                               |
                               | REST /api/*
                               v
                    +----------------------+
                    | api-gateway :8080    |
                    | auth + roteamento    |
                    +----+----+----+-------+
                         |    |    |
      +------------------+    |    +------------------+
      |                       |                       |
      v                       v                       v
+-------------+       +---------------+       +---------------+
|user :8081   |       |schedule :8083 |       |payment :8084  |
+------+------+       +-------+-------+       +-------+-------+
       |                      |                       |
       |                      +--> Redis (cache)      +--> Mercado Pago
       +--> Cloudinary

+-------------+       +---------------+       +------------------+
|barber :8082 |       |product :8086  |       |notification :8085|
+------+------+       +-------+-------+       +---------+--------+
       |                      |                         |
       +--> Cloudinary        |                         +--> Redis (dedup)
                              |
            +-----------------+------------------------------+
            |           RabbitMQ (eventos)                   |
            +-----------------+------------------------------+
                              |
                    +---------v---------+
                    | discovery :8761   |
                    | Eureka            |
                    +-------------------+

            Persistência relacional por serviço (ambiente local em MySQL)
```

---

## Mapeamento resumido por responsabilidade

- **Entrada e segurança:** `api-gateway` valida Firebase e injeta `X-User-*`.
- **Descoberta:** `discovery-service` centraliza registro/lookup via Eureka.
- **Dados e integração interna:** MySQL, Redis e RabbitMQ dentro da malha de execução.
- **Integrações externas:** Mercado Pago, Cloudinary e Firebase.

## Inventário operacional dos containers (docker compose)

> Referência: consolidação informada no `docker compose ps` com **12 containers ativos**.

### Total por categoria

- **Microsserviços:** 8 containers
  - **Negócio:** 6
  - **Infraestrutura:** 2
- **Frontend:** 1 container
- **Dados/Ferramentas:** 3 containers

### 1) Microsserviços de negócio (6)

- **`user-service`** — gestão de usuários (cadastro, perfil, papéis e dados de identidade de domínio).
- **`barbershop-service`** — gestão de barbearias (dados da unidade, catálogo e informações da loja).
- **`schedule-service`** — gestão de agenda e agendamentos.
- **`payment-service`** — gestão e integração de pagamentos.
- **`product-service`** — gestão de produtos e estoque.
- **`notification-service`** — envio e orquestração de notificações orientadas a evento.

### 2) Microsserviços de infraestrutura (2)

- **`api-gateway`** — entrada única das requisições externas; autentica/autoriza no edge e distribui para serviços internos.
- **`discovery-service`** — servidor Eureka para registro e descoberta entre serviços.

### 3) Frontend (1)

- **`cortaai-web`** — interface web da aplicação (entrega da SPA), executada em container próprio.

### 4) Bancos de dados e ferramentas (3)

- **`cortaai-mysql`** — banco relacional principal.
- **`cortaai-redis`** — cache e suporte a idempotência/deduplicação.
- **`cortaai-rabbitmq`** — broker de mensageria para eventos assíncronos.

### Leitura final da topologia

- A malha está separada em **camada de entrada (gateway)**, **camada de domínio (6 serviços)** e **camada de suporte (dados/mensageria/discovery)**.
- O conjunto de **12 containers** está coerente com uma arquitetura de microsserviços com fronteiras claras entre negócio e infraestrutura.

## Organização entre os documentos

- **Este arquivo (`DIAGRAMA_IMPLANTACAO.md`)** concentra o **diagrama visual operacional** e o **detalhamento dos containers**.
- **`DIAGRAMA_COMPONENTES.md`** concentra o **detalhamento lógico-funcional** dos componentes e contratos.

## Legenda UML utilizada

- **Nó (`node`)**: ambiente/container/processo implantado.
- **Artefato (`artifact`)**: unidade executável/entregável.
- **Nuvem (`cloud`)**: dependência externa.
- **Conectores (`-->`)**: fluxo de comunicação e dependência operacional.
