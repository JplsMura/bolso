# 001 · Base do projeto

Status: **concluída** (08/10/2026) · 08/10/2026 · João Pedro

## Objetivo

Deixar o repositório pronto para as features 002 a 006: backend e frontend sobem, os testes de arquitetura rodam e travam o build, o banco é criado só pelo Flyway, e tudo sobe com um único `docker compose up`. Nenhuma regra de negócio entra aqui.

## Requisitos cobertos

- RNF01: `docker compose up` sobe banco, API, front e backup.
- RNF04: base para dinheiro (utilitário de centavos no front; `numeric(14,2)` fica para as migrações de cada feature).
- RNF05: Flyway desde a primeira migração, aplicada do zero em teste.
- RNF06: backup diário do banco em `./backups` (versão simples; restauração documentada no README).
- RNF07: testes do back e do front rodam com um comando cada.
- RNF10: OpenAPI publicado pela API e script que gera os tipos no front.
- RNF11: segredos só no `.env` (ignorado); `.env.example` versionado.

## Versões

Conferidas em 08/10/2026. As do backend não foram compiladas aqui (a rede da sessão bloqueia o Maven Central): a confirmação vem do primeiro `./mvnw verify` na sua máquina.

| Peça | Versão | Observação |
| --- | --- | --- |
| Java | 25 (LTS) | imagem `eclipse-temurin:25` |
| Spring Boot | 4.1.1 | GA mais recente (20/08/2026) |
| Spring Modulith | 2.1.1 | linha 2.1 saiu junto com o Boot 4.1; a 2.2 já vai para o Boot 4.2 |
| springdoc-openapi | 3.1.1 | a 3.1.x é a linha do Boot 4.1 |
| ArchUnit | 1.4.2 | |
| Testcontainers | gerenciado pelo Boot | linha 2.x: artefatos `testcontainers-postgresql`, `testcontainers-junit-jupiter` |
| PostgreSQL | 18 | imagem `postgres:18-alpine` |
| Maven | 3.9.11 via wrapper (`mvnw`) | |
| Node | 24 LTS | npm 11; o lock gerado pelo npm 11 não serve para o npm 10 (dependência opcional do Vitest) |
| React | 19.3 | |
| Vite | 8.3 | |
| TypeScript | 5.9 | a 6 e a 7 já saíram, mas o openapi-typescript ainda exige a 5 (e o typescript-eslint vai até a 6.0) |
| Tailwind CSS | 4.3 | plugin do Vite, tokens em `@theme` |
| TanStack Router / Query | 1.170 / 5.104 | Router com rotas por arquivo |
| React Hook Form / Zod | 7.89 / 4.6 | instalados agora, usados a partir da 002 |
| Vitest / Testing Library / MSW | 5.0 / 16.3 / 3.0 | |
| ESLint | 10 + `eslint-plugin-boundaries` 7 | fronteiras entre features |

## Decisões desta spec

1. **Maven** com wrapper. O CLAUDE.md já cita `./mvnw verify`.
2. **Pacote base `dev.bolso`**: um pacote por módulo (`dev.bolso.identidade`, `lancamentos`, `cartoes`, `orcamento`, `faturamento`), e dentro de cada um `domain`, `application`, `adapter/in/web` e `adapter/out/persistence`, que nascem vazios.
3. **Módulos vazios, mas declarados**: cada um tem um `package-info.java` com `@ApplicationModule`. A interface `<Modulo>Api` nasce com a primeira feature que precisar dela, não antes.
4. **Regras ArchUnit** (com `allowEmptyShould`, porque os pacotes nascem vazios):
   - `..domain..` não depende de `org.springframework..`, de `..application..` nem de `..adapter..`;
   - `..application..` não depende de `..adapter..`;
   - `..adapter.in..` e `..adapter.out..` não dependem um do outro.
   Se o domínio pode ou não ter anotações JPA (`jakarta.persistence`) fica para a spec 003, que cria o primeiro agregado.
