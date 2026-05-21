# CortaAi — SDD Ciclo 1 · Sprints 1–4
> Branch: `feature/migracao-microservicos` · 08/05/2026  
> **Auto-contido** — tudo para implementar os 19 épicos está aqui.  
> Ciclo 2 (IA, Geo, Maps, PDF): `CICLO2_SDD.md`

---

## Épicos

| Sprint | ID | Título | Serviços | Esforço |
|---|---|---|---|---|
| 1 | E-04 | Remover 'Hoje/Amanhã' dos labels de data | frontend | XS |
| 1 | E-09 | Anti-flicker em refresh automático | frontend | S |
| 1 | E-10 | Fix upload foto de perfil do barbeiro | user-service, frontend | S |
| 1 | E-11 | Logo navbar sem texto | frontend | XS |
| 1 | E-15 | Spinner substituindo texto de carregamento | frontend | XS |
| 1 | E-17 | Foto barbeiro nos cards de seleção | frontend | XS |
| 1 | E-23 | Barbeiro sem skill = opaco no card | frontend | XS |
| 2 | E-12 | Colaborador: ocultar aba Serviços, exibir só Habilidades | frontend | S |
| 2 | E-14 | Tamanho fixo de fotos + crop 1:1 | f
rontend | M |
| 2 | E-16 | PWA — prompt nativo + push sem popup intermediário | frontend | S |
| 2 | E-21 | Indicativo barbearia ativa na navbar | frontend | S |
| 2 | E-22 | UI indisponibilidade barbeiro (BarberBlock — backend pronto) | frontend | M |
| 3 | E-03 | Agenda — remover abas inferiores, adicionar Encaixe/Pendente | frontend | M |
| 3 | E-05 | Agenda equipe — seletores Dia/Semana/Mês + grid | frontend | L |
| 3 | E-06 | EXPIRED status + reagendamento automático (min 3h) | schedule-service, frontend | L |
| 4 | E-01 | Categorias dinâmicas de estoque (multi-tenant) | product-service, frontend | M |
| 4 | E-02 | Modal de baixa de estoque com flags | product-service, frontend | M |
| 4 | E-07 | Time management: listagem, comissão por serviço, remoção | barbershop-service, frontend | XL |
| 4 | E-08 | Dashboard fix (filtrar por barbershopId) + eye icon | barbershop-service, frontend | L |

> XS < 2h · S 2–4h · M 4–8h · L 8–16h · XL > 16h

---

## ADRs

| ADR | Decisão resumida |
|---|---|
| ADR-01 | `ProductCategory` enum → entidade `Category(id, name, barbershopId)`. Exclusão bloqueada se produto ativo. |
| ADR-02 | `MovementType`: `IN`, `OUT_CONSUMPTION`, `OUT_SALE`, `LOSS`, `RETURN`. `OUT_SALE` registra `unitSalePrice`. |
| ADR-03 | `EXPIRED` é lazy — calculado na leitura, **não persiste** no banco. Critério: `PAYMENT_PENDING && now > startTime + 1h`. |
| ADR-04 | `RescheduleAppointmentDTO` ganha `barberId` nullable (backward compatible). Mínimo 3h de antecedência. Somente o cliente dono pode reagendar. |
| ADR-05 | Time derivado de `BarbershopJoinRequest` status `ACCEPTED` + Feign ao user-service. Sem nova entidade. |
| ADR-06 | Comissão por serviço: tabela `barber_commission_rules(barbershop_id, barber_id, activity_id, percentage)`. |
| ADR-07 | Remoção de colaborador: owner visualiza conflitos e escolhe redistribuir OU cancelar com notificação. Evento `barber.removed`. |
| ADR-08 | Analytics: `barbershopId` extraído do header `X-User-Id` via lookup interno — nunca aceitar como query param. |
| ADR-09 | PWA: remover `InstallAppPopup` e `PushNotificationPrompt`. Chamar funções diretas. Pop-up = nativo do browser. |
| ADR-10 | Agenda equipe: CSS grid `auto-fit / minmax(180px, 1fr)` — sem scroll lateral, adapta para N barbeiros. |
| ADR-11 | Indisponibilidade em dois fluxos separados na mesma tela: "Horas avulsas" e "Dias avulsos / Período". |

