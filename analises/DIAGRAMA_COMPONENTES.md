# 🧩 Diagrama de Componentes — CortaAi

> **Data:** 08 de maio de 2026  
> **Escopo:** componentes lógicos e contratos entre módulos

---

## Visão de componentes da solução

```mermaid
flowchart LR
  subgraph FE[Frontend]
    Pages[Páginas React]
    UIServices[frontend/src/services/*Service.js]
    ApiWrapper[services/api.js\nAxios Wrapper]
    Pages --> UIServices --> ApiWrapper
  end

  subgraph GW[API Gateway]
    Routes[Configuração de Rotas]
    AuthFilter[Filter de autenticação Firebase]
    HeaderInjector[Injeção de headers X-User-*]
    ExHandlerGW[GlobalExceptionHandler]
    Routes --> AuthFilter --> HeaderInjector
  end

  subgraph US[user-service]
    CtrlUS[Controllers (DTO)]
    SvcUS[Services]
    RepoUS[Repositories]
    MapperUS[Mappers]
    MsgUS[Messaging Publisher/Listener]
    CtrlUS --> SvcUS --> RepoUS
    SvcUS --> MapperUS
    SvcUS --> MsgUS
  end

  subgraph BS[barbershop-service]
    CtrlBS[Controllers (DTO)]
    SvcBS[Services]
    RepoBS[Repositories]
    MapperBS[Mappers]
    FeignBS[Feign Clients]
    MsgBS[Messaging Publisher/Listener]
    CtrlBS --> SvcBS --> RepoBS
    SvcBS --> MapperBS
    SvcBS --> FeignBS
    SvcBS --> MsgBS
  end

  subgraph SS[schedule-service]
    CtrlSS[Controllers (DTO)]
    SvcSS[Services]
    RepoSS[Repositories]
    MapperSS[Mappers]
    FeignSS[Feign Clients]
    MsgSS[Messaging Publisher/Listener]
    CtrlSS --> SvcSS --> RepoSS
    SvcSS --> MapperSS
    SvcSS --> FeignSS
    SvcSS --> MsgSS
  end

  subgraph PS[payment-service]
    CtrlPS[Controllers (DTO)]
    SvcPS[Services]
    RepoPS[Repositories]
    MapperPS[Mappers]
    FeignPS[Feign Clients]
    MsgPS[Messaging Publisher/Listener]
    CtrlPS --> SvcPS --> RepoPS
    SvcPS --> MapperPS
    SvcPS --> FeignPS
    SvcPS --> MsgPS
  end

  subgraph PRS[product-service]
    CtrlPRS[Controllers (DTO)]
    SvcPRS[Services]
    RepoPRS[Repositories]
    MapperPRS[Mappers]
    FeignPRS[Feign Clients]
    MsgPRS[Messaging Publisher/Listener]
    CtrlPRS --> SvcPRS --> RepoPRS
    SvcPRS --> MapperPRS
    SvcPRS --> FeignPRS
    SvcPRS --> MsgPRS
  end

  subgraph NS[notification-service]
    ListenerNS[Messaging Listeners]
    SvcNS[Notification Services]
    DedupNS[Deduplicação Redis]
    ProviderNS[Providers Email/Push]
    ListenerNS --> SvcNS --> DedupNS
    SvcNS --> ProviderNS
  end

  subgraph Infra[Infra Compartilhada]
    Rabbit[RabbitMQ]
    Redis[Redis]
    MySQL[(MySQL)]
    Firebase[Firebase Auth]
    MercadoPago[Mercado Pago]
    Cloudinary[Cloudinary]
  end

  ApiWrapper --> Routes
  AuthFilter --> Firebase

  HeaderInjector --> CtrlUS
  HeaderInjector --> CtrlBS
  HeaderInjector --> CtrlSS
  HeaderInjector --> CtrlPS
  HeaderInjector --> CtrlPRS
  HeaderInjector --> ListenerNS

  MsgUS <--> Rabbit
  MsgBS <--> Rabbit
  MsgSS <--> Rabbit
  MsgPS <--> Rabbit
  MsgPRS <--> Rabbit
  ListenerNS <--> Rabbit

  RepoUS --> MySQL
  RepoBS --> MySQL
  RepoSS --> MySQL
  RepoPS --> MySQL
  RepoPRS --> MySQL

  SvcSS --> Redis
  DedupNS --> Redis

  SvcPS --> MercadoPago
  SvcUS --> Cloudinary
  SvcBS --> Cloudinary
```

---

## Regras arquiteturais representadas

- Controllers expõem apenas DTOs (sem exposição direta de entidades JPA).
- Escritas entre serviços são orientadas a eventos via RabbitMQ.
- Leitura cross-service ocorre via Feign em serviços que precisam de consulta externa.
- `api-gateway` é o ponto de validação Firebase e propagação de contexto do usuário.
