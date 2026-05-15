# Planning de Testes — Fase 1 e Fase 2 (SSE)
> Gerado em: 2026-05-15  
> Branch: `feature/migracao-microservicos`  
> Commits cobertos: `0e35310` (Fase 1) · `80e11ae` (Fase 2)

---

## Contexto

Este documento cobre os testes necessários para validar as duas fases de otimização de requisições frontend implementadas neste ciclo:

| Fase | Escopo | Commit |
|---|---|---|
| **Fase 1** | Remoção de polling em `ServicesHomeBarber`, `BarberServicesPage` e `BarberDashboardPage` | `0e35310` |
| **Fase 2** | Substituição do polling de 30s em `NotificationBell` por SSE (`/api/notifications/stream`) | `80e11ae` |

---

## 1. Testes — Fase 1 (Remoção de polling)

### 1.1 ServicesHomeBarber — polling de 20s removido

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F1-01 | Serviços carregam ao abrir a tela | Usuário logado como barbeiro | Navegar para `/barberHome` | Lista de serviços exibida corretamente 1× ao montar |
| F1-02 | Sem requisições periódicas | DevTools → Network aberto | Aguardar 1 minuto na tela | **Zero** requisições para `/barbershops/{id}/activities` após o carregamento inicial |
| F1-03 | Dados não somem ao ficar em segundo plano | Abrir outra aba e voltar após 2 min | Retornar para a aba | Serviços ainda exibidos (sem reload forçado) |

### 1.2 BarberServicesPage — polling de 15s removido (somente owners)

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F1-04 | Serviços carregam ao abrir | Usuário owner logado | Navegar para `/barberHome/services` | Lista de serviços exibida |
| F1-05 | Sem polling para owner | DevTools → Network | Aguardar 1 minuto | Zero chamadas periódicas para `/barbershops/my-shop/activities` |
| F1-06 | Sem polling para employee | Usuário barbeiro não-owner | Navegar para a página de serviços | Mesmo comportamento — sem polling |

### 1.3 BarberDashboardPage — auto-refresh dos painéis removido

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F1-07 | Dashboard carrega ao abrir | Owner logado com barbearia | Navegar para `/barberHome/dashboard` | KPIs, gráficos e relatórios exibidos após carregamento inicial |
| F1-08 | Botão "🔄 Atualizar dados" presente | Dashboard aberto | Inspecionar botão no Hero Section | Botão visível ao lado do botão PDF |
| F1-09 | Botão atualiza todos os painéis | Dashboard com dados | Clicar em "🔄 Atualizar dados" | Todos os painéis recarregam e exibem dados atualizados |
| F1-10 | Sem auto-refresh nos painéis | DevTools → Network | Aguardar 5 minutos sem interação | Zero requisições para `/payments/analytics/**` após carregamento |

---

## 2. Testes — Fase 2 (SSE — NotificationBell)

### 2.1 Conexão SSE

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F2-01 | Conexão SSE estabelecida ao montar | Usuário logado (barber ou customer) | Abrir qualquer página com `NotificationBell` no header | DevTools → Network: requisição `notifications/stream` com status **200** e type **`eventsource`** |
| F2-02 | Token enviado via query param | DevTools → Network | Clicar em `notifications/stream` | URL contém `?token=<firebase_token>` |
| F2-03 | Evento `unread-count` recebido ao conectar | Usuário com notificações não lidas | Montar o componente | Badge do sino exibe contagem correta imediatamente (vem do SSE, não do polling) |
| F2-04 | Sem polling ativo | DevTools → Network | Aguardar 2 minutos com o sino montado | **Zero** requisições periódicas para `notifications/unread-count` — apenas a conexão SSE aberta |

### 2.2 Atualização em tempo real

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F2-05 | Badge atualiza ao receber notificação | Duas sessões abertas (ou forçar evento via RabbitMQ) | Criar agendamento que gera evento `appointment.created` | Badge do barbeiro atualiza em < 2s **sem refresh da página** |
| F2-06 | Badge atualiza ao receber join request | Owner logado com SSE ativo | Criar `barbershop_join_request` via API | Badge do owner incrementa em tempo real |
| F2-07 | Badge zera ao marcar tudo como lido | Notificações não lidas presentes | Abrir dropdown e clicar em cada notificação | `unreadCount` cai progressivamente; ao zerar, badge some |