---

## Migrations pendentes (confirmar antes de executar)

```sql
-- E-01: categorias dinâmicas
CREATE TABLE categories (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    barbershop_id CHAR(36) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_category (name, barbershop_id)
);
ALTER TABLE products ADD COLUMN category_id CHAR(36);
ALTER TABLE products ADD CONSTRAINT fk_product_category
    FOREIGN KEY (category_id) REFERENCES categories(id);

-- E-07: comissão por serviço
CREATE TABLE barber_commission_rules (
    id CHAR(36) PRIMARY KEY,
    barbershop_id CHAR(36) NOT NULL,
    barber_id CHAR(36) NOT NULL,
    activity_id CHAR(36) NOT NULL,
    percentage DECIMAL(5,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_barber_activity (barbershop_id, barber_id, activity_id)
);
```

## Views SQL necessárias (verificar `SHOW FULL TABLES WHERE Table_type = 'VIEW'`)

```sql
CREATE OR REPLACE VIEW v_stock_health_alert AS
SELECT p.barbershop_id, p.id AS product_id, p.name, p.quantity, p.min_stock_threshold,
       CASE WHEN p.quantity <= p.min_stock_threshold THEN 'CRITICAL'
            WHEN p.quantity <= p.min_stock_threshold * 1.5 THEN 'WARNING'
            ELSE 'OK' END AS status
FROM products p WHERE p.active = true;

CREATE OR REPLACE VIEW v_barber_financial_performance AS
SELECT a.barber_id, a.barbershop_id,
       DATE_FORMAT(a.start_time, '%Y-%m') AS month,
       COUNT(*) AS total_appointments,
       SUM(a.total_price) AS gross_revenue,
       SUM(a.total_price * COALESCE(bcr.percentage, 0) / 100) AS commission_value
FROM appointments a
LEFT JOIN barber_commission_rules bcr ON bcr.barber_id = a.barber_id AND bcr.barbershop_id = a.barbershop_id
WHERE a.status IN ('COMPLETED','CONCLUDED')
GROUP BY a.barber_id, a.barbershop_id, month;

CREATE OR REPLACE VIEW v_agenda_thermometer AS
SELECT barbershop_id, DATE(start_time) AS day, COUNT(*) AS total_slots,
       SUM(CASE WHEN status IN ('SCHEDULED','CONFIRMED','IN_PROGRESS') THEN 1 ELSE 0 END) AS occupied,
       ROUND(SUM(CASE WHEN status IN ('SCHEDULED','CONFIRMED','IN_PROGRESS') THEN 1 ELSE 0 END)*100.0/COUNT(*),2) AS occupancy_pct
FROM appointments GROUP BY barbershop_id, day;

CREATE OR REPLACE VIEW v_barber_skill_matrix AS
SELECT b.barbershop_id, b.barber_id,
       GROUP_CONCAT(a.name ORDER BY a.name SEPARATOR ', ') AS skills, COUNT(a.id) AS skill_count
FROM barber_activities b JOIN activities a ON a.id = b.activity_id
GROUP BY b.barbershop_id, b.barber_id;
```

---

## Sprint 1 — Fixes visuais (sem backend)

### E-04 · Remover 'Hoje/Amanhã/Ontem' dos labels de data

**Arquivo:** `MeusAgendamentosPage.jsx`

```js
// Substituir formatDateDisplay para sempre retornar data formatada:
const formatDateDisplay = (date) =>
  format(date, "EEEE, dd 'de' MMMM", { locale: ptBR });
```

Manter o botão "Hoje" (navega para data atual) — só o **label** muda.  
Stats do topo: incluir `WALK_IN` e `PAYMENT_PENDING` nos totais.

