# 📁 analises/ — Documentação Técnica do CortaAí

> **Branch:** `feature/migracao-microservicos` | **Atualizado:** Abril/2026

Esta pasta contém a documentação técnica ativa do projeto. Todos os arquivos abaixo podem ser utilizados como **anexo/apêndice** na documentação formal (TCC, relatório técnico, etc.).

---

## 📄 Documentos Ativos

| Arquivo | Propósito | Uso recomendado |
|---------|-----------|----------------|
| [`DOCUMENTACAO_SISTEMA_CORTAAI.md`](DOCUMENTACAO_SISTEMA_CORTAAI.md) | Documento mestre: visão geral, tecnologias, arquitetura, regras de negócio, casos de uso, infra, deploy | Capítulo principal do TCC / documentação de entrega |
| [`ARQUITETURA.md`](ARQUITETURA.md) | Diagrama ASCII da arquitetura de microsserviços, fluxo de dados, stack de cada serviço | Apêndice de arquitetura |
| [`COMPONENTES.md`](COMPONENTES.md) | Catálogo de todos os componentes criados: POMs, Docker Compose, schemas, cada microsserviço (controller, service, repository, DTOs, configs) | Apêndice de componentes / rastreabilidade |
| [`FRF_PRD_TD_BACKEND_COMPLETO.md`](FRF_PRD_TD_BACKEND_COMPLETO.md) | FRF (Requisitos Funcionais) + PRD (Produto) + TD (Técnico) do backend completo | Documento de requisitos e especificação técnica |
| [`ENV_PRODUCAO.md`](ENV_PRODUCAO.md) | Template completo de `.env` para produção (ZimaOS), com instruções de preenchimento | Guia operacional de deploy |
| [`OTIMIZACAO_MEMORIA.md`](OTIMIZACAO_MEMORIA.md) | Análise de consumo de RAM, otimizações aplicadas (JVM flags, multi-stage Docker, Spring tuning) | Apêndice de infraestrutura / performance |
| [`FLUXO_AUTENTICACAO_E_CADASTRO.md`](FLUXO_AUTENTICACAO_E_CADASTRO.md) | Fluxo detalhado de auth/cadastro por perfil (Customer, Barber, Owner) com exemplos de requisição | Apêndice de fluxos de autenticação |
| [`FLUXO_CADASTRO_BARBEIRO_BARBEARIA_SERVICO.md`](FLUXO_CADASTRO_BARBEIRO_BARBEARIA_SERVICO.md) | Fluxo completo: cadastro do barbeiro → verificação de e-mail → perfil → barbearia → serviços → habilidades. Inclui diagrama de sequência e tabelas | Apêndice de fluxo de negócio do barbeiro |
| [`RELATORIO_FLUXO_ATUAL_AUTENTICACAO_CADASTRO.md`](RELATORIO_FLUXO_ATUAL_AUTENTICACAO_CADASTRO.md) | Relatório técnico do fluxo real de auth em produção: validações, claims Firebase, fontes de verdade no código | Referência técnica interna / debugging |
| **[`ROTEIRO_TESTES_API.md`](ROTEIRO_TESTES_API.md)** | **Roteiro completo de testes da API: 14 fases, 69 endpoints, corpos de requisição/resposta, checklists, testes negativos e sequência mínima de 30 min** | **Guia de testes — usar diretamente no Hoppscotch/Postman** |

---

## 🗂️ Histórico (_historico/)

A subpasta [`_historico/`](_historico/) contém documentos de planejamento e progresso da migração monólito → microsserviços. São úteis como **referência histórica** mas não são mais documentação ativa.

| Arquivo | Conteúdo |
|---------|----------|
| `PLANO_REESTRUTURACAO_CORTAAI.md` | Plano mestre de reestruturação com todas as fases, estratégia de BD, segurança, cronograma macro |
| `GUIA_MIGRACAO_PASSO_A_PASSO.md` | Guia sequencial de migração (padrão Strangler Fig), ordem de extração dos serviços |
| `PROGRESSO_MIGRACAO.md` | Log de progresso das etapas 0–7 com checklist detalhado do que foi feito |
| `RELATORIO_ENDPOINTS_MONOLITO_VS_MICROSERVICOS.md` | Comparativo: quais endpoints existiam no monólito vs. microsserviços (migrado, removido, novo, pendente) |
| `PLANEJAMENTO_DEV1_DEV2.md` | Distribuição de tarefas entre Dev 1 e Dev 2, mapa de infraestrutura (RabbitMQ, Feign, Gateway, Docker) |
| `GUIA_TRABALHO_PARALELO_DEV1_DEV2_DEV3.md` | Guia anti-conflito para trabalho paralelo de 3 devs, mapa de propriedade de arquivos |
| `RELATORIO_TAREFAS_DEV1.md` | Checklist detalhado de todas as tarefas do Dev 1 (schedule-service + barbershop-service extras) |

---

## 📌 Ordem de leitura recomendada

Para quem está conhecendo o projeto pela primeira vez:

```
1. DOCUMENTACAO_SISTEMA_CORTAAI.md   ← visão geral e negócio
2. ARQUITETURA.md                    ← como os serviços se conectam
3. FRF_PRD_TD_BACKEND_COMPLETO.md   ← requisitos e regras técnicas
4. FLUXO_AUTENTICACAO_E_CADASTRO.md ← como o usuário interage
5. ROTEIRO_TESTES_API.md             ← como testar tudo
```

Para deploy em produção:
```
ENV_PRODUCAO.md → OTIMIZACAO_MEMORIA.md
```
