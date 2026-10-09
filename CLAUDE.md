# Bolso

Sistema de finanças pessoais e PJ (MEI -> ME em jan/2027) do João Pedro. Dois espaços (Casa / Empresa) trocados por menu. Substitui o bloco de notas mensal.

Contexto completo: `docs/CONTEXTO.md`. Requisitos, banco e arquitetura: `docs/*.md`. Telas: `design/`.

## Estrutura
- `backend/`  Java 25 + Spring Boot 4.1, monólito modular (Spring Modulith) + hexagonal. Detalhes em `backend/CLAUDE.md`
- `frontend/` React 19 + TypeScript strict + Vite, organizado por feature. Detalhes em `frontend/CLAUDE.md`
- `docs/`     requisitos, modelagem, arquiteturas, `specs/` (uma spec por feature)
- `design/`   canvas de telas (.dc.html) e paleta
- `infra/`    scripts do compose (backup)
- `docker-compose.yml` sobe db, api, web e backup (`.env` a partir de `.env.example`)

## Regras que não mudam
- Fluxo SDD: spec em `docs/specs/NNN-nome.md` ANTES do código. Sem spec aprovada, não codar.
- Uma feature por conversa/sessão. Contexto limpo a cada fase.
- Módulos do back falam só pela `<modulo>Api`. `ApplicationModules.verify()` + ArchUnit precisam passar.
- Dinheiro: `numeric(14,2)` no banco, string na API, centavos no front. Nunca float.
- Migrações só com Flyway, nunca editar migração já aplicada.
- Nada de dado financeiro real em teste, log ou prompt de IA.
- Quem confere o código não é quem escreveu (subagente `revisor`, somente leitura). Merge é humano.

## Antes de dar uma tarefa como pronta
- Back: `cd backend && ./mvnw verify` (Windows: `mvnw.cmd verify`; precisa do Docker aberto)
- Front: `cd frontend && npm test && npm run lint && npm run typecheck && npm run build`
- Depois, pedir revisão ao subagente `revisor`.

## Armadilhas conhecidas (já aconteceram)
- **Repositório público.** Nada de valor financeiro real, cidade ou nome de família em `docs/`, `design/`, testes ou prompts: só exemplos fictícios. Dados reais ficam no banco local, no `.env` e em `backups/` (todos fora do Git).
- **Node e npm iguais na máquina e no Docker** (Node 24 LTS / npm 11, `node:24-alpine`). Um `package-lock.json` gerado por outra versão do npm quebra o `npm ci` do Docker (dependência opcional do Vitest).
- **Terminal do IDE é o bash do MSYS2 (Windows).** Maven: `cmd //c mvnw.cmd verify` (`.\mvnw.cmd` não funciona no bash). Comandos de PowerShell, só numa janela de PowerShell.
- **Depois de instalar Java ou Node, reabrir o IDE inteiro** (o PATH novo só vale assim).
- **Sessões do Claude na nuvem não alcançam o Maven Central.** Mudanças no backend só estão confirmadas depois do `mvnw verify` na máquina do João Pedro: deixar isso como pendência na spec.
- **Pasta conectada sem permissão de apagar:** não rodar comandos git de escrita dali (deixam `.git/index.lock`). Só leitura (`git status`, `git ls-files`); commit é do João Pedro.
- **Gravar em `.claude/` e `.mvn/`** do computador só funciona pelo shell (as ferramentas de cópia recusam essas pastas).