**Aceite:** Nenhum label exibe 'Hoje', 'Amanhã' ou 'Ontem'.

---

### E-09 · Anti-flicker em refresh automático

**Arquivos:** `MeusAgendamentosPage.jsx`, `BarberDashboardPage.jsx`, `BarberStockPage.jsx`, `AgendamentoPage.jsx`

```jsx
import { startTransition } from 'react';

// Loading só na inicialização:
const [initialized, setInitialized] = useState(false);
const load = async () => {
  if (!initialized) setLoading(true);
  const data = await fetchData();
  startTransition(() => { setData(data); setLoading(false); setInitialized(true); });
};

// Refresh silencioso (sem setLoading):
const refresh = async () => {
  const data = await fetchData();
  startTransition(() => setData(data));
};
```

**Aceite:** Spinner só no carregamento inicial. Refreshes automáticos silenciosos.

---

### E-10 · Fix upload foto de perfil do barbeiro

**Cadeia confirmada:** `Barber.imageUrl` → `toUserInfoDTO` → `UserInfoDTO.imageUrl` → `BarberPublicDTO.imageUrl` ✅

**Arquivo:** `BarberProfilePage.jsx`

```jsx
const handleUploadProfilePhoto = async (file) => {
  setUploadingPhoto(true);
  try {
    const response = await uploadBarberProfilePhoto(file);
    setProfileData(prev => ({ ...prev, imageUrl: response.data.imageUrl }));
    toast.success('Foto atualizada com sucesso!');
  } catch {
    toast.error('Erro ao enviar foto. Tente novamente.');
  } finally {
    setUploadingPhoto(false);
  }
};
```

Verificar: endpoint retorna `{ imageUrl: string }`. URL deve ser `https://` (não `http://`).

**Aceite:** Upload funciona. Foto exibida sem reload.

---

### E-11 · Logo navbar sem texto

**Arquivo:** `BarberHeader.jsx`

```jsx
// Remover:
<div className={styles.brandText}>
  <span>CortaAI</span>
  <span className={styles.brandSubtitle}>Painel profissional</span>
</div>

// Manter e ajustar:
<img src={logoImg} alt="CortaAi" className={styles.brandLogo} />
```
```css
.brandLogo { height: 120px; width: auto; object-fit: contain; }
```

Verificar `CustomerHeader.jsx` — aplicar mesma remoção se existir texto similar.

---

### E-15 · Spinner substituindo texto de carregamento de datas

**Arquivo:** `AgendamentoPage.jsx` — linha ~561

```jsx
// Substituir:
<p className={Styles.info_text}>Carregando datas inteligentes...</p>
// Por:
<div className={Styles.dateSpinner} aria-label="Carregando datas" />
```
```css
.dateSpinner {
  width: 32px; height: 32px;
  border: 3px solid var(--color-primary-light, #e0e0e0);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 24px auto;
}
@keyframes spin { to { transform: rotate(360deg); } }
```

---

### E-17 · Foto do barbeiro nos cards de seleção

**Arquivo:** `AgendamentoPage.jsx` — linha ~521

```jsx
// Substituir sigla por foto com fallback:
{barber.imageUrl
  ? <img src={barber.imageUrl} alt={barber.name} className={Styles.barberAvatarImg}
         onError={e => { e.currentTarget.style.display='none'; e.currentTarget.nextSibling.style.display='flex'; }} />
  : null}
<span className={Styles.barberAvatar} style={{ display: barber.imageUrl ? 'none' : 'flex' }}>
  {getInitials(barber.name)}
</span>
```
```css
.barberAvatarImg { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; border: 2px solid var(--color-primary); }
```

---

### E-23 · Barbeiro sem skill = opaco no card

**Arquivo:** `AgendamentoPage.jsx` — linha ~528

