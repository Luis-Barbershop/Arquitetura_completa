# CortaAi — Resumo de Solicitações e Decisões · Maio 2026

> Documento de referência rápida.  
> **Implementação:** `CICLO1_SDD.md` (Sprints 1–4) e `CICLO2_SDD.md` (Sprints 5–7).

---

## Como ler este documento

Cada item segue o padrão:

> **Você solicitou:** o que foi pedido  
> **Decisão:** como foi resolvido e por quê

---

## Estoque (product-service)

### Categorias dinâmicas de estoque (E-01)

**Você solicitou:** Substituir o enum `ProductCategory` (fixo) por categorias que o owner possa criar, editar e excluir conforme a necessidade de cada barbearia.

**Decisão:**
- `ProductCategory` (enum) → entidade `Category(id, name, barbershopId)` no banco, completamente multi-tenant.
- CRUD em `/products/categories` acessível somente por `isOwner=true`.
- Exclusão bloqueada com `409 ConflictException` se houver produto ativo na categoria.
- Colaborador não enxerga nem acessa a seção de categorias.
- Migration necessária: nova tabela `categories` + FK em `products.category_id` (confirmar antes de executar).

---

### Modal de baixa de estoque com flags (E-02)

**Você solicitou:** Remover os botões `+` / `-` inline na linha do produto e substituir por um modal de baixa com tipificação do movimento.

**Decisão:**
- Botões removidos; ícone ✏️ abre modal.
- `MovementType` ganha 5 valores semânticos: `IN`, `OUT_CONSUMPTION`, `OUT_SALE`, `LOSS`, `RETURN`.
- `OUT_SALE` publica evento `product.sold` no RabbitMQ (`cortaai.product.exchange`) para cruzamento futuro com o `payment-service`.
- Estoque negativo bloqueado via `BusinessException`.
- Campo "Motivo / Observação" opcional no modal.

---

## Agenda (schedule-service)

### Remover abas inferiores + adicionar Encaixe/Pendente (E-03)

**Você solicitou:** Remover as abas inferiores da Minha Agenda (Todos/Agendados/Pendentes/Encaixe/Concluidos/Cancelados) e trazer Encaixe e Pendente como cards de resumo no topo.

**Decisão:**
- Bloco de abas inferiores removido completamente.
- 6 cards clicáveis no topo: Hoje, Ativos, Concluídos, Cancelados, **Encaixe** e **Pendente**.
- Filtro aplicado por clique no card — estado local React, sem chamada nova de API.

---

### Remover labels "Hoje/Amanhã" das datas (E-04)

**Você solicitou:** A data selecionada no filtro exibe "Hoje" ou "Amanhã" de forma confusa e duplicada.

**Decisão:**
- Label substituído por data completa formatada: `"segunda-feira, 08 de maio"`.
- Botão "Hoje" (navegação) mantido; apenas o label do filtro é alterado.

---

### Status EXPIRED + reagendamento automático (E-06 e parte de E-05)

**Você solicitou:** Agendamentos `PAYMENT_PENDING` que passam 1h do horário marcado devem expirar. Cliente deve conseguir reagendar com mínimo de 3h de antecedência, sem precisar de aprovação do barbeiro.

**Decisão:**
- `EXPIRED` é **lazy** — calculado na leitura (não job `@Scheduled`), mas **persiste** no banco ao detectar.
- Critério: `status == PAYMENT_PENDING && startTime + 1h < now`.
- Evento `appointment.expired` publicado → `notification-service` notifica cliente e barbeiro.
- Reagendamento: `PATCH /appointments/{id}/reschedule` — somente o próprio cliente, somente em agendamentos que ainda não expiraram, mínimo 3h antes.
- Barbeiro pode ser trocado desde que tenha a skill do serviço original (validação via Feign → `user-service`).
- `RescheduleAppointmentDTO` ganha `barberId` nullable (backward compatible com contratos existentes).

---

### Agenda equipe — seletores Dia/Semana/Mês + grid responsivo (E-05)

**Você solicitou:** A aba "Equipe" da agenda não tem os seletores de período (Dia/Semana/Mês) que existem na aba "Minha Agenda". O grid de colunas não se adapta para N barbeiros.

