# CortaAi — Planning & SDD | Ciclo Maio 2026

> **Branch de trabalho:** `feature/migracao-microservicos`
> **Data de geração:** 08/05/2026
> **Responsável técnico:** GitHub Copilot — validado pelo owner

---

## Índice

1. [Decisões de Arquitetura (ADRs rápidos)](#1-decisões-de-arquitetura)
2. [Épicos e PRs — visão geral](#2-épicos-e-prs)
3. [Detalhamento por Épico](#3-detalhamento-por-épico)
4. [Views/Queries de Analytics mapeadas](#4-viewsqueries-de-analytics)
5. [Mapa de impacto por microsserviço](#5-mapa-de-impacto-por-microsserviço)
6. [Ordem de execução recomendada](#6-ordem-de-execução)
7. [Pendências e decisões em aberto](#7-pendências)

---

## 1. Decisões de Arquitetura

| # | Decisão | Alternativa rejeitada | Motivo |
|---|---|---|---|
| ADR-01 | `ProductCategory` enum → entidade `Category` no banco, multi-tenant por `barbershopId` | Manter enum + lista paralela | Flexibilidade para owner, isolamento correto |
| ADR-02 | `MovementType` ganha 4 valores: `IN`, `OUT_CONSUMPTION`, `OUT_SALE`, `LOSS`, `RETURN` | Flags booleanas no `StockMovement` | Semântica clara, filtragem eficiente em relatórios |
| ADR-03 | `AppointmentStatus` ganha `EXPIRED` | Flag lazy sem status | Relatórios, filtros e notificações precisam distinguir estados |
| ADR-04 | Expiração lazy: verifica no `GET /appointments` e persiste `EXPIRED` se `startTime + 1h < now` + `status == PAYMENT_PENDING` | Job `@Scheduled` | Menor overhead de infra, sem job adicional |
| ADR-05 | Reagendamento pelo cliente: automático, sem confirmação do barbeiro, mínimo 3h antes | Fluxo com aprovação | Barbeiro já teve horário aceito; cliente escolhe de slots disponíveis |
| ADR-06 | Remoção de colaborador com agendamentos futuros: owner visualiza conflitos e decide (redistribuir ou cancelar com notificação) | Bloquear / cascatear | Mais profissional, sem cascata incontrolável |
| ADR-07 | Categorias dinâmicas isoladas por `barbershopId`; exclusão bloqueada se houver produto ativo | Soft-delete ou realocação | Integridade + UX clara |
| ADR-08 | Chat IA: Gemini Flash 1.5 (primário) → Groq LLaMA 3.3 (fallback) → sem resposta com aviso | Single provider | Resiliência sem custo |
| ADR-09 | Export de relatório: PDF estilizado com logo CortaAi via `@react-pdf/renderer` | CSV | Branding + legibilidade |
| ADR-10 | `BarberBlock` já existe no `schedule-service`; front cria UI de calendário sobre endpoint existente | Criar nova entidade | Reutilização do modelo já correto |
| ADR-11 | Banner da barbearia → aparece na página de detalhes da barbearia (`/barbearia/:id`); remover opção de upload de banner do perfil | Banner no header de todas as páginas | Uso pontual e correto |
| ADR-12 | Crop de foto: `react-image-crop` (leve, sem dependências nativas) | `cropperjs` | Menor bundle, React-nativo |
| ADR-13 | Ocultar Dashboard/Relatório: estado por sessão React (não persiste em banco/localStorage) | Persistência | Simplicidade, sem overhead de API |
| ADR-14 | Geolocalização: `navigator.geolocation` nativo; fallback = manter ordenação padrão sem reordenar | Pedir novamente | UX correta segundo especificação |
| ADR-15 | Link Maps: `https://maps.google.com?q=<endereço>` (sem API key) abre em nova aba | Iframe embed | Gratuito, sem chave de API |

---

## 2. Épicos e PRs

| ID | Épico | Serviços impactados | Prioridade | Estimativa |
|---|---|---|---|---|
| E-01 | Categorias dinâmicas de estoque | `product-service`, Frontend | 🔴 Alta | M |
| E-02 | Nova baixa de estoque (modal + flags) | `product-service`, Frontend | 🔴 Alta | M |
| E-03 | Filtros superiores da Minha Agenda + remoção abas inferiores | Frontend | 🟡 Média | S |
| E-04 | Correções visuais da Minha Agenda (data, equipe, período) | Frontend | 🟡 Média | S |
| E-05 | Expiração de agendamentos (`EXPIRED`) + notificação | `schedule-service`, `notification-service` | 🔴 Alta | M |
| E-06 | Reagendamento pelo cliente | `schedule-service`, `notification-service`, Frontend | 🔴 Alta | M |
| E-07 | Gestão completa do time (Meu Time) | `user-service`, `barbershop-service`, `schedule-service`, Frontend | 🟡 Média | L |
| E-08 | Dashboard: restrição owner + ícone olho + export PDF | Frontend, `payment-service` | 🔴 Alta | M |
| E-09 | Fix piscando / auto-refresh sem flicker | Frontend | 🟡 Média | S |
| E-10 | Fix upload foto de perfil do barbeiro | `user-service`, Frontend | 🔴 Alta | S |
| E-11 | Logo CortaAi na navbar (sem texto) | Frontend | 🟢 Baixa | XS |
| E-12 | Colaborador: ocultar serviços, mostrar apenas habilidades | Frontend | 🟡 Média | S |
| E-13 | Foto fixa nos cards + crop arrastável | Frontend | 🟡 Média | M |
| E-14 | Barbeiro sem habilidade → opaco e não clicável | Frontend | 🟡 Média | S |
| E-15 | Substituir texto "Carregando datas inteligentes" por spinner | Frontend | 🟢 Baixa | XS |
| E-16 | PWA: install nativo + notificações push | Frontend, `notification-service` | 🔴 Alta | M |
| E-17 | Foto do barbeiro no card de seleção | Frontend, `user-service` | 🟡 Média | S |
| E-18 | Chat IA (barbeiro/owner) | Frontend, novo `ai-gateway` ou endpoint no `schedule-service` | 🟡 Média | L |
| E-19 | Geolocalização na home do cliente | Frontend | 🟡 Média | S |
| E-20 | Link Maps no endereço da barbearia + página de detalhes | Frontend, `barbershop-service` | 🟡 Média | S |
| E-21 | Indicativo de barbearia ativa na navbar | Frontend | 🟢 Baixa | XS |
| E-22 | Indisponibilidade avulsa do barbeiro (calendário) | `schedule-service`, Frontend | 🔴 Alta | M |
| E-23 | Export PDF de relatórios com período | Frontend | 🟡 Média | M |
| E-24 | BarberHome — auditoria e correções | Frontend | 🟡 Média | S |

> Estimativas: XS=<2h | S=2-4h | M=4-8h | L=8-16h

---

## 3. Detalhamento por Épico

---

### E-01 — Categorias dinâmicas de estoque

**Objetivo:** Substituir `ProductCategory` enum por entidade `Category` gerenciada pelo owner.

#### Backend — `product-service`

**Nova entidade `Category`:**
```
product_categories
  id          UUID PK
  name        VARCHAR(100) NOT NULL
  barbershop_id UUID NOT NULL
  created_at  DATETIME
  UNIQUE(name, barbershop_id)
```

**Novos endpoints:**
- `POST /products/categories` — cria categoria (owner)
- `GET /products/categories` — lista categorias da barbearia do owner
- `PUT /products/categories/{id}` — edita (owner)
- `DELETE /products/categories/{id}` — exclui, **bloqueia** se existir `Product` com `categoryId` ativo

**Alterações no `Product`:**
- Campo `category` (enum) → `category_id UUID FK → product_categories.id`
- Migration necessária: **pedir confirmação antes de executar** (conversão enum → FK)

**DTOs novos:**
- `CategoryRequestDTO(name: String)`
- `CategoryResponseDTO(id, name, barbershopId)`

**Regras:**
- Somente `ROLE_BARBER` + `isOwner=true` acessa os endpoints de escrita
- Colaborador não vê nem acessa esses endpoints

#### Frontend — `BarberStockPage`

**Nova seção "Categorias"** (tab ou card colapsável acima do formulário de produto):
- Botão **"+ Nova categoria"** → modal inline com campo nome + salvar
- Lista das categorias existentes com ícone ✏️ (editar inline) e 🗑️ (excluir com confirmação)
- Ao excluir categoria com produtos: toast de erro com mensagem `"Existem produtos nessa categoria. Realize a baixa ou reclassifique-os antes de excluir."`
- Seletor de categoria no formulário de produto passa a buscar de `/products/categories` (lista dinâmica)
- **Colaborador:** `isOwner === false` → esconder aba/seção de categorias completamente

---

### E-02 — Nova baixa de estoque (modal + flags)

**Objetivo:** Remover controles `+/-` inline; substituir por ícone ✏️ que abre modal de baixa.

#### Backend — `product-service`

**Enum `MovementType` atualizado:**
```java
public enum MovementType {
    IN,
    OUT_CONSUMPTION,  // consumo interno (ex: produto usado no serviço)
    OUT_SALE,         // venda ao cliente (impacta dashboard financeiro)
    LOSS,             // perda / descarte
    RETURN            // devolução / entrada por retorno
}
```

**Endpoint de baixa existente** (verificar se já existe `POST /products/{id}/movements`):
- Aceitar `type: MovementType` no request
- Impedir estoque negativo: lançar `BusinessException("Quantidade insuficiente em estoque")` se `product.quantity - requested < 0`
- Para `OUT_SALE`: publicar evento RabbitMQ `product.sold` na exchange `cortaai.product.exchange` → consumidor futuro no `payment-service` para cruzamento de receita

**DTOs:**
```java
record StockMovementRequestDTO(
    @NotNull MovementType type,
    @Min(1) Integer quantity,
    String reason
) {}
```

#### Frontend — `BarberStockPage`

- Remover botões `+` / `-` da linha do produto
- Adicionar ícone ✏️ (lápis) na linha
- Ao clicar: abre modal similar ao modal de pagamento existente com:
  - Campo numérico "Quantidade"
  - Radio/toggle com as flags:
    - **Consumo interno** (`OUT_CONSUMPTION`)
    - **Venda** (`OUT_SALE`)
    - **Perda** (`LOSS`)
    - **Devolução / Entrada** (`RETURN` — aumenta estoque)
  - Campo opcional "Motivo / Observação"
  - Botão "Confirmar baixa"
- Atualizar item na lista localmente após confirmação (sem reload completo)

---

### E-03 — Filtros superiores da Minha Agenda

**Objetivo:** Cards de resumo no topo passam a incluir Encaixe e Pendente; remover abas inferiores.

#### Frontend — `MeusAgendamentosPage`

**Novos stats:**
```js
const stats = useMemo(() => ({
  today:     appointments.filter(a => a.startTime?.slice(0,10) === todayStr).length,
  active:    appointments.filter(a => ['SCHEDULED','CONFIRMED'].includes(a.status)).length,
  completed: appointments.filter(a => a.status === 'COMPLETED').length,
  cancelled: appointments.filter(a => a.status === 'CANCELLED').length,
  walkin:    appointments.filter(a => a.status === 'WALK_IN').length,
  pending:   appointments.filter(a => a.status === 'PAYMENT_PENDING').length,
}), [appointments, todayStr]);
```

**Novos cards clicáveis:**
- Hoje → `setActiveFilter('TODAY')`
- Ativos → `setActiveFilter('ACTIVE')`
- Concluídos → `setActiveFilter('COMPLETED')`
- Cancelados → `setActiveFilter('CANCELLED')`
- Encaixe → `setActiveFilter('WALK_IN')`
- Pendente → `setActiveFilter('PAYMENT_PENDING')`

**Lógica de filtro expandida em `filteredAppointments`:**
```js
case 'TODAY':  return app.startTime?.slice(0,10) === todayStr;
case 'ACTIVE': return ['SCHEDULED','CONFIRMED'].includes(app.status);
// demais já existem por status exato
```

**Remover:** bloco inteiro das abas inferiores (Todos/Agendados/Pendentes/Encaixe/Concluidos/Cancelados).

---

### E-04 — Correções visuais da Minha Agenda

**Objetivo:** Corrigir indicativo de data, aba equipe sem período, duplicidade de "Hoje".

#### Frontend — `MeusAgendamentosPage`

1. **Indicativo de data:** Remover lógica `"Hoje"` / `"Amanhã"` do label da data selecionada. Exibir apenas a data formatada: `new Date(dateFilter).toLocaleDateString('pt-BR', { weekday:'long', day:'2-digit', month:'long' })`.

2. **Aba Equipe — seletores Dia/Semana/Mês:** Quando `agendaView === 'team'`, renderizar os mesmos botões `[Dia | Semana | Mês]` que existem para `agendaView === 'mine'`. Usar estado compartilhado `rangeMode` (renomear `mineRangeMode` → `rangeMode`). Lógica de carregamento de agendamentos da equipe deve aplicar o range conforme o modo selecionado, filtrado por `barbershopId` do owner. **Garantir que nunca trafegue agendamentos de outra barbearia.**

3. **Duplicidade "Hoje":** Remover o label fixo "Hoje" que aparece como indicativo da data quando a data selecionada é hoje. Manter apenas o botão funcional "Hoje" que reposiciona para data atual.

---

### E-05 — Expiração de agendamentos (`EXPIRED`)

**Objetivo:** Agendamentos `PAYMENT_PENDING` que passam 1h do `startTime` expiram automaticamente.

#### Backend — `schedule-service`

**Enum `AppointmentStatus`:**
```java
EXPIRED  // novo valor — pagamento não realizado dentro do prazo
```

**Lógica lazy no `AppointmentService.findById` / `findAll`:**
```java
// Ao retornar agendamentos, verificar e persistir expiração:
if (appointment.getStatus() == PAYMENT_PENDING
    && appointment.getStartTime().plusHours(1).isBefore(LocalDateTime.now())) {
    appointment.setStatus(EXPIRED);
    appointmentRepository.save(appointment);
    // publicar evento para notification-service
    eventPublisher.publish("appointment.expired", new AppointmentExpiredEvent(appointment));
}
```

**Evento RabbitMQ:**
- Exchange: `cortaai.schedule.exchange`
- Routing key: `appointment.expired`
- Queue: `notification-service.appointment.expired.queue` (com DLQ)

**Payload `AppointmentExpiredEvent`:**
```java
record AppointmentExpiredEvent(
    UUID appointmentId,
    UUID customerId,
    UUID barberId,
    String customerEmail,
    String barberEmail,
    LocalDateTime startTime,
    String barbershopName
) {}
```

#### `notification-service`

**Novo listener** `AppointmentExpiredListener`:
- Notifica **cliente**: "Seu agendamento em [barbearia] no dia [data/hora] expirou pois o pagamento não foi realizado."
- Notifica **barbeiro**: "Agendamento de [cliente] em [data/hora] expirou por falta de pagamento."
- Desduplicação via Redis com chave `notif:expired:{appointmentId}`

#### Frontend

- Adicionar `EXPIRED` ao mapa de labels: `'EXPIRED': 'Pagamento expirado'`
- Card de status `EXPIRED` com cor âmbar/laranja distinta de `CANCELLED`

---

### E-06 — Reagendamento pelo cliente

**Objetivo:** Cliente pode reeditar horário/barbeiro em agendamento, mínimo 3h antes, automático.

#### Backend — `schedule-service`

**Novo endpoint:** `PATCH /appointments/{id}/reschedule`

**`RescheduleRequestDTO`:**
```java
record RescheduleRequestDTO(
    @NotNull UUID barberId,        // pode ser o mesmo ou outro com a skill
    @NotNull LocalDateTime newStartTime,
    @NotNull LocalDateTime newEndTime
) {}
```

**Validações:**
- `newStartTime` deve ser ≥ `now + 3h` → `BusinessException("Reagendamento permitido somente com 3h de antecedência")`
- Barbeiro selecionado deve ter a skill do serviço original → validar via Feign para `user-service`
- Novo horário não pode colidir com outro agendamento do barbeiro → consulta no banco
- Apenas o próprio cliente pode reagendar (`customerId` do token = `customerId` do agendamento)

**Evento:**
- Exchange: `cortaai.schedule.exchange`
- Routing key: `appointment.rescheduled`
- Notificação ao barbeiro: "O cliente [nome] reagendou para [nova data/hora]"

#### Frontend — `MeusAgendamentosPage` (cliente)

- Botão "Reagendar" visível apenas para `isCustomer && status in [SCHEDULED, CONFIRMED, PAYMENT_PENDING] && startTime - now > 3h`
- Modal de reagendamento:
  1. Seleção de barbeiro (apenas com a skill do serviço original) → busca via `/barbershops/{id}/barbers`
  2. Seleção de data disponível (calendário)
  3. Seleção de horário disponível → `GET /schedule/availability`
  4. Confirmar → `PATCH /appointments/{id}/reschedule`

---

### E-07 — Gestão completa do time

**Objetivo:** Após convite aceito, owner tem painel completo de gerência do colaborador.

#### Backend — `user-service` / `barbershop-service`

**Nova entidade `BarbershopTeamMember`** (no `barbershop-service`):
```
barbershop_team_members
  id             UUID PK
  barbershop_id  UUID NOT NULL
  barber_id      UUID NOT NULL  (referência ao user-service)
  barber_name    VARCHAR(100)   (snapshot)
  commission_pct DECIMAL(5,2)   -- % de comissão
  work_days      VARCHAR(50)    -- ex: "MON,TUE,WED,THU,FRI"
  work_start     TIME
  work_end       TIME
  joined_at      DATETIME
  removed_at     DATETIME       -- soft-delete para histórico
  UNIQUE(barbershop_id, barber_id, removed_at IS NULL)
```

> ⚠️ **Migration necessária — confirmar antes de executar**

**Endpoints novos:**
- `GET /barbershops/me/team` — lista membros ativos
- `PATCH /barbershops/me/team/{memberId}` — editar comissão, dias/horários
- `DELETE /barbershops/me/team/{memberId}` — remove (soft-delete), verifica agendamentos futuros
  - Se houver agendamentos futuros: retorna `409 ConflictException` com lista de conflitos
  - Frontend exibe conflitos ao owner para decisão (redistribuir ou cancelar)

**`TeamMemberResponseDTO`:**
```java
record TeamMemberResponseDTO(
    UUID memberId, UUID barberId, String barberName, String barberPhotoUrl,
    BigDecimal commissionPct, List<String> workDays, LocalTime workStart, LocalTime workEnd,
    LocalDateTime joinedAt, int totalAppointmentsAllTime
) {}
```

**Histórico:** `BarbershopTeamMember` com `removed_at` preenchido = histórico. Endpoint `GET /barbershops/me/team/history`.

**Relatório de comissão:** novo endpoint `GET /barbershops/me/team/{memberId}/commission-report?from=&to=` — usa dados de `appointments` via Feign para `schedule-service`.

#### Frontend — `BarberTeamPage`

**Após convite aceito, novo painel de gestão:**
- Lista de membros com: foto (ou avatar), nome, dias de trabalho, horário, comissão %
- Ações por membro:
  - ✏️ Editar: modal com campos de comissão, dias e horário
  - ❌ Remover: popup de confirmação → se houver conflitos, exibe lista com opção "Redistribuir" ou "Cancelar agendamento"
- Aba "Histórico" com membros removidos e métricas
- Card de comissão do período (semana/mês) por colaborador

---

### E-08 — Dashboard: restrição owner + ícone olho + export PDF

**Objetivo:** Dados visíveis apenas para owner; botões de texto → ícone olho; export PDF.

#### Frontend — `DashReportPanel` e `BarberDashboardPage`

**Restrição owner:**
```jsx
// No topo de BarberDashboardPage:
if (!isOwner) return <Navigate to="/barberHome" replace />;
```
- Verificar também no `api-gateway` se o endpoint de analytics valida `isOwner` via header `X-User-Role` + `X-User-Id` comparado com `barbershopId`.

**Botões texto → ícone olho (`DashReportPanel`):**
```jsx
// Substituir botões "Ocultar Dashboard" / "Mostrar Dashboard" por:
<button onClick={() => setDashVisible(v => !v)} title={dashVisible ? 'Ocultar dashboard' : 'Mostrar dashboard'}>
  {dashVisible ? <FiEye /> : <FiEyeOff />}
</button>
```
- Posicionar o olhinho no canto superior direito de cada seção (Dashboard e Relatório separadamente).

**Export PDF:**
- Novo botão 📥 "Exportar" em cada painel
- Ao clicar: modal com seleção de período (`de:` / `até:`)
- Após confirmar: gerar PDF com `@react-pdf/renderer` contendo:
  - Header com logo CortaAi + nome da barbearia + período selecionado
  - Dados do painel (tabela e/ou gráfico capturado via `html2canvas`)
  - Rodapé com data de geração

---

### E-09 — Fix piscando / auto-refresh sem flicker

**Objetivo:** Dados atualizam a cada 30s sem re-renderizar o container inteiro (sem "piscar").

#### Estratégia

- **Nunca** setar `setLoading(true)` em refreshes automáticos — `loading` só para o carregamento inicial
- Criar flag `isRefreshing` (boolean separado) que não desmonta o componente
- Usar `startTransition` do React 18 para marcas de atualização como não-urgentes
- Para listas: usar key estável (por `id`) no map — o React faz diff sem desmontar cards
- Auditar todos os `setInterval` no frontend e centralizar em `DashReportPanel.onRefresh`

**Componentes afetados a auditar:** `DashReportPanel`, `NextScheduling`, `MeusAgendamentosPage`, `BarberHomePage`.

---

### E-10 — Fix upload foto de perfil do barbeiro

**Objetivo:** Foto de perfil salva corretamente e persiste após reload.

#### Investigação e correção

**Hipótese principal:** O endpoint `POST /barbers/me/upload-photo` no `user-service` salva a URL no Cloudinary, mas o campo `photoUrl` no banco não está sendo retornado pelo `GET /auth/me` (ou está sendo retornado com URL incorreta).

**Verificar:**
1. `BarberProfilePage.jsx` linha ~383: `uploadBarberProfilePhoto` retorna a nova URL? Confirmar se `barber.photoUrl` é atualizado no estado local após o upload.
2. Endpoint `GET /barbers/me` no `user-service`: confirmar que `photoUrl` está mapeado no `BarberResponseDTO`.
3. Se a URL salva no banco é `http://` e o app está em `https://`, ocorre mixed content block → substituir por URL `https://`.

**Correção esperada:**
```js
const handleUploadProfilePhoto = async (event) => {
    const file = event.target.files[0];
    if (!file) return;
    setUploadingProfilePhoto(true);
    try {
        const response = await uploadBarberProfilePhoto(file);
        // Atualizar estado local imediatamente sem reload:
        setBarber(prev => ({ ...prev, photoUrl: response.photoUrl }));
        toast.success('Foto atualizada com sucesso!');
    } catch (err) {
        toast.error('Erro ao enviar foto. Tente novamente.');
    } finally {
        setUploadingProfilePhoto(false);
    }
};
```

---

### E-11 — Logo CortaAi na navbar (sem texto)

**Arquivo:** `BarberHeader.jsx`

**Alteração:** Remover `<div className={styles.brandText}>` com `<span>CortaAI</span>` e `<span>Painel profissional</span>`. Aumentar o `width` da `brandLogo` para equivaler ao tamanho do texto anterior (aprox. `120px`).

**Verificar:** `CustomerHeader.jsx` deve ter a mesma padronização.

---

### E-12 — Colaborador: ocultar serviços, mostrar apenas habilidades

**Arquivo:** `BarberServicesPage.jsx` e `BarberProfilePage.jsx`

- Onde `isOwner === false`: ocultar completamente a seção de cadastro/edição de serviços
- Exibir apenas o seletor de habilidades (skills) que já existe
- Remover link/aba de navegação para serviços na navbar quando colaborador

---

### E-13 — Foto fixa nos cards + crop arrastável

**Objetivo:** Tamanho fixo nos cards de barbearia/barbeiro; crop antes de salvar.

#### Crop de foto

**Lib:** `react-image-crop` (instalar: `npm install react-image-crop`)

**Componente `CropModal`** (novo em `components/`):
```
CropModal.jsx
CropModal.module.css
```
- Props: `file`, `aspectRatio`, `circularCrop`, `onConfirm(croppedBlob)`, `onCancel`
- Abre ao selecionar qualquer foto (perfil, logo)
- Canvas processa o recorte e retorna `Blob` para envio ao backend

**Uso:** `BarberProfilePage`, `CustomerProfilePage`, `BarberProfilePage` (logo barbearia)

**Cards de barbearia (tamanho fixo):**
- Manter dimensões atuais mockadas (extrair do CSS atual e forçar com `object-fit: cover` + `width/height` fixos)
- Não quebrar o card independente da proporção da imagem enviada

---

### E-14 — Barbeiro sem habilidade: opaco e não clicável

**Arquivo:** `AgendamentoPage.jsx` linha ~528

**Alteração:**
- Remover o texto `"⚠️ Sem serviços"` que quebra o card
- Quando `rawActivities === []` (habilidades carregadas e vazias): adicionar classe CSS `disabled` ao card do barbeiro → `opacity: 0.45`, `pointer-events: none`, `cursor: not-allowed`
- Quando cliente seleciona serviço primeiro e barbeiro não possui a skill: mesma classe `disabled`

---

### E-15 — Substituir texto "Carregando datas inteligentes" por spinner

**Arquivo:** `AgendamentoPage.jsx` linha ~561

```jsx
// Antes:
<p className={Styles.info_text}>Carregando datas inteligentes...</p>

// Depois:
<div className={Styles.spinnerSmall} aria-label="Carregando" />
```

CSS:
```css
.spinnerSmall {
  width: 20px; height: 20px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin: 8px auto;
}
```

---

### E-16 — PWA: install nativo + notificações push

**Objetivo:** Remover popup customizado; triggerar prompt nativo do navegador. Notificações push funcionando.

#### Install PWA

**Remover:** `InstallAppPopup.jsx` e todas as suas importações

**Novo fluxo:**
- Capturar evento `beforeinstallprompt` em `App.jsx` e chamar `.prompt()` diretamente quando o usuário clicar em botão discreto na navbar/header (ícone de instalação `📲`)
- Comportamento: se já instalado (`navigator.standalone` ou `display-mode: standalone`), esconder o botão

#### Notificações push

**Verificação:** `notification-service` já possui `DeviceToken`, `PushPlatform`, e testes com `FirebaseMessaging` → FCM está estruturado.

**Pendência verificada:**
1. Frontend: `PushNotificationPrompt.jsx` existe — verificar se está registrando o service worker e enviando o FCM token para `POST /notifications/device-tokens`
2. Confirmar que `firebase-messaging-sw.js` existe em `public/`
3. Se não existir, criar o service worker com configuração FCM

---

### E-17 — Foto do barbeiro no card de seleção

**Arquivo:** `AgendamentoPage.jsx`

- Onde exibe sigla do barbeiro: substituir por `<img src={barber.photoUrl} />` com fallback para SVG de bigode
- `object-fit: cover` em círculo de tamanho fixo (ex: 48x48px)
- Não quebrar o card em nenhuma hipótese (testar com e sem foto)

**Listagem na `MeusAgendamentosPage`:** verificar se o campo `barberPhotoUrl` é trafegado no `AppointmentResponseDTO`. Se não, adicionar ao DTO e ao mapper.

---

### E-18 — Chat IA (barbeiro/owner)

**Objetivo:** Chat contextualizado com agenda do barbeiro. Gemini Flash 1.5 (primário) + Groq (fallback).

#### Arquitetura

Não criar microsserviço novo — adicionar endpoint no `schedule-service`:

```
POST /schedule/ai/chat
Header: X-User-Id, X-User-Role
Body: { "message": "string" }
```

**Lógica do serviço:**
1. Carregar contexto do barbeiro logado: agendamentos do dia/semana via query interna
2. Montar prompt com contexto (sem dados sensíveis de clientes — apenas nome + horário):
   ```
   Você é um assistente de agenda para barbeiros. 
   Contexto: [lista de agendamentos sem CPF/telefone].
   Pergunta do barbeiro: [message]
   Nunca informe dados pessoais de clientes além do nome.
   ```
3. Chamar Gemini Flash 1.5 via HTTP
4. Fallback: se Gemini retornar erro/rate limit → chamar Groq LLaMA 3.3

**Configuração (application.yml):**
```yaml
ai:
  gemini:
    api-key: ${GEMINI_API_KEY}
    url: https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
  groq:
    api-key: ${GROQ_API_KEY}
    url: https://api.groq.com/openai/v1/chat/completions
```

#### Frontend

**Componente `AiChatWidget`** (novo em `components/`):
```
AiChatWidget.jsx
AiChatWidget.module.css
```

**Desktop:** `position: fixed; bottom: 24px; right: 24px` — botão flutuante que expande para janela de chat
**Mobile:** ícone na BarberNavbar (somente para barbeiro/owner logado)
**Cliente:** não vê o chat

---

### E-19 — Geolocalização na home do cliente

**Arquivo:** `HomePage.jsx` e serviço de barbearias

**Fluxo:**
1. Botão "📍 Perto de mim" visível na home
2. Ao clicar: `navigator.geolocation.getCurrentPosition(success, deny)`
3. Sucesso: enviar `lat, lng` para `GET /barbershops?lat={}&lng={}&orderBy=distance`
4. Recusa: manter estado atual, não pedir novamente até próximo clique
5. Backend `barbershop-service`: adicionar parâmetro opcional de ordenação por distância usando fórmula Haversine na query

---

### E-20 — Link Maps + página de detalhes da barbearia

**Link Maps:**
- No card da barbearia e na página de detalhes: endereço clicável → `window.open('https://maps.google.com?q=' + encodeURIComponent(address), '_blank')`
- Ícone 📍 ao lado do endereço

**Página de detalhes `/barbearia/:id`:**
- Header com `bannerUrl` da barbearia
- Logo + nome + avaliação
- Endereço com link Maps
- Lista de serviços e preços
- Lista de barbeiros (com foto e skills)
- Botão "Agendar"
- Esta página é pública (cliente não logado pode visualizar; só agenda se logado)

---

### E-21 — Indicativo de barbearia ativa na navbar

**Arquivo:** `BarberHeader.jsx` (e `BarberNavbar.jsx`)

- Ao clicar no avatar/sigla que expande o menu (perfil/sair): adicionar no topo do dropdown o nome da barbearia ativa
- Estilo: texto pequeno, cor secundária, não clicável — apenas informativo

---

### E-22 — Indisponibilidade avulsa do barbeiro

**Objetivo:** Barbeiro seleciona datas/períodos que ficará indisponível além dos dias fixos do perfil.

**Observação:** `BarberBlock` já existe no `schedule-service` com `barberId`, `startTime`, `endTime`, `reason`.

#### Backend — `schedule-service`

**Novos endpoints:**
- `GET /barber-blocks/me` — lista bloqueios do barbeiro logado
- `POST /barber-blocks/me` — cria bloqueio
  ```java
  record BarberBlockRequestDTO(
      @NotNull LocalDateTime startTime,
      @NotNull LocalDateTime endTime,
      String reason
  ) {}
  ```
- `DELETE /barber-blocks/me/{blockId}` — remove bloqueio

**Verificação de disponibilidade:** endpoint de `GET /schedule/availability` já deve consultar `barber_blocks` — confirmar e adicionar se não estiver.

#### Frontend — `MeusAgendamentosPage`

- Botão **"Bloquear período"** (ícone 🚫 ou calendário com X) no header da página, visível apenas para barbeiro (`!isCustomer`)
- Modal com:
  - Calendário para seleção de data de início
  - Calendário para seleção de data de fim
  - Campo "Motivo" (opcional)
  - Lista de bloqueios ativos com botão para remover
- Bloqueio criado impede o cliente de selecionar aquele horário/data na jornada de agendamento

---

### E-23 — Export PDF de relatórios

**Objetivo:** Botão de export em cada painel de relatório com seleção de período.

**Lib:** `@react-pdf/renderer` (instalar: `npm install @react-pdf/renderer`)

**Componente `ExportReportModal`** (novo em `components/Dashboard/`):
- Modal com: data início, data fim, botão "Gerar PDF"
- PDF gerado contém:
  - Header: logo CortaAi (PNG), nome da barbearia, período
  - Conteúdo: tabela de dados do relatório selecionado
  - Rodapé: "Gerado em [data] — CortaAi"
  - Paleta de cores da marca

**Integração:** `DashReportPanel` recebe prop `onExport(from, to)` que chama a lógica de geração.

---

### E-24 — BarberHome: auditoria

**Arquivo:** `BarberHomePage.jsx`

**Checar:**
- `Invoicing`, `NextScheduling`, `ServicesHomeBarber`, `ActionsBarber` — todos os sub-componentes carregam com estado correto?
- `isOwner` sincronizado do servidor (já existe a lógica, confirmar que está funcionando)
- Colaborador: `isOwner === false` → ocultar `ActionsBarber` que expõe ações exclusivas de owner
- `showInsights` (feature flag `VITE_ENABLE_BARBER_INSIGHTS`) — documentar estado atual

---

## 4. Views/Queries de Analytics

Todos os dashboards abaixo são provenientes de **views/queries SQL** mapeadas com `@Immutable` no JPA:

| View | Serviço | Usado no dashboard |
|---|---|---|
| `v_stock_health_alert` | `product-service` | Alerta de estoque mínimo |
| `v_barber_financial_performance` | `payment-service` | Performance financeira por barbeiro |
| `v_customer_acquisition` | `user-service` | Aquisição de novos clientes |
| `v_customer_retention` | `user-service` | Retenção de clientes |
| `v_agenda_thermometer` | `schedule-service` | Termômetro de agenda (ociosidade/pico) |
| `v_barber_skill_matrix` | `schedule-service` | Matriz de habilidades por barbeiro |

> ⚠️ **Bug confirmado:** Os endpoints que retornam dados dessas views não validam se o `barberId` logado é `isOwner`. A correção (E-08) deve adicionar verificação de `isOwner` tanto no frontend quanto nos controllers dos respectivos serviços.

**Views que precisam de DDL no banco para existir** (não estão no `init.sql` atual — verificar se existem no banco de produção/dev):
- Executar `SHOW FULL TABLES WHERE Table_type = 'VIEW'` no banco de cada serviço para confirmar.

---

## 5. Mapa de impacto por microsserviço

| Serviço | Épicos que o impactam | Tipo de mudança |
|---|---|---|
| `product-service` | E-01, E-02 | Nova entidade, novo enum, novos endpoints, migration |
| `schedule-service` | E-05, E-06, E-07, E-18, E-22 | Novo status enum, novos endpoints, lógica lazy, integração IA |
| `notification-service` | E-05, E-06 | Novo listener, novo template de e-mail/push |
| `barbershop-service` | E-07, E-19, E-20 | Nova entidade team, ordenação por distância |
| `user-service` | E-10, E-17 | Fix foto, campo `photoUrl` no DTO |
| `payment-service` | E-08 | Restrição de acesso a analytics por owner |
| Frontend (global) | Todos | Múltiplos componentes e páginas |

---

## 6. Ordem de Execução

Execução por sprint de 2 semanas. Ordenada por dependência e risco.

### Sprint 1 — Fundação e bugs críticos
1. **E-10** Fix foto de perfil (bug bloqueador para E-17)
2. **E-11** Logo navbar (XS, rápido)
3. **E-15** Spinner "datas inteligentes" (XS)
4. **E-09** Fix piscando / flicker
5. **E-14** Barbeiro opaco sem habilidade
6. **E-03** Filtros superiores + remover abas inferiores (Minha Agenda)
7. **E-04** Correções visuais Minha Agenda

### Sprint 2 — Estoque e agenda
1. **E-01** Categorias dinâmicas (backend + frontend)
2. **E-02** Nova baixa de estoque (modal + flags)
3. **E-05** Expiração `EXPIRED` + notificação
4. **E-22** Indisponibilidade avulsa (aproveita BarberBlock existente)

### Sprint 3 — Funcionalidades de cliente e equipe
1. **E-06** Reagendamento pelo cliente
2. **E-07** Gestão completa do time
3. **E-12** Colaborador: ocultar serviços
4. **E-17** Foto barbeiro no card de seleção
5. **E-13** Crop de foto + tamanho fixo cards

### Sprint 4 — Dashboard, export e PWA
1. **E-08** Dashboard owner-only + ícone olho + export
2. **E-23** Export PDF relatórios
3. **E-16** PWA install nativo + push notifications
4. **E-24** BarberHome auditoria

### Sprint 5 — Novas features e IA
1. **E-19** Geolocalização home cliente
2. **E-20** Link Maps + página detalhes barbearia
3. **E-21** Indicativo barbearia ativa
4. **E-18** Chat IA (Gemini + Groq)

---

## 7. Pendências

| # | Pendência | Decisão necessária |
|---|---|---|
| P-01 | Migration `ProductCategory` enum → FK | **Confirmar antes de executar** — necessário script de conversão dos dados existentes |
| P-02 | Migration `BarbershopTeamMember` | **Confirmar antes de executar** |
| P-03 | Confirmar existência das views SQL no banco dev/prod | Executar `SHOW FULL TABLES WHERE Table_type = 'VIEW'` nos bancos |
| P-04 | Chave de API Gemini (`GEMINI_API_KEY`) | Criar conta Google AI Studio e adicionar ao `.env` |
| P-05 | Chave de API Groq (`GROQ_API_KEY`) | Criar conta groq.com e adicionar ao `.env` |
| P-06 | `firebase-messaging-sw.js` em `public/` | Verificar existência; criar se não existir |
| P-07 | Página de detalhes `/barbearia/:id` — confirmar se banner deve aparecer ou se owner pode remover campo do perfil | **Decidido:** remover upload de banner do perfil; banner aparece apenas na página de detalhes |
| P-08 | Comissão de colaborador impacta relatório financeiro do `payment-service`? | Definir se cruza com dados de pagamento ou fica isolado no `barbershop-service` |
