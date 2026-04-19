# Relatório Refinado — UI/UX, Qualidade de Código, Segurança de Dados e PWA (sem regressão)

> **Projeto:** CortaAi  
> **Branch base:** `feature/migracao-microservicos`  
> **Data:** 19/04/2026  
> **Princípio inegociável:** **não quebrar o que funciona hoje**

---

## 1. Confirmação de fuso horário (status atual)

No `schedule-service`, o cálculo de disponibilidade já foi ajustado para timezone configurável com default Brasil:

- `app.timezone: ${APP_TIMEZONE:America/Sao_Paulo}` em `application.yml`
- `getAvailability(...)` usa `LocalDateTime.now(ZoneId.of(appTimezone))`

✅ **Conclusão:** o fuso padrão agora é Brasil (`America/Sao_Paulo`) caso nenhuma variável de ambiente seja definida.

---

## 2. Diagnóstico consolidado do frontend (estado atual)

### 2.1 Arquitetura visual e técnica

- Stack atual: React + Vite + CSS Modules + React Router + Axios + Toastify.
- Design atual é majoritariamente dark, mas com tokens visuais ainda espalhados em módulos.
- Responsividade existe em vários componentes, porém sem um sistema unificado de breakpoints/layout.
- Microinterações existem, mas sem política de animação/acessibilidade global (`prefers-reduced-motion`).

### 2.2 Pontos fortes

- Fluxo de agendamento já moderno (cards de data + slots de disponibilidade).
- Boa separação por páginas e componentes reutilizáveis.
- Uso de CSS Modules evita vazamento de estilo global.

### 2.3 Pontos críticos

1. **Duplicação de lógica de navegação e guardas de autenticação** em múltiplas páginas.
2. **Valores de estilo hardcoded** (cores, espaçamentos, bordas) em muitos módulos.
3. **Ausência de estratégia global de tema** (dark/light por tokens).
4. **Sem infraestrutura PWA** (manifest, service worker, install flow).

---

## 3. Melhorias de código para reduzir duplicação (sem remover o que existe hoje)

> Objetivo: reduzir duplicação **sem refatoração destrutiva**.

### 3.1 Estratégia de migração segura (Strangler no frontend)

Em vez de apagar o legado agora, criar camadas novas e migrar gradualmente:

1. **Criar utilitários centrais** (novo)  
2. **Apontar páginas novas para utilitários** (novo)  
3. **Manter páginas antigas compatíveis** (sem remoção imediata)  
4. **Medir uso e regressões**  
5. **Só depois descontinuar duplicatas**

### 3.2 Alvos concretos de deduplicação

#### A) Navegação por perfil/aba

**Problema:** regras de `navigate(...)` repetidas em várias páginas (`BarberHomePage`, `MeusAgendamentosPage`, `AgendaBarbeariaPage`, etc.).

**Melhoria proposta (incremental):**
- Criar `frontend/src/services/navigationService.js` com:
  - `goToBarberTab(tab, navigate)`
  - `goToCustomerTab(tab, navigate)`

**Benefício:** um único ponto de mudança para rotas/tab mapping.

---

#### B) Guardas de autenticação/autorização

**Problema:** verificações de token/role repetidas por página.

**Melhoria proposta:**
- Criar hook `useAuthGuard({ allowCustomer, allowBarber, allowOwner })`
- Usar em páginas novas e gradualmente nas antigas.

**Benefício:** evita bugs de comportamento diferente por tela.

---

#### C) Chamadas de disponibilidade/agendamento

**Problema:** cada página implementa parte da lógica de slots.

**Melhoria proposta:**
- Criar `appointmentAvailabilityService.js` com:
  - `fetchAvailability({ barberId, date, duration })`
  - helpers de parsing `TimeSlotDTO -> HH:mm`

**Benefício:** corrige bug uma vez, vale para todas as telas.

---

#### D) Sistema de tokens visuais

**Problema:** duplicação de estilos por componente.

**Melhoria proposta:**
- Criar `frontend/src/styles/tokens.css` (cores, spacing, radius, shadow)
- Gradualmente migrar CSS Modules para consumir tokens.

