# 🏗️ Arquitetura Técnica Detalhada - CortaAi

## 📊 Diagrama de Entidades (DER -Simplificado)

```
CUSTOMER
├── id (UUID, PK)
├── name (String)
├── email (String, UNIQUE)
├── documentCPF (String, UNIQUE)
├── tell (String)
├── password (encoded)
├── profilePhoto (URL/Cloudinary)
└── role: ROLE_CUSTOMER

BARBER
├── id (UUID, PK)
├── name (String)
├── email (String, UNIQUE)
├── documentCPF (String, UNIQUE)
├── tell (String)
├── password (encoded)
├── profilePhoto (URL/Cloudinary)
├── workStartTime (LocalTime)
├── workEndTime (LocalTime)
├── role: ROLE_BARBER ou ROLE_OWNER
└── barbershop_id (FK) [opcional]

BARBERSHOP
├── id (UUID, PK)
├── name (String)
├── cnpj (String, UNIQUE)
├── address (String)
├── city (String)
├── state (String)
├── zipCode (String)
├── logo (URL/Cloudinary)
├── owner_id (FK → BARBER)
├── createdAt (LocalDateTime)
└── updatedAt (LocalDateTime)

ACTIVITY (Serviço)
├── id (UUID, PK)
├── name (String)
├── description (String)
├── price (BigDecimal)
├── duration (Integer - minutos)
├── barbershop_id (FK → BARBERSHOP)
└── isActive (Boolean)

BARBER_ACTIVITY (Relacionamento N:N)
├── barber_id (FK → BARBER, PK1)
├── activity_id (FK → ACTIVITY, PK2)
└── isActive (Boolean)

APPOINTMENTS
├── id (UUID, PK)
├── customer_id (FK → CUSTOMER)
├── barbershop_id (FK → BARBERSHOP)
├── barber_id (FK → BARBER)
├── scheduledDate (LocalDate)
├── scheduledTime (LocalTime)
├── duration (Integer - calculado)
├── totalPrice (BigDecimal)
├── status (SCHEDULED, COMPLETED, CANCELLED)
├── activities (List<Activity>)
├── createdAt (LocalDateTime)
└── updatedAt (LocalDateTime)

BARBERSHOP_JOIN_REQUEST
├── id (UUID, PK)
├── barber_id (FK → BARBER)
├── barbershop_id (FK → BARBERSHOP)
├── status (PENDING, APPROVED, REJECTED)
├── requestedAt (LocalDateTime)
└── respondedAt (LocalDateTime)

BARBERSHOP_HIGHLIGHT
├── id (UUID, PK)
├── barbershop_id (FK → BARBERSHOP)
├── imageUrl (URL/Cloudinary)
├── description (String)
└── order (Integer - para ordenação)
```

---

## 🔄 Relacionamentos

```
CUSTOMER ──┐
           │ 1:N
           └──→ APPOINTMENTS

BARBER ────┐  N:1
           ├──→ BARBERSHOP
           │
           ├──┐ N:N
           │  └──→ ACTIVITY (via BARBER_ACTIVITY)
           │
           └──┐ 1:N
              └──→ APPOINTMENTS

BARBERSHOP ┬──┐ 1:N
           │  └──→ ACTIVITY
           │
           ├──┐ 1:N
           │  └──→ BARBERSHOP_HIGHLIGHT
           │
           ├──┐ 1:N
           │  └──→ APPOINTMENTS
           │
           ├──┐ 1:N
           │  └──→ BARBERSHOP_JOIN_REQUEST
           │
           └──┐ N:1
              └──→ BARBER (owner)
```

---

## 🔐 Sistema de Autenticação

```
┌─────────────────────────────────────────┐
│ Spring Security + JWT                   │
├─────────────────────────────────────────┤
│                                         │
│ 1. Usuário faz login                   │
│    POST /api/{customers|barbers}/login  │
│    Body: { email, password }            │
│                                         │
│ 2. Backend:                            │
│    - Valida credenciais                │
│    - Gera JWT Token (JwtTokenService)  │
│    - Retorna: { token, userData }      │
│                                         │
│ 3. Frontend localStorage:              │
│    - token (JWT)                       │
│    - role (ROLE_CUSTOMER | ROLE_BARBER)│
│    - userId (UUID)                     │
│    - userName (String)                 │
│    - user (JSON completo)              │
│                                         │
│ 4. Requisições subsequentes:           │
│    - Axiom Interceptor adiciona:       │
│    - Header: Authorization: Bearer XXX │
│                                         │
│ 5. Backend valida token:               │
│    - Spring Security verifica signature│
│    - Principal injetado nos methods    │
│    - @PreAuthorize verifica roles      │
│                                         │
└─────────────────────────────────────────┘
```

