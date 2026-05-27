# Artefatos UML — Diagrama de Componentes e Diagrama de Implantação

> **Data:** 27 de maio de 2026
> **Referências:** `DIAGRAMA_COMPONENTES.md`, `DIAGRAMA_IMPLANTACAO.md`

---

## 1. Diagrama de Componentes

Artefatos lógicos representados como `«component»`, `«database»` ou pacotes UML.

### Package `Frontend`

| Componente | Estereótipo |
|---|---|
| Páginas e Componentes (React 18) | `«component»` |
| Camada de Serviços (`*Service.js`) | `«component»` |
| API Wrapper (`api.js`) | `«component»` |

### Package `Edge`

| Componente | Estereótipo |
|---|---|
| API Gateway (Spring Cloud Gateway) | `«component»` |
| Filtro Auth Firebase | `«component»` |
| Injetor de Headers `X-User-*` | `«component»` |

### Package `Descoberta`

| Componente | Estereótipo |
|---|---|
| Discovery Service (Eureka) | `«component»` |

### Package `Microsserviços de Domínio`

| Componente | Estereótipo |
|---|---|
| `user-service` | `«component»` |
| `barbershop-service` | `«component»` |
| `schedule-service` | `«component»` |
| `payment-service` | `«component»` |
| `product-service` | `«component»` |
| `notification-service` | `«component»` |

### Package `Infraestrutura`

| Componente | Estereótipo |
|---|---|
| MySQL | `«database»` |
| RabbitMQ | `«component»` |
| Redis | `«component»` |

### Package `Sistemas Externos`

| Componente | Estereótipo |
|---|---|
| Firebase Auth | `«component»` |
| Mercado Pago | `«component»` |
| Cloudinary | `«component»` |

---

## 2. Diagrama de Implantação

Artefatos no sentido UML: tudo que precisa existir, ser provisionado ou configurado para que o sistema seja implantado com sucesso. Fonte: `docker-compose.server.yml` e `.env.prod`.

---

### 2.1 Arquivos presentes no servidor (volumes e configuração)

> Devem existir fisicamente no host antes de executar `docker compose up`.

| Artefato | Caminho no servidor | Consumido por |
|---|---|---|
| `docker-compose.server.yml` | raiz do repositório | orquestração Docker |
| `.env.prod` | raiz do repositório | todos os containers via `--env-file` |
| `firebase-service-account.json` | raiz do repositório | `api-gateway`, `user-service`, `notification-service` (montado em `/app/`) |
| `init.sql` | raiz do repositório | `db` (MySQL — executado na inicialização do container) |
| `views.sql` | raiz do repositório | `db` (MySQL — views opcionais) |

---

### 2.2 Volumes persistentes no host

> Diretórios que devem existir no sistema de arquivos do servidor para persistência de dados.

| Volume | Caminho no host | Container |
|---|---|---|
| Dados MySQL | `/DATA/cortaai/mysql` | `cortaai-mysql` |
| Dados SonarQube | `/DATA/cortaai/sonarqube/data` | `sonarqube` |
| Logs SonarQube | `/DATA/cortaai/sonarqube/logs` | `sonarqube` |
| Extensões SonarQube | `/DATA/cortaai/sonarqube/extensions` | `sonarqube` |

---

### 2.3 Containers Docker (imagens e portas expostas)

#### Infraestrutura interna

| Container | Imagem | Porta host:container |
|---|---|---|
| `cortaai-mysql` | `mysql:8.0` | `3307:3306` |
| `cortaai-rabbitmq` | `rabbitmq:3-management` | `5673:5672`, `15673:15672` |
| `cortaai-redis` | `redis:7-alpine` | `6380:6379` |
| `sonarqube-db` | `postgres:15-alpine` | — (interno) |
| `sonarqube` | `sonarqube:10-community` | `9001:9000` |

#### Microsserviços (build local)

| Container | Imagem gerada | Porta host:container |
|---|---|---|
| `discovery-service` | `cortaai/discovery-service:latest` | `8761:8761` |
| `api-gateway` | `cortaai/api-gateway:latest` | `8082:8080` |
| `user-service` | `cortaai/user-service:latest` | — (interno via Eureka) |
| `barbershop-service` | `cortaai/barbershop-service:latest` | — (interno via Eureka) |
| `schedule-service` | `cortaai/schedule-service:latest` | — (interno via Eureka) |
| `payment-service` | `cortaai/payment-service:latest` | — (interno via Eureka) |
| `notification-service` | `cortaai/notification-service:latest` | — (interno via Eureka) |
| `product-service` | `cortaai/product-service:latest` | — (interno via Eureka) |
| `frontend` | `cortaai/frontend:latest` | `80:80` / `443:443` |

---

### 2.4 Credenciais e configurações de infraestrutura interna

> Definidas em `.env.prod`, configuradas nos containers via variáveis de ambiente.

