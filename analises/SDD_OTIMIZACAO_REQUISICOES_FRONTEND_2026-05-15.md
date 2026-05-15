# SDD — Otimização de Requisições do Frontend (Polling → SSE)

> **Data:** 2026-05-15  
> **Branch:** `feature/migracao-microservicos`  
> **Status:** 📋 PLANEJADO  

---

## 1. Diagnóstico — Polling atual identificado

### 1.1 Mapa de Intervalos Ativos

| Componente / Página | Endpoint chamado | Intervalo | Usuários afetados | Criticidade |
|---|---|---|---|---|
| `NotificationBell.jsx` (×2 — componentes duplicados) | `GET /notifications/unread-count` | **30s** | Todos logados (barber + customer) | 🔴 ALTA — 2 instâncias ativas simultaneamente |
| `BarberServicesPage.jsx` | `GET /barbershops/{id}/activities` | **15s** | Owner logado em `/barberHome/servicos` | 🟡 MÉDIA |
| `ServicesHomeBarber.jsx` | `GET /barbershops/{id}/activities` | **20s** | Barbeiro em `/barberHome` | 🟡 MÉDIA — duplica a anterior quando estão ambas ativas |
| `BarberProfilePage.jsx` | `GET /barbershop-join-requests/pending-invites` | **30s** | Barbeiro sem barbearia na página de perfil | 🟢 BAIXA — contexto muito específico |
| `DashReportPanel.jsx` | `onRefresh()` callback (7 endpoints paralelos) | **30s** | Owner no dashboard | 🔴 ALTA — dispara até **7 requisições simultâneas** a cada 30s |

### 1.2 Cálculo de Requisições por Minuto (pior caso)

Cenário: **1 barbeiro owner** logado com `/barberHome/dashboard` e `/barberHome/servicos` abertos:

| Fonte | Req/min |
|---|---|
| `NotificationBell` × 2 instâncias | 4 req/min |
| `BarberServicesPage` (15s) | 4 req/min |
| `ServicesHomeBarber` (20s) | 3 req/min |
| `DashReportPanel` × 7 endpoints (30s) | 14 req/min |
| **Total** | **≈ 25 req/min por usuário** |

Com **10 usuários ativos simultâneos** → ~250 req/min desnecessárias só de polling.

### 1.3 Problemas adicionais identificados

1. **`NotificationBell` duplicado:** existe em `components/NotificationBell/` e `components/BarberPage/` — código idêntico, dois `setInterval` separados → **2× as requisições de notificação**.

2. **`DashReportPanel` com `onRefresh` estático:** o `fetchAll` no `BarberDashboardPage` está memoizado com `useCallback([barbershopId, expenseMonth, expenseYear])` — se qualquer um mudar, o `DashReportPanel` reinicia o intervalo.

3. **Dashboard faz 7 chamadas paralelas a cada refresh**, incluindo views analíticas cross-DB (`getBarberPerformance`, `getCustomerRetention`) que são pesadas.

---

## 2. Análise das Alternativas

### Opção A — WebSocket (STOMP/SockJS)
**Prós:** bidirecional, tempo real real, ideal para chat.  
**Contras:** conexão persistente por usuário (consome thread/memória no servidor), complexidade de infraestrutura (balanceamento de carga requer sticky sessions ou broker externo), não há WebSocket no stack atual.  
**Veredicto:** ❌ Overkill para este caso. O CortaAi não tem caso de uso bidirecional em tempo real — nem chat ao vivo entre cliente e barbeiro.

### Opção B — Webhook (client → server)
**Prós:** elimina polling em integrações externas (ex: Mercado Pago já usa isso).  
**Contras:** webhook é uma notificação de sistema externo para o backend — não resolve o problema de atualização do frontend.  
**Veredicto:** ❌ Não se aplica ao frontend. Webhook é backend-to-backend.

### Opção C — Server-Sent Events (SSE)
**Prós:** unidirecional (server → client), HTTP padrão (sem protocolo novo), funciona com Spring Boot via `SseEmitter`, suportado por todos os browsers modernos, leve (1 conexão persistente longa vs N requisições curtas), sem necessidade de infra extra.  
**Contras:** unidirecional (mas é suficiente para notificações), reconexão automática nativa do browser.  
**Veredicto:** ✅ **Melhor fit** para o problema de notificações.

### Opção D — Eliminar polling, aumentar intervalo + refresh manual
**Prós:** zero complexidade, resolve o problema imediatamente.  
**Contras:** UX pior — barbeiro não vê novo agendamento até clicar "atualizar".  
**Veredicto:** ✅ **Complementar** — válido para dashboard (dados analíticos não precisam de tempo real).

---

## 3. Decisão Arquitetural

| Caso de uso | Solução adotada | Justificativa |
|---|---|---|
| Notificações (sino / badge de não lidas) | **SSE** via `notification-service` | Evento já existe no RabbitMQ — só precisa de emitter no endpoint |
| Convites pendentes (`BarberProfilePage`) | **Driven por SSE de notificação** | `JOIN_REQUEST_RECEIVED` já é um `NotificationType` — badge atualiza via SSE |
| Serviços da barbearia (`BarberServicesPage`, `ServicesHomeBarber`) | **Remover polling — dados estáticos na prática** | Serviços mudam raramente (owner edita manualmente) — sem necessidade de refresh automático |
| Dashboard analítico (`BarberDashboardPage`) | **Aumentar intervalo para 5 min + botão manual** | KPIs são agregações históricas — latência de 5min é aceitável |
| `NotificationBell` duplicado | **Unificar em 1 componente** | Elimina requisições duplicadas independentemente da solução |

---

