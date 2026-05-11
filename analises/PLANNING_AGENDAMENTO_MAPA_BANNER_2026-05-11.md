# Planning — Slots de Agendamento, Mapa e Banner da Barbearia
**Data:** 2026-05-11 (revisado após análise completa do código)
**Branch:** `feature/migracao-microservicos`  
**Escopo:** frontend (`AgendamentoPage`, `RescheduleModal`, `BarbershopDetailPage`) + backend (`schedule-service`)

---

## 1. Diagnóstico real (após leitura de todos os arquivos)

### 1.1 Slots ocupados aparecendo no agendamento / reagendamento

**Sintoma:** cliente vê horários já ocupados como clicáveis.

**Causa raiz real — backend sem `@CacheEvict`:**

O `AppointmentService.java` tem `@Cacheable` em `getAvailability` com TTL Redis de 1 min, MAS **nenhum dos métodos que alteram estado tem `@CacheEvict`**:

| Método | O que faz | Tem @CacheEvict? |
|---|---|---|
| `createAppointment` | salva com `saveAndFlush` | ❌ |
| `cancelAppointment` | salva com `save` | ❌ |
| `rescheduleAppointment` | salva com `save` | ❌ |
| `createManualBooking` | salva com `saveAndFlush` | ❌ |

Resultado: após qualquer agendamento, o cache Redis continua retornando o slot como disponível por até 60 segundos. Somado ao cache frontend (TTL 60s em `appointmentAvailabilityService.js`), o cliente pode ver o slot como livre por até **2 minutos** após ele ter sido ocupado.

**Causa secundária — duração errada no `RescheduleModal`:**

`RescheduleModal.jsx` linha 40:
```js
const durationMinutes = appointment.totalDurationMinutes || appointment.durationMinutes || 30;
```
O `AppointmentDTO` **não tem nem `totalDurationMinutes` nem `durationMinutes`** — apenas `startTime` e `endTime`, e uma lista `activities` onde cada `AppointmentActivityDTO` tem `durationMinutes`. Logo ambas as propriedades são `undefined` → cai no fallback `30`, ignorando a duração real. Se o serviço for de 60 min e o modal calcular com 30, ele mostrará slots inválidos.

**Fix correto:** calcular duração via `endTime - startTime` ou somar `activities[].durationMinutes`.

---

### 1.2 Mapa não aparece em `BarbershopDetailPage`

**Diagnóstico real — mais complexo que o planejado inicialmente:**

Após ler todos os arquivos, o status completo é:

| Item | Status |
|---|---|
| `geocodeAddress` em `barbershopService.js` | ✅ **JÁ EXISTE** (linha ~250) — usando Nominatim |
| `latitude`/`longitude` na entidade `Barbershop.java` | ✅ **JÁ EXISTE** (`@Column name="latitude"/"longitude"`) |
| `latitude`/`longitude` em `BarbershopDTO.java` | ✅ **JÁ EXISTE** |
| `latitude`/`longitude` em `UpdateBarbershopDTO.java` | ✅ **JÁ EXISTE** |
| `updateBarbershop` salva lat/lng se enviados | ✅ **JÁ EXISTE** |
| `BarbershopMap` componente Leaflet | ✅ **Funcionando** |
| Colunas no banco (`barbershop_db`) | ⚠️ **PROVAVELMENTE NULL** — dados existentes nunca foram geocodificados |

**A causa real do mapa não aparecer é:** as barbearias no banco têm `latitude = NULL` e `longitude = NULL`. O `BarbershopDetailPage` chama `geocodeAddress` como fallback on-the-fly, mas:

1. O Nominatim pode falhar/timeout com endereços como `"Rua X, 123, Sorocaba"` se o formato não for preciso
2. Mesmo que geocode funcione, **as coordenadas NÃO são salvas de volta no banco** — próxima visita da página repete o problema
3. Se a barbearia foi criada por `CreateBarbershopPage` que também não geocodifica, ela nunca terá coordenadas

**Fix correto:** No `BarbershopService.java`, ao criar ou atualizar a barbearia, se `address` for enviado e `latitude`/`longitude` não forem fornecidos, geocodificar via Nominatim no backend e salvar. OU: criar endpoint `POST /barbershops/my-shop/geocode` para o owner disparar a geocodificação. A opção mais simples e imediata é melhorar o frontend para salvar as coordenadas após geocodificar.

---

### 1.3 Banner em `BarbershopDetailPage`

**Diagnóstico real:**

| Item | Status |
|---|---|
| Entity `banner_url` + `banner_url_public_id` | ✅ |
| `BarbershopDTO.bannerUrl` | ✅ |
| Endpoint `POST /my-shop/upload-banner` | ✅ |
| `uploadMyBarbershopBanner` em `barbershopService.js` | ✅ |
| Renderização do banner no JSX | ✅ |
| Botão de upload para o owner em `BarbershopDetailPage` | ❌ **FALTA** |
| `CropImageModal` importado em `BarbershopDetailPage` | ❌ **FALTA** |
| `useRef` + estados de upload | ❌ **FALTA** |

Tudo no backend está pronto. Apenas o front da `BarbershopDetailPage` não tem o botão de edição para o owner.

---

## 2. Trabalho a realizar

### Tarefa 1 — Fix crítico: `@CacheEvict` no `AppointmentService` `[schedule-service]`

**Prioridade:** Crítica  
**Arquivo:** `backend/schedule-service/.../service/AppointmentService.java`

Injetar `CacheManager` e chamar `evictAvailabilityForBarberAndDate(barberId, date)` nos 4 pontos de mutação:

