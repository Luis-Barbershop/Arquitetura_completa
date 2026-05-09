# 🧩 Diagrama de Componentes — CortaAi (UML)

> **Data:** 08 de maio de 2026  
> **Escopo:** componentes lógicos, contratos e integrações entre módulos

---

## Diagrama de componentes (padrão UML / PlantUML)

```plantuml
@startuml
title CortaAi - Diagrama de Componentes UML
left to right direction
skinparam componentStyle rectangle
skinparam packageStyle rectangle

package "Frontend" {
  component "Páginas e Componentes\n(React 18)" as FE_PAGES
  component "Camada de Serviços\n(frontend/src/services/*Service.js)" as FE_SERVICES
  component "API Wrapper\n(frontend/src/services/api.js)" as FE_API

  FE_PAGES --> FE_SERVICES : usa
  FE_SERVICES --> FE_API : delega HTTP
}

package "Edge" {
  component "API Gateway\n(Spring Cloud Gateway)" as GW
  component "Filtro Auth Firebase" as GW_AUTH
  component "Injetor de Headers\nX-User-*" as GW_HEADERS

  GW --> GW_AUTH : aplica filtro
  GW_AUTH --> GW_HEADERS : contexto autenticado
}

package "Descoberta" {
  component "Discovery Service\n(Eureka)" as EUREKA
}

package "Microsserviços de Domínio" {
  component "User Service" as USER
  component "Barbershop Service" as BARBERSHOP
  component "Schedule Service" as SCHEDULE
  component "Payment Service" as PAYMENT
  component "Product Service" as PRODUCT
  component "Notification Service" as NOTIFICATION
}

package "Infraestrutura" {
  database "MySQL" as MYSQL
  component "RabbitMQ" as RABBIT
  component "Redis" as REDIS
}

package "Sistemas Externos" {
  component "Firebase Auth" as FIREBASE
  component "Mercado Pago" as MP
  component "Cloudinary" as CLOUDINARY
}

FE_API --> GW : REST
GW_AUTH --> FIREBASE : valida token

GW_HEADERS --> USER : REST interno
GW_HEADERS --> BARBERSHOP : REST interno
GW_HEADERS --> SCHEDULE : REST interno
GW_HEADERS --> PAYMENT : REST interno
GW_HEADERS --> PRODUCT : REST interno
GW_HEADERS --> NOTIFICATION : REST interno

GW --> EUREKA : discovery
USER --> EUREKA : registry/discovery
BARBERSHOP --> EUREKA : registry/discovery
SCHEDULE --> EUREKA : registry/discovery
PAYMENT --> EUREKA : registry/discovery
PRODUCT --> EUREKA : registry/discovery
NOTIFICATION --> EUREKA : registry/discovery

USER --> MYSQL : persistência própria
BARBERSHOP --> MYSQL : persistência própria
SCHEDULE --> MYSQL : persistência própria
PAYMENT --> MYSQL : persistência própria
PRODUCT --> MYSQL : persistência própria

USER --> RABBIT : publish/consume eventos
BARBERSHOP --> RABBIT : publish/consume eventos
SCHEDULE --> RABBIT : publish/consume eventos
PAYMENT --> RABBIT : publish/consume eventos
PRODUCT --> RABBIT : publish/consume eventos
NOTIFICATION --> RABBIT : consume eventos

SCHEDULE --> REDIS : cache operacional
NOTIFICATION --> REDIS : deduplicação/idempotência

PAYMENT --> MP : transações/webhooks
USER --> CLOUDINARY : upload mídia
BARBERSHOP --> CLOUDINARY : upload portfólio
@enduml
```

## Regras arquiteturais representadas

- Controllers expõem apenas DTOs.
- Mutações cross-service devem ser orientadas a eventos no RabbitMQ.
- Consultas cross-service devem ocorrer via Feign nos serviços consumidores.
- `api-gateway` é o ponto central de validação Firebase e propagação de contexto do usuário.

## O que é cada coisa (visão lógica)

### Frontend e borda

- **Páginas e Componentes (React 18):** camada de interface que renderiza telas e orquestra ações do usuário.
- **Camada de Serviços (`frontend/src/services/*Service.js`):** encapsula chamadas HTTP por domínio; evita requisição direta em componente.
- **API Wrapper (`frontend/src/services/api.js`):** cliente HTTP base (axios) com interceptação e configuração comum.
- **API Gateway:** ponto único de entrada; roteia endpoints e aplica políticas transversais.
- **Filtro Auth Firebase (gateway):** valida o ID token recebido do cliente.
- **Injetor `X-User-*` (gateway):** propaga identidade confiável para os microsserviços internos.

### Descoberta e comunicação

- **Discovery Service (Eureka):** registro e descoberta de instâncias para roteamento dinâmico entre serviços.
- **RabbitMQ:** barramento de eventos de domínio para comunicação assíncrona e desacoplada.

### Microsserviços de domínio

- **`user-service`:** gestão de usuários (cliente/barbeiro), perfis e dados de conta.
- **`barbershop-service`:** gestão de barbearias, catálogo e informações da operação.
- **`schedule-service`:** agenda, horários, confirmações e fluxo de agendamentos.
- **`payment-service`:** orquestração de pagamentos e ciclo financeiro da transação.
- **`product-service`:** estoque, produtos e movimentações relacionadas.
- **`notification-service`:** envio de notificações e tratamento idempotente de eventos.

### Infra de suporte e integrações externas

- **MySQL:** persistência relacional dos serviços (com isolamento lógico por serviço).
- **Redis:** cache operacional e deduplicação/idempotência.
- **Firebase Auth:** identidade/autenticação externa validada no gateway.
- **Mercado Pago:** processamento externo de pagamentos.
- **Cloudinary:** armazenamento e entrega de mídia (imagens/portfólio).

## Organização entre os documentos

- **Este arquivo (`DIAGRAMA_COMPONENTES.md`)** foca em **visão lógica** (quem depende de quem e qual responsabilidade de cada bloco).
- **`DIAGRAMA_IMPLANTACAO.md`** foca em **visão operacional** (containers, portas, execução e inventário do `docker compose ps`).

## Legenda UML utilizada

- **Dependência (`-->`)**: consumo de contrato/componente.
- **Componente (`component`)**: unidade implantável/lógica da solução.
- **Pacote (`package`)**: fronteira arquitetural.