```jsx
// Remover tag "⚠️ Sem serviços"
<div
  className={`${Styles.barberCard} ${!barber.hasSkills ? Styles.barberCardDisabled : ''}`}
  onClick={!barber.hasSkills ? undefined : () => handleSelectBarber(barber)}
>
```
```css
.barberCardDisabled { opacity: 0.45; pointer-events: none; cursor: not-allowed; }
```

---

## Sprint 2 — UX, PWA e BarberBlock UI

### E-12 · Colaborador: ocultar aba Serviços

**Arquivo:** `BarberProfilePage.jsx` (componente de tabs)

```jsx
const isOwner = localStorage.getItem('isOwner') === 'true';
const tabs = [
  { id: 'perfil', label: 'Perfil' },
  isOwner && { id: 'servicos', label: 'Serviços' },
  { id: 'habilidades', label: 'Habilidades' },
  { id: 'agenda', label: 'Agenda' },
].filter(Boolean);
```

Remover link de navegação para `/barber/servicos` na navbar quando `!isOwner`.

---

### E-14 · Tamanho fixo de fotos + crop 1:1

**Instalar:** `npm install react-image-crop`

**Novo componente:** `src/components/CropImageModal/CropImageModal.jsx`

```jsx
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';
import { useState, useRef } from 'react';
import styles from './CropImageModal.module.css';

export function CropImageModal({ imageSrc, aspect = 1, onConfirm, onCancel }) {
  const [crop, setCrop] = useState();
  const imgRef = useRef(null);

  function onImageLoad(e) {
    const { width, height } = e.currentTarget;
    setCrop(centerCrop(makeAspectCrop({ unit: '%', width: 80 }, aspect, width, height), width, height));
  }

  function handleConfirm() {
    const canvas = document.createElement('canvas');
    const img = imgRef.current;
    const sx = img.naturalWidth / img.width, sy = img.naturalHeight / img.height;
    canvas.width = 400; canvas.height = 400;
    canvas.getContext('2d').drawImage(img, crop.x*sx, crop.y*sy, crop.width*sx, crop.height*sy, 0, 0, 400, 400);
    canvas.toBlob(blob => onConfirm(blob), 'image/jpeg', 0.9);
  }

  return (
    <div className={styles.overlay}>
      <div className={styles.modal}>
        <ReactCrop crop={crop} onChange={setCrop} aspect={aspect} circularCrop={aspect === 1}>
          <img ref={imgRef} src={imageSrc} onLoad={onImageLoad} alt="Cortar" />
        </ReactCrop>
        <div className={styles.actions}>
          <button onClick={onCancel}>Cancelar</button>
          <button onClick={handleConfirm} disabled={!crop}>Confirmar</button>
        </div>
      </div>
    </div>
  );
}
```

Integrar em: `BarberProfilePage.jsx` (`aspect={1}`) e edição de logo da barbearia (`aspect={1}`).  
Banner usa `aspect={16/9}` — ver E-24 no Ciclo 2.

---

### E-16 · PWA — prompt nativo + push sem popup

Ver **ADR-09**.

**Arquivo:** `App.jsx`

```jsx
// Remover imports:
// import InstallAppPopup from './components/InstallAppPopup/InstallAppPopup';
// import PushNotificationPrompt from './components/PushNotificationPrompt/PushNotificationPrompt';

useEffect(() => {
  const handle = () => {
    requestPwaInstall();                              // prompt nativo do browser
    requestPushNotificationsPermissionAndRegister();  // permissão do SO
  };
  window.addEventListener('cortaai:login-success', handle);
  return () => window.removeEventListener('cortaai:login-success', handle);
}, []);

// Remover do JSX:
// {showInstallPrompt && <InstallAppPopup ... />}
// {showPushPrompt && <PushNotificationPrompt ... />}
```

---

### E-21 · Indicativo de barbearia ativa na navbar

**Arquivo:** `BarberNavbar.jsx` ou `BarberHeader.jsx`

```jsx
const isOwner = localStorage.getItem('isOwner') === 'true';
const barbershopName = localStorage.getItem('barbershopName');

{isOwner && barbershopName && (
  <span className={styles.activeBarbershop}>🏪 {barbershopName}</span>
)}
```

