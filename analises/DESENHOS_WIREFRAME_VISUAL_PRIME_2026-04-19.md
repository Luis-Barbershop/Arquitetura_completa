# Desenhos de referência — Visual/Layout Prime (CortaAi)

> Objetivo: visualizar como as sugestões de UI/UX premium podem se materializar no frontend.
> Formato: wireframes textuais (ASCII) para guiar implementação incremental sem regressão.

## Perfis de visualização (obrigatório)

O frontend precisa respeitar **3 visões distintas**:

1. **Cliente**
   - foco em descoberta, agendamento e acompanhamento dos próprios horários.
2. **Barbeiro (funcionário)**
   - foco em agenda operacional, atendimentos do dia e execução.
3. **Barbeiro Owner**
   - visão de gestão: time, estoque, dashboard, financeiro e Mercado Pago.

> Regra visual: manter linguagem de design consistente, mas com navegação e prioridade de ações específicas por perfil.

---

## 1) Home Barbeiro — Desktop (12 colunas)

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ Header Premium                                                                              │
│ [Logo]   [Busca rápida]                    [Notificações] [Avatar + Menu]                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ KPI Row (cards padronizados)                                                                │
│ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐                 │
│ │ Hoje           │ │ Semana         │ │ Faturamento    │ │ Avaliação      │                 │
│ │ 12 atend.      │ │ 54 atend.      │ │ R$ 4.280       │ │ ★ 4.8          │                 │
│ │ +8%            │ │ +12%           │ │ +6%            │ │ +0.2           │                 │
│ └────────────────┘ └────────────────┘ └────────────────┘ └────────────────┘                 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ Grid principal                                                                              │
│ ┌──────────────────────────────────────────────┐  ┌───────────────────────────────────────┐ │
│ │ Agenda de hoje                               │  │ Ações rápidas                          │ │
│ │ 09:00 João (Corte)                           │  │ [Novo agendamento]                     │ │
│ │ 10:00 Pedro (Barba)                          │  │ [Confirmar presença]                   │ │
│ │ 11:30 Lucas (Combo)                          │  │ [Registrar pagamento]                  │ │
│ │ ...                                          │  │ [Ver agenda completa]                  │ │
│ └──────────────────────────────────────────────┘  └───────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ Footer / status                                                                              │
│ Última sincronização MP: há 2 min                                                           │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2) Home Barbeiro — Mobile (4 colunas)

```text
┌──────────────────────────────┐
│ [☰] CortaAi      [🔔][👤]    │
├──────────────────────────────┤
│ KPI carrossel                │
│ ┌──────────────────────────┐ │
│ │ Hoje: 12 atendimentos    │ │
│ │ +8%                      │ │
│ └──────────────────────────┘ │
├──────────────────────────────┤
│ Ação principal               │
│ [ + Novo Agendamento ]       │
├──────────────────────────────┤
│ Próximos atendimentos        │
│ 09:00 João - Corte           │
│ 10:00 Pedro - Barba          │
│ 11:30 Lucas - Combo          │
├──────────────────────────────┤
│ [Início] [Agenda] [Gestão]   │
└──────────────────────────────┘
```

---

## 2.1) Home Cliente — Desktop (12 colunas)

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ Header Premium                                                                              │
│ [Logo]   [Busca por barbearia/serviço]           [Favoritos] [Notificações] [Perfil]       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ Banner principal                                                                            │
│ "Agende em minutos"                                                [Agendar agora]         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────┐  ┌───────────────────────────────────────┐ │
│ │ Próximos horários                             │  │ Meus atalhos                          │ │
│ │ [Amanhã 10:30 - Prime Cuts]                   │  │ [Meus agendamentos]                   │ │
│ │ [Quarta 14:00 - Barber House]                 │  │ [Favoritos]                           │ │
│ └──────────────────────────────────────────────┘  │ [Reagendar]                            │ │
│                                                   └───────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ Sessões de descoberta: [Mais bem avaliadas] [Próximas de você] [Promoções]                 │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2.2) Home Cliente — Mobile (4 colunas)

```text
┌──────────────────────────────┐
│ CortaAi          [🔔][👤]     │
├──────────────────────────────┤
│ [Buscar barbearia/serviço]   │
├──────────────────────────────┤
│ Próximo agendamento          │
│ Amanhã 10:30 - Prime Cuts    │
│ [Ver detalhes] [Reagendar]   │
├──────────────────────────────┤
│ Descubra                     │
│ [Mais avaliadas]             │
│ [Perto de você]              │
├──────────────────────────────┤
│ [Início] [Buscar] [Agenda]   │
└──────────────────────────────┘
```

---

