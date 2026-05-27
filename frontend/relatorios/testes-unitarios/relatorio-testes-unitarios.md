# Relatório de Testes Unitários do Front-end

**Projeto:** CortaAi Frontend  
**Tecnologia:** React 18 + Vite  
**Data:** 21/05/2026

## Objetivo

Registrar o estado atual da estratégia de testes unitários no front-end e indicar os próximos passos mínimos para a implementação de uma suíte automatizada.

## Diagnóstico atual

Após a análise da estrutura do front-end, o cenário atual é este:

- Não existe script de teste configurado no `package.json`.
- Não foram encontradas dependências de testes como Vitest, Jest ou Testing Library.
- Não há arquivos de teste identificados no front-end, como `*.test.jsx`, `*.spec.jsx` ou equivalentes.
- O projeto mantém apenas os scripts de desenvolvimento, build, lint e preview.

## Conclusão

No estado atual, o front-end não possui uma suíte de testes unitários automatizada configurada. Isso significa que a validação de comportamento depende principalmente de execução manual, lint e build.

## Riscos identificados

- Maior chance de regressão em páginas com lógica de formulário, validação e transformação de dados.
- Falta de proteção automática para componentes que consomem serviços e estados locais.
- Dificuldade para evoluir telas críticas sem uma base de testes de regressão.

## Recomendações

- Adicionar Vitest como runner de testes do front-end.
- Adicionar React Testing Library para testar componentes e interações de usuário.
- Criar testes para camadas com mais lógica, principalmente:
  - componentes reutilizáveis
  - serviços em `src/services`
  - funções utilitárias em `src/utils`
  - formulários e validações em páginas críticas
- Incluir um script `test` no `package.json`.
- Definir um padrão de cobertura mínima para novas entregas.

## Sugestão de priorização

1. Configurar a base de testes.
2. Cobrir funções puras e utilitários.
3. Cobrir serviços HTTP do front-end.
4. Cobrir componentes com comportamento relevante.
5. Evoluir depois para testes de integração da interface.

## Observação final

Este relatório descreve o estado real do front-end no momento da análise. Se a suíte de testes for adicionada depois, este documento pode ser atualizado com a lista de arquivos testados, cobertura e resultado das execuções.
