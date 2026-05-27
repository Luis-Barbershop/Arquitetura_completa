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

Artefatos físicos/executáveis deployados em nós (`«node»`) e containers Docker.

### Nó: `Dispositivo do Cliente`

| Artefato | Tipo |
|---|---|
| Browser / WebView | `«artifact»` |

### Nó: `Host Web` → `Frontend Container`

| Artefato | Tipo |
|---|---|
| React 18 + Vite (build estático servido por Nginx) | `«artifact»` |

### Nó: `Host Backend (Docker Compose)` → Sub-nó `Edge`

| Artefato / Container | Porta |
|---|---|
| `api-gateway` | `:8080` |
| `discovery-service` | `:8761` |

### Nó: `Host Backend` → Sub-nó `Domínio`

| Artefato / Container | Porta |
|---|---|
| `user-service` | `:8081` |
| `barbershop-service` | `:8082` |
| `schedule-service` | `:8083` |
| `payment-service` | `:8084` |
| `notification-service` | `:8085` |
| `product-service` | `:8086` |

### Nó: `Host Backend` → Sub-nó `Infra de Dados/Mensageria`

| Artefato | Tipo |
|---|---|
| MySQL | `«database»` |
| Redis | `«artifact»` |
| RabbitMQ | `«artifact»` |

### Nó: `Cloud` (serviços externos)

| Artefato | Tipo |
|---|---|
| Firebase Auth | `«cloud»` |
| Mercado Pago | `«cloud»` |
| Cloudinary | `«cloud»` |

---

## 3. Referências

- Diagramas PlantUML completos: [`DIAGRAMA_COMPONENTES.md`](DIAGRAMA_COMPONENTES.md)
- Diagrama de implantação completo: [`DIAGRAMA_IMPLANTACAO.md`](DIAGRAMA_IMPLANTACAO.md)
- Arquivos editáveis: `DIAGRAMA_COMPONENTES.drawio`, `DIAGRAMA_IMPLANTACAO.drawio`
