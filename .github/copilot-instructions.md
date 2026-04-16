# CortaAi — Contexto Mestre para Assistentes de IA

> **Leia este arquivo ANTES de qualquer sugestão, geração de código ou refatoração.**
> Este documento define as regras inquebráveis do projeto. Violações serão rejeitadas em code review.
> Responda sempre em **português brasileiro**, com tom técnico e direto.

---

## 1. Contexto do Projeto

- **Nome:** CortaAi — Sistema de Gestão e Marketplace para Barbearias.
- **Modelo de negócio:** Multi-tenant. Um cliente agenda em qualquer barbearia; um barbeiro pode ser dono (owner) ou funcionário; estoque, serviços e pagamentos são isolados por barbearia.
- **Branch atual:** `feature/migracao-microservicos` — **migração ativa de Monolito → Microsserviços**. Código legado pode coexistir temporariamente, mas **toda nova feature DEVE ser escrita no padrão microsserviço**.
- **Idioma:** Código, commits e documentação em português brasileiro. Nomes de classes/métodos em inglês (padrão Java/Spring).

---

## 2. Stack Tecnológica (Fixa — não sugerir alternativas)

### Backend
- **Linguagem:** Java 17+
- **Framework:** Spring Boot 3.x, Spring Cloud, Spring Data JPA, Spring Security (no gateway)
- **Comunicação síncrona:** OpenFeign (apenas para consultas cross-service)
- **Comunicação assíncrona:** RabbitMQ (eventos de domínio — padrão obrigatório para mutações cross-service)
- **Persistência:** PostgreSQL (produção) / MySQL (dev local conforme `docker-compose.yml`)
- **Cache e desduplicação:** Redis
- **Service Discovery:** Eureka (`discovery-service`)
- **API Gateway:** Spring Cloud Gateway (`api-gateway`)
- **Containerização:** Docker + Docker Compose (`docker-compose.yml` local, `docker-compose.server.yml` produção)

### Integrações externas
- **Firebase Authentication:** validação de ID tokens, login social (Google), email/senha, reset de senha.
- **Mercado Pago:** split de pagamentos, OAuth do lojista, webhooks de transação.
- **Cloudinary:** upload e CDN de imagens (fotos de perfil, portfólio, serviços).

### Frontend
- **Framework:** React 18 + Vite
- **Estilo:** CSS Modules (`*.module.css`) — **proibido** Tailwind, styled-components ou CSS global novo
- **Roteamento:** react-router-dom
- **Notificações:** react-toastify
- **HTTP:** axios (via wrapper único em `frontend/src/services/api.js`)

---

## 3. Microsserviços (estrutura em `backend/`)

| Serviço | Responsabilidade | Banco próprio |
|---|---|---|
| `api-gateway` | Roteamento, CORS, validação de token Firebase, injeção de headers `X-User-*` | — |
| `discovery-service` | Servidor Eureka | — |
| `user-service` | Clientes (Customer), Barbeiros (Barber), autenticação, perfis, favoritos | ✅ |
| `barbershop-service` | Barbearias, horários de funcionamento, portfólio, avaliações | ✅ |
| `schedule-service` | Agenda, agendamentos (Appointment), bloqueios, walk-ins | ✅ |
| `product-service` | Estoque, produtos, pedidos de reposição | ✅ |
| `payment-service` | Integração Mercado Pago, transações, split, webhooks | ✅ |
| `notification-service` | E-mails transacionais, desduplicação via Redis, templates | — (usa Redis) |

**Regra:** cada serviço é dono exclusivo do seu banco. **Nunca** consulte tabelas de outro serviço diretamente — use Feign (leitura) ou evento RabbitMQ (escrita).

---

## 4. Regras Inquebráveis — Backend

### 4.1 DTOs e Mapeamento (crítico)
- **NUNCA** exponha `@Entity` em `@RestController`. Nenhum controller retorna ou aceita uma entidade JPA diretamente.
- Use sempre pares `XxxRequestDTO` (entrada) e `XxxResponseDTO` (saída), localizados em `dto/`.
- Conversão entidade ↔ DTO SEMPRE via classe `XxxMapper` dedicada (MapStruct preferencial; conversão manual aceita em serviços legados).
- DTOs são `record` por padrão (imutáveis). Validações via `jakarta.validation` (`@NotBlank`, `@Email`, `@Valid`).

### 4.2 Comunicação entre serviços
- **Mutações cross-service:** EXCLUSIVAMENTE via RabbitMQ. Publique um evento de domínio (`UserCreatedEvent`, `AppointmentConfirmedEvent`, etc.) e deixe o consumidor reagir.
- **Consultas cross-service:** use `FeignClient` em `feign/`, sempre com:
  - Timeout configurado
  - `fallback` ou `fallbackFactory` tratando indisponibilidade
  - Nunca lance `RuntimeException` genérica — mapeie para exceção de domínio
- **Proibido:** chamadas HTTP diretas (`RestTemplate`, `WebClient` solto) fora de Feign para comunicação inter-serviços.

### 4.3 Tratamento de exceções
- Lance exceções de domínio específicas: `NotFoundException`, `ConflictException`, `ValidationException`, `UnauthorizedException`, `BusinessException`.
- **NUNCA** retorne `ResponseEntity.status(500).body("erro")` ou capture `Exception` no controller.
- O `GlobalExceptionHandler` (`@RestControllerAdvice`) é o ÚNICO ponto de conversão exceção → HTTP. Ele retorna sempre o formato `ApiErrorResponse { timestamp, status, error, message, path }`.
- Novos tipos de erro → adicione um `@ExceptionHandler` no `GlobalExceptionHandler`, não trate no controller.

