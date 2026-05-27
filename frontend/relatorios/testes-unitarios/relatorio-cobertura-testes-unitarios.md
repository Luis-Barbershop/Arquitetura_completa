# Relatorio de Cobertura dos Testes Unitarios do Front-end

**Projeto:** CortaAi Frontend  
**Tecnologia:** React + Vite + Vitest + React Testing Library  
**Data da analise:** 22/05/2026  
**Comando de cobertura:** `npm run coverage`  
**Relatorio HTML gerado em:** `frontend/coverage/index.html`

## Objetivo

Este documento registra a cobertura atual dos testes unitarios do front-end e explica o significado dos indicadores gerados pelo Vitest com o provider `v8`.

A cobertura mostra quais partes do codigo fonte foram executadas durante os testes automatizados. Ela nao garante ausencia de bugs, mas ajuda a medir o quanto a suite protege funcoes, fluxos, condicoes, componentes e paginas contra regressoes.

## Resultado geral da execucao

Resultado da ultima execucao:

- Arquivos de teste executados: 41
- Suites aprovadas: 41
- Suites ignoradas: 0
- Testes aprovados: 129
- Testes ignorados: 0
- Testes reprovados: 0

## Evolucao da cobertura

| Momento | Statements | Branches | Functions | Lines |
| --- | ---: | ---: | ---: | ---: |
| Inicio da ampliacao | 17.37% | 9.81% | 16.13% | 18.38% |
| Apos primeira rodada de paginas | 19.86% | 12.29% | 18.03% | 21.01% |
| Apos Home/Site/componentes | 22.00% | 13.96% | 21.08% | 23.26% |
| Apos perfil/detalhe/verificacao | 25.18% | 17.04% | 23.82% | 26.67% |
| Apos paginas administrativas do barbeiro | 33.94% | 22.88% | 33.02% | 35.84% |
| Apos perfil, gestao e agendamentos | 46.69% | 34.47% | 43.15% | 49.17% |
| Estado atual | 53.73% | 40.13% | 50.91% | 55.96% |

## Cobertura geral atual

| Indicador | Cobertura |
| --- | ---: |
| Statements | 53.73% |
| Branches | 40.13% |
| Functions | 50.91% |
| Lines | 55.96% |

## Como interpretar os indicadores

**Statements** mede quantas instrucoes do codigo foram executadas pelos testes. Exemplo: chamadas de funcao, atribuicoes, retornos e operacoes internas.

**Branches** mede quantos caminhos condicionais foram testados. Exemplo: `if`, `else`, operador ternario, retornos alternativos, tratamento de erro e estados opcionais.

**Functions** mede quantas funcoes declaradas foram chamadas durante os testes.

**Lines** mede quantas linhas executaveis do codigo foram percorridas pela suite.

Em geral, `Lines` e `Statements` indicam cobertura de execucao. `Branches` costuma ser menor porque exige testar varios cenarios para a mesma funcao, como sucesso, erro, dados vazios, usuario sem permissao e estados alternativos.

## Cobertura por area