Popular `barbershopName` no serviço de auth ao processar login:
```js
localStorage.setItem('barbershopName', response.data.barbershopName);
```

**Nova chave localStorage:** `barbershopName` (justificada — não existe nas chaves padronizadas atuais).

---

### E-22 · UI de Indisponibilidade do Barbeiro

**Backend 100% pronto** — endpoints no `schedule-service`:
- `POST /api/appointments/barber-blocks`
- `GET /api/appointments/barber-blocks?barberId=&date=`
- `DELETE /api/appointments/barber-blocks/{id}`

**Novo service:** `src/services/barberBlockService.js`

```js
import api from './api';
export const createBarberBlock = (data) => api.post('/appointments/barber-blocks', data);
export const getBarberBlocks = (barberId, date) => api.get('/appointments/barber-blocks', { params: { barberId, date } });
export const deleteBarberBlock = (id) => api.delete(`/appointments/barber-blocks/${id}`);
```

**Nova página:** `src/pages/BarberBlockPage.jsx` | Rota: `/barber/indisponibilidade`

Ver **ADR-11** — dois fluxos na mesma tela:

```
[A] Horas avulsas
    Data: ___  De: HH:MM  Até: HH:MM  [Bloquear horas]

[B] Dias avulsos / Período
    ○ Dias avulsos (calendário multi-select)
    ○ Período (range de datas)
    [Bloquear dias]

Bloqueios ativos:
• 10/05 09:00–12:00  [✕]
• 15/05 dia inteiro  [✕]
```

DTO criação: `{ barberId, startTime: "2026-05-10T09:00:00", endTime: "2026-05-10T12:00:00", reason? }`  
Dia inteiro: `startTime = T00:00:00`, `endTime = T23:59:59`.

---

## Sprint 3 — Agenda

### E-03 · Remover abas inferiores, adicionar Encaixe/Pendente

**Arquivo:** `MeusAgendamentosPage.jsx`

```jsx
// 1. Remover: array filterItems + componente de abas inferiores

// 2. Cards no topo com toggle (segundo clique remove filtro):
<div className={Styles.quickFilterCards}>
  {[
    { filter: 'WALK_IN',        label: 'Encaixes',  count: stats.walkIn  },
    { filter: 'PAYMENT_PENDING', label: 'Pendentes', count: stats.pending },
  ].map(({ filter, label, count }) => (
    <button key={filter}
      className={`${Styles.quickCard} ${activeFilter === filter ? Styles.active : ''}`}
      onClick={() => setActiveFilter(f => f === filter ? 'ALL' : filter)}
    >
      <span className={Styles.count}>{count}</span>
      <span>{label}</span>
    </button>
  ))}
</div>
```

Stats:
```js
const stats = useMemo(() => ({
  walkIn:  appointments.filter(a => a.status === 'WALK_IN').length,
  pending: appointments.filter(a => a.status === 'PAYMENT_PENDING').length,
}), [appointments]);
```

---

### E-05 · Agenda equipe — Dia/Semana/Mês + grid por barbeiro

**Arquivo:** `MeusAgendamentosPage.jsx` — ver **ADR-10**.

```jsx
const [rangeMode, setRangeMode] = useState('day');

{agendaView === 'team' && (
  <div className={Styles.rangeModeSelector}>
    {[['day','Dia'],['week','Semana'],['month','Mês']].map(([mode, label]) => (
      <button key={mode}
        className={`${Styles.rangeBtn} ${rangeMode === mode ? Styles.active : ''}`}
        onClick={() => setRangeMode(mode)}
      >{label}</button>
    ))}
  </div>
)}

{rangeMode === 'day' && agendaView === 'team' && (
  <div className={Styles.teamDayGrid}>
    {barberGroups.map(({ barber, appointments }) => (
      <div key={barber.id} className={Styles.barberColumn}>
        <div className={Styles.barberColumnHeader}>
          <img src={barber.imageUrl} alt={barber.name} className={Styles.colAvatar} />
          <span>{barber.name}</span>
        </div>
        {appointments.map(apt => <AppointmentCard key={apt.id} data={apt} />)}
        {appointments.length === 0 && <p className={Styles.emptyCol}>—</p>}
      </div>
    ))}
  </div>
)}
```