### 4.4 Segurança
- O **api-gateway é o único ponto** que valida o ID token do Firebase.
- Após validação, o gateway injeta headers confiáveis nos requests para serviços downstream:
  - `X-User-Id` (Firebase UID)
  - `X-User-Email`
  - `X-User-Role` (`ROLE_CUSTOMER` | `ROLE_BARBER`)
- Microsserviços **confiam nesses headers** — não revalidam o token. Acesso direto aos serviços (fora do gateway) é bloqueado por rede/compose.
- **Nunca** propague o `Authorization: Bearer` original para serviços downstream, exceto quando explicitamente necessário (documentar o motivo).

### 4.5 Estrutura de pastas por microsserviço
```
backend/<servico>/src/main/java/ifsp/edu/projeto/cortaai/<servico>/
  ├── controller/      → @RestController (só DTOs)
  ├── service/         → interfaces de serviço
  │   └── impl/        → implementações
  ├── repository/      → JpaRepository
  ├── model/           → @Entity
  ├── dto/             → records Request/Response
  ├── mapper/          → Entity ↔ DTO
  ├── feign/           → clients Feign
  ├── messaging/       → publishers e listeners RabbitMQ
  ├── config/          → beans, RabbitMQ config, Redis config
  └── exception/       → exceções de domínio + GlobalExceptionHandler
```

### 4.6 RabbitMQ — convenções
- **Exchange:** `cortaai.<dominio>.exchange` (ex: `cortaai.user.exchange`)
- **Routing key:** `<recurso>.<acao>` em passado (ex: `user.created`, `appointment.confirmed`, `payment.failed`)
- **Queue:** `<servico-consumidor>.<evento>.queue`
- **DLQ obrigatória** para eventos críticos (pagamentos, notificações).
- **Idempotência:** consumidores devem tolerar redelivery. Use Redis para desduplicação quando relevante (já implementado em `notification-service`).

---

## 5. Regras Inquebráveis — Frontend

- **Chamadas HTTP NÃO vivem em componentes.** Toda requisição fica em `frontend/src/services/<dominio>Service.js`, exportada como função nomeada. Componentes importam e chamam.
- O wrapper `services/api.js` já injeta o token Firebase — **nunca** crie um axios novo.
- **Estilização:** exclusivamente CSS Modules (`Component.module.css`), colocalizado com o componente ou em `pages/CSS/`. Proibido inline styles para layout (aceitável apenas para valores dinâmicos); proibido CSS global novo em `index.css` / `App.css`.
- **Estrutura:**
  ```
  frontend/src/
    ├── components/     → componentes reutilizáveis
    ├── pages/          → páginas roteadas (1 arquivo = 1 rota)
    │   └── CSS/        → módulos CSS das páginas
    ├── services/       → chamadas HTTP + lógica de integração
    └── AppRoutes.jsx   → definição de rotas
  ```
- **localStorage:** chaves padronizadas — `token`, `userId`, `userEmail`, `userName`, `userRole`, `isOwner`, `authProvider`, `barbershopId`. Não crie novas sem justificativa.
- **Validação de input:** máscaras e limites no `onChange` (ex: CPF, CNPJ, telefone). Nunca confie só no backend para UX.

---

## 6. Diretrizes para a IA

### Faça
- **Vá direto ao código.** Respostas curtas. Explique só o que não é óbvio do diff.
- **Respeite a estrutura de pastas** do serviço em que está trabalhando — identifique pelo path do arquivo aberto.
- **Antes de criar algo novo, verifique se já existe:**
  - Um evento RabbitMQ equivalente (busque em `messaging/`)
  - Um DTO apropriado (busque em `dto/`)
  - Um serviço/função no frontend (`services/`)
  - Um mapper (`mapper/`)
- **Antes de alterar fluxos de autenticação, pagamento ou cadastro**, leia primeiro os arquivos relevantes em `analises/` (especialmente `FLUXO_AUTENTICACAO_E_CADASTRO.md`, `FRF_PRD_TD_BACKEND_COMPLETO.md`).
- **Ao mexer em microsserviço X, não altere microsserviço Y** sem sinalizar explicitamente.
- **Commits:** mensagens no formato `<tipo>(<escopo>): <descrição>` em pt-BR. Tipos: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`.

### Não faça
- Não explique o que é microsserviço, DTO, Feign, RabbitMQ ou conceitos básicos — assuma que o dev sênior sabe.
- Não sugira mudar de stack (ex: "e se usasse Kafka?", "poderia migrar para NestJS"). A stack é fixa.
- Não crie código "didático" com comentários óbvios (`// cria a variável X`).
- Não retorne `Map<String, Object>` ou `Object` em endpoints — sempre DTO tipado.
- Não use `System.out.println` — use SLF4J (`log.info`, `log.warn`, `log.error`).
- Não gere migrations sem pedir confirmação (há `init.sql` e scripts legados).
- Não quebre contratos de API existentes sem sinalizar breaking change.

### Quando houver ambiguidade
- Prefira a solução que **não quebra o monolito legado** ainda em produção.
- Prefira evento RabbitMQ a chamada Feign quando ambos resolvem.
- Prefira adicionar novo endpoint/DTO a sobrecarregar um existente.

---

## 7. Checagens obrigatórias antes de finalizar uma resposta

- [ ] Código respeita a estrutura de pastas do serviço alvo?
- [ ] Controller não expõe `@Entity`?
- [ ] Exceções são de domínio (não genéricas)?
- [ ] Comunicação cross-service usa Feign (leitura) ou RabbitMQ (escrita)?
- [ ] Frontend: chamada HTTP está em `services/`?
- [ ] Frontend: estilo via CSS Module?
- [ ] Nomes e mensagens em pt-BR; código em inglês?

Se alguma resposta for **não**, corrija antes de entregar.