### 2.3 Reconexão automática

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F2-08 | Reconexão após queda de rede | SSE ativo | Simular queda: DevTools → Network → Offline | Após reconectar (10s), nova requisição SSE estabelecida automaticamente |
| F2-09 | Reconexão após restart do serviço | SSE ativo | Reiniciar o `notification-service` no Docker | Em até 10s, conexão SSE restabelecida; badge volta a funcionar |
| F2-10 | Token expirado não gera loop infinito | SSE ativo | Forçar token expirado (aguardar 1h ou invalidar no Firebase) | Erro SSE → reconexão → 401 → conexão encerrada (sem loop). Console não exibe erros repetitivos |

### 2.4 Segurança — Gateway (`?token=` restrito à rota SSE)

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F2-11 | `?token=` não funciona em rotas protegidas comuns | Token válido | `GET /api/notifications/my-notifications?token=<token>` (sem header `Authorization`) | Resposta **401** — gateway não aceita query param fora da rota SSE |
| F2-12 | SSE requer token válido | Token inválido | `GET /api/notifications/stream?token=token_invalido` | Resposta **401** do gateway |
| F2-13 | SSE requer token (sem token) | Nenhum token | `GET /api/notifications/stream` | Resposta **401** |

### 2.5 Backend — SseEmitterService

| # | Cenário | Pré-condição | Ação | Resultado esperado |
|---|---|---|---|---|
| F2-14 | Múltiplos usuários conectados simultaneamente | Dois usuários diferentes logados | Ambos abrem o sino | Dois emitters distintos na registry; eventos enviados apenas para o userId correto |
| F2-15 | Emitter removido ao fechar conexão | SSE ativo | Fechar a aba do browser | `emitter.onCompletion` disparado → `SseEmitterService.remove(userId)` chamado |
| F2-16 | Emitter removido em caso de erro de escrita | SSE ativo | Forçar `IOException` no envio (mock) | Emitter removido da registry; sem tentativas de re-envio para emitter morto |

---

## 3. Testes de Regressão

> Verificar que as remoções de polling não quebraram funcionalidades existentes.

| # | Funcionalidade | Ação | Resultado esperado |
|---|---|---|---|
| R-01 | Lista de serviços do barbeiro | Abrir `ServicesHomeBarber` | Serviços da barbearia exibidos corretamente |
| R-02 | CRUD de serviços (owner) | Criar, editar e excluir serviço em `BarberServicesPage` | Operações funcionam; lista atualiza via re-fetch manual |
| R-03 | Dashboard financeiro | Abrir `BarberDashboardPage` | KPIs, gráficos de receita e agenda exibidos |
| R-04 | Export PDF do dashboard | Clicar em "Baixar PDF" | PDF gerado e baixado corretamente |
| R-05 | Notificações — listar | Clicar no sino | Dropdown abre com lista de notificações (GET `/my-notifications`) |
| R-06 | Notificações — marcar lida | Clicar em notificação | Status atualiza para lido; badge decrementa |
| R-07 | Notificações — novo agendamento | Criar agendamento como cliente | Barbeiro recebe notificação SSE em tempo real |
| R-08 | Header carrega sem erro | Logar como barbeiro ou cliente | Header exibe sem console errors relacionados ao SSE |

---

## 4. Testes de Carga (não-funcionais)

| # | Métrica | Como medir | Meta |
|---|---|---|---|
| NF-01 | Redução de requisições HTTP | DevTools → Network durante 5 min | < 5 req/min por usuário (era ~25 req/min) |
| NF-02 | Conexões SSE simultâneas | JMeter ou k6: 50 usuários conectados | `notification-service` CPU < 20%; sem memory leak |
| NF-03 | Latência SSE ponta-a-ponta | Medir do `@RabbitListener` até evento no browser | < 1.5s em P95 |
| NF-04 | Reconexão SSE após restart | Reiniciar serviço com clientes conectados | 100% dos clientes reconectam em < 15s |

---

## 5. Ambiente de Teste

```
Produção:  ssh Edu@10.147.19.1  (docker-compose.server.yml)
Container: cortaai-mysql        (MySQL 8.0.46)
Branch:    feature/migracao-microservicos

Pré-requisitos para os testes F2-05 e F2-06:
  - notification-service rodando e conectado ao RabbitMQ
  - api-gateway com SSE token-query-param ativo (commit 80e11ae)
  - Browser com DevTools → Network → Filter: "eventsource"
```

---

## 6. Execução Prioritária (smoke tests)

Ordem mínima para validar que o deploy não quebrou nada:

```
F1-01 → F1-07 → F1-08 → F1-09
F2-01 → F2-03 → F2-04 → F2-05
R-05  → R-06  → R-07
```
