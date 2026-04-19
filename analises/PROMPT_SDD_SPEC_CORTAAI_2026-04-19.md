# Prompt SDD — Especificação Orientada a Domínio (CortaAi)

> Use este prompt em ferramenta de IA para gerar especificação técnica **implementável** sem quebrar os contratos atuais.

---

## Prompt

Você é um arquiteto de software sênior e especialista em SDD (Software Design Description) para sistemas Java Spring Boot em microsserviços e frontend React.

Seu trabalho é produzir uma SDD completa, objetiva e acionável para o projeto **CortaAi**, respeitando rigorosamente as regras abaixo.

### Contexto obrigatório

- Sistema multi-tenant para marketplace/gestão de barbearias.
- Arquitetura 100% microsserviços.
- Backend: Java 17+, Spring Boot 3.x, Feign (consulta), RabbitMQ (mutações cross-service), Redis, MySQL/PostgreSQL.
- Gateway é ponto único de validação do token Firebase e injeção de headers confiáveis.
- Frontend: React + Vite + CSS Modules + Axios wrapper único.
- Existem 3 perfis de experiência no frontend: `cliente`, `barbeiro` e `barbeiro owner`.

### Restrições inegociáveis

1. **Não quebrar o que funciona hoje**.
2. Não propor troca de stack.
3. Não expor entidade JPA em controller.
4. Não criar comunicação cross-service fora de Feign/RabbitMQ.
5. Não introduzir mudanças big-bang.

### Objetivo da SDD

Gerar plano técnico para evoluir o sistema em 5 frentes:

1. Redução de duplicação no frontend sem regressão.
2. Evolução visual/layout premium orientada ao usuário final (site moderno e elegante).
3. Refinamento UI/UX (responsividade + animações acessíveis).
4. Endurecimento de segurança de dados (gateway, webhook, segredos, sessão).
5. Introdução progressiva de PWA com cache seguro para domínio transacional.

### Estrutura obrigatória da resposta

Responda exatamente com as seções abaixo:

1. **Resumo executivo (1 página)**
2. **Escopo e não-escopo**
3. **Domínios impactados** (frontend, gateway, schedule, payment, user)
4. **Arquitetura alvo incremental** (AS-IS, TO-BE, transição)
5. **Contratos e compatibilidade**
   - APIs que não podem quebrar
   - DTOs/eventos adicionados
   - estratégia de versionamento
6. **Design por frente de trabalho**
   - Deduplicação frontend (services/hooks/facades)
   - Visual/layout system (grid, templates de página, tipografia, hierarquia visual, componentes base)
   - UI/UX system (tokens, breakpoints, motion, acessibilidade)
   - Diferenças de experiência por perfil (cliente/barbeiro/barbeiro owner)
   - Segurança (ameaças, controles, mitigação)
   - PWA (manifest, SW, cache strategy, update)
7. **Plano faseado de implementação** (fases pequenas + rollback)
8. **Plano de testes e quality gates**
   - Build/lint/test/unit/integration/smoke
   - critérios de aceite por fase
9. **Observabilidade e operação**
   - logs, métricas, alertas, auditoria
10. **Riscos e mitigação**
11. **Backlog técnico priorizado** (P1/P2/P3)
12. **Checklist final de não regressão**

### Requisitos de qualidade da SDD

- Ser específica em arquivos/camadas quando possível.
- Incluir decisões com trade-offs (por que esta opção, por que não outra).
- Incluir pelo menos 5 edge cases críticos.
- Incluir tabela de impacto x risco x esforço.
- Incluir plano de rollout e rollback por fase.
- Incluir “Definition of Done” por frente.
- Incluir critérios objetivos de qualidade visual para usuário final (clareza de CTA, consistência de layout, estados de loading/erro/vazio, experiência mobile).
- Incluir backlog visual priorizado (quick wins + evolução estrutural).
- Incluir seção “Impacto percebido pelo usuário final (antes x depois)”.
- Incluir uma matriz **Funcionalidade x Perfil** e uma matriz **Navegação x Perfil** para `cliente`, `barbeiro` e `barbeiro owner`.

### Formato de saída

- Português brasileiro, técnico e direto.
- Markdown bem estruturado.
- Sem explicações genéricas de conceitos básicos.
- Foco em decisões implementáveis pela squad atual.

Agora gere a SDD completa.

---

## Como usar internamente no CortaAi

1. Execute este prompt com contexto do branch atual.
2. Revise seções 5, 7, 8 e 12 com Tech Lead + QA antes de codar.
3. Converter cada fase em épicos e PRs pequenos.
4. Só avançar de fase com quality gates verdes.
