# Diagrama de Componentes e Implantacao - CortaAi

> Data: 13/05/2026  
> Escopo: visao consolidada dos componentes logicos e da topologia de implantacao do CortaAi.

> Diagramas em Markdown puro, usando caracteres, sem dependencia de Mermaid.

---

## 1. Diagrama de Componentes

```text
+--------------------------------------------------------------+
|                      FRONTEND                                |
|                                                              |
|  +----------------+     +----------------+     +-----------+ |
|  | Paginas React  | --> | Services JS    | --> | Axios API | |
|  | Componentes    |     | por dominio    |     | Wrapper   | |
|  +----------------+     +----------------+     +-----+-----+ |
+--------------------------------------------------------|-----+
                                                         |
                                                         | REST /api/*
                                                         v
+--------------------------------------------------------------+
|                         EDGE                                 |
|                                                              |
|  +-------------------+     +-------------------+             |
|  | api-gateway       | --> | Firebase Token    |             |
|  | Spring Gateway    |     | Gateway Filter    |             |
|  | :8080             |     +---------+---------+             |
|  +---------+---------+               |                       |
|            |                         | valida ID token        |
|            |                         v                       |
|            |                  +--------------+                |
|            |                  | Firebase Auth|                |
|            |                  +--------------+                |
|            |                                                 |
|            v                                                 |
|  +-------------------+                                       |
|  | Contexto confiavel|                                       |
|  | X-User-*          |                                       |
|  | X-Correlation-Id  |                                       |
|  +---------+---------+                                       |
+------------|-------------------------------------------------+
             |
             | roteamento /api/*
             v
+--------------------------------------------------------------+
|                  MICROSSERVICOS DE DOMINIO                   |
|                                                              |
|  +-------------------+     +-------------------+             |
|  | user-service      |     | barbershop-service|             |
|  | :8081             |     | :8082             |             |
|  | usuarios/perfis   |     | barbearias/equipe |             |
|  +---------+---------+     +---------+---------+             |
|            |                         |                       |
|            | Feign                   | Feign                 |
|            |                         |                       |
|  +---------v---------+     +---------v---------+             |
|  | schedule-service  |     | payment-service   |             |
|  | :8083             |     | :8084             |             |
|  | agenda/bloqueios  |     | pagamentos        |             |
|  +---------+---------+     +---------+---------+             |
|            |                         |                       |
|            | eventos                 | eventos               |
|            |                         |                       |
|  +---------v---------+     +---------v---------+             |
|  | notification-svc  |     | product-service   |             |
|  | :8085             |     | :8086             |             |
|  | email/push        |     | estoque/produtos  |             |
|  +-------------------+     +-------------------+             |
+------------|-------------------------|-----------------------+
             |                         |
             | registro/lookup         | persistencia/eventos/cache
             v                         v
+---------------------------+     +----------------------------+
|         DESCOBERTA        |     |       INFRAESTRUTURA       |
|                           |     |                            |
|  +---------------------+  |     |  +----------------------+  |
|  | discovery-service   |  |     |  | MySQL 8              |  |
|  | Eureka :8761        |  |     |  | schemas por servico  |  |
|  +---------------------+  |     |  +----------------------+  |
|                           |     |  +----------------------+  |
|                           |     |  | RabbitMQ             |  |
|                           |     |  | eventos assincronos  |  |
|                           |     |  +----------------------+  |
|                           |     |  +----------------------+  |
|                           |     |  | Redis                |  |
|                           |     |  | cache / dedup        |  |
|                           |     |  +----------------------+  |
+---------------------------+     +----------------------------+

+--------------------------------------------------------------+
|                    SISTEMAS EXTERNOS                         |
|                                                              |
|  Firebase Auth/FCM | Cloudinary | Mercado Pago | SMTP | IA   |
+--------------------------------------------------------------+
```

### Leitura do diagrama de componentes

| Camada | Responsabilidade |
|---|---|
| Frontend | Renderiza a SPA, organiza paginas/componentes e concentra chamadas HTTP nos services JS. |
| Edge | Centraliza entrada externa, CORS, autenticacao Firebase, roteamento e propagacao de identidade para os servicos. |
| Descoberta | Registra e localiza instancias via Eureka para o gateway e chamadas internas. |
| Dominio | Separa usuarios, barbearias, agenda, pagamentos, notificacoes e estoque em microsservicos independentes. |
| Infraestrutura | Sustenta persistencia relacional, eventos assincronos e cache/deduplicacao. |
| Sistemas externos | Provedores consumidos pela aplicacao: Firebase, Cloudinary, Mercado Pago, email e IA. |

---

## 2. Diagrama de Implantacao