**Benefício:** consistência visual + manutenção simples.

---

## 4. Refinamento UI/UX (sem ruptura)

### 4.1 Animações recomendadas (baixo risco)

1. **Card enter animation** (fade + translateY 8px, 160–220ms)
2. **Botão press feedback** (`transform: scale(0.98)`) 
3. **Accordion transitions** suaves em blocos de horários
4. **Page transition leve** (opacidade 120–180ms)

### 4.2 Acessibilidade de movimento (obrigatório)

Adicionar política global:

- `@media (prefers-reduced-motion: reduce)` desabilitando animações não essenciais.

### 4.3 Layout mobile/desktop

#### Mobile
- CTA primário sticky em fluxos longos
- Espaço de toque mínimo 44px
- Priorizar conteúdo principal acima de elementos secundários

#### Desktop
- Layout em duas colunas para páginas com resumo (ex.: agendamento, dashboard)
- Densidade de informação controlada por grid consistente

### 4.4 Tema (dark/light)

- Manter dark como default atual
- Introduzir `data-theme` no `html` com tokens
- Adicionar switch gradual por página

**Sem quebra:** páginas não migradas continuam funcionando com tokens fallback.

### 4.5 Diretrizes de visual premium (look & feel "prime")

Para elevar percepção de qualidade sem quebrar fluxo atual:

1. **Hierarquia tipográfica forte**
  - Definir escala única (ex.: `display`, `h1`, `h2`, `body`, `caption`) com line-height consistente.
  - Aplicar pesos por contexto (informação crítica com maior contraste visual).

2. **Superfícies com profundidade controlada**
  - Variantes de card (`flat`, `elevated`, `glass-subtle`) com uso restrito por contexto.
  - Evitar “sombra em excesso”; usar elevação apenas para ações prioritárias.

3. **Paleta semântica premium**
  - Separar tokens de marca (`brand.*`) de tokens funcionais (`success.*`, `warning.*`, `danger.*`).
  - Garantir consistência de estados de foco/hover/pressed em botões e inputs.

4. **Consistência de cantos e espaçamento**
  - Radius padronizado por nível (`sm`, `md`, `lg`, `xl`) para evitar aspecto “colcha de retalhos”.
  - Escala de spacing única (4/8/12/16/24/32) para ritmo visual limpo.

### 4.6 Experiência premium nos fluxos críticos

#### A) Agendamento (cliente e barbeiro)
- Exibir **resumo fixo** da seleção (barbeiro, serviço, duração, preço, horário) durante o fluxo.
- Destacar o “próximo melhor horário disponível” quando slot escolhido ficar indisponível.
- Skeleton de carregamento para slots (evita “salto” visual e sensação de travamento).

#### B) Área do barbeiro (gestão)
- Dashboard com cartões de KPI em formato padrão (título, valor, variação, período).
- Ações rápidas com prioridade visual: `Novo agendamento`, `Confirmar`, `Remarcar`, `Registrar pagamento`.
- Estados vazios orientados por ação (empty state com CTA claro, não apenas texto).

#### C) Mercado Pago / financeiro
- Status de conexão com semântica clara: `Conectado`, `Atenção`, `Desconectado`.
- Exibir “última sincronização” e feedback imediato após ações sensíveis (ex.: desconectar).
- Diferenciar visualmente sucesso operacional vs. alerta de negócio.

### 4.7 Microinterações e motion system (padrão único)

Definir um mini motion system para todo o frontend:

- **Duração:** rápida (120ms), padrão (180ms), lenta (240ms).
- **Easing:** `ease-out` para entrada e `ease-in-out` para transições de estado.
- **Estados animáveis permitidos:** opacidade, transform; evitar animar propriedades custosas (layout/reflow).
- **Feedback de ação:** botão com estado `loading` e `success` curto para confirmar execução.

### 4.8 Acessibilidade visual e usabilidade (nível produto)

1. Contraste mínimo AA em componentes críticos (texto, botão, badges de status).
2. Foco visível universal (`:focus-visible`) com padrão único de borda/sombra.
3. Navegação por teclado em menus, dropdowns e lista de horários.
4. Mensagens de erro orientadas para ação (“como corrigir”).
5. Tamanho mínimo de alvo de toque (44x44) em elementos móveis.

