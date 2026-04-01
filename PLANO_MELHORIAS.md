# 🚀 PLANO DE MELHORIAS - CortaAi

## Resumo das Recomendações Priorizadas

Este documento apresenta um plano acionável de melhorias para levar o CortaAi de um projeto de TCC para produção.

---

## 📋 PRIORITY 1 - Crítico (Semanas 1-2)

### Backend

#### 1. GlobalExceptionHandler
**Problema:** Sem tratamento centralizado de exceções  
**Impacto:** Erro responses inconsistentes  
**Solução:**

```java
// src/main/java/ifsp/edu/projeto/cortaai/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleEntityNotFound(EntityNotFoundException e) {
        ErrorDTO errorDTO = new ErrorDTO(
            "ENTITY_NOT_FOUND",
            e.getMessage(),
            System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDTO);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidationError(MethodArgumentNotValidException e) {
        ErrorDTO errorDTO = new ErrorDTO(
            "VALIDATION_ERROR",
            "Erro de validação nos dados enviados",
            System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericError(Exception e) {
        ErrorDTO errorDTO = new ErrorDTO(
            "INTERNAL_ERROR",
            "Erro interno do servidor",
            System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }
}
```

**Tempo:** 2 horas  
**Prioridade:** 🔴 CRÍTICO

---

#### 2. Validar production-ready configuration
**Problema:** `ddl-auto: update` é perigoso em produção  
**Solução:**

```yaml
# application.yml - desenvolvimento
spring:
  jpa:
    hibernate:
      ddl-auto: update

# application-prod.yml - produção
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

**Tempo:** 30 minutos  
**Prioridade:** 🔴 CRÍTICO

---

#### 3. Adicionar Spring Boot Actuator
**Propósito:** Monitoramento e métricas  

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: when-authorized
```

**Tempo:** 1 hora  
**Prioridade:** 🟠 IMPORTANTE

---

### Frontend

#### 1. Variáveis de Ambiente
**Problema:** baseURL hardcoded  
**Solução:**

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api

# .env.production
VITE_API_BASE_URL=https://api.cortaai.com/api
```

```javascript
// src/services/api.js
const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
});
```

**Tempo:** 30 minutos  
**Prioridade:** 🔴 CRÍTICO

---

#### 2. Error Boundary
**Propósito:** Capturar erros de componentes

```jsx
// src/components/ErrorBoundary.jsx
import { Component } from 'react';

export class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }
    
    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }
    
    componentDidCatch(error, errorInfo) {
        console.error('Error caught:', error, errorInfo);
    }
    
    render() {
        if (this.state.hasError) {
            return (
                <div style={{ padding: '20px', textAlign: 'center' }}>
                    <h1>Algo deu errado</h1>
                    <p>{this.state.error?.message}</p>
                </div>
            );
        }
        
        return this.props.children;
    }
}
```

**Tempo:** 1 hora  
**Prioridade:** 🔴 CRÍTICO

---

## 📋 PRIORITY 2 - Importante (Semanas 3-4)

### Backend

#### 1. Adicionar Testes Unitários

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

```java
// src/test/java/ifsp/edu/projeto/cortaai/service/BarberServiceTest.java
@SpringBootTest
@DataJpaTest
public class BarberServiceTest {
    
    @MockBean
    private BarberRepository barberRepository;
    
    @InjectMocks
    private BarberService barberService;
    
    @Test
    public void testGetBarber_Success() {
        UUID id = UUID.randomUUID();
        Barber barber = new Barber();
        barber.setId(id);
        barber.setName("João Silva");
        
        when(barberRepository.findById(id)).thenReturn(Optional.of(barber));
        
        BarberDTO result = barberService.get(id);
        
        assertNotNull(result);
        assertEquals("João Silva", result.getName());
    }
}
```

**Tempo:** 4 horas (por service)  
**Prioridade:** 🟠 IMPORTANTE

#### 2. Implementar Paginação

```java
// BarberController.java
@GetMapping
public ResponseEntity<Page<BarberDTO>> getAllBarbers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
) {
    Page<Barber> barbers = barberService.findAll(PageRequest.of(page, size));
    return ResponseEntity.ok(barbers.map(barberMapper::toDTO));
}
```

**Tempo:** 2 horas  
**Prioridade:** 🟠 IMPORTANTE

---

### Frontend

#### 1. TypeScript Migration
**Meta:** Adicionar type safety  

```bash
npm install -D typescript @types/react @types/react-dom
```

```typescript
// src/services/api.ts
import axios, { AxiosInstance } from 'axios';

const api: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
});