**Decisão:**
- Estado `rangeMode` unificado entre as duas abas (renomear `mineRangeMode`).
- Grid: CSS `grid-template-columns: repeat(auto-fit, minmax(180px, 1fr))` — sem scroll lateral, expande automaticamente para o número de barbeiros.
- Nunca trafegar agendamentos de outra barbearia — `barbershopId` sempre extraído do header `X-User-Id`.

---

## Gestão do Time (barbershop-service)

### Time management completo (E-07)

**Você solicitou:** Após o convite aceito, o owner precisa gerenciar o colaborador: editar comissão, definir dias/horários de trabalho, ver histórico e remover com tratamento de conflitos.

**Decisão:**
- Nova tabela `barber_commission_rules(barbershop_id, barber_id, activity_id, percentage)` para comissão por serviço (mais granular que % global).
- `BarbershopJoinRequest` com `status=ACCEPTED` é a fonte de verdade do vínculo — sem nova entidade de membro.
- Remoção: se houver agendamentos futuros, retorna `409` com lista de conflitos; owner decide redistribuir (Feign → `schedule-service`) ou cancelar (evento RabbitMQ `barber.removed`).
- Histórico via soft-delete (`removed_at`).
- Relatório de comissão: `GET /barbershops/me/team/{id}/commission-report?from=&to=`.
- Feign ao `user-service` para buscar foto e nome do barbeiro na montagem do `TeamMemberResponseDTO`.

---

## Dashboard e Relatórios (barbershop-service + frontend)

### Dashboard filtrar por barbershopId + ícone olho (E-08)

**Você solicitou:** O dashboard exibe dados de todas as barbearias misturadas. Botões "Ocultar/Mostrar Dashboard" precisam virar ícone de olho. Owner deve ser o único a ver o dashboard.

**Decisão:**
- `barbershopId` extraído de `X-User-Id` via lookup interno — **nunca** aceito como query param (ADR-08).
- `isOwner !== true` → redirecionar para `/barberHome`.
- Botões de texto → `<FiEye />` / `<FiEyeOff />` (Feather Icons) no canto superior direito de cada painel.
- Estado de visibilidade por sessão React (não persiste em `localStorage`).

---

## Frontend — Fixes visuais (Sprints 1–2)

### Anti-flicker em refresh automático (E-09)

**Você solicitou:** A tela "pisca" (flicker) durante o refresh automático de dados.

**Decisão:** Separar estado de "loading inicial" (skeleton/spinner) de "refresh em background" (sem remover conteúdo atual). Usar flag `isFirstLoad` para controlar exibição do skeleton.

---

### Fix upload foto de perfil do barbeiro (E-10)

**Você solicitou:** Upload de foto de perfil do barbeiro está com bug — não salva ou não exibe corretamente.

**Decisão:**
- Verificar e corrigir a cadeia: Cloudinary upload → URL retornada → `PATCH /users/barbers/me` com `imageUrl` → `user-service` persiste → resposta retorna `imageUrl` atualizado → frontend atualiza `localStorage.userName` e estado local.
- Validação de extensão e tamanho no frontend antes do upload.

---

### Logo navbar sem texto (E-11)

**Você solicitou:** A navbar exibe logo + texto "CortaAi". Remover o texto, manter só o ícone/logo.

**Decisão:** Remover o `<span>` ou `<p>` com o texto da navbar. Manter apenas o elemento `<img>` ou SVG do logo.

---

### Colaborador: ocultar aba Serviços, exibir só Habilidades (E-12)

**Você solicitou:** Colaborador não é dono e não deve ver nem gerenciar serviços da barbearia. Deve ver apenas suas habilidades.

**Decisão:** `isOwner === false` → esconder completamente a aba/seção "Serviços". Manter aba "Habilidades" com os serviços que o barbeiro selecionou como skills. Nenhuma chamada de API bloqueada no backend — controle puramente de UI.

---

### Tamanho fixo de fotos + crop 1:1 (E-14)

**Você solicitou:** Fotos de perfil e cards têm tamanhos inconsistentes. Precisa de crop padronizado.

