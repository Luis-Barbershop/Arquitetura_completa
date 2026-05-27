# 📋 Documentação Técnica e de Negócio — CortaAí

> **Versão:** 1.1 | **Branch:** `feature/migracao-microservicos` | **Data:** 27/05/2026

---

## Sumário

1. [Visão Geral do Sistema](#1-visão-geral-do-sistema)
2. [Tecnologias Utilizadas](#2-tecnologias-utilizadas)
3. [Arquitetura da Aplicação](#3-arquitetura-da-aplicação)
4. [Microserviços — Detalhamento Completo](#4-microserviços--detalhamento-completo)
5. [Funcionalidades e Regras de Negócio](#5-funcionalidades-e-regras-de-negócio)
6. [Casos de Uso](#6-casos-de-uso)
7. [Comunicação entre Serviços](#7-comunicação-entre-serviços)
8. [Bancos de Dados e Acesso](#8-bancos-de-dados-e-acesso)
9. [Infraestrutura e Deploy](#9-infraestrutura-e-deploy)
10. [Relações entre Entidades](#10-relações-entre-entidades)
11. [Guia de Acesso ao Ambiente](#11-guia-de-acesso-ao-ambiente)

---

## 1. Visão Geral do Sistema

O **CortaAí** é uma plataforma digital de **agendamento de serviços de barbearia**. Ele conecta clientes que desejam agendar cortes e serviços com barbearias e barbeiros cadastrados na plataforma.

### O que o sistema faz?

- Permite que **clientes** descubram barbearias, visualizem serviços disponíveis e façam agendamentos online.
- Permite que **barbeiros** gerenciem sua agenda, horários de trabalho e disponibilidade.
- Permite que **donos de barbearia** cadastrem e administrem sua barbearia, serviços, equipe e vitrine fotográfica.
- Integra com **Mercado Pago** para que o pagamento do agendamento possa ser feito online.
- Envia **notificações automáticas** para cliente e barbeiro a cada evento importante (agendamento, cancelamento, pagamento, conclusão).
- Possui módulo de **e-commerce** para venda de produtos das barbearias.

### Atores do Sistema

| Ator | Papel |
|------|-------|
| **Cliente (Customer)** | Agenda serviços, paga online, compra produtos, acompanha notificações |
| **Barbeiro (Barber)** | Gerencia agenda, bloqueia horários, confirma/conclui atendimentos |
| **Dono de Barbearia (Owner)** | É um Barber com `isOwner=true`; cadastra a barbearia, gerencia equipe e serviços |

---

## 2. Tecnologias Utilizadas

### 2.1 Backend — Spring Boot (Java 17)

**O que é:** Framework Java para construção de aplicações web robustas. Cada microserviço é uma aplicação Spring Boot independente.

**Para que serve:** Define os endpoints REST, regras de negócio, acesso ao banco de dados e integração com outros serviços.

**Onde está configurado:** Cada módulo em `backend/<nome-do-servico>/`, com arquivo principal `pom.xml` e configuração em `src/main/resources/application.yml`.

---

### 2.2 Spring Cloud Gateway

**O que é:** Componente Spring que age como um **proxy reverso** e roteador de requisições HTTP.

**Para que serve:** É o **único ponto de entrada** da aplicação. Toda requisição vinda do frontend ou de clientes externos passa pelo Gateway, que decide para qual microserviço encaminhar com base no caminho da URL (ex.: `/api/appointments/**` vai para o `schedule-service`). Também é responsável pela validação do token JWT antes de encaminhar a requisição.

**Onde está configurado:** `backend/api-gateway/src/main/resources/application.yml`

```
/api/customers/**, /api/barbers/**, /api/auth/**  → user-service
/api/barbershops/**                                → barbershop-service
/api/appointments/**                               → schedule-service
/api/payments/**                                   → payment-service
/api/products/**, /api/orders/**                   → product-service
/api/notifications/**                              → notification-service
```

---

### 2.3 Netflix Eureka (Service Discovery)

**O que é:** Um **registro de serviços** (service registry). Funciona como uma "lista telefônica" de microserviços.

**Para que serve:** Quando o `schedule-service` precisa chamar o `user-service`, ele não precisa saber o IP ou porta exata do `user-service`. Ele pergunta ao Eureka: *"Onde está o user-service?"*, e o Eureka responde com o endereço atual. Isso permite que os serviços escalem e mudem de endereço sem quebrar as integrações.

**Onde está configurado:** `backend/discovery-service/` | Porta: **8761** | Interface web acessível em `http://servidor:8761`

---

### 2.4 OpenFeign (Comunicação Síncrona)

**O que é:** Biblioteca que cria **clientes HTTP declarativos** — você define uma interface Java com as chamadas que quer fazer, e o Feign gera o código de rede automaticamente.

**Para que serve:** Permite que um microserviço chame o endpoint de outro de forma simples e transparente, com balanceamento de carga via Eureka integrado.

**Onde está configurado:** Arquivos `feign/*.java` dentro de cada microserviço. Exemplos:
- `barbershop-service/feign/UserServiceClient.java` → chama endpoints do `user-service`
- `schedule-service/feign/BarbershopServiceClient.java` → chama endpoints do `barbershop-service`
- `payment-service/feign/ScheduleServiceClient.java` → chama endpoints do `schedule-service`

---

### 2.5 RabbitMQ (Mensageria Assíncrona)

**O que é:** Um **broker de mensagens** (intermediário). Os serviços publicam eventos (mensagens) em filas, e outros serviços consomem essas filas de forma independente.

**Para que serve:** Permite a comunicação **desacoplada** entre serviços. Por exemplo: quando um agendamento é criado, o `schedule-service` publica um evento no RabbitMQ e não precisa esperar nem saber que o `notification-service` vai processar. O `notification-service` consome a mensagem no seu próprio ritmo. Isso torna o sistema mais resiliente — se o `notification-service` estiver fora do ar, as mensagens ficam na fila e são processadas quando ele voltar.

**Configuração:**
- Exchange único: `cortaai.events` (TopicExchange)
- Interface de gerenciamento: `http://servidor:15673` (prod) / `http://localhost:15672` (local)
- Credenciais: variáveis `RABBITMQ_USER` e `RABBITMQ_PASS` no `.env`

**Eventos que trafegam:**

| Routing Key | Publicado por | Consumido por |
|---|---|---|
| `appointment.created` | schedule-service | notification-service |
| `appointment.cancelled` | schedule-service | notification-service |
| `appointment.concluded` | schedule-service | notification-service |
| `payment.approved` | payment-service | notification-service |

---

### 2.6 Redis (Cache e Deduplicação)

**O que é:** Banco de dados em memória, extremamente rápido, usado para cache e armazenamento temporário de dados.

**Para que serve no sistema:** Tem dois usos distintos:

1. **Cache de horários do barbeiro** (no `schedule-service`): Os horários de início e fim de trabalho do barbeiro são consultados frequentemente ao calcular slots de disponibilidade. Para evitar chamadas repetidas ao `user-service`, o resultado é armazenado no Redis por **5 minutos**.

2. **Deduplicação de eventos** (no `notification-service`): Como o RabbitMQ pode, em casos de falha, entregar a mesma mensagem mais de uma vez, o `notification-service` usa o Redis para registrar quais eventos já foram processados (usando uma chave única por evento). Se o mesmo evento chegar duas vezes, a segunda entrega é ignorada.

**Onde está configurado:**
- `schedule-service/config/RedisConfig.java` — cache com TTL de 5 minutos
- `notification-service/config/RedisConfig.java` — deduplicação de eventos
- Porta: **6380** (servidor) / **6379** (local)

---

### 2.7 MySQL 8.0 (Banco de Dados Relacional)

**O que é:** Sistema de gerenciamento de banco de dados relacional, amplamente usado em produção.

**Para que serve:** Armazena todos os dados persistentes da aplicação — usuários, barbearias, agendamentos, pagamentos, produtos, notificações.

**Padrão adotado:** O sistema usa o padrão **Database per Service** (um banco de dados lógico por microserviço), mas todos estão dentro da **mesma instância MySQL**. Os bancos são criados automaticamente pelo `init.sql` na inicialização.

**Onde está configurado:** `init.sql` na raiz do projeto | Porta: **3307** (servidor) / **3306** (local)

---

### 2.8 Firebase Authentication

**O que é:** Serviço de autenticação gerenciado pelo Google. O CortaAi **não gerencia senhas** — toda identidade é delegada ao Firebase.

**Para que serve:** O usuário faz login via Firebase SDK (email/senha ou Google). O Firebase retorna um **Firebase ID Token** (JWT assinado com chave RSA do Google, expira em 1h). Esse token é enviado em toda requisição e validado pelo API Gateway.

**Custom Claims:** O `user-service` grava no Firebase `custom claims` com `role` (`ROLE_CUSTOMER` ou `ROLE_BARBER`) e `isOwner` (boolean). O API Gateway extrai esses claims e injeta nos headers `X-User-Role` e `X-User-Owner`.

**Onde está configurado:** `api-gateway/config/FirebaseAuthFilter.java` | Credenciais: arquivo `firebase-adminsdk.json` mapeado no container via volume | Variável: `FIREBASE_CREDENTIALS_PATH`

---

### 2.9 Cloudinary (Armazenamento de Imagens)

**O que é:** Serviço de nuvem especializado em upload, transformação e entrega de imagens e vídeos.

**Para que serve:** Toda imagem da aplicação (foto de perfil de cliente, foto de barbeiro, logo de barbearia, banner, fotos de serviços e destaques da vitrine) é armazenada no Cloudinary — **não** no servidor local. Isso garante que as imagens sejam servidas por uma CDN rápida e não sobrecarreguem o servidor.

**Onde está configurado:** `user-service/service/storage/CloudinaryStorageServiceImpl.java` | Credenciais: variáveis `CLOUDINARY_*` no `.env`

---

### 2.10 Mercado Pago (Gateway de Pagamento)

**O que é:** Plataforma de pagamentos online amplamente usada no Brasil, com suporte a cartão, Pix e boleto.

**Para que serve:** O `payment-service` integra com a API **Checkout Pro** do Mercado Pago. Quando um cliente inicia o pagamento de um agendamento, o sistema cria uma "preferência" no Mercado Pago e retorna um link de checkout. O cliente é redirecionado para a página do Mercado Pago para pagar. Após o pagamento, o Mercado Pago notifica o sistema via **webhook** com o resultado.

**Onde está configurado:** `payment-service/config/MercadoPagoConfiguration.java` | Token: variável `MP_ACCESS_TOKEN` no `.env`

---

### 2.11 Docker e Docker Compose

**O que é:** Docker é uma plataforma de containerização — empacota a aplicação e todas as suas dependências em um container isolado. Docker Compose orquestra múltiplos containers.

**Para que serve:** Permite subir toda a infraestrutura (MySQL, RabbitMQ, Redis, todos os microserviços e o frontend) com um único comando, garantindo que o ambiente seja idêntico em qualquer máquina.

**Arquivos:**
- `docker-compose.yml` → ambiente local de desenvolvimento
- `docker-compose.server.yml` → ambiente de produção (servidor, com portas diferentes)

---

### 2.12 Cloudflare Tunnel

**O que é:** Serviço da Cloudflare que cria um túnel seguro e criptografado entre o servidor e a rede da Cloudflare, sem precisar abrir portas no firewall.

**Para que serve:** Expõe a aplicação (API Gateway e Frontend) na internet de forma segura com HTTPS, usando o domínio configurado. O túnel funciona de dentro para fora — o servidor inicia a conexão com a Cloudflare, não o contrário.

**Funcionamento:**
```
Usuário (internet)
    ↓ HTTPS (domínio público, ex: api.cortaai.com.br)
Cloudflare Edge (terminação TLS)
    ↓ Túnel criptografado
cloudflared daemon (rodando no servidor)
    ↓ HTTP interno
API Gateway (porta 8082)
    ↓
Microserviços
```

---

### 2.13 React + Vite (Frontend)

**O que é:** React é a biblioteca JavaScript para construção de interfaces. Vite é o bundler/servidor de desenvolvimento moderno e rápido.

**Para que serve:** É a camada de apresentação da aplicação — telas de cadastro, login, descoberta de barbearias, agendamento, perfil, notificações, etc.

**Onde está configurado:** Pasta `frontend/` | Porta: **5173**

---

### 2.14 SpringDoc OpenAPI (Swagger)

**O que é:** Biblioteca que gera automaticamente a documentação interativa dos endpoints REST a partir do código Java.

**Para que serve:** Permite visualizar e testar todos os endpoints de cada microserviço em uma interface web. O API Gateway agrega a documentação de todos os serviços em um único Swagger UI.

**Acesso:** `http://servidor:8082/swagger-ui.html` (ou porta local 8080)

---

## 3. Arquitetura da Aplicação

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INTERNET / CLOUDFLARE                        │
│              HTTPS com domínio público (Cloudflare Tunnel)          │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
              ┌───────────▼────────────┐
              │      API GATEWAY       │
              │   (Spring Cloud GW)    │◄── Valida JWT, roteia por path
              │      Porta 8082        │
              └───────────┬────────────┘
                          │ Eureka Discovery (lb://)
          ┌───────────────┼────────────────────────────┐
          │               │                            │
    ┌─────▼──────┐ ┌──────▼───────┐  ┌────────────────▼──────┐
    │  user-     │ │ barbershop-  │  │    schedule-service    │
    │  service   │ │   service    │  │    (agendamentos)      │
    │  user_db   │ │barbershop_db │  │     schedule_db        │
    └─────┬──────┘ └──────┬───────┘  └─────────┬─────────────┘
          │◄──Feign────────┘                    │
          │◄──Feign─────────────────────────────┘
          │
    ┌─────▼─────────────┐    ┌───────────────────────┐
    │  payment-service  │    │   product-service      │
    │   payment_db      │    │    product_db          │
    └─────────┬─────────┘    └───────────────────────┘
              │ RabbitMQ (cortaai.events)
    ┌─────────▼─────────────────────────────────────┐
    │            notification-service               │
    │         notification_db  +  Redis              │
    └───────────────────────────────────────────────┘
              ↓ E-mail SMTP / Notificações IN-APP
         Usuário final

    ┌─────────────────────────────────────┐
    │         INFRAESTRUTURA              │
    │  MySQL 8.0 ← 6 bancos lógicos      │
    │  RabbitMQ  ← 4 filas/exchanges     │
    │  Redis     ← cache + deduplicação  │
    │  Eureka    ← service registry       │
    └─────────────────────────────────────┘
```

---

## 4. Microserviços — Detalhamento Completo

### 4.1 user-service

**Responsabilidade:** Gerenciar todos os usuários da plataforma — clientes e barbeiros.

**Banco:** `user_db` — tabelas: `customers`, `barbers`

**Principais recursos:**
- Cadastro e login de clientes (com hash de senha BCrypt e geração de JWT)
- Cadastro e login de barbeiros (com Spring Security + AuthenticationManager)
- Gestão de perfil (atualizar dados, foto de perfil via Cloudinary)
- **Endpoint interno** `/api/internal/users/{id}` usado pelos demais serviços via Feign para buscar dados de usuários

**Portas e endpoints principais:**
- `POST /api/auth/verify` — valida token Firebase e cria/encontra usuário no banco
- `POST /api/auth/email/login` — login por email/senha via Firebase
- `POST /api/auth/email/register` — registro via Firebase
- `POST /api/auth/customers/complete-profile` — completa perfil após cadastro
- `POST /api/auth/barbers/complete-profile` — completa perfil do barbeiro
- `GET /api/customers/{id}` — buscar cliente
- `GET /api/barbers/{id}` — buscar barbeiro
- `PUT /api/customers/me` — atualizar perfil do cliente autenticado
- `DELETE /api/customers/me` — solicitar exclusão (LGPD)
- `GET /api/customers/me/favorites` — barbearias favoritas
- `POST/DELETE /api/customers/me/favorites/{barbershopId}` — gerenciar favoritos

---

### 4.2 barbershop-service

**Responsabilidade:** Gerenciar barbearias, seus serviços (atividades), equipe de barbeiros e vitrine fotográfica.

**Banco:** `barbershop_db` — tabelas: `barbershops`, `activities`, `barbershop_join_requests`, `barbershop_highlights`

**Dependências externas:** Chama `user-service` via Feign para buscar/atualizar dados de barbeiros e donos.

**Principais recursos:**
- CRUD completo de barbearias (apenas para donos)
- CRUD de serviços/atividades da barbearia (nome, preço, duração, foto)
- Sistema de ingresso de barbeiros à barbearia (join request)
- Gestão de imagens: logo, banner e destaques (galeria de fotos)
- **Endpoint interno** `/api/internal/barbershops/{id}` para o `schedule-service`

**Endpoints principais:**
- `POST /api/barbershops` → criar barbearia (requer BARBER autenticado)
- `GET /api/barbershops` → listar todas as barbearias (público)
- `POST /api/barbershops/join` → barbeiro solicita entrar em uma barbearia
- `POST /api/barbershops/join-requests/{id}/approve` → dono aprova solicitação
- `DELETE /api/barbershops/barbers/{id}` → dono remove barbeiro da equipe
- `POST /api/barbershops/activities` → criar serviço
- `PUT /api/barbershops/activities/{id}` → editar serviço
- `DELETE /api/barbershops/activities/{id}` → remover serviço

---

### 4.3 schedule-service

**Responsabilidade:** Gerenciar agendamentos, disponibilidade de horários e bloqueios de agenda dos barbeiros.

**Banco:** `schedule_db` — tabelas: `appointments`, `appointment_activities`, `barber_blocks`

**Dependências externas:**
- Chama `user-service` via Feign → valida cliente e barbeiro, busca horários de trabalho
- Chama `barbershop-service` via Feign → valida barbearia e busca dados das atividades
- Publica eventos no RabbitMQ → `appointment.created`, `appointment.cancelled`, `appointment.concluded`
- Usa Redis → cache dos horários de trabalho do barbeiro (TTL 5 min)

**Principais recursos:**
- Criar agendamento (com validações completas de conflito, bloqueio e existência)
- Confirmar / Cancelar / Concluir agendamento
- Consultar disponibilidade de horários por barbeiro e data
- Gerenciar bloqueios de agenda do barbeiro (ex.: folga, feriado)
- Endpoint interno para payment-service atualizar status de pagamento

---

### 4.4 payment-service

**Responsabilidade:** Gerenciar pagamentos de agendamentos integrado ao Mercado Pago.

**Banco:** `payment_db` — tabelas: `transactions`, `webhook_logs`

**Dependências externas:**
- Chama `schedule-service` via Feign → busca dados do agendamento para criar a preferência de pagamento
- Chama `schedule-service` via Feign → atualiza status do agendamento para `PAID` quando o pagamento é aprovado
- Publica eventos no RabbitMQ → `payment.approved`
- API externa: Mercado Pago (Checkout Pro)

**Principais recursos:**
- Gerar link de checkout do Mercado Pago para um agendamento
- Processar webhooks do Mercado Pago (com idempotência via `WebhookLog`)
- Mapear status do Mercado Pago para o status interno (`PENDING`, `APPROVED`, `REJECTED`, `REFUNDED`, `IN_PROCESS`)
- Consultar pagamentos do cliente

---

### 4.5 notification-service

**Responsabilidade:** Centralizar todas as notificações do sistema, criando notificações in-app e enviando e-mails.

**Banco:** `notification_db` — tabela: `notifications`  
**Cache:** Redis — deduplicação de eventos RabbitMQ

**Dependências externas:**
- Consome eventos do RabbitMQ: `appointment.created`, `appointment.cancelled`, `appointment.concluded`, `payment.approved`
- Servidor SMTP (Gmail ou outro) para envio de e-mails

**Principais recursos:**
- Criar notificações IN_APP automaticamente ao receber eventos
- Garantir que o mesmo evento não gere notificação duplicada (via Redis)
- Marcar notificações como lidas
- Contar notificações não lidas
- Endpoint para o usuário consultar suas notificações

---

### 4.6 product-service

**Responsabilidade:** E-commerce de produtos das barbearias — catálogo, pedidos e controle de estoque.

**Banco:** `product_db` — tabelas: `products`, `orders`, `order_items`, `stock_movements`

**Principais recursos:**
- CRUD de produtos (por barbearia)
- Controle de estoque com movimentações (entrada/saída)
- Criação de pedidos com validação de estoque em tempo real
- Snapshots: ao criar o pedido, nome e preço do produto são copiados no `OrderItem` — o histórico não é afetado se o produto mudar de preço depois
- Listagem de pedidos por cliente

---

### 4.7 api-gateway

**Responsabilidade:** Ponto único de entrada — roteamento, segurança e agregação de documentação.

**Sem banco de dados.**

**Principais recursos:**
- Roteamento de requisições para o microserviço correto
- Validação do JWT (autenticação)
- Agregação do Swagger UI de todos os serviços
- Balanceamento de carga via Eureka

---

### 4.8 discovery-service (Eureka)

**Responsabilidade:** Registro e descoberta de serviços.

**Sem banco de dados.**

**Porta:** 8761 | Interface web: `http://servidor:8761` (mostra quais instâncias estão registradas e saudáveis)

---

## 5. Funcionalidades e Regras de Negócio

### 5.1 Autenticação e Segurança

**Como funciona:**
1. O usuário faz login pelo Firebase SDK (`signInWithEmailAndPassword` ou `signInWithPopup` para Google).
2. O Firebase retorna um **Firebase ID Token** (JWT assinado com chaves RSA públicas do Google, expira em 1h).
3. O frontend envia o token em toda requisição: `Authorization: Bearer <Firebase ID Token>`.
4. O **API Gateway é o único ponto** que valida a assinatura do token (chaves públicas Google, via Firebase Admin SDK).
5. Após validação, o Gateway injeta headers confiáveis para os serviços downstream:
   - `X-User-Id` — Firebase UID
   - `X-User-Email` — email do usuário
   - `X-User-Role` — `ROLE_CUSTOMER` ou `ROLE_BARBER` (extraído do custom claim)
   - `X-User-Owner` — `true` ou `false`
6. Os microsserviços **confiam nesses headers** — não revalidam o token.

**Regras:**
- O CortaAi **não armazena senhas**. Toda autenticação é delegada ao Firebase.
- O custom claim `isOwner` é gravado pelo `user-service` via Firebase Admin SDK ao criar barbearia.
- Rotas públicas (sem auth obrigatório): `POST /api/auth/verify`, `POST /api/auth/email/**`, `POST /api/payments/webhook`, `GET /api/payments/mp-callback`, `GET /api/barbershops/{id}`.

---

### 5.2 Gestão de Barbearias

**Regras de negócio:**
- Apenas um usuário com `userType = BARBER` pode criar uma barbearia.
- **Cada barbeiro pode ser dono de apenas uma barbearia** — tentativas de criar uma segunda resultam em erro `"Você já possui uma barbearia."`.
- O CNPJ é único no sistema — não é possível cadastrar duas barbearias com o mesmo CNPJ.
- Quando uma barbearia é criada, o `barbershopId` do barbeiro-dono é atualizado no `user-service` via Feign.
- O dono pode fechar a barbearia (delete cascata remove serviços, destaques e solicitações pendentes).
- Imagens (logo, banner, destaques) são armazenadas no Cloudinary. Ao atualizar uma imagem, a anterior é excluída do Cloudinary para não gerar lixo.

---

### 5.3 Ingresso de Barbeiros (Join Request)

**Fluxo completo:**
1. Um barbeiro sem barbearia busca uma barbearia pelo CNPJ e envia uma solicitação de ingresso.
2. A solicitação fica com status `PENDING`.
3. O dono da barbearia vê as solicitações pendentes (com nome e e-mail do barbeiro buscados no `user-service`).
4. O dono aprova (`APPROVED`) → o `barbershopId` do barbeiro é atualizado no `user-service` via Feign.

**Regras:**
- Um barbeiro já associado a uma barbearia **não pode** solicitar ingresso em outra sem sair primeiro.
- Não pode haver dois pedidos pendentes do mesmo barbeiro para a mesma barbearia.
- O **dono não pode sair** da barbearia como um barbeiro comum — ele precisa fechar a barbearia.
- O dono pode remover qualquer barbeiro da equipe, o que zera o `barbershopId` do barbeiro no `user-service`.

---

### 5.4 Agendamento de Serviços

**Fluxo completo:**
1. O cliente escolhe uma barbearia, um barbeiro, um ou mais serviços e um horário.
2. O sistema valida via Feign: cliente existe? Barbeiro existe? Barbearia existe? Serviços existem?
3. O sistema calcula o `endTime` somando as durações de todos os serviços selecionados.
4. Verifica **conflito de horário** — se o barbeiro já tem agendamento no período calculado.
5. Verifica **bloqueio de agenda** — se o barbeiro tem um `BarberBlock` no período.
6. Se tudo OK, cria o agendamento com status `SCHEDULED` e **snapshots** dos dados (nomes da barbearia, barbeiro e cliente copiados).
7. Publica evento `appointment.created` no RabbitMQ.
8. O `notification-service` consome o evento e cria notificações para o cliente e para o barbeiro.

**Regras:**
- **Snapshots desnormalizados:** os nomes do cliente, barbeiro e barbearia são **copiados** para o agendamento no momento da criação. Isso garante que, mesmo que alguém mude o nome depois, o histórico do agendamento permanece correto.
- **Cada serviço também vira um snapshot** (`AppointmentActivity`) com nome, preço e duração — por isso o total pago está sempre correto mesmo que o preço do serviço mude depois.
- Um barbeiro não pode ter dois agendamentos sobrepostos.
- A detecção de conflito é: `startTime do novo < endTime existente E endTime do novo > startTime existente`.

**Status do Agendamento:**

```
PAYMENT_PENDING → (pagamento aprovado) → SCHEDULED
        ↓ (30min sem pagamento)
     CANCELLED

SCHEDULED → CONFIRMED → IN_PROGRESS → COMPLETED
    ↓            ↓
 CANCELLED    CANCELLED

WALK_IN   → IN_PROGRESS → COMPLETED  (atendimento imediato)
NO_SHOW                                 (cliente não compareceu)
EXPIRED                                 (PAYMENT_PENDING + start_time passado, lazy)
```

- `PAYMENT_PENDING` → agendamento criado aguardando pagamento online
- `SCHEDULED` → agendamento confirmado (local ou após pagamento aprovado)
- `CONFIRMED` → barbeiro/dono confirmou explicitamente
- `IN_PROGRESS` → atendimento em andamento
- `COMPLETED` → atendimento concluído (dispara notificação de avaliação)
- `CANCELLED` → cancelado por cliente, barbeiro ou dono
- `WALK_IN` → atendimento imediato sem agendamento prévio
- `NO_SHOW` → cliente não compareceu (não bloqueia slots futuros)
- `EXPIRED` → projeção lazy (PAYMENT_PENDING + start_time passado)

**Quem pode cancelar:**
- O próprio **cliente** do agendamento
- O **barbeiro** do agendamento
- O **dono da barbearia** (verificado via Feign no `barbershop-service`)

---

### 5.5 Disponibilidade de Horários

**Como funciona:**
1. O frontend solicita os slots disponíveis para um barbeiro em uma data específica.
2. O `schedule-service` busca os **horários de trabalho do barbeiro** no `user-service` (resultado cacheado no Redis por 5 min).
3. Busca todos os **agendamentos ativos** do barbeiro no dia.
4. Busca todos os **bloqueios** do barbeiro no dia.
5. Gera slots de **30 em 30 minutos** dentro do horário de trabalho.
6. Marca cada slot como `available: true` ou `false` baseado em sobreposição com agendamentos ou bloqueios.

**Regra:** Agendamentos com status `CANCELLED` ou `NO_SHOW` **não** bloqueiam slots.

---

### 5.6 Bloqueios de Agenda (BarberBlock)

**O que é:** O barbeiro pode bloquear períodos da sua agenda (folga, almoço, compromisso pessoal) sem criar um agendamento real.

**Regras:**
- O barbeiro só pode criar bloqueios **para si mesmo**.
- O `endTime` deve ser posterior ao `startTime`.
- Não pode haver sobreposição entre dois bloqueios do mesmo barbeiro.
- O bloqueio também pode ser excluído apenas pelo próprio barbeiro.

---

### 5.7 Pagamento via Mercado Pago

**Fluxo completo:**
1. O cliente inicia o pagamento de um agendamento via `POST /api/payments`.
2. O `payment-service` busca os dados do agendamento via Feign no `schedule-service`.
3. Valida que o cliente autenticado é o mesmo do agendamento.
4. Cria uma **preferência de pagamento** no Mercado Pago com: título, descrição, valor, URL de retorno e URL do webhook.
5. Salva a `Transaction` com status `PENDING` e o link de checkout.
6. Retorna o `checkoutUrl` para o frontend redirecionar o cliente.
7. O cliente paga no Mercado Pago.
8. O Mercado Pago chama o webhook configurado (`/api/payments/webhook`).
9. O `payment-service` verifica idempotência no `WebhookLog` (evita processar o mesmo webhook duas vezes).
10. Consulta o pagamento na API do Mercado Pago para confirmar o status real.
11. Se aprovado:
    - Atualiza a `Transaction` para `APPROVED`.
    - Chama o `schedule-service` via Feign para atualizar o status do agendamento para `PAID`.
    - Publica evento `payment.approved` no RabbitMQ.
12. O `notification-service` consome o evento e notifica o cliente do pagamento aprovado.

**Regras:**
- Não se pode criar dois pagamentos para o mesmo agendamento com status `PENDING` ou `APPROVED`.
- A idempotência do webhook é garantida pela tabela `webhook_logs`: se o mesmo `mpResourceId` já foi processado (`processed=true`), a requisição é ignorada.
- O status do Mercado Pago é mapeado para o status interno do sistema.

---

### 5.8 Notificações

**Como funciona:** O `notification-service` escuta quatro filas do RabbitMQ e, ao receber um evento, cria registros de notificação no banco e pode enviar e-mail.

**Eventos e notificações geradas:**

| Evento | Para quem | Mensagem |
|---|---|---|
| `appointment.created` | Cliente | "Seu agendamento na {barbearia} com {barbeiro} foi confirmado para {data}." |
| `appointment.created` | Barbeiro | "Você tem um novo agendamento com {cliente} em {data}." |
| `appointment.cancelled` (pelo cliente) | Barbeiro | "O cliente cancelou o agendamento." |
| `appointment.cancelled` (pelo barbeiro) | Cliente | "O barbeiro cancelou o seu agendamento. Tente agendar novamente." |
| `appointment.concluded` | Cliente | "Seu atendimento foi concluído. Que tal deixar uma avaliação?" |
| `payment.approved` | Cliente | "Seu pagamento de R$ {valor} foi aprovado com sucesso." |

**Deduplicação:** Antes de processar cada evento, o `notification-service` verifica no Redis se aquele `appointmentId` ou `transactionId` já foi processado para aquele tipo de evento. Se sim, o evento é descartado silenciosamente.

---

### 5.9 E-Commerce de Produtos

**Como funciona:**
- Donos de barbearia cadastram produtos com nome, descrição, preço, categoria, quantidade em estoque e foto.
- Clientes fazem pedidos selecionando produtos de uma barbearia.
- No momento do pedido:
  1. Valida estoque de cada produto.
  2. Cria o pedido com os itens em snapshot (nome e preço copiados).
  3. Deduz a quantidade do estoque de cada produto.
  4. Registra movimentações de estoque do tipo `OUT`.

**Regras:**
- Produto inativo não pode ser comprado.
- Se o estoque for insuficiente para qualquer item, o pedido inteiro é rejeitado.
- O histórico de movimentações de estoque é mantido para rastreabilidade.

---

## 6. Casos de Uso

### UC-01: Cliente se cadastra na plataforma
**Ator:** Cliente  
**Fluxo:**
1. Faz login pelo Firebase SDK (email/senha ou Google).
2. Sistema chama `POST /api/auth/verify` com o Firebase ID Token.
3. `user-service` valida o token via Firebase Admin SDK e cria o registro no banco.
4. Cliente completa perfil em `POST /api/auth/customers/complete-profile` com nome, telefone, CPF.
5. `user-service` criptografa CPF, email e telefone (AES/GCM) e grava `email_hash`.
6. Retorna o UUID do cliente criado.

---

### UC-02: Barbeiro se cadastra na plataforma
**Ator:** Barbeiro  
**Fluxo:**
1. Faz login pelo Firebase SDK.
2. Sistema chama `POST /api/auth/verify` com `userType: BARBER`.
3. Completa perfil em `POST /api/auth/barbers/complete-profile` com dados pessoais e horário de trabalho.
4. Cria conta com `role = ROLE_BARBER` e `isOwner = false` (custom claim no Firebase).

---

### UC-03: Barbeiro cria sua barbearia
**Ator:** Barbeiro autenticado  
**Pré-condição:** O barbeiro não pode já ser dono de outra barbearia.  
**Fluxo:**
1. Envia dados da barbearia (nome, CNPJ, endereço) e opcionalmente logo.
2. Sistema valida CNPJ único.
3. Cria a barbearia com `ownerId = ID do barbeiro`.
4. Atualiza `barbershopId` do barbeiro no `user-service`.
5. Marca o barbeiro como `isOwner = true` (implicito pelo JWT).

---

### UC-04: Barbeiro solicita ingresso em uma barbearia
**Ator:** Barbeiro autenticado sem barbearia  
**Fluxo:**
1. Barbeiro informa o CNPJ da barbearia.
2. Sistema cria `BarbershopJoinRequest` com status `PENDING`.
3. Dono da barbearia vê a solicitação em sua listagem.
4. Dono aprova → sistema atualiza `barbershopId` do barbeiro.

---

### UC-05: Cliente agenda um serviço
**Ator:** Cliente autenticado  
**Fluxo:**
1. Cliente escolhe barbearia, barbeiro, serviço(s) e data/hora.
2. Sistema valida via Feign os envolvidos.
3. Sistema calcula duração total e `endTime`.
4. Sistema verifica conflitos de horário e bloqueios de agenda.
5. Cria agendamento com snapshots e publica evento no RabbitMQ.
6. Cliente e barbeiro recebem notificação.

---

### UC-06: Barbeiro consulta sua agenda do dia
**Ator:** Barbeiro autenticado  
**Fluxo:**
1. Barbeiro informa sua data desejada.
2. Sistema retorna lista de agendamentos do dia com status, cliente, serviços e horários.

---

### UC-07: Dono consulta disponibilidade de horários
**Ator:** Qualquer usuário autenticado  
**Fluxo:**
1. Informa o UUID do barbeiro e a data.
2. Sistema busca horário de trabalho do barbeiro (cache Redis 5 min).
3. Sistema gera slots de 30 min e marca disponíveis/ocupados.
4. Retorna lista de slots com `available: true/false`.

---

### UC-08: Barbeiro bloqueia horário da agenda
**Ator:** Barbeiro autenticado  
**Fluxo:**
1. Informa período (start/end) e motivo.
2. Sistema verifica sobreposição com bloqueios existentes.
3. Cria `BarberBlock` — o período não aparece mais como disponível.

---

### UC-09: Cliente paga o agendamento online
**Ator:** Cliente autenticado  
**Fluxo:**
1. Cliente solicita link de pagamento para um agendamento.
2. Sistema cria preferência no Mercado Pago e retorna `checkoutUrl`.
3. Cliente é redirecionado para o checkout do Mercado Pago.
4. Após pagamento, Mercado Pago notifica o sistema via webhook.
5. Sistema processa o webhook (com idempotência) e atualiza os status.
6. Cliente recebe notificação de pagamento aprovado.

---

### UC-10: Cliente cancela agendamento
**Ator:** Cliente autenticado  
**Fluxo:**
1. Cliente solicita cancelamento de um agendamento seu.
2. Sistema verifica que o agendamento não está já cancelado.
3. Atualiza status para `CANCELLED`.
4. Publica evento `appointment.cancelled` → barbeiro é notificado.

---

### UC-11: Barbeiro conclui atendimento
**Ator:** Barbeiro autenticado  
**Fluxo:**
1. Barbeiro marca agendamento como concluído.
2. Sistema verifica que o barbeiro é o responsável pelo agendamento.
3. Atualiza status para `CONCLUDED`.
4. Publica evento `appointment.concluded` → cliente recebe notificação solicitando avaliação.

---

### UC-12: Dono gerencia vitrine fotográfica
**Ator:** Dono da barbearia  
**Fluxo:**
1. Envia imagens para destaques da barbearia.
2. Sistema faz upload para Cloudinary e salva como `BarbershopHighlight`.
3. Pode excluir destaques — imagem é removida do Cloudinary.
4. Pode atualizar logo e banner da barbearia.

---

### UC-13: Cliente compra produtos
**Ator:** Cliente autenticado  
**Fluxo:**
1. Cliente visualiza catálogo de produtos de uma barbearia.
2. Seleciona itens e quantidades.
3. Sistema valida estoque de cada item.
4. Cria pedido com snapshots dos preços e deduz estoque.
5. Registra movimentações de estoque.

---

## 7. Comunicação entre Serviços

### 7.1 Comunicação Síncrona (OpenFeign)

Chamadas HTTP diretas via endpoints internos, balanceadas pelo Eureka:

```
barbershop-service → user-service
    GET  /api/internal/users/{id}            → buscar usuário por ID
    GET  /api/internal/users/by-email/{email}→ buscar usuário por e-mail
    PUT  /api/internal/users/{id}/barbershop → atualizar barbershopId do barbeiro

schedule-service → user-service
    GET  /api/internal/users/{id}            → validar cliente/barbeiro
    GET  /api/internal/users/by-email/{email}→ resolver caller pelo e-mail do JWT

schedule-service → barbershop-service
    GET  /api/internal/barbershops/{id}       → validar e buscar dados da barbearia
    GET  /api/internal/barbershops/{id}/activities?ids=... → buscar dados dos serviços

payment-service → schedule-service
    GET  /api/internal/appointments/{id}     → buscar dados do agendamento para criar cobrança
    PUT  /api/internal/appointments/{id}/payment-status → atualizar status após pagamento
```

### 7.2 Comunicação Assíncrona (RabbitMQ)

Exchange: `cortaai.events` (TopicExchange)

```
schedule-service    →[appointment.created]→     notification-service
schedule-service    →[appointment.cancelled]→   notification-service
schedule-service    →[appointment.concluded]→   notification-service
schedule-service    →[appointment.rescheduled]→ notification-service
schedule-service    →[appointment.reminder]→    notification-service  (scheduler — 5min)
payment-service     →[payment.approved]→        notification-service
barbershop-service  →[barbershop.join-request.created]→ notification-service
barbershop-service  →[barber.removed]→          notification-service
user-service        →[customer.deleted]→        schedule-service      (anonimiza agendamentos)
user-service        →[customer.deleted]→        payment-service       (anonimiza transações)
user-service        →[customer.deleted]→        notification-service  (deleta notificações)
```

As filas são **durable** (sobrevivem a restart do RabbitMQ) e as mensagens são serializadas em JSON via `Jackson2JsonMessageConverter`.

---

## 8. Bancos de Dados e Acesso

### 8.1 Quantos bancos existem?

Existe **1 instância MySQL** com **6 bancos lógicos** separados (um por microserviço), seguindo o padrão *Database per Service*. Os bancos não possuem chaves estrangeiras entre si — a ligação é feita por UUIDs armazenados como colunas simples.

| Banco | Dono | Tabelas |
|---|---|---|
| `user_db` | user-service | `customers`, `barbers`, `customer_favorite_barbershops` |
| `barbershop_db` | barbershop-service | `barbershops`, `activities`, `barbershop_join_requests`, `barbershop_highlights`, `barber_commission_rules`, `fixed_expenses`, `barbershop_reviews` |
| `schedule_db` | schedule-service | `appointments`, `appointment_activities`, `barber_blocks` |
| `payment_db` | payment-service | `transactions`, `webhook_logs`, `dashboard_kpi_daily` |
| `product_db` | product-service | `products`, `categories`, `stock_movements` |
| `notification_db` | notification-service | `notifications`, `device_tokens` |

Além do MySQL, o **Redis** é usado como armazenamento em memória por dois serviços (`schedule-service` e `notification-service`).

### 8.2 Portas dos serviços de dados

| Serviço | Porta (local) | Porta (servidor) | Observação |
|---|---|---|---|
| MySQL | 3306 | **3307** | Volume: `/DATA/cortaai/mysql` no servidor |
| RabbitMQ AMQP | 5672 | **5673** | Protocolo de mensagens |
| RabbitMQ Management | 15672 | **15673** | Interface web de gerenciamento |
| Redis | 6379 | **6380** | Cache em memória |

### 8.3 Como acessar o MySQL pelo terminal

```bash
# Acessar o container MySQL
docker exec -it cortaai-mysql mysql -u root -p
# (informe a senha do MYSQL_ROOT_PASSWORD no .env)

# Listar todos os bancos
SHOW DATABASES;

# Entrar em um banco específico
USE user_db;
SHOW TABLES;
SELECT * FROM customers LIMIT 10;
```

### 8.4 Como acessar o MySQL via ferramenta gráfica (DBeaver, TablePlus, Workbench)

```
Host:     IP_DO_SERVIDOR ou domínio
Porta:    3307  (servidor) / 3306 (local)
Usuário:  root  (ou valor de DB_USERNAME)
Senha:    valor de DB_PASSWORD no .env
```

### 8.5 Como acessar o RabbitMQ Management

```
URL:      http://IP_DO_SERVIDOR:15673  (servidor)
          http://localhost:15672        (local)
Usuário:  valor de RABBITMQ_USER no .env
Senha:    valor de RABBITMQ_PASS no .env
```

### 8.6 Como acessar o Redis

```bash
docker exec -it cortaai-redis redis-cli

# Listar todas as chaves
KEYS *

# Ver o valor de uma chave
GET "nome-da-chave"
```

---

## 9. Infraestrutura e Deploy

### 9.1 Arquivos de configuração de ambiente

| Arquivo | Uso |
|---|---|
| `.env.example` | Template com todas as variáveis necessárias |
| `.env` | Arquivo real com os valores (nunca versionado) |
| `docker-compose.yml` | Ambiente local de desenvolvimento |
| `docker-compose.server.yml` | Ambiente de produção (servidor) |
| `init.sql` | Criação dos 6 bancos MySQL na primeira inicialização |
| `deploy-server.sh` | Script de deploy no servidor |

### 9.2 Variáveis de ambiente necessárias

```bash
# MySQL
MYSQL_ROOT_PASSWORD=   # Senha root do MySQL
DB_USERNAME=           # Usuário das aplicações
DB_PASSWORD=           # Senha do usuário das aplicações

# Firebase
FIREBASE_CREDENTIALS_PATH= # Caminho para o arquivo firebase-adminsdk.json

# RabbitMQ
RABBITMQ_USER=         # Usuário do RabbitMQ
RABBITMQ_PASS=         # Senha do RabbitMQ

# Mercado Pago
MP_ACCESS_TOKEN=       # Token da plataforma CortaAi no Mercado Pago
MP_CLIENT_ID=          # Client ID para OAuth do lojista
MP_CLIENT_SECRET=      # Client Secret para OAuth do lojista
MP_MARKETPLACE_FEE_PERCENT= # Taxa da plataforma (padrão: 5.0)

# Cloudinary (armazenamento de imagens)
CLOUDINARY_CLOUD_NAME= # Nome do cloud no Cloudinary
CLOUDINARY_API_KEY=    # API Key do Cloudinary
CLOUDINARY_API_SECRET= # API Secret do Cloudinary

# Criptografia de dados PII (LGPD)
CORTAAI_DATA_CRYPTO_KEY= # Chave AES/GCM para campos sensíveis (CPF, email, telefone)

# E-mail (SMTP)
MAIL_HOST=             # Host SMTP (ex: smtp.gmail.com)
MAIL_PORT=             # Porta SMTP (ex: 587)
MAIL_USERNAME=         # E-mail de envio
MAIL_PASSWORD=         # Senha de app do e-mail
NOTIFICATION_FROM_EMAIL= # E-mail remetente nas notificações
```

### 9.3 Cloudflare Tunnel — Fluxo de Acesso Externo

```
Usuário final (qualquer lugar do mundo)
    ↓  HTTPS — domínio público (ex: api.cortaai.com.br)
Cloudflare Edge (rede global, termina o TLS)
    ↓  Túnel criptografado (iniciado de dentro do servidor)
cloudflared daemon (processo rodando no servidor)
    ↓  HTTP local
API Gateway (porta 8082 no servidor)
    ↓  Eureka load balance
Microserviço correto
```

**Vantagens do Cloudflare Tunnel:**
- Não é necessário abrir portas no firewall (nem ter IP fixo).
- HTTPS automático com certificado gerenciado pela Cloudflare.
- Proteção DDoS gratuita da Cloudflare.
- O MySQL, Redis e RabbitMQ ficam **totalmente inacessíveis** pela internet — só o API Gateway é exposto.

---

## 10. Relações entre Entidades

### 10.1 Visão Macro das Relações

Como o sistema usa microserviços com bancos separados, as relações entre entidades de bancos diferentes são feitas por **UUID referencial** (sem FK de banco cruzando bancos). Veja o mapa:

```
[user_db]
  customers (id UUID)
  barbers   (id UUID, barbershopId → barbershop_db.barbershops.id)
       ↑ referenciado em:
       │── barbershop_db.barbershops.ownerId
       │── barbershop_db.barbershop_join_requests.barberId
       │── barbershop_db.barbershop_highlights.barbershopId (via barbershop)
       │── schedule_db.appointments.customerId / barberId
       │── schedule_db.barber_blocks.barberId
       │── payment_db.transactions.customerId
       │── notification_db.notifications.userId
       └── product_db.orders.customerId

[barbershop_db]
  barbershops (id UUID, ownerId → user_db.barbers.id)
      ├── activities          (barbershop_id FK — DENTRO do mesmo banco)
      ├── barbershop_join_requests (barbershop_id FK, barberId → user_db)
      └── barbershop_highlights   (barbershop_id FK)
       ↑ referenciado em:
       │── barbers.barbershopId (user_db)
       │── appointments.barbershopId (schedule_db)
       └── products.barbershopId (product_db)

[schedule_db]
  appointments (customerId→user_db, barberId→user_db, barbershopId→barbershop_db)
      └── appointment_activities (appointment_id FK — mesmo banco)
                                  activityId → barbershop_db.activities.id (referencial)
  barber_blocks (barberId → user_db.barbers.id)

[payment_db]
  transactions (appointmentId → schedule_db, customerId → user_db)
  webhook_logs (independente — log de idempotência)

[product_db]
  products (barbershopId → barbershop_db)
  orders (customerId → user_db, barbershopId → barbershop_db)
      └── order_items (order_id FK — mesmo banco, productId → products — mesmo banco)
  stock_movements (productId → products — mesmo banco)

[notification_db]
  notifications (userId → user_db.customers.id ou user_db.barbers.id)
```

### 10.2 Relações DENTRO de cada banco (JPA/FK reais)

**barbershop_db:**
- `Barbershop` 1 → N `Activity` (cascade ALL)
- `Barbershop` 1 → N `BarbershopJoinRequest` (cascade ALL)
- `Barbershop` 1 → N `BarbershopHighlight` (cascade ALL)

**schedule_db:**
- `Appointment` 1 → N `AppointmentActivity` (cascade ALL)

**product_db:**
- `Order` 1 → N `OrderItem` (cascade ALL)

---

## 11. Guia de Acesso ao Ambiente

### 11.1 Portas e serviços expostos no servidor

| Serviço | Porta no servidor | Como acessar |
|---|---|---|
| API Gateway | 8082 | Via Cloudflare Tunnel (domínio) |
| Eureka Dashboard | 8761 | `http://servidor:8761` (interno) |
| RabbitMQ Management | 15673 | `http://servidor:15673` |
| MySQL | 3307 | Client MySQL (DBeaver etc.) |
| Redis | 6380 | `docker exec` ou cliente Redis |
| Frontend | 5173 | Via Cloudflare Tunnel (domínio) |

### 11.2 Portas locais (docker-compose.yml)

| Serviço | Porta |
|---|---|
| API Gateway | 8080 |
| Eureka Dashboard | 8761 |
| RabbitMQ Management | 15672 |
| MySQL | 3306 |
| Redis | 6379 |
| Frontend | 5173 |

### 11.3 Swagger UI — Documentação dos Endpoints

Acesse `http://servidor:8082/swagger-ui.html` (ou `http://localhost:8080/swagger-ui.html` localmente) para ver e testar todos os endpoints de todos os microserviços em uma única interface.

### 11.4 Comandos úteis no servidor

```bash
# Ver containers rodando
docker ps

# Ver logs de um serviço
docker logs -f schedule-service
docker logs -f cortaai-mysql
docker logs -f cortaai-rabbitmq

# Acessar o MySQL
docker exec -it cortaai-mysql mysql -u root -p

# Acessar o Redis CLI
docker exec -it cortaai-redis redis-cli

# Reiniciar um serviço específico
docker restart schedule-service

# Subir toda a stack (produção)
docker-compose -f docker-compose.server.yml up -d

# Ver uso de recursos
docker stats
```

---

*Documentação gerada a partir da análise do código-fonte do repositório `AppCortaAi/Arquitetura_completa`, branch `feature/migracao-microservicos`.*