api.interceptors.request.use(async (config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;
```

**Tempo:** 8+ horas (projeto completo)  
**Prioridade:** 🟠 IMPORTANTE

#### 2. Context API para Estado Global

```jsx
// src/context/AuthContext.jsx
import { createContext, useState, useCallback } from 'react';

export const AuthContext = createContext();

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    
    const login = useCallback(async (email, password, userType) => {
        setLoading(true);
        setError(null);
        try {
            const response = await loginUser(email, password, userType);
            setUser(response.userData);
            return response;
        } catch (err) {
            setError(err.message);
            throw err;
        } finally {
            setLoading(false);
        }
    }, []);
    
    const logout = useCallback(() => {
        logoutUser();
        setUser(null);
    }, []);
    
    return (
        <AuthContext.Provider value={{ user, loading, error, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}
```

```jsx
// src/pages/LoginPage.jsx
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export default function LoginPage() {
    const { login, loading, error } = useContext(AuthContext);
    
    const handleLogin = async (email, password) => {
        await login(email, password, 'customer');
    };
    
    return (
        // JSX
    );
}
```

**Tempo:** 3 horas  
**Prioridade:** 🟠 IMPORTANTE

---

## 📋 PRIORITY 3 - Melhoria (Semanas 5-6)

### Backend

#### 1. Implementar Caching
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```java
@Service
@EnableCaching
public class BarbershopService {
    
    @Cacheable(value = "barbershops", key = "#id")
    public BarbershopDTO get(UUID id) {
        // ...
    }
    
    @CacheEvict(value = "barbershops", key = "#id")
    public void update(UUID id, BarbershopDTO dto) {
        // ...
    }
}
```

#### 2. Rate Limiting
```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.10.0</version>
</dependency>
```

#### 3. Logs Estruturados
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

---

### Frontend

#### 1. Validação de Formulários com React Hook Form

```bash
npm install react-hook-form zod @hookform/resolvers
```

```jsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const loginSchema = z.object({
    email: z.string().email('Email inválido'),
    password: z.string().min(6, 'Mínimo 6 caracteres'),
});

export function LoginForm() {
    const { register, handleSubmit, formState: { errors } } = useForm({
        resolver: zodResolver(loginSchema),
    });
    
    const onSubmit = async (data) => {
        // submit
    };
    
    return (
        <form onSubmit={handleSubmit(onSubmit)}>
            <input
                {...register('email')}
                type="email"
                placeholder="Email"
            />
            {errors.email && <span>{errors.email.message}</span>}
            {/* ... */}
        </form>
    );
}
```

#### 2. Testes com Vitest

```bash
npm install -D vitest @testing-library/react @testing-library/jest-dom
```

```javascript
// src/services/__tests__/api.test.js
import { describe, it, expect, beforeEach, vi } from 'vitest';
import api from '../api';

describe('API', () => {
    beforeEach(() => {
        localStorage.clear();
    });
    
    it('should add token to request header', async () => {
        const token = 'test-token-123';
        localStorage.setItem('token', token);
        
        const config = {};
        const result = await api.interceptors.request.handlers[0].fulfilled(config);
        
        expect(result.headers.Authorization).toBe(`Bearer ${token}`);
    });
});
```

---

## 📊 Roadmap de Implementação

```
SEMANA 1
├─ GlobalExceptionHandler (Backend)
├─ .env configuration (Frontend)
└─ Error Boundary (Frontend)

SEMANA 2
├─ ddl-auto validation (Backend)
├─ Actuator setup (Backend)
└─ Variáveis de ambiente (Frontend)

SEMANA 3
├─ Testes unitários (Backend)
├─ Paginação (Backend)
└─ Context API (Frontend)

SEMANA 4
├─ TypeScript migration (Frontend)
├─ React Hook Form (Frontend)
└─ Testes frontend (Frontend)

SEMANA 5-6
├─ Redis Caching (Backend)
├─ Rate limiting (Backend)
├─ Logs estruturados (Backend)
└─ Otimizações de performance
```

---

## 💡 Checklist de Produção

- [ ] GlobalExceptionHandler implementado
- [ ] ddl-auto em `validate` para produção
- [ ] Variáveis de ambiente configuradas
- [ ] Error Boundary no App.jsx
- [ ] Testes unitários (Backend) com >70% cobertura
- [ ] Paginação em endpoints críticos
- [ ] TypeScript configurado
- [ ] Context API para estado global
- [ ] React Hook Form para validação
- [ ] Actuator habilitado para monitoramento
- [ ] Logs estruturados
- [ ] Rate limiting nos endpoints de autenticação
- [ ] CORS configurado corretamente
- [ ] HTTPS obrigatório em produção
- [ ] Secrets não commitados (.env ignorado)

---

## 📚 Recursos Adicionais

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [React 19 Docs](https://react.dev)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [React Hook Form](https://react-hook-form.com/)
- [Vitest](https://vitest.dev/)

---

**Última atualização:** 31 de março de 2026