## 4. Plano de Implementação

### Fase 1 — Quick wins (sem SSE, impacto imediato)

#### 4.1 Unificar `NotificationBell` (remove 50% das requisições de notificação)

- Deletar `frontend/src/components/BarberPage/NotificationBell.jsx`
- Todos os imports em `BarberHeader.jsx` passam a usar `components/NotificationBell/NotificationBell.jsx`
- Adicionar prop `userType` se necessário para lógica de redirect (já existe no componente unificado)

#### 4.2 Remover polling de serviços

- `ServicesHomeBarber.jsx` — remover o `useEffect` com `setInterval` (linhas 24–30). Dados carregam 1× no mount; owner vê mudanças ao navegar de volta para a home.
- `BarberServicesPage.jsx` — remover o `useEffect` com `setInterval` para owners (linhas 162–175). Manter refresh explícito após criar/deletar serviço (já existente).

#### 4.3 Aumentar intervalo do Dashboard para 5 minutos

- `BarberDashboardPage.jsx` → `DashReportPanel` não recebe `onRefresh` (remove o auto-refresh).
- Adicionar botão "Atualizar dados" explícito que chama `fetchAll()`.

---

### Fase 2 — SSE para notificações

#### 4.4 Backend — `notification-service`: endpoint SSE

**Arquivo novo:** `backend/notification-service/.../controller/NotificationSseController.java`

```java
@GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@RequestHeader("X-User-Id") String userId) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    sseService.register(userId, emitter);
    emitter.onCompletion(() -> sseService.remove(userId));
    emitter.onTimeout(() -> sseService.remove(userId));
    return emitter;
}
```

**Arquivo novo:** `backend/notification-service/.../service/SseEmitterService.java`

```java
@Service
public class SseEmitterService {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(String userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
    }

    public void remove(String userId) {
        emitters.remove(userId);
    }

    public void sendUnreadCount(String userId, int count) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                .name("unread-count")
                .data(Map.of("unreadCount", count)));
        } catch (IOException e) {
            emitters.remove(userId);
        }
    }
}
```

**Integração com listeners RabbitMQ existentes:**  
Nos listeners que já processam eventos de notificação (`NotificationListener.java`), após salvar a `Notification` no banco, chamar `sseEmitterService.sendUnreadCount(userId, count)`.

#### 4.5 Frontend — hook `useNotificationStream`

**Arquivo novo:** `frontend/src/hooks/useNotificationStream.js`

```javascript
import { useEffect, useRef } from 'react';
import { getAuth } from 'firebase/auth';

export function useNotificationStream(onUnreadCount) {
    const esRef = useRef(null);

    useEffect(() => {
        let active = true;

        const connect = async () => {
            const token = localStorage.getItem('token');
            if (!token) return;

            const url = `${import.meta.env.VITE_API_URL}/notifications/stream`;
            const es = new EventSource(`${url}?token=${token}`);
            esRef.current = es;

            es.addEventListener('unread-count', (e) => {
                if (!active) return;
                const { unreadCount } = JSON.parse(e.data);
                onUnreadCount(unreadCount);
            });

            es.onerror = () => {
                es.close();
                // reconecta após 10s em caso de erro
                if (active) setTimeout(connect, 10_000);
            };
        };

        connect();
        return () => {
            active = false;
            esRef.current?.close();
        };
    }, [onUnreadCount]);
}
```

**Atualização de `NotificationBell.jsx`:**  
- Remover `setInterval(fetchCount, 30_000)`
- Usar `useNotificationStream((count) => setUnreadCount(count))`
- Manter fetch inicial 1× no mount para o count inicial

#### 4.6 Autenticação do SSE no Gateway

O `api-gateway` valida o token Firebase. `EventSource` nativo **não suporta headers customizados**. Opções:

**Opção recomendada:** passar token como query param e validar no gateway:
```
GET /notifications/stream?token=<firebase_id_token>
```

No `api-gateway`, adicionar rota SSE com filtro que move `?token=` para `Authorization: Bearer` antes de rotear para o `notification-service`.

---

## 5. Impacto esperado pós-implementação

| Métrica | Antes | Depois (Fase 1) | Depois (Fase 2) |
|---|---|---|---|
| Req/min por usuário (pior caso) | ~25 | ~6 | ~1 (só heartbeat SSE) |
| Latência de notificação | até 30s | até 30s | < 1s |
| Conexões persistentes por usuário | 0 | 0 | 1 (SSE) |
| Notificações de convites no perfil | polling 30s | polling 30s | tempo real via SSE |

---

## 6. Ordem de execução sugerida

1. `chore`: Unificar `NotificationBell` → **commit isolado**
2. `fix`: Remover polling de serviços (`ServicesHomeBarber`, `BarberServicesPage`) → **commit isolado**
3. `refactor`: Aumentar intervalo do Dashboard + botão manual → **commit isolado**
4. `feat(notification-service)`: `SseEmitterService` + `NotificationSseController` → **commit**
5. `feat(api-gateway)`: Rota SSE com filtro de token → **commit**
6. `feat(frontend)`: Hook `useNotificationStream` + atualização `NotificationBell` → **commit**

---

## 7. O que NÃO será implementado (e por quê)

| Ideia | Motivo de descarte |
|---|---|
| WebSocket (STOMP) | Bidirecional não necessário; overhead de infra injustificado |
| Webhooks para frontend | Webhook é server → server; não resolve atualização de UI |
| Firebase Realtime Database/Firestore para sync | Introduz novo serviço gerenciado fora da stack definida — proibido por `copilot-instructions.md` |
| Long polling | Piora os problemas atuais; SSE é superior em todos os aspectos |