5. **Flyway `V1__schemas.sql`**: cria os schemas `identity`, `finance`, `billing`, `planning` e `platform`, mais as extensões `citext` e `btree_gist`. As tabelas entram nas migrações de cada feature (V2 em diante), o que ajusta a numeração `V1__identity…V4__platform` da modelagem.
6. **Hibernate em `validate`**: o banco é só do Flyway. `open-in-view` desligado. Fuso UTC no JDBC; a conversão para America/Sao_Paulo é feita na borda.
7. **Sem Spring Security nesta feature**: entra na 002 junto com o login. Até lá a API só expõe `/actuator/health` e `/v3/api-docs`.
8. **Front em shell vazio**: layout com seletor Casa/Empresa no endereço (`/casa`, `/empresa`), menu do espaço com itens desabilitados, página inicial de cada espaço com estado "em construção", 404. Visual com a paleta escura e a fonte DM Sans (pacote local, sem CDN).
9. **Utilitário de dinheiro** em `shared/lib/money.ts`: de string da API (`"110.00"`) para centavos inteiros, e de centavos para `R$ 110,00`. É a primeira regra com teste e vale para todas as features.
10. **Fronteiras no front** (`eslint-plugin-boundaries`):
    - uma feature não importa outra;
    - `shared` não importa `features` nem `app`;
    - `ui` e `pages` de uma feature não importam `shared/api` (passam pelos hooks da pasta `api` da feature).
11. **Revisor**: `.claude/agents/revisor.md`, subagente somente leitura que confere as regras de arquitetura e a spec. Skills (`nova-spec`, `novo-modulo`, `nova-migracao`) e hooks ficam para quando o passo a passo se repetir.

## Contrato da API

Nenhum endpoint de negócio. Ficam expostos:

- `GET /actuator/health`: `{"status":"UP"}` com banco conectado;
- `GET /v3/api-docs`: contrato OpenAPI, de onde sai `npm run gen:api` → `src/shared/api/schema.d.ts`.

Prefixo reservado para as features: `/api/v1/...`. Em dev, o Vite repassa `/api`, `/actuator` e `/v3` para `localhost:8080`; no Docker, quem repassa é o nginx do front.

## Banco e migrações

- `V1__schemas.sql`: os 5 schemas e as extensões `citext` e `btree_gist`.
- Teste de integração (Testcontainers) sobe um PostgreSQL 18 vazio, aplica tudo e confere que os 5 schemas existem.

## Estrutura de arquivos

```
docker-compose.yml        db, api, web, backup
.env.example
backend/
  pom.xml, mvnw, .mvn/
  Dockerfile
  src/main/java/dev/bolso/BolsoApplication.java
  src/main/java/dev/bolso/{identidade,lancamentos,cartoes,orcamento,faturamento}/package-info.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__schemas.sql
  src/test/java/dev/bolso/{ModularityTest,ArquiteturaHexagonalTest,MigracoesIT,TestcontainersConfig}.java
frontend/
  package.json, vite.config.ts, tsconfig*.json, eslint.config.js, Dockerfile, nginx.conf
  src/app/            router, providers, layout com seletor Casa/Empresa
  src/routes/         rotas por arquivo (TanStack Router)
  src/features/{identidade,lancamentos,cartoes,orcamento,faturamento}/index.ts
  src/shared/{ui,api,lib}/
  src/test/           setup do Vitest, MSW e axe
```

## Telas

Shell do `design/project/Main.dc.html` (visual base): fundo, superfície, tipografia, seletor de espaço e menu. Nenhuma tela de negócio.

## Critérios de aceite

1. `cd backend && ./mvnw verify` passa, incluindo:
   - `ApplicationModules.of(BolsoApplication.class).verify()` verde e listando os 5 módulos;
   - as regras ArchUnit verdes;
   - `MigracoesIT`: Flyway aplica a V1 num PostgreSQL 18 vazio e os 5 schemas existem.