**Decisão:**
- Instalar `react-image-crop` (mais leve que `cropperjs`, sem dependências nativas).
- Novo componente `CropImageModal` com `aspect={1}` (1:1) para fotos de perfil.
- Mesmo modal reutilizado com `aspect={16/9}` no upload de banner (E-24).
- Imagens nos cards com `object-fit: cover` e dimensões fixas via CSS Module.

---

### PWA — install nativo + push sem popup intermediário (E-16)

**Você solicitou:** O fluxo PWA tem popup intermediário antes do prompt nativo. Notificações push têm etapas desnecessárias.

**Decisão:**
- Remover componentes `InstallAppPopup` e `PushNotificationPrompt` completamente (ADR-09).
- Chamar `promptEvent.prompt()` diretamente no clique de um botão simples.
- Solicitar permissão de notificação diretamente via `Notification.requestPermission()`.
- Pop-up = nativo do browser — não criar modal customizado intermediário.

---

### Indicativo de barbearia ativa na navbar (E-21)

**Você solicitou:** Barbeiro que trabalha em mais de uma barbearia (ou está em processo de associação) não consegue identificar qual barbearia está "ativa" na sessão.

**Decisão:** Exibir nome ou logo da barbearia ativa no header da navbar para barbeiros logados. Valor lido de `localStorage.barbershopId` via Feign para buscar o nome. Atualizado ao trocar de barbearia ativa.

---

### UI de indisponibilidade do barbeiro (E-22)

**Você solicitou:** O backend `BarberBlock` já existe no `schedule-service`, mas a UI de cadastro de indisponibilidade ainda não foi implementada.

**Decisão:**
- Tela em dois fluxos separados (ADR-11):
  1. **Horas avulsas:** selecionar data + intervalo de horas no dia.
  2. **Dias avulsos / Período:** selecionar data inicial e final (dias inteiros bloqueados).
- Ambos os fluxos na mesma tela, em seções colapsáveis.
- Calendário visual mostra os bloqueios já cadastrados (readonly).
- Endpoints: `POST /schedule/blocks`, `GET /schedule/blocks`, `DELETE /schedule/blocks/{id}`.

---

### Spinner substituindo texto de carregamento (E-15)

**Você solicitou:** O texto "Carregando datas inteligentes..." aparece enquanto carrega e é verboso.

**Decisão:** Substituir por um componente `<Spinner />` ou `<LoadingDots />` existente no projeto. Zero texto. Duração máxima com fallback de mensagem de erro se ultrapassar 10s.

---

### Foto do barbeiro nos cards de seleção (E-17)

**Você solicitou:** No fluxo de agendamento do cliente, os cards de seleção de barbeiro não exibem a foto.

**Decisão:**
- `BarberPublicDTO` no `barbershop-service` deve incluir `imageUrl`.
- Cadeia: `Barber.imageUrl` (user-service) → `toUserInfoDTO` → `UserInfoDTO.imageUrl` → `BarberPublicDTO.imageUrl` (via Feign).
- Frontend: `<img src={barber.imageUrl || defaultAvatar} />` com fallback para avatar genérico.

---

### Barbeiro sem skill = opaco no card (E-23)

**Você solicitou:** No fluxo de agendamento, barbeiros que não têm a skill do serviço selecionado aparecem clicáveis, causando erro.

**Decisão:** Card de barbeiro sem a skill do serviço: `opacity: 0.4`, `pointer-events: none`, tooltip `"Não realiza este serviço"`. Filtro aplicado no frontend com base no array `barber.skills[]` vs `serviceId` selecionado.

---

## Ciclo 2 — Features avançadas (Sprints 5–7)

### Export PDF de relatórios (E-13)

**Você solicitou:** Exportar relatórios do dashboard como PDF estilizado com identidade visual do CortaAi.

**Decisão:**
- PDF 100% client-side com `@react-pdf/renderer` — sem endpoint de backend.
- Modal com seleção de período (`de:` / `até:`).
- PDF gerado com: logo CortaAi, nome da barbearia, período e dados filtrados.
- Botão "📄 Exportar PDF" no `BarberDashboardPage`.

---

### Chat IA "gustave" — Gemini + Groq fallback (E-18)