### Fluxo Completo Auth
```
Frontend Login Button
    ↓
authService.loginUser(email, password, userType)
    ↓
POST /api/customers/login (ou /barbers/login)
    ↓
Backend: CustomerService.login(LoginDTO)
    ↓
Busca usuário em BD
    ↓
Valida senha (BCrypt comparison)
    ↓
JwtTokenService.generateToken(userId, role)
    ↓
Retorna: LoginResponseDTO { token, userData }
    ↓
Frontend: localStorage.setItem('token', token)
    ↓
localStorage.setItem('role', 'ROLE_...')
    ↓
Redireciona para HomePage
```

---

## 📡 Endpoints REST (Estrutura)

### Base URL
```
Development:  http://localhost:8080/api
Production:   https://api.cortaai.com/api
```

### Autenticação
```
POST   /customers/register      - Registrar cliente
POST   /customers/login         - Login cliente
POST   /barbers/register        - Registrar barbeiro
POST   /barbers/login           - Login barbeiro
```

### Clientes (ROLE_CUSTOMER)
```
GET    /customers               - Listar (admin)
GET    /customers/{id}          - Detalhe
GET    /customers/me            - Perfil autenticado
PUT    /customers/me            - Atualizar
PUT    /customers/me/profile-photo
GET    /customers/me/appointments
GET    /appointments            - Meus agendamentos
POST   /appointments            - Criar agendamento
GET    /appointments/{id}       - Detalhe agendamento
PUT    /appointments/{id}       - Atualizar agendamento
DELETE /appointments/{id}       - Cancelar agendamento
```

### Barbeiros (ROLE_BARBER)
```
GET    /barbers                 - Listar
GET    /barbers/{id}            - Detalhe
GET    /barbers/me              - Perfil autenticado
PUT    /barbers/me              - Atualizar
PUT    /barbers/me/work-hours   - Definir horário trabalho
POST   /barbers/me/assign-activities - Atribuir habilidades
GET    /barbers/me/my-activities - Minhas habilidades
GET    /barbers/me/appointments - Agenda do barbeiro
```

### Barbearias (ROLE_OWNER)
```
GET    /barbershops             - Listar
GET    /barbershops/{id}        - Detalhe
POST   /barbershops             - Criar
PUT    /barbershops/{id}        - Atualizar
DELETE /barbershops/{id}        - Deletar
PUT    /barbershops/{id}/logo
PUT    /barbershops/{id}/banner

Serviços:
GET    /barbershops/{id}/activities
POST   /barbershops/{id}/activities
PUT    /barbershops/{id}/activities/{actId}
DELETE /barbershops/{id}/activities/{actId}

Agenda:
GET    /barbershops/{id}/schedule - Agenda completa
GET    /barbershops/{id}/schedule/{date}
GET    /barbershops/{id}/available-slots

Highlights:
GET    /barbershops/{id}/highlights
POST   /barbershops/{id}/highlights
DELETE /barbershops/{id}/highlights/{highlightId}

Equipe:
GET    /barbershops/{id}/barbers
POST   /barbershops/{id}/join-requests
GET    /barbershops/{id}/join-requests
PUT    /barbershops/{id}/join-requests/{requestId}/approve
PUT    /barbershops/{id}/join-requests/{requestId}/reject
DELETE /barbershops/{id}/barbers/{barberId}
```

### Análise/Insights (ROLE_OWNER)
```
GET    /barbershops/{id}/insights/daily
GET    /barbershops/{id}/insights/revenue
GET    /barbershops/{id}/insights/appointments
GET    /barbershops/{id}/stock
```

---

## 🗂️ Estrutura de Pastas (Recomendada)