2. Se uma classe de `lancamentos` importar um pacote interno de `cartoes`, o `verify()` falha. Isso se confere com um teste temporário que não vai para o commit.
3. `cd frontend && npm test && npm run lint && npm run typecheck && npm run build` passam.
4. O lint falha se `features/lancamentos` importar de `features/cartoes`, ou se `shared` importar de `features`.
5. `money.ts` tem testes: `"110.00"` → `11000`, `"0.10"` → `10`, `"1234567.89"` → `123456789`, entrada inválida lança erro, `11000` → `R$ 110,00`, e nenhum cálculo passa por `parseFloat`.
6. O teste do layout troca entre Casa e Empresa pelo seletor, a URL muda, e o axe-core não aponta violações.
7. `docker compose up --build` sobe os 4 serviços; `http://localhost:5173` mostra o shell e `http://localhost:8080/actuator/health` responde `UP`.
8. O serviço de backup grava `backups/bolso-AAAA-MM-DD.sql.gz` ao subir e a cada 24 h, e mantém 30 dias.
9. O `.env` não é versionado e o `.env.example` lista todas as variáveis.
10. Os CLAUDE.md (raiz, backend e frontend) trazem os comandos exatos.

## Fora de escopo

Login e segurança (002), qualquer tabela de negócio, Playwright (entra com o primeiro fluxo completo), CI em nuvem, Spotless, skills e hooks do Claude, modo claro e telas de estado vazio de verdade.

## Riscos

- O Boot 4.1 é recente e o Maven não roda nesta sessão: se `./mvnw verify` falhar por versão, o ajuste entra no `pom.xml` antes de seguir.
- O Testcontainers precisa do Docker Desktop rodando no Windows.

## Implementação (08/10/2026)

**Verificado na sessão:**
- Front: `npm test` (38 testes), `lint`, `typecheck`, `build` e Prettier passando. Inclui teste automático das fronteiras do ESLint e da proibição de `parseFloat`.
- `V1__schemas.sql` aplicada num PostgreSQL 18.4 real; pode ser reaplicada sem erro.
- `backup.sh`: grava o dump, detecta falha do `pg_dump` (antes uma falha no pipe passava como "ok") e mantém exatamente N dias.
- Revisão do subagente revisor: aprovada sem bloqueios. Os itens "ajustar" foram aplicados.

**Mudanças em relação ao texto acima:**
- TypeScript 5.9, porque o `openapi-typescript` ainda exige o 5.
- springdoc `-webmvc-api` (sem Swagger UI) e só `health` exposto no actuator, como na decisão 7.
- `ArquiteturaHexagonalMordeTest` prova, com classes de exemplo em `src/test/java/fixtures`, que as regras ArchUnit pegam violações.
- Arquivos `*.test.ts(x)` podem importar `src/test`; código de produção não.
- Wrapper do Maven escrito à mão (`mvnw`, `mvnw.cmd` e `.mvn/wrapper/baixar-maven.ps1`), porque o Maven Central estava bloqueado na sessão. Dá para trocar pelo oficial com `mvn -N wrapper:wrapper`.

**Verificado na máquina do João Pedro (Windows, Docker Desktop):**
- [x] `cd backend && mvnw.cmd verify` com o Docker Desktop aberto: confirma Boot 4.1.1, Modulith 2.1.1, Testcontainers 2 e a detecção dos 5 módulos vazios (o ponto de maior risco).
- [x] Critério 2: teste temporário de violação entre módulos (o Modulith acusou `lancamentos` → `cartoes.internal`).
- [x] `cd frontend && npm install && npm test` (38 testes), lint, typecheck e build.
- [x] `docker compose up --build` (critérios 7 e 8): 4 serviços no ar, shell em localhost:5173, backup `bolso-2026-10-08.sql.gz` com os 5 schemas.