### 4.9 Performance perceptiva para aparência premium

Mesmo sem alterar backend, o front pode parecer muito mais rápido:

- Prefetch de rotas mais acessadas pós-login.
- Lazy loading de blocos secundários (gráficos pesados, listas extensas).
- Skeleton e placeholders sem shimmer agressivo.
- Evitar layout shift (reservar altura para cards/listas antes dos dados chegarem).

### 4.10 Biblioteca de componentes internos (incremental)

Criar, em paralelo ao legado, um kit interno de componentes base:

- `Button`, `Input`, `Select`, `Modal`, `Card`, `Badge`, `Tabs`, `Toast`, `EmptyState`, `Skeleton`.
- Cada componente com variantes (`size`, `tone`, `state`) e documentação de uso.
- Estratégia de adoção gradual por páginas novas/refatoradas.

**Benefício:** acelera entrega e mantém visual premium consistente ao longo da evolução.

### 4.11 Métricas de UX para validar evolução

Para provar ganho de qualidade (não só percepção):

- Tempo até primeira ação útil (ex.: selecionar horário).
- Taxa de abandono por etapa em agendamento.
- Taxa de erro por formulário (antes/depois das melhorias).
- Cliques para completar fluxo crítico.
- NPS interno de usabilidade com barbeiros (rodadas curtas).

### 4.12 Backlog recomendado de front “prime” (rápido)

#### P1 (alto impacto, baixo risco)
- Design tokens (cor/tipografia/spacing/radius).
- Estados de loading/skeleton padronizados.
- Foco acessível + contraste em componentes principais.
- Padronização de cards e CTAs nos fluxos críticos.

#### P2 (alto impacto, médio risco)
- Motion system global e transições de página leves.
- Component library interna (base).
- Tema dark/light por `data-theme`.

#### P3 (médio impacto, médio/alto risco)
- Personalização avançada por perfil de usuário.
- Evolução visual com gráficos ricos e interações avançadas.
- PWA com estratégia offline mais robusta para jornadas não transacionais.

### 4.13 Sistema de layout premium (estrutura visual)

Para deixar o produto com aparência de plataforma madura, definir um layout system explícito:

1. **Grid responsivo unificado**
  - Mobile: 4 colunas, Tablet: 8, Desktop: 12.
  - Larguras máximas por contexto (`content`, `dashboard`, `full`).
  - Gutter padronizado para evitar telas “quebradas” visualmente.

2. **Ritmo vertical previsível**
  - Espaçamento entre seções por escala fixa (ex.: 24/32/40).
  - Título + subtítulo + bloco de ação com distância constante.

3. **Padrão de alinhamento de conteúdo**
  - Cabeçalhos de páginas com mesma anatomia: título, contexto, ações.
  - Cards em alturas coerentes para evitar sensação de desorganização.

4. **Containers semânticos por uso**
  - `container-default` (páginas comuns),
  - `container-form` (fluxos com formulário),
  - `container-dashboard` (KPIs + gráficos).

### 4.14 Direção de arte para marca (visual moderno e lindo)

Transformar o “dark atual” em identidade visual premium:

- **Cor de marca protagonista** (uso intencional em CTAs e destaques, sem poluição).
- **Neutros sofisticados** para fundos e superfícies (camadas claras de hierarquia).
- **Tipografia com personalidade**: combinar legibilidade alta com presença visual.
- **Ícones consistentes** (espessura e proporção únicas em todo produto).
- **Ilustrações/empty states de marca** para experiência mais humana.

### 4.15 Padrões de página (templates reutilizáveis)

Criar templates visuais base para reduzir improviso por tela:

1. **Template de listagem**
  - Header com busca/filtro/ação primária.
  - Lista com estados: loading, vazio, erro, sucesso.

2. **Template de detalhe**
  - Bloco principal + barra lateral de resumo/ações.
  - CTA persistente em ações críticas.

3. **Template de formulário**
  - Etapas claras (progress indicator).
  - Erro inline + resumo de validações no topo quando necessário.