```text
                              +--------------------------+
                              | Usuario / Browser        |
                              +------------+-------------+
                                           |
                                           | HTTPS / SPA
                                           |
                                           v
                              +--------------------------+
                              | cortaai-web :5173        |
                              | React 19 + Vite          |
                              +------------+-------------+
                                           |
                                           | REST /api/*
                                           |
                                           v
                              +--------------------------+          +--------------------------+
                              | api-gateway :8080        | <------> | discovery-service :8761  |
                              | auth + rotas             |          | Eureka                   |
                              +------------+-------------+          +--------------------------+
                                           |
                                           | rotas internas
                                           |
                                           v
+------------------------------------------------------------------------------------------------------+
| MICROSSERVICOS - containers Spring Boot                                                              |
|                                                                                                      |
|  +------------------+       +--------------------+       +--------------------+                      |
|  | user-service     |       | barbershop-service |       | schedule-service   |                      |
|  | :8081            |       | :8082              |       | :8083              |                      |
|  +------------------+       +--------------------+       +--------------------+                      |
|                                                                                                      |
|  +------------------+       +--------------------+       +--------------------+                      |
|  | payment-service  |       | notification-svc   |       | product-service    |                      |
|  | :8084            |       | :8085              |       | :8086              |                      |
|  +------------------+       +--------------------+       +--------------------+                      |
+-------------------+--------------------------------+--------------------------------+---------------+
                    |                                |                                |
                    | JDBC                           | AMQP                           | Redis
                    |                                |                                |
                    v                                v                                v
+---------------------------+          +------------------------+          +--------------------------+
| cortaai-mysql :3306       |          | cortaai-rabbitmq       |          | cortaai-redis :6379      |
|                           |          | AMQP :5672             |          | cache / dedup            |
| Schemas:                  |          | Management :15672      |          +--------------------------+
| - user_db                 |          +------------------------+
| - barbershop_db           |
| - schedule_db             |
| - payment_db              |
| - notification_db         |
| - product_db              |
+---------------------------+

                                           chamadas externas dos servicos
                                                       |
                                                       |
                                                       v
+---------------------------------------------------------------------------------------------------------------------------------+
| DEPENDENCIAS EXTERNAS                                                                                                           |
|                                                                                                                                 |
|  +----------------+     +---------------+     +-------------------+     +----------------+     +-------------------------------+ |
|  | Firebase       |     | Cloudinary    |     | Mercado Pago      |     | SMTP / email   |     | Provedores IA                 | |
|  | Auth / FCM     |     | imagens       |     | pagamentos/hooks  |     | provider       |     | OpenRouter / Cohere            | |
|  +----------------+     +---------------+     +-------------------+     +----------------+     +-------------------------------+ |
+---------------------------------------------------------------------------------------------------------------------------------+
```

### Inventario de implantacao

| Container | Porta | Papel |
|---|---:|---|
| `cortaai-web` | 5173 | SPA React/Vite servida em desenvolvimento/container. |
| `api-gateway` | 8080 | Entrada unica da API, autenticacao, CORS, roteamento e headers de contexto. |
| `discovery-service` | 8761 | Registro e descoberta de servicos via Eureka. |
| `user-service` | 8081 | Usuarios, perfis, autenticacao de dominio e imagens de perfil. |
| `barbershop-service` | 8082 | Barbearias, equipe, servicos, avaliacoes e portfolio. |
| `schedule-service` | 8083 | Agendamentos, disponibilidade, bloqueios, agenda e assistente IA. |
| `payment-service` | 8084 | Pagamentos, Mercado Pago, webhooks e visao financeira. |
| `notification-service` | 8085 | Notificacoes, email, push e deduplicacao de eventos. |
| `product-service` | 8086 | Produtos, categorias, estoque e movimentacoes. |
| `cortaai-mysql` | 3306 | Banco relacional com schemas separados por servico. |
| `cortaai-rabbitmq` | 5672 / 15672 | Broker AMQP e console de administracao. |
| `cortaai-redis` | 6379 | Cache e suporte a idempotencia/deduplicacao. |

### Regras arquiteturais representadas

- O frontend chama apenas o `api-gateway` pela base `/api`.
- O gateway valida o token Firebase e encaminha contexto confiavel para os microsservicos.
- Cada servico possui fronteira de dominio propria e schema relacional separado.
- Integracoes sincronas entre servicos usam Feign/Eureka quando necessario.
- Eventos de dominio e notificacoes cross-service passam pelo RabbitMQ.
- Redis fica restrito a cache operacional, deduplicacao e idempotencia.
- Sistemas externos permanecem fora da malha Docker e sao consumidos por adapters dos servicos.

### Fontes usadas para consolidacao

- `docker-compose.yml`
- `backend/api-gateway/src/main/resources/application.yml`
- `init.sql`
- `analises/DIAGRAMA_COMPONENTES.md`
- `analises/DIAGRAMA_IMPLANTACAO.md`
- `analises/ARQUITETURA.md`