| Area | Statements | Branches | Functions | Lines | Interpretacao |
| --- | ---: | ---: | ---: | ---: | --- |
| `src/services` | 69.66% | 53.61% | 79.66% | 71.48% | Boa cobertura da camada de servicos, com chamadas HTTP, normalizacao de payloads, fallbacks e regras de negocio isoladas. |
| `src/hooks` | 79.24% | 82.35% | 87.50% | 80.39% | Boa cobertura dos hooks testados, principalmente autenticacao de rotas e stream de notificacoes. |
| `src/pages` | 67.47% | 56.83% | 64.69% | 70.30% | Evoluiu bastante com testes dedicados de paginas de cliente e barbeiro, incluindo criacao/gestao de barbearia, perfil, agendamentos, dashboard, home do barbeiro e encaixe manual. |
| `src/components/HomePage/Barbershops` | 97.72% | 91.11% | 100% | 100% | Cobertura alta para listagem, filtro, favoritos, rating, distancia e estado vazio. |
| `src/components/HomePage/Favorite_barbershops` | 85.18% | 72.72% | 87.50% | 84.61% | Cobre carregamento, favoritos encontrados, erro e navegacao para detalhe. |
| `src/components/StockMovementModal` | 93.10% | 86.36% | 100% | 100% | Cobertura alta do modal de movimentacao de estoque, incluindo validacoes e payload enviado. |
| `src/components/Site/Faq` | 100% | 83.33% | 100% | 100% | Componente coberto em renderizacao e interacao de abrir/fechar perguntas. |
| `src/components/Site/Header` | 78.57% | 50% | 66.66% | 78.57% | Cobre navegacao para login/cadastro e scroll do menu. |
| `src/pages/HomePage.jsx` | 100% | 83.33% | 93.33% | 100% | Fluxos de busca, favoritos, logout, localizacao e redirecionamento de barbeiro cobertos. |
| `src/pages/BarbershopDetailPage.jsx` | 94.23% | 75.80% | 76.92% | 95.74% | Cobre carregamento de dados, geocoding, persistencia de coordenadas, servicos, barbeiros, mapa, CTA e estados de erro/vazio. |
| `src/pages/CustomerProfilePage.jsx` | 92.64% | 73.68% | 92.30% | 93.65% | Cobre carregamento de perfil, edicao, upload de foto, logout, erro e redirecionamento de barbeiro. |
| `src/pages/VerifyEmailPage.jsx` | 85.07% | 91.48% | 87.50% | 88.70% | Cobre modo aguardando verificacao, reenvio, senha ausente, alteracao de e-mail, sucesso e erro no link Firebase. |
| `src/pages/BarberBlockPage.jsx` | 79.41% | 68.51% | 65.62% | 79.68% | Cobre carregamento de barbeiro, listagem de bloqueios, criacao por horario/dia, validacao de horario e remocao. |
| `src/pages/BarberServicesPage.jsx` | 68.90% | 60.00% | 83.33% | 68.38% | Cobre listagem de servicos, metricas, criacao, validacao, exclusao, atribuicao de habilidades e redirecionamentos. |
| `src/pages/BarberStockPage.jsx` | 68.15% | 54.23% | 80.00% | 72.45% | Cobre inventario, cadastro de produto, categorias, edicao de categoria, movimentacao de estoque, exclusao e guardas de acesso. |
| `src/pages/BarberTeamPage.jsx` | 65.45% | 52.42% | 65.85% | 74.43% | Cobre carregamento da equipe, convite por CPF, comissoes, remocao segura e redirecionamento de perfil sem permissao. |
| `src/pages/AgendaBarbeariaPage.jsx` | 75.47% | 75.75% | 77.77% | 88.60% | Cobre guarda de dono, redirecionamento para agenda de equipe, carregamento, filtros por status, paginacao, troca de data, refresh, logout e navegacao. |
| `src/pages/BarberDashboardPage.jsx` | 84.55% | 76.78% | 64.28% | 85.26% | Cobre carregamento dos indicadores, paineis mockados, exportacao PDF, atualizacao, cadastro/exclusao de gastos fixos, logout, guardas e navegacao por abas. |
| `src/pages/BarberHomePage.jsx` | 98.41% | 85.00% | 100% | 98.33% | Cobre home com barbearia, painel sem barbearia, atalhos, sincronizacao de localStorage, retorno Mercado Pago, modal de logout e navegacao. |
| `src/pages/BarberManualBookingPage.jsx` | 87.34% | 75.00% | 85.71% | 89.31% | Cobre carregamento de servicos atribuidos, selecao de data/horario, resumo de preco/duracao, payload de encaixe, validacoes, erro 409, aviso offline, logout e navegacao. |
| `src/pages/CreateBarbershopPage.jsx` | 82.29% | 64.58% | 73.33% | 85.22% | Cobre consulta de CEP, montagem de endereco, mascara de CNPJ, criacao da barbearia, refresh de sessao, navegacao, validacao e crop de logo. |
| `src/pages/ManageBarbershopPage.jsx` | 86.04% | 69.89% | 80.76% | 92.23% | Cobre carregamento da barbearia, edicao de dados publicos, geocoding, captura de localizacao, upload de miniatura/banner, guardas de dono, logout e navegacao. |
| `src/pages/BarberProfilePage.jsx` | 62.39% | 56.82% | 50.60% | 66.09% | Cobre perfil do dono e colaborador, horarios de trabalho, alternancia de atuar como barbeiro, convites, saida da barbearia, upload de foto e redirecionamento de cliente. |
| `src/pages/MeusAgendamentosPage.jsx` | 57.08% | 54.07% | 54.62% | 58.86% | Cobre agenda do cliente, remarcacao, cancelamento, conclusao, avaliacao da barbearia, guardas de login e visao de equipe do dono. |
| `src/pages/LoginPage.jsx` | 100% | 50% | 100% | 100% | Pagina simples totalmente executada nos testes existentes, com branch parcial. |
| `src/components/BarberPage` | 0% | 0% | 0% | 0% | Area ainda sem testes unitarios dedicados. |
| `src/components/Dashboard` | 0% | 0% | 0% | 0% | Componentes foram mockados nos testes da pagina para isolar o comportamento do dashboard; ainda precisam de testes proprios. |