```java
private void evictAvailabilityCache(UUID barberId, LocalDate date) {
    Cache cache = cacheManager.getCache("appointmentAvailability");
    if (cache == null) return;
    // Evicta todas as durações do dia: barberId:date:15, :30, :45, :60, :75, :90, :120
    for (int dur : List.of(15, 30, 45, 60, 75, 90, 120)) {
        cache.evict(barberId + ":" + date + ":" + dur);
    }
}
```

Chamar após `saveAndFlush`/`save` em:
- `createAppointment` → evict `barberId + startTime.toLocalDate()`
- `cancelAppointment` → evict `barberId + startTime.toLocalDate()`
- `rescheduleAppointment` → evict data anterior E nova data
- `createManualBooking` → evict `barberId + startTime.toLocalDate()`

---

### Tarefa 2 — Fix: duração correta no `RescheduleModal` `[front]`

**Prioridade:** Alta  
**Arquivo:** `frontend/src/components/RescheduleModal/RescheduleModal.jsx`

```js
// linha 40 — antes:
const durationMinutes = appointment.totalDurationMinutes || appointment.durationMinutes || 30;

// depois (usa endTime - startTime como fonte primária, activities como secundária):
const durationMinutes = (() => {
  if (appointment.startTime && appointment.endTime) {
    const mins = Math.round((new Date(appointment.endTime) - new Date(appointment.startTime)) / 60000);
    if (mins > 0) return mins;
  }
  if (Array.isArray(appointment.activities) && appointment.activities.length > 0) {
    const sum = appointment.activities.reduce((acc, a) => acc + (a.durationMinutes || 0), 0);
    if (sum > 0) return sum;
  }
  return 30;
})();
```

---

### Tarefa 3 — Fix: mapa — geocodificar e **persistir** coordenadas `[front + back]`

**Prioridade:** Alta  
**Arquivos:**
- `frontend/src/pages/BarbershopDetailPage.jsx`
- `frontend/src/pages/CreateBarbershopPage.jsx` (verificar se geocodifica ao criar)
- `backend/barbershop-service/.../service/BarbershopService.java` (geocodificar no `updateBarbershop`)

**Estratégia:** Quando o frontend geocodifica com sucesso via Nominatim e a barbearia não tem coordenadas, **salvar via `updateMyBarbershop`** para que a próxima visita já tenha as coordenadas.

Em `BarbershopDetailPage.jsx`:
```jsx
// Se geocode funcionou E o usuário é o owner, persiste silenciosamente
if (coords && isOwnerOfThisShop) {
  updateMyBarbershop({ latitude: coords.lat, longitude: coords.lng }).catch(() => {});
}
```

Em `BarbershopService.java`, opcionalmente fazer geocodificação automática no backend ao salvar `address` — via `RestTemplate` ou `WebClient` para Nominatim. Isso é opcional na primeira entrega.

---

### Tarefa 4 — Feature: botão de upload de banner para o owner em `BarbershopDetailPage` `[front]`

**Prioridade:** Média  
**Arquivos:**
- `frontend/src/pages/BarbershopDetailPage.jsx`
- `frontend/src/pages/CSS/BarbershopDetailPage.module.css`

Replicar exatamente o padrão já implementado em `AgendamentoPage.jsx`:
- `useRef(null)` para `bannerFileInputRef`
- Estados: `editingBanner`, `selectedBannerSrc`, `uploadingBanner`
- `handleBannerFileChange` → abre `CropImageModal` (16:9)
- `handleBannerCropConfirm` → chama `uploadMyBarbershopBanner(file)` → atualiza `shopInfo.bannerUrl`
- Botão `✏️ Editar banner` visível apenas para `isOwnerOfThisShop`
- CSS: `.editBannerBtn` já existe em `AgendamentoPage.module.css` — replicar para `BarbershopDetailPage.module.css`

**Imports a adicionar:**
```jsx
import { useRef } from 'react'; // já tem useState
import CropImageModal from '../components/CropImageModal/CropImageModal';
import { ..., uploadMyBarbershopBanner, updateMyBarbershop } from '../services/barbershopService';
```

---

## 3. Ordem de execução

```
[1] Tarefa 1  — backend: @CacheEvict em AppointmentService (4 pontos)
[2] Tarefa 2  — frontend: duração correta no RescheduleModal
[3] Tarefa 3  — frontend: BarbershopDetailPage geocode + persist
[4] Tarefa 4  — frontend: banner upload para owner em BarbershopDetailPage
[5] Build + deploy schedule-service (só back mudou)
[6] Testes:
     - Agendar slot A → outro cliente tenta o mesmo slot → não aparece (< 5s)
     - Reagendar → modal usa duração correta (não 30 fixo)
     - Abrir página da barbearia → mapa aparece (geocode ou coordenadas do banco)
     - Owner abre página → vê botão "Editar banner" → upload funciona
```

---

## 4. Itens confirmados (não precisam de confirmação)

- ✅ `geocodeAddress` já existe — não precisa criar
- ✅ Colunas `latitude`/`longitude` já existem na entidade e no banco (baseado nos `@Column` e query `WHERE latitude IS NOT NULL`)
- ✅ `UpdateBarbershopDTO` já aceita `latitude`/`longitude`
- ✅ Backend do banner 100% pronto

## 5. Único item que precisa de confirmação

- [ ] **Passo de slots**: manter 15 min fixo (padrão atual) ou mudar para passo igual à duração do serviço? **Recomendo manter 15 min** — dá mais granularidade ao cliente sem alterar o comportamento de bloqueio (o bloqueio já usa a duração total corretamente).
