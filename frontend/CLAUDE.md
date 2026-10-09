# Front-end (Bolso)

- Node 24 LTS (npm 11, igual ao Docker). React 19, TypeScript 5.9 strict, Vite 8, Tailwind 4 (tokens em `src/index.css`, `@theme`). Sem micro-frontend.
- TanStack Router (rotas por arquivo em `src/routes`, `routeTree.gen.ts` é gerado e versionado) + TanStack Query. React Hook Form + Zod.
- `src/app` (router, providers, layout), `src/routes`, `src/features/{identidade,lancamentos,cartoes,orcamento,faturamento}/{api,model,ui,pages}` + `index.ts` (fachada), `src/shared/{ui,api,lib}`.
- Fronteiras (ESLint `boundaries/dependencies`, testadas em `src/test/fronteiras.test.ts`):
  - feature não importa outra feature; `app` e `routes` só usam a fachada `index.ts`;
  - `shared` não importa `features` nem `app`;
  - `ui`/`pages` não importam `shared/api`: passam pelos hooks de `api/` da própria feature.
- Espaço na URL: `/casa/...` e `/empresa/...` (param `$espaco`). O layout busca os espaços na API (`useEspacos`, feature identidade) e põe o atual em contexto: as features pegam o id com `useEspacoAtual()` de `src/shared/lib/espacoAtual`.
- Cliente da API gerado do OpenAPI (`npm run gen:api` com a API rodando). Sessão por cookie HttpOnly; nada de token no localStorage.
- Dinheiro: `src/shared/lib/money.ts` (string da API ↔ centavos inteiros ↔ `R$`). `parseFloat` é barrado no lint.
- Componentes shadcn/ui caem em `src/shared/ui` (`npx shadcn add <componente>`).

## Comandos (rodar dentro de `frontend/`)

- `npm test` · `npm run lint` · `npm run typecheck` · `npm run build`
- `npm run dev` (porta 5173; repassa `/api`, `/actuator` e `/v3` para `localhost:8080`)

## Testes

- Arquivos `*.test.ts(x)` em qualquer pasta podem importar `@/test/*`; código de produção não.
- Vitest + Testing Library + MSW (`src/test/msw/server.ts` já responde `/api/v1/espacos`, ids em `handlers.ts`; sobrescreva com `server.use(...)`; requisição sem handler falha o teste) + axe-core (`src/test/axe.ts`).
- App inteiro numa URL: `renderizarRota('/casa')` de `src/test/renderizarRota.tsx`.
- Testar o que o usuário vê (papéis e textos), não detalhes internos.