| Variável | Finalidade | Containers que usam |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` / `DB_USERNAME` / `DB_PASSWORD` | Acesso ao MySQL | todos os microsserviços de domínio |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | Acesso ao RabbitMQ | `barbershop`, `schedule`, `payment`, `notification`, `product` |
| `REDIS_PASSWORD` | Acesso ao Redis | `api-gateway`, `schedule-service`, `notification-service` |
| `CORTAAI_DATA_CRYPTO_KEY` | Chave AES de criptografia de dados PII (LGPD) | `user-service`, `barbershop-service`, `schedule-service` |
| `SONAR_DB_USER` / `SONAR_DB_PASSWORD` | Acesso ao PostgreSQL do SonarQube | `sonarqube` |

---

### 2.5 Integrações externas — Firebase

> Projeto Firebase `cortaai-480b8` deve estar criado e configurado no Google Cloud Console.

| Artefato / Configuração | Variável / Arquivo | Finalidade |
|---|---|---|
| Service Account JSON | `firebase-service-account.json` | Admin SDK — validação de tokens no gateway e envio de push |
| Web API Key | `FIREBASE_WEB_API_KEY` | Fluxos REST de autenticação (reset de senha) no `user-service` |
| Firebase Project ID | `cortaai-480b8` (hardcoded + `NOTIFICATION_PUSH_PROJECT_ID`) | FCM push notifications no `notification-service` |
| VAPID Key (FCM Web) | `VITE_FIREBASE_VAPID_KEY` | Push notifications no frontend (PWA) |
| Firebase App Config (frontend) | `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_STORAGE_BUCKET`, `VITE_FIREBASE_MESSAGING_SENDER_ID`, `VITE_FIREBASE_APP_ID` | SDK Firebase no React (autenticação, push) |

---

### 2.6 Integrações externas — Mercado Pago

> Aplicação MP deve estar criada no painel de desenvolvedores e o OAuth configurado.

| Artefato / Configuração | Variável | Finalidade |
|---|---|---|
| Access Token do vendedor | `MP_ACCESS_TOKEN` | Chamadas à API MP no `payment-service` |
| Public Key | `MP_PUBLIC_KEY` | SDK frontend MP (checkout) |
| Client ID / Secret | `MP_CLIENT_ID` / `MP_CLIENT_SECRET` | OAuth do lojista (split de pagamentos) |
| Redirect URI OAuth | `MP_REDIRECT_URI` (`https://api.cortaai.shop/api/payments/mp-callback`) | Callback de autorização do lojista |
| URL pós-conexão | `MP_POST_CONNECT_REDIRECT_URL` (`https://web.cortaai.shop/barberHome`) | Redirecionamento após OAuth concluído |
| Base URLs | `MP_AUTH_BASE_URL` / `MP_API_BASE_URL` | Endpoints da API MP |

---

### 2.7 Integrações externas — Cloudinary

| Artefato / Configuração | Variável | Finalidade |
|---|---|---|
| Cloud Name | `CLOUDINARY_CLOUD_NAME` | CDN e upload de imagens |
| API Key | `CLOUDINARY_API_KEY` | Autenticação no `user-service` e `barbershop-service` |
| API Secret | `CLOUDINARY_API_SECRET` | Assinatura de uploads |

---

### 2.8 Integração externa — SMTP (Gmail)

| Artefato / Configuração | Variável | Finalidade |
|---|---|---|
| Host SMTP | `MAIL_HOST` (`smtp.gmail.com`) | Envio de e-mails transacionais |
| Porta | `MAIL_PORT` (`587`) | TLS STARTTLS |
| Credenciais | `MAIL_USERNAME` / `MAIL_PASSWORD` | App Password do Gmail (2FA obrigatório) |
| Remetente | `NOTIFICATION_FROM_EMAIL` | Endereço exibido nos e-mails |

---

### 2.9 Integrações externas — Motores de IA (Gustave)

> Utilizados pelo `schedule-service` para sugestões inteligentes de agenda.

| Provedor | Variável | Modelo padrão |
|---|---|---|
| Google Gemini | `GEMINI_API_KEY` | — |
| Groq | `GROQ_API_KEY` | `GROQ_MODEL` (`llama-3.3-70b-versatile`) |
| OpenRouter | `OPENROUTER_API_KEY` | `OPENROUTER_MODEL` |
| Cohere | `COHERE_API_KEY` | `COHERE_MODEL` (`command-a-03-2025`) |

---

### 2.10 DNS e TLS

> Devem estar configurados no provedor de DNS antes do deploy.

| Registro | Aponta para | Finalidade |
|---|---|---|
| `api.cortaai.shop` | IP do Host Backend | Ponto de entrada do `api-gateway` |
| `web.cortaai.shop` | IP do Host Web | Frontend React |
| Certificado TLS | Let's Encrypt (Nginx/Caddy no host) | HTTPS nos dois domínios |

---

## 3. Referências

- Diagramas PlantUML completos: [`DIAGRAMA_COMPONENTES.md`](DIAGRAMA_COMPONENTES.md)
- Diagrama de implantação completo: [`DIAGRAMA_IMPLANTACAO.md`](DIAGRAMA_IMPLANTACAO.md)
- Arquivos editáveis: `DIAGRAMA_COMPONENTES.drawio`, `DIAGRAMA_IMPLANTACAO.drawio`
- Orquestração de produção: [`docker-compose.server.yml`](../docker-compose.server.yml)
