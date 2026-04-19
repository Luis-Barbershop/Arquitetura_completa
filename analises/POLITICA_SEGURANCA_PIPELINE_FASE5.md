# Política de Segurança no Pipeline — Fase 5

> Projeto: CortaAi  
> Data: 19/04/2026  
> Escopo: `PR-F5-01` e `PR-F5-02`

---

## 1) Objetivo

Automatizar verificação de vulnerabilidades de dependências no CI para bloquear merge com risco elevado.

---

## 2) Regra de bloqueio

O pipeline deve **falhar** quando houver vulnerabilidade de dependência com severidade:

- `CRITICAL`
- `HIGH`

A verificação é feita no workflow:

- `.github/workflows/security-dependency-scan.yml`

---

## 3) Cobertura

A varredura considera manifests e lockfiles do repositório, cobrindo:

- Frontend (`frontend/package.json`, `frontend/package-lock.json`)
- Backend Maven (`backend/**/pom.xml`)

Arquivos de build gerados (`target`, `dist`) ficam fora do escopo.

---

## 4) Execução no CI

Disparos:

- `pull_request` (qualquer branch)
- `push` em `main` e `feature/**`
- `workflow_dispatch`

Resultados:

1. Job de política (`dependency-scan-policy`) com `exit-code: 1` para `HIGH/CRITICAL`.
2. Job de relatório (`dependency-scan-report`) com upload de SARIF para aba Security.

---

## 5) Tratamento de achados

### Quando houver bloqueio

1. Confirmar pacote/versão afetados.
2. Atualizar dependência para versão corrigida.
3. Reexecutar pipeline.

### Exceção temporária (somente com aprovação)

Aceita apenas quando:

- não há versão corrigida disponível; e
- há plano de mitigação com prazo.

A exceção deve ser registrada em PR com:

- dependência impactada;
- justificativa técnica;
- prazo máximo para correção;
- responsável.

---

## 6) Critério de sucesso da Fase 5 (parcial)

- Pipeline com status de segurança visível.
- Merge bloqueado para `HIGH/CRITICAL` de dependências.
- Política documentada e versionada no repositório.