## Arquivos de teste considerados

### Componentes

- `src/components/HomePage/HomePageComponents.test.jsx`
- `src/components/Site/SiteComponents.test.jsx`
- `src/components/StockMovementModal/StockMovementModal.test.jsx`
- `src/components/UpdateAvailableBanner.test.jsx`

### Hooks

- `src/hooks/useAuthGuard.test.jsx`
- `src/hooks/useNotificationStream.test.jsx`

### Paginas

- `src/pages/__tests__/AgendamentoPage.test.jsx`
- `src/pages/__tests__/AgendaBarbeariaPage.test.jsx`
- `src/pages/__tests__/BarberDashboardPage.test.jsx`
- `src/pages/__tests__/BarberBlockPage.test.jsx`
- `src/pages/__tests__/BarberHomePage.test.jsx`
- `src/pages/__tests__/BarberManualBookingPage.test.jsx`
- `src/pages/__tests__/BarberProfilePage.test.jsx`
- `src/pages/__tests__/BarberServicesPage.test.jsx`
- `src/pages/__tests__/BarberStockPage.test.jsx`
- `src/pages/__tests__/BarberTeamPage.test.jsx`
- `src/pages/__tests__/BarbershopDetailPage.test.jsx`
- `src/pages/__tests__/BasicPages.test.jsx`
- `src/pages/__tests__/ChangePasswordPage.test.jsx`
- `src/pages/__tests__/CreateBarbershopPage.test.jsx`
- `src/pages/__tests__/CustomerProfilePage.test.jsx`
- `src/pages/__tests__/ForgotPasswordPage.test.jsx`
- `src/pages/__tests__/HomePage.test.jsx`
- `src/pages/__tests__/LoginPage.test.jsx`
- `src/pages/__tests__/ManageBarbershopPage.test.jsx`
- `src/pages/__tests__/MeusAgendamentosPage.test.jsx`
- `src/pages/__tests__/VerifyEmailPage.test.jsx`

### Servicos

- `src/services/analyticsService.test.js`
- `src/services/appointmentAvailabilityService.test.js`
- `src/services/appointmentService.test.js`
- `src/services/authService.test.js`
- `src/services/barberBlockService.test.js`
- `src/services/barbershopService.test.js`
- `src/services/gustaveService.test.js`
- `src/services/navigationService.test.js`
- `src/services/offlineTransactionalService.test.js`
- `src/services/pwaService.test.js`
- `src/services/pwaTelemetryService.test.js`
- `src/services/userContext.test.js`
- `src/services/userProfileService.test.js`

### Utilitarios

- `src/utils/inputMasks.test.js`