```css
.teamDayGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px; align-items: start;
}
```

---

### E-06 · EXPIRED status + reagendamento automático

Ver **ADR-03** e **ADR-04**. Backend: `rescheduleAppointment` já existe no `schedule-service`.

#### Backend — `schedule-service`

**`AppointmentStatus.java`** — adicionar:
```java
EXPIRED  // projeção de leitura — não persiste no banco
```

**Lógica lazy** em todos os métodos de listagem:
```java
private AppointmentStatus resolveStatus(Appointment apt) {
    if (apt.getStatus() == AppointmentStatus.PAYMENT_PENDING
            && LocalDateTime.now().isAfter(apt.getStartTime().plusHours(1)))
        return AppointmentStatus.EXPIRED;
    return apt.getStatus();
}
// Aplicar no toResponseDTO. NÃO chamar repository.save() aqui.
```

**`RescheduleAppointmentDTO.java`** — adicionar campo nullable:
```java
public record RescheduleAppointmentDTO(
    @NotNull LocalDateTime newStartTime,
    UUID barberId   // null = mantém barbeiro atual
) {}
```

**Validações em `rescheduleAppointment`:**
```java
if (!appointment.getCustomerId().equals(customerId))
    throw new UnauthorizedException("Somente o cliente dono pode reagendar");
if (Duration.between(LocalDateTime.now(), dto.newStartTime()).toHours() < 3)
    throw new ValidationException("Reagendamento requer mínimo de 3 horas de antecedência");
if (dto.barberId() != null) {
    validateBarberBelongsToBarbershop(dto.barberId(), appointment.getBarbershopId());
    appointment.setBarberId(dto.barberId());
}
appointment.setStartTime(dto.newStartTime());
```

#### Frontend

```jsx
// Botão visível apenas quando:
const canReschedule = isCustomer
  && ['SCHEDULED','CONFIRMED'].includes(apt.status)
  && differenceInHours(parseISO(apt.startTime), new Date()) > 3;

// Sem modal de confirmação — automático:
const handleReschedule = async (appointmentId, newStartTime, barberId = null) => {
  await rescheduleAppointment(appointmentId, { newStartTime, barberId });
  toast.success('Agendamento remarcado!');
  refresh(); // silencioso (E-09)
};
```

**Aceite:** `EXPIRED` retornado na API, não persiste. < 3h → 422. Não-dono → 403. `barberId` null mantém barbeiro.

---

## Sprint 4 — Estoque, Time e Dashboard

### E-01 · Categorias dinâmicas de estoque

Ver **ADR-01**. Migration acima.

#### Backend — `product-service`

```java
@Entity @Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false) private UUID barbershopId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

Endpoints:
```
POST   /api/products/categories          (owner only)
GET    /api/products/categories
PUT    /api/products/categories/{id}     (owner only)
DELETE /api/products/categories/{id}     (bloqueado se produto ativo)
```

DTOs:
```java
public record CategoryRequestDTO(@NotBlank String name) {}
public record CategoryResponseDTO(UUID id, String name, UUID barbershopId) {}
```

Validação exclusão:
```java
if (productRepository.existsActiveByCategoryId(categoryId))
    throw new ConflictException("Categoria possui produtos ativos. Reclassifique antes de excluir.");
```

#### Frontend — `BarberStockPage.jsx`

- Substituir `PRODUCT_CATEGORIES` hardcoded por `GET /api/products/categories`
- Aba "Categorias" (owner only): CRUD inline com ✏️ e 🗑️
- Colaborador: visualiza estoque, sem botões de edição

---

### E-02 · Modal de baixa de estoque com flags

Ver **ADR-02**.

#### Backend — `product-service`

```java
public enum MovementType { IN, OUT_CONSUMPTION, OUT_SALE, LOSS, RETURN }