## 2.3) Home Barbeiro Owner — Desktop (12 colunas)

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ Header Owner                                                                                │
│ [Logo] [Busca interna]                 [Notificações] [Status MP] [Avatar + Gestão]        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ KPI de gestão                                                                             │
│ [Receita dia] [Agendamentos] [Taxa ocupação] [NPS] [Recompra]                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────┐  ┌───────────────────────────────────────┐ │
│ │ Operação do dia                              │  │ Gestão rápida                          │ │
│ │ Agenda equipe / atrasos / encaixes           │  │ [Equipe] [Estoque] [Serviços]         │ │
│ │                                              │  │ [Dashboard] [Financeiro MP]            │ │
│ └──────────────────────────────────────────────┘  └───────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ Alertas de negócio: estoque baixo | pagamento pendente | queda de ocupação                 │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3) Fluxo de Agendamento — Desktop (cliente)

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ Header + progresso                                                                           │
│ Etapa 1 Serviço   Etapa 2 Profissional   Etapa 3 Horário   Etapa 4 Confirmação             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────────────┐  ┌───────────────────────────────┐ │
│ │ Seleção principal                                     │  │ Resumo fixo                    │ │
│ │ [Serviço: Corte + Barba]                              │  │ Barbearia: Prime Cuts          │ │
│ │ [Profissional: Rafael]                                │  │ Serviço: Combo                 │ │
│ │                                                       │  │ Duração: 45 min                │ │
│ │ Datas (cards)                                         │  │ Preço: R$ 70,00               │ │
│ │ [Hoje] [Amanhã] [Qua] [Qui] [Sex]                     │  │ Horário: 10:30                │ │
│ │                                                       │  │                               │ │
│ │ Horários (grid)                                       │  │ [Confirmar agendamento]       │ │
│ │ [09:00] [09:15] [09:30] [09:45] ...                   │  │                               │ │
│ │ Slot indisponível? -> sugerir próximo melhor          │  │                               │ │
│ └───────────────────────────────────────────────────────┘  └───────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4) Fluxo de Agendamento — Mobile (cliente)

```text
┌──────────────────────────────┐
│ Agendar horário              │
│ Etapa 3 de 4                 │
├──────────────────────────────┤
│ Resumo compacto              │
│ Combo • Rafael • 45min       │
│ R$ 70,00                     │
├──────────────────────────────┤
│ Datas                         │
│ [Hoje] [Amanhã] [Qua]        │
├──────────────────────────────┤
│ Horários                      │
│ [09:00] [09:15] [09:30]      │
│ [09:45] [10:00] [10:15]      │
│ ...                           │
├──────────────────────────────┤
│ Próximo melhor: 10:30         │
│ [Usar 10:30]                  │
├──────────────────────────────┤
│ [Confirmar agendamento]       │
└──────────────────────────────┘
```

---

## 5) Dashboard Financeiro + Mercado Pago (Desktop)

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│ Financeiro                                                                                   │
│ [Conectado ao Mercado Pago ✅]   Última sincronização: 14:32   [Gerenciar conexão]          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌───────────────────────┐ ┌───────────────────────┐ ┌─────────────────────────────────────┐ │
│ │ Receita Hoje          │ │ Ticket Médio          │ │ Gráfico 7 dias                       │ │
│ │ R$ 1.240              │ │ R$ 78                 │ │ ▁▃▅▆▇▆▅                              │ │
│ │ +5%                   │ │ +2%                   │ │                                     │ │
│ └───────────────────────┘ └───────────────────────┘ └─────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│ Últimas transações                                                                          │
│ [10:32] João  R$70  aprovado                                                                 │
│ [11:10] Pedro R$40  pendente                                                                 │
│ [12:01] Ana   R$95  aprovado                                                                 │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5.1) Diferença de navegação por perfil (resumo visual)

```text
CLIENTE
[Início] [Buscar] [Agendamentos] [Favoritos] [Perfil]

BARBEIRO (FUNCIONÁRIO)
[Início] [Agenda] [Novo agendamento] [Perfil]

BARBEIRO OWNER
[Início] [Agenda] [Equipe] [Estoque] [Serviços] [Dashboard] [Financeiro] [Perfil]
```

---

## 6) Estados visuais obrigatórios (componentes)

```text
Botão Primário
[Normal] [Hover] [Pressed] [Loading...] [Success ✓] [Disabled]

Input
[Normal] [Focus com borda visível] [Erro: mensagem clara] [Sucesso]

Card de conteúdo
[Loading skeleton] [Com dados] [Empty state + CTA] [Erro + tentar novamente]
```

---

## 7) Navbar/Headers harmonizados (cliente e barbeiro)

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ Marca | Navegação principal | Busca | Notificações | Perfil                │
│ - mesma altura                                                     (48-56px)│
│ - mesma lógica de espaçamento                                      (8/12/16)│
│ - mesmo padrão de foco/hover/ativo                                         │
│ - mesmos componentes-base, mudando apenas itens por perfil                 │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 8) Direção de implementação (sem regressão)

1. Aplicar primeiro em 2 telas piloto:
   - `Home Cliente`
   - `Home Barbeiro Owner`
2. Medir percepção e conversão antes/depois.
3. Expandir para `Home Barbeiro (funcionário)` e `Agendamento`.
4. Expandir para dashboard e financeiro.
5. Só depois migrar restante de telas.

---

## 9) Resultado esperado para o usuário final

- Site com estética premium e coerente.
- Menor esforço para concluir tarefas principais.
- Sensação de velocidade e estabilidade maior.
- Confiança visual em fluxos críticos (agenda e pagamento).
