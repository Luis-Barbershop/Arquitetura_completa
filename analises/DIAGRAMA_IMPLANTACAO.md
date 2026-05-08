# 🚀 Diagrama de Implantação — CortaAi

> **Data:** 08 de maio de 2026  
> **Escopo:** ambiente de execução em arquitetura de microsserviços

---

## Visão de implantação (Docker Compose / ambiente servidor)

```mermaid
flowchart TB
  %% =========================
  %% CAMADAS DE IMPLANTAÇÃO
  %% =========================
  subgraph Z0[Dispositivos do Cliente]
    Browser[🌐 Browser / SPA]
  end

  subgraph Z1[Camada Web]
    React[Frontend\nReact 18 + Vite]
  end

  subgraph Z2[Edge / Entrada]
    Gateway[api-gateway\nSpring Cloud Gateway\n:8080]
    Eureka[discovery-service\nEureka Server\n:8761]
  end

  subgraph Z3[Microsserviços de Domínio]
    User[user-service\n:8081]
    Barbershop[barbershop-service\n:8082]
    Schedule[schedule-service\n:8083]
    Payment[payment-service\n:8084]
    Notification[notification-service\n:8085]
    Product[product-service\n:8086]
  end

  subgraph Z4[Infra de Dados e Mensageria]
    MySQL[(MySQL)]
    Redis[(Redis)]
    Rabbit[(RabbitMQ)]
  end

  subgraph Z5[Serviços Externos]
    Firebase[Firebase Auth]
    MercadoPago[Mercado Pago]
    Cloudinary[Cloudinary CDN]
  end

  %% =========================
  %% FLUXO DE ENTRADA
  %% =========================
  Browser -->|HTTPS| React
  React -->|REST /api/*| Gateway

  %% =========================
  %% ROTEAMENTO DE API
  %% =========================
  Gateway -->|/api/users| User
  Gateway -->|/api/barbershops| Barbershop
  Gateway -->|/api/appointments| Schedule
  Gateway -->|/api/payments| Payment
  Gateway -->|/api/notifications| Notification
  Gateway -->|/api/products| Product

  %% =========================
  %% SERVICE DISCOVERY
  %% =========================
  Gateway -. registro/descoberta .-> Eureka
  User -. registro/descoberta .-> Eureka
  Barbershop -. registro/descoberta .-> Eureka
  Schedule -. registro/descoberta .-> Eureka
  Payment -. registro/descoberta .-> Eureka
  Notification -. registro/descoberta .-> Eureka
  Product -. registro/descoberta .-> Eureka

  %% =========================
  %% DADOS E EVENTOS
  %% =========================
  User -->|JPA| MySQL
  Barbershop -->|JPA| MySQL
  Schedule -->|JPA| MySQL
  Payment -->|JPA| MySQL
  Product -->|JPA| MySQL

  Schedule -->|cache| Redis
  Notification -->|deduplicação| Redis

  User <--> |eventos| Rabbit
  Barbershop <--> |eventos| Rabbit
  Schedule <--> |eventos| Rabbit
  Payment <--> |eventos| Rabbit
  Notification <--> |eventos| Rabbit
  Product <--> |eventos| Rabbit

  %% =========================
  %% INTEGRAÇÕES EXTERNAS
  %% =========================
  Gateway -->|validação token| Firebase
  Payment -->|transações/webhooks| MercadoPago
  User -->|upload de mídia| Cloudinary
  Barbershop -->|upload portfólio| Cloudinary

  %% =========================
  %% ESTILOS
  %% =========================
  classDef client fill:#E8F4FD,stroke:#1F78B4,stroke-width:1.5px,color:#0A2A43;
  classDef web fill:#E9FFF4,stroke:#0E9F6E,stroke-width:1.5px,color:#094D36;
  classDef edge fill:#FFF8E1,stroke:#F59E0B,stroke-width:1.5px,color:#6B450A;
  classDef svc fill:#F3E8FF,stroke:#7C3AED,stroke-width:1.5px,color:#3E1A78;
  classDef infra fill:#F1F5F9,stroke:#475569,stroke-width:1.5px,color:#1E293B;
  classDef ext fill:#FFE4E6,stroke:#E11D48,stroke-width:1.5px,color:#6A1028;

  class Browser client;
  class React web;
  class Gateway,Eureka edge;
  class User,Barbershop,Schedule,Payment,Notification,Product svc;
  class MySQL,Redis,Rabbit infra;
  class Firebase,MercadoPago,Cloudinary ext;
```

---

## Mapeamento resumido por responsabilidade

- **Entrada e segurança:** `api-gateway` valida token Firebase e injeta headers confiáveis `X-User-*`.
- **Descoberta:** `discovery-service` centraliza registro via Eureka.
- **Dados:** banco por serviço (compartilhando instância de MySQL no ambiente local), Redis para cache/deduplicação.
- **Integrações:** Mercado Pago (pagamentos), Cloudinary (mídia), Firebase (identidade).
- **Comunicação assíncrona:** RabbitMQ para eventos de domínio entre serviços.

## Legenda de leitura do diagrama

- **Seta contínua (`-->`)**: chamada síncrona/fluxo principal.
- **Seta tracejada (`-.->`)**: registro e descoberta no Eureka.
- **Seta bidirecional (`<-->`)**: publicação e consumo de eventos RabbitMQ.