4. **Template dashboard**
  - Área de KPIs no topo.
  - Conteúdo analítico em blocos priorizados por decisão de negócio.

### 4.16 Pontos de alto impacto visual imediato (quick wins)

Mudanças rápidas que já elevam percepção premium:

- Padronizar botão primário com presença forte (cor, sombra sutil, foco visível).
- Reestilizar cabeçalhos de páginas com tipografia e espaçamento de produto premium.
- Aplicar skeleton em telas de maior uso (agendamento, agenda, dashboard).
- Melhorar empty states com mensagem orientativa + CTA de ação.
- Harmonizar navbar/header entre áreas de cliente e barbeiro.

### 4.17 Checklist de revisão visual por PR (frontend)

Antes de aprovar PR com impacto de interface:

1. Página respeita grid e espaçamento padrão?
2. CTA principal está visualmente claro em mobile e desktop?
3. Estados de loading/erro/vazio estão implementados?
4. Contraste e foco visível atendem acessibilidade mínima?
5. Há consistência com componentes já existentes (sem “novo padrão isolado”)?

### 4.18 Experiência percebida pelo usuário final (resultado esperado)

Com as melhorias acima, o usuário deve perceber:

- Navegação mais elegante e intuitiva.
- Menor esforço para completar agendamento e gestão.
- Mais confiança em ações financeiras e status críticos.
- Sensação de aplicativo moderno, estável e profissional.
- Melhor experiência em dispositivos móveis sem perda de qualidade visual.

### 4.19 Matriz de visualização por perfil (Funcionalidade x Perfil)

Para eliminar ambiguidade no design, o front deve explicitar as diferenças de experiência entre perfis:

| Bloco/Funcionalidade | Cliente | Barbeiro (funcionário) | Barbeiro Owner |
|---|---|---|---|
| Home principal | Descoberta + próximos agendamentos | Agenda do dia + execução | KPIs + operação + gestão |
| Navegação principal | Início, Buscar, Agenda, Favoritos, Perfil | Início, Agenda, Novo Agendamento, Perfil | Início, Agenda, Equipe, Estoque, Serviços, Dashboard, Financeiro, Perfil |
| CTA principal | Agendar horário | Registrar/confirmar atendimento | Acompanhar indicadores e ações de gestão |
| Bloco de destaque | Sugestões e barbearias recomendadas | Próximos atendimentos e encaixes | Receita, ocupação, ticket médio, alertas |
| Financeiro / MP | Visualização de pagamento do próprio agendamento | Somente visão operacional necessária | Gestão completa de integração e desempenho |
| Profundidade analítica | Baixa (orientada a tarefa) | Média (orientada a operação) | Alta (orientada a decisão) |

### 4.20 Diretrizes visuais por perfil (sem quebrar consistência)

#### Cliente
- Linguagem visual acolhedora e orientada à descoberta.
- Priorizar clareza de fluxo (buscar → escolher → agendar).
- Cards de recomendação e disponibilidade com leitura imediata.

#### Barbeiro (funcionário)
- Foco em produtividade operacional (menos distração visual).
- Hierarquia centrada no que acontece “agora” (próximo atendimento, atraso, confirmação).
- CTAs rápidos e visíveis para rotina diária.

#### Barbeiro Owner
- Linguagem de gestão com maior densidade de informação.
- KPIs e alertas com semântica visual clara (normal, atenção, crítico).
- Acesso rápido a equipe, estoque, serviços e financeiro.

### 4.21 Regra de ouro de design multi-perfil

Manter o mesmo design system base (tokens, componentes, grid e motion), alterando por perfil apenas:

1. prioridade de conteúdo,
2. composição da navegação,
3. densidade de informação,
4. CTAs de alto valor.

Assim o produto ganha personalização por perfil sem parecer “3 produtos diferentes”.

---

## 5. Segurança de dados — melhorias gerais (priorizadas)

> Escopo: backend + frontend + operação.

## 5.1 Alta prioridade

### A) Gestão de tokens no frontend

**Risco atual:** uso de `localStorage` para sessão facilita impacto em caso de XSS.