**Você solicitou:** Um assistente de IA para barbeiros/owners analisarem a agenda e receita com linguagem natural.

**Decisão:**
- Assistente se chama **gustave** (sem maiúscula).
- Endpoint `POST /api/schedule/ai/chat` no `schedule-service`.
- Stack: Gemini 1.5 Flash → Groq LLaMA 3.3 → mensagem de fallback amigável (ADR-13).
- Dois modos: **Previsão** (agenda futura) e **Consolidado** (atendimentos realizados nos últimos 30 dias).
- **Colaborador:** vê apenas própria agenda e receita. **Owner:** vê equipe completa + total barbearia.
- Componente flutuante `GustaveChat.jsx` — visível apenas para `ROLE_BARBER`.
- Variáveis necessárias: `GEMINI_API_KEY` e `GROQ_API_KEY`.

---

### Geolocalização — filtro por distância (E-19)

**Você solicitou:** Clientes devem poder filtrar barbearias próximas usando a geolocalização do dispositivo.

**Decisão:**
- `GET /api/barbershops?lat=&lng=&radiusKm=10` com query Haversine nativa (MySQL) no `barbershop-service`.
- `BarbershopSummaryDTO` ganha campo `distanceKm` (null quando filtro não aplicado).
- Migration: `ALTER TABLE barbershops ADD COLUMN latitude DOUBLE NULL; ADD COLUMN longitude DOUBLE NULL`.
- Frontend: se usuário negar permissão → manter filtro anterior sem pedir novamente; posição salva em `sessionStorage` (não `localStorage`) (ADR-14).

---

### Mini mapa OSM + link Google Maps (E-20)

**Você solicitou:** Exibir localização da barbearia em um mapa na página de detalhes, com link para abrir no Google Maps.

**Decisão:**
- `react-leaflet` + OpenStreetMap (sem API key, gratuito) (ADR-12).
- Geocoding: Nominatim chamado no **frontend** ao salvar endereço — backend apenas persiste `latitude` e `longitude`.
- Mapa com `scrollWheelZoom={false}` e `dragging={false}` (não interfere com scroll da página).
- Link externo: `https://maps.google.com/?q={lat},{lng}` em nova aba.
- Sem lat/lng → mapa não renderiza (graceful degradation).

---

### Upload banner somente na página de detalhes (E-24)

**Você solicitou:** O upload de banner está no perfil do barbeiro (lugar errado). Deve aparecer apenas na página pública de detalhes da barbearia.

**Decisão:**
- Remover upload de banner de `BarberProfilePage.jsx`.
- Adicionar botão "✏️ Editar banner" na página `/barbearia/:id`, visível apenas para o owner autenticado (`isOwner === true && shop.ownerFirebaseUid === userId`).
- Crop 16:9 via `CropImageModal` (mesmo componente do E-14, `aspect={16/9}`).

---

## Tabela de dependências entre épicos

| Épico | Depende de |
|---|---|
| E-23 (barbeiro opaco) | Nenhuma — UI pura |
| E-14 (crop) | Nenhuma — componente novo |
| E-24 (banner) | E-14 (CropImageModal já implementado) |
| E-06 (reagendamento) | E-05 (status EXPIRED já no enum) |
| E-20 (mini mapa) | Migration lat/lng (compartilhada com E-19) |
| E-18 (gustave) | Nenhuma de código — necessita `GEMINI_API_KEY` |
| E-07 (time management) | ADR-06 (tabela `barber_commission_rules`) |

---

## Decisões que afetam múltiplos serviços

| Decisão | Impacto |
|---|---|
| `barbershopId` nunca como query param — sempre extraído de `X-User-Id` | Todos os endpoints de analytics e time |
| `EXPIRED` não é job — é lazy na leitura | `schedule-service` (leitura de agendamentos) |
| Geocoding no frontend (Nominatim) | `barbershop-service` só persiste coordenadas, não chama APIs externas |
| PDF client-side | `payment-service` e `barbershop-service` não ganham novos endpoints para esta feature |
| Chat IA no `schedule-service` (não serviço separado) | Sem novo microsserviço — endpoint no serviço que tem os dados de agenda |