public record StockMovementRequestDTO(
    @NotNull UUID productId,
    @NotNull MovementType type,
    @NotNull @Positive int quantity,
    BigDecimal unitSalePrice,  // obrigatório se type == OUT_SALE
    String notes
) {}
```

Validação: `product.quantity - requested < 0` → `BusinessException("Quantidade insuficiente em estoque")`.

#### Frontend — novo `StockMovementModal.jsx`

```jsx
const FLAGS = [
  { type: 'OUT_CONSUMPTION', label: 'Consumo interno', icon: '✂️' },
  { type: 'OUT_SALE',        label: 'Venda',            icon: '💰' },
  { type: 'LOSS',            label: 'Perda / Descarte', icon: '🗑️' },
  { type: 'RETURN',          label: 'Devolução',        icon: '↩️' },
];
// Campo: flag + quantidade + preço (se venda) + observação opcional
// POST /api/products/stock-movements → atualizar lista local sem reload
```

Botão de abertura: ícone ✏️ por produto (substitui controles +/-).

---

### E-07 · Time management

Ver **ADR-05**, **ADR-06**, **ADR-07**. Migration acima.

#### Backend — `barbershop-service`

**`GET /barbershops/my-shop/team`**
```java
public record TeamMemberResponseDTO(
    UUID barberId, String name, String imageUrl, String email,
    Boolean isOwner, LocalTime workStartTime, LocalTime workEndTime,
    List<CommissionRuleDTO> commissions
) {}
public record CommissionRuleDTO(UUID id, UUID activityId, String activityName, BigDecimal percentage) {}
```

**Endpoints de comissão:**
```
POST   /barbershops/my-shop/team/{barberId}/commissions
GET    /barbershops/my-shop/team/{barberId}/commissions
DELETE /barbershops/my-shop/team/{barberId}/commissions/{ruleId}
```

**`GET /barbershops/my-shop/team/{barberId}/conflicts`** via Feign ao schedule-service:
```java
@GetMapping("/appointments/by-barber/{barberId}/future")
List<AppointmentSummaryDTO> getFutureAppointmentsByBarber(@PathVariable UUID barberId);
```

**`DELETE /barbershops/my-shop/team/{barberId}`**:
```json
{ "action": "REDISTRIBUTE", "redistributeToId": "uuid" }
// ou
{ "action": "CANCEL" }
```
Publica evento `barber.removed` na exchange `cortaai.barbershop.exchange`.

#### Frontend — `BarberTeamPage.jsx`

```
Lista membros → Card (foto + nome + horário)
  → Botão "Comissões" → drawer: serviços + % editável
  → Botão "Remover" → modal de conflitos
      → Lista agendamentos futuros
      → "Redistribuir para:" (select) | "Cancelar e notificar"
Botão "Convidar" (fluxo existente — manter)
```

---

### E-08 · Dashboard fix + eye icon

Ver **ADR-08**.

#### Backend — `barbershop-service`

Todos os endpoints de analytics extraem `barbershopId` do header:
```java
@GetMapping("/analytics/financial-performance")
public ResponseEntity<?> getFinancialPerformance(@RequestHeader("X-User-Id") String ownerUid) {
    UUID barbershopId = barbershopService.getBarbershopByOwnerUid(ownerUid);
    return ResponseEntity.ok(analyticsService.getFinancialPerformance(barbershopId));
}
```

#### Frontend — `DashReportPanel.jsx`

```jsx
import { Eye, EyeSlash } from '@phosphor-icons/react';

<button className={styles.toggleBtn} onClick={() => setVisible(v => !v)}
        aria-label={visible ? 'Ocultar seção' : 'Exibir seção'}>
  {visible ? <EyeSlash size={20} /> : <Eye size={20} />}
</button>
```

Estado de visibilidade: sessão React apenas (sem localStorage).