### Backend
```
src/main/java/ifsp/edu/projeto/cortaai/
├── config/              # Configurações do App
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── JpaConfig.java
│   └── WebConfig.java
│
├── controller/          # REST Controllers
│   ├── BarberController.java
│   ├── CustomerController.java
│   ├── AppointmentsController.java
│   └── BarbershopController.java
│
├── service/             # Business Logic
│   ├── BarberService.java
│   ├── CustomerService.java
│   ├── AppointmentsService.java
│   ├── BarbershopService.java
│   ├── impl/
│   │   ├── BarberServiceImpl.java
│   │   ├── CustomerServiceImpl.java
│   │   └── ...
│   ├── JwtTokenService.java
│   └── StorageService.java
│
├── repository/          # Data Access
│   ├── BarberRepository.java
│   ├── CustomerRepository.java
│   ├── AppointmentsRepository.java
│   ├── BarbershopRepository.java
│   ├── ActivityRepository.java
│   └── BarbershopJoinRequestRepository.java
│
├── model/              # JPA Entities
│   ├── Customer.java
│   ├── Barber.java
│   ├── Barbershop.java
│   ├── Activity.java
│   ├── Appointments.java
│   ├── BarbershopJoinRequest.java
│   └── BarbershopHighlight.java
│
├── dto/                # Data Transfer Objects
│   ├── CustomerDTO.java
│   ├── BarberDTO.java
│   ├── BarbershopDTO.java
│   ├── ActivityDTO.java
│   ├── AppointmentDTO.java
│   ├── LoginDTO.java
│   ├── LoginResponseDTO.java
│   └── CreateBarbershopDTO.java
│
├── mapper/             # MapStruct Mappers
│   ├── CustomerMapper.java
│   ├── BarberMapper.java
│   ├── BarbershopMapper.java
│   ├── ActivityMapper.java
│   └── AppointmentMapper.java
│
├── validator/          # Custom Validators
│   ├── CPF.java
│   ├── CPFValidator.java
│   ├── CNPJ.java
│   ├── CNPJValidator.java
│   └── UniqueEmailValidator.java
│
├── exception/          # Exception Classes
│   ├── GlobalExceptionHandler.java
│   ├── EntityNotFoundException.java
│   ├── InvalidCredentialsException.java
│   └── DuplicateEntityException.java
│
├── event/              # Domain Events
│   ├── AppointmentCreatedEvent.java
│   └── BarbershipJoinRequestedEvent.java
│
├── listener/           # Event Listeners
│   ├── AppointmentListener.java
│   └── EmailNotificationListener.java
│
└── CortaaiApplication.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── db/migration/       # Flyway migrations
    ├── V1__initial_schema.sql
    └── V2__add_indexes.sql

src/test/java/...
├── BarberServiceTest.java
├── CustomerServiceTest.java
├── AppointmentsServiceTest.java
└── AppointmentControllerTest.java
```

### Frontend
```
src/
├── App.jsx
├── App.css
├── main.jsx
├── index.css
├── AppRoutes.jsx
│
├── components/         # Componentes Reutilizáveis
│   ├── AgendamentoPage/
│   ├── BarberPage/
│   ├── HomePage/
│   ├── Login/
│   ├── Sign_In/
│   ├── Site/
│   └── Common/
│       ├── Header.jsx
│       ├── Footer.jsx
│       ├── ErrorBoundary.jsx
│       └── Loading.jsx
│
├── pages/              # Page Components
│   ├── LoginPage.jsx
│   ├── HomePage.jsx
│   ├── BarberHomePage.jsx
│   ├── AgendamentoPage.jsx
│   └── CSS/
│
├── services/           # API Integration
│   ├── api.js
│   ├── authService.js
│   ├── appointmentService.js
│   ├── barbershopService.js
│   └── activityService.js
│
├── context/            # React Context (recomendado)
│   ├── AuthContext.jsx
│   ├── AppointmentContext.jsx
│   └── BarbershopContext.jsx
│
├── hooks/              # Custom Hooks
│   ├── useAuth.js
│   ├── useAppointment.js
│   └── useBarbershop.js
│
├── utils/              # Utilitários
│   ├── formatters.js
│   ├── validators.js
│   └── constants.js
│
├── styles/             # Estilos globais
│   ├── variables.css
│   ├── reset.css
│   └── typography.css
│
└── __tests__/          # Testes
    ├── services/
    ├── components/
    └── pages/

public/
├── Icons/
└── assets/
```

---

## 🔒 Segurança

### Implementado
✅ JWT tokens  
✅ Spring Security  
✅ @PreAuthorize  
✅ BCrypt password encoding  
✅ Input validation (DTOs)  

### Recomendado
⚠️ CORS ainda mais restritivo  
⚠️ Rate limiting  
⚠️ HTTPS em produção  
⚠️ Sanitização de output  
⚠️ SQL injection protection (JPA já protege)  
⚠️ CSRF protection  
⚠️ Secrets em environment variables  

---

## 📦 Deploy Targets

### Development
```
Backend:  localhost:8080
Frontend: localhost:5173 (Vite dev server)
Database: Docker MySQL
Storage:  Cloudinary
```

### Production  
### Option 1: Traditional Server
```
Backend:  AWS EC2 instance
Frontend: CloudFront + S3
Database: AWS RDS (MySQL)
Storage:  Cloudinary
```

### Option 2: AWS Lambda (Backend está preparado)
```
Backend:  AWS Lambda + API Gateway
Frontend: CloudFront + S3
Database: AWS RDS
Storage:  Cloudinary
```

### Option 3: Containers
```
Backend:  Docker → AWS ECR → ECS
Frontend: Docker → AWS ECR → ECS
Database: AWS RDS
Storage:  Cloudinary
```

---

## 📊 Métricas de Desempenho

### Database
```
Conexões HikariCP: 10 (máximo)
Timeout conexão: 30s
ddl-auto: update (desenvolvimento) → validate (prod)
```

### Backend
```
Timeout requisição: padrão (30s)
Thread pool: padrão
Memory: -Xmx512m (recomendado aumentar)
```

### Frontend
```
Build: Vite (ultra-rápido)
Bundle size: ~200KB (gzipped)
Lazy loading: não implementado
Code splitting: não implementado
```

---

**Documento gerado:** 31 de março de 2026