**Melhoria recomendada (sem ruptura imediata):**
- Curto prazo: endurecer CSP + sanitização + reduzir superfície de XSS
- Médio prazo: migrar refresh/session sensível para cookie HttpOnly + SameSite

---

### B) Hardening no API Gateway (fonte de verdade de identidade)

- Garantir limpeza de headers sensíveis antes de encaminhar (`Authorization`, `X-User-*` vindos do cliente).
- Injetar apenas headers confiáveis após validação do token Firebase.
- Auditar rotas internas para não exposição indevida.

---

### C) Proteção de segredos

- Não persistir tokens externos em logs.
- Criptografia em repouso para credenciais sensíveis (ex.: MP tokens) com chave de ambiente/KMS.
- Rotação periódica de segredos.

---

### D) Segurança de webhook

- Validar assinatura/origem do Mercado Pago (além de idempotência existente).
- Limitar replay window e armazenar hash do payload.

---

## 5.2 Média prioridade

### E) Segurança de dados pessoais (LGPD)

- Política de retenção por tipo de dado.
- Mascaramento sistemático para logs e respostas públicas.
- Trilhas de auditoria para acesso/alteração de dados críticos.

### F) Segurança de APIs internas

- MTLS ou rede interna restrita para endpoints `/api/internal/**`
- Rate limiting por rota sensível
- Alertas para padrões anômalos de acesso

---

## 5.3 Baixa prioridade (mas recomendada)

### G) SAST/DAST e dependências

- Pipeline com verificação automática de CVE
- Baseline de lint segurança JS/Java
- Testes de segurança regressivos (smoke)

---

## 6. PWA — o que precisa para virar Progressive Web App

## 6.1 Itens obrigatórios

1. `manifest.webmanifest` em `frontend/public`
2. Service Worker (recomendado `vite-plugin-pwa`)
3. Registro SW no bootstrap (`main.jsx`)
4. Ícones 192/512 + maskable
5. Estratégia de cache:
   - App shell: cache-first
   - API dinâmica (agenda/pagamento): network-first
6. HTTPS em produção

## 6.2 Itens recomendados

- Banner de instalação (`beforeinstallprompt`)
- Tela offline customizada
- Banner de “nova versão disponível”
- Estratégia de atualização controlada (skipWaiting sob confirmação)

## 6.3 Riscos de PWA em app transacional

- Cache incorreto de APIs pode exibir disponibilidade antiga.
- Necessário versionar cache por domínio de dados e invalidar com critério.

---

## 7. Regras de execução sem regressão

1. **Feature flag** para cada melhoria transversal.
2. **Compatibilidade retroativa** durante migração (adapters/facades).
3. **Sem big-bang refactor**.
4. **Quality gates por fase:** build, lint, testes de fluxo crítico.
5. **Rollback simples** por commit/flag.

---

## 8. Matriz de priorização (impacto x risco)

| Iniciativa | Impacto | Risco | Prioridade |
|---|---:|---:|---:|
| Tokens visuais + breakpoints unificados | Alto | Baixo | P1 |
| Deduplicação de navegação/guardas via service/hooks | Alto | Médio | P1 |
| Hardening de gateway + headers confiáveis | Alto | Médio | P1 |
| Segurança webhook (assinatura/replay) | Alto | Médio | P1 |
| Tema dark/light por tokens | Médio | Baixo | P2 |
| PWA base (manifest + SW) | Médio | Médio | P2 |
| Migração de sessão para HttpOnly | Alto | Alto | P2/P3 |
| Camada offline avançada | Médio | Alto | P3 |

---

## 9. Conclusão executiva

A aplicação está em bom estágio funcional, e o caminho recomendado é de **evolução incremental sem ruptura**:

- reduzir duplicação por serviços/hook compartilhados,
- reforçar segurança dos dados em gateway, tokens e webhooks,
- profissionalizar UX com design tokens e responsividade sistêmica,
- habilitar PWA com cache consciente para domínio transacional.

✅ Sem quebrar o que funciona hoje.  
✅ Sem “gambiarra”.  
✅ Com trilha clara de migração segura.
