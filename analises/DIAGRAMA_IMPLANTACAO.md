# 🚀 Diagrama de Implantação — CortaAi

> **Data:** 08 de maio de 2026  
> **Escopo:** ambiente de execução em arquitetura de microsserviços

---

## Visão de implantação (Docker Compose / ambiente servidor)

```mermaid
flowchart TB
  subgraph Cliente[Cliente]
    Browser[Browser / App Web]
  end

  subgraph Frontend[Container Frontend]
    React[React 18 + Vite]
  end

  subgraph Edge[Camada de Entrada]
    Gateway[api-gateway\nSpring Cloud Gateway\n:8080]
    Eureka[discovery-service\nEureka Server\n:8761]
  end

  subgraph Servicos[Containers de Microsserviços]
    User[user-service\n:8081]
    Barbershop[barbershop-service\n:8082]
    Schedule[schedule-service\n:8083]
    Payment[payment-service\n:8084]
    Notification[notification-service\n:8085]
    Product[product-service\n:8086]
  end

  subgraph Dados[Camada de Dados e Mensageria]
    MySQL[(MySQL)]
    Redis[(Redis)]
    Rabbit[(RabbitMQ)]
  end

  subgraph Externos[Serviços Externos]
    Firebase[Firebase Auth]
    MercadoPago[Mercado Pago]
    Cloudinary[Cloudinary]
  end

  Browser --> React
  React --> Gateway

  Gateway --> User
  Gateway --> Barbershop
  Gateway --> Schedule
  Gateway --> Payment
  Gateway --> Notification
  Gateway --> Product

  User -. registro/descoberta .-> Eureka
  Barbershop -. registro/descoberta .-> Eureka
  Schedule -. registro/descoberta .-> Eureka
  Payment -. registro/descoberta .-> Eureka
  Notification -. registro/descoberta .-> Eureka
  Product -. registro/descoberta .-> Eureka
  Gateway -. registro/descoberta .-> Eureka

  User --> MySQL
  Barbershop --> MySQL
  Schedule --> MySQL
  Payment --> MySQL
  Product --> MySQL

  Notification --> Redis
  Schedule --> Redis

  User <--> Rabbit
  Barbershop <--> Rabbit
  Schedule <--> Rabbit
  Payment <--> Rabbit
  Notification <--> Rabbit
  Product <--> Rabbit

  Gateway --> Firebase
  Payment --> MercadoPago
  User --> Cloudinary
  Barbershop --> Cloudinary
```

---

## Mapeamento resumido por responsabilidade

- **Entrada e segurança:** `api-gateway` valida token Firebase e injeta headers confiáveis `X-User-*`.
- **Descoberta:** `discovery-service` centraliza registro via Eureka.
- **Dados:** banco por serviço (compartilhando instância de MySQL no ambiente local), Redis para cache/deduplicação.
- **Integrações:** Mercado Pago (pagamentos), Cloudinary (mídia), Firebase (identidade).
- **Comunicação assíncrona:** RabbitMQ para eventos de domínio entre serviços.