## O que ja esta bem coberto

A suite atual protege principalmente:

- Mascaras e formatacoes de entrada.
- Contexto do usuario salvo em `localStorage`.
- Resolucao de navegacao e permissoes.
- Fluxos principais de autenticacao.
- Recuperacao, alteracao e verificacao de e-mail.
- Servicos de agendamento, barbearia, analytics, perfil, PWA e notificacoes.
- Tratamento de erro e retorno vazio em chamadas HTTP.
- Home do cliente, busca, favoritas, localizacao e logout.
- Perfil do cliente, atualizacao de dados e upload de foto.
- Detalhe de barbearia, servicos, barbeiros, mapa e CTA de agendamento.
- Painel de barbeiro para estoque, servicos, time e indisponibilidade.
- Criacao, edicao, exclusao e validacao de dados em paginas administrativas do barbeiro.
- Criacao e gestao de barbearia, incluindo CEP, endereco, geocoding, midias e guardas de dono.
- Perfil do barbeiro, com horarios, convites, saida de barbearia, foto de perfil e configuracao de dono.
- Meus agendamentos, com remarcacao, cancelamento, conclusao, avaliacao e agenda da equipe.
- Agenda da barbearia, dashboard do dono, home do barbeiro e encaixe manual.
- Interacoes simples de componentes reutilizaveis.

Essa cobertura e importante porque essas partes concentram regras que podem quebrar silenciosamente mesmo sem alteracao visual.

## Por que a cobertura global ainda nao e alta

A cobertura geral chegou a 55.96% de linhas, mas o projeto ainda possui componentes extensos sem testes diretos.

As maiores areas descobertas continuam sendo:

- Componentes do painel do barbeiro em `src/components/BarberPage`.
- Componentes de dashboard e graficos.
- Formularios longos de cadastro e remarcacao.
- Componentes como `RescheduleModal`, `SignIn_inputs`, `Login_Inputs`, `GustaveChat` e `PushNotificationToggle`.

Mesmo com `src/services`, `src/hooks`, paginas de cliente e varias paginas administrativas do barbeiro bem cobertas, essas telas grandes contam no denominador da cobertura total.

## Leitura tecnica do resultado

O resultado atual ja passa de uma cobertura inicial basica e cobre melhor os fluxos de cliente e a camada logica.

Para evoluir com qualidade, a melhor estrategia nao e apenas testar linhas soltas, mas priorizar fluxos com regra de negocio:

- estados de carregamento, sucesso e erro;
- formularios e validacoes;
- permissoes por perfil;
- transformacao de payload antes de chamar API;
- navegacao apos acoes importantes.

## Recomendacao de evolucao

Prioridade sugerida para aumentar cobertura com melhor retorno:

1. Componentes de `BarberPage`
2. Componentes de `Dashboard/panels`
3. `RescheduleModal.jsx`
4. `SignIn_inputs.jsx`
5. `Login_Inputs.jsx`
6. `PushNotificationToggle.jsx`
7. `GustaveChat.jsx`
8. `NotificationBell.jsx`
9. `CropImageModal.jsx`
10. Componentes internos de `AgendamentoPage`

Essas areas possuem muitos estados, formularios, chamadas de API e regras condicionais. Testa-las tende a aumentar bastante a cobertura de `branches`, `functions` e `lines`.

## Conclusao

A suite atual esta estavel e executa sem falhas: 129 testes aprovados, 0 ignorados e 0 reprovados.

A cobertura global atual e:

- 53.73% statements
- 40.13% branches
- 50.91% functions
- 55.96% lines

O maior ganho desta etapa foi elevar a cobertura de paginas que ainda estavam zeradas ou com teste ignorado: `AgendaBarbeariaPage`, `BarberDashboardPage`, `BarberHomePage` e `BarberManualBookingPage`.

O relatorio HTML em `frontend/coverage/index.html` pode ser usado para navegar arquivo por arquivo e visualizar exatamente quais linhas foram ou nao executadas pelos testes.
