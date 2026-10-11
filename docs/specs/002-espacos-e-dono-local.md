# 002 · Espaços e dono local

Status: **concluída** (09/10/2026) · 09/10/2026 · João Pedro

## Objetivo

Os dois espaços, Casa e Empresa, passam a existir no banco e na API, e o seletor do front deixa de ser fixo e lê do servidor. Todo dado das próximas features já nasce pertencendo a um espaço e, por ele, a um dono. Não há login: o dono é um usuário local fixo, criado junto com o banco. Como a API aceita qualquer chamada como sendo do dono, as portas só abrem em `127.0.0.1`.

Esta feature entrega a fundação de dono e espaço. Nenhum dado financeiro entra aqui.

## Requisitos cobertos

- RF19 (parcial): os dois espaços existem e são trocados por menu. O repasse da Empresa para a Casa fica para a 006.
- RNF02 (base): a API só devolve espaços dos quais o usuário atual é membro, e espaço de outro usuário responde 404. A regra vale a partir de agora para toda rota `/api/v1/espacos/{espacoId}/...`.
- RNF01: `docker compose up` continua subindo os 4 serviços, agora só acessíveis nesta máquina.

Fora desta feature: RF08 e RNF03 (login), que seguem para a 007.

## Decisões desta spec

1. **Dono local é uma porta, não um atalho espalhado.** O módulo `identidade` define a porta de saída `UsuarioAtual` (devolve o id do usuário que está chamando). Nesta feature o adaptador devolve sempre o dono local. A 007 troca só esse adaptador por um que lê a sessão; nenhum caso de uso, controller ou tabela muda.
2. **Sem Spring Security.** A decisão 7 da 001 previa a segurança na 002 junto com o login. Com o login indo para o fim, ela vai junto para a 007. A proteção até lá é a rede: portas em `127.0.0.1`.
3. **Só as tabelas necessárias agora.** `V2` cria `identity.app_user`, `identity.workspace` e `identity.workspace_member`. `auth_identity`, `email_login_code` e `refresh_token` nascem na 007.
4. **Dono e espaços iniciais por migração (`V3`), com ids fixos.** É a forma mais simples de garantir "criados ao subir" sem janela em que a API já responde mas os espaços ainda não existem, e roda uma vez só. É a única exceção à regra "UUID v7 gerado pela aplicação"; o gerador de UUID v7 entra com a primeira feature que grava dados (003).
5. **Valores em inglês no banco e na API** (`HOME`, `COMPANY`, `OWNER`), como no resto da modelagem. O front traduz para `casa`/`empresa` (URL) e usa o `nome` vindo da API como rótulo.
6. **Um espaço de cada tipo por usuário nesta fase.** A URL `/casa` e `/empresa` resolve o espaço do usuário atual pelo tipo. Se o compartilhamento (RF12) criar mais de um da mesma espécie, a URL passa a levar o id; a mudança fica para aquela spec.
7. **Leituras com `JdbcClient`**, como manda o CLAUDE.md. Sem entidade JPA nesta feature, porque nada é gravado pela API.
8. **Row Level Security fica para a 003**, quando surgir a primeira tabela com `workspace_id`. Aqui não há dado a proteger além dos próprios espaços.

## Contrato da API

Prefixo `/api/v1`. Erros em `application/problem+json` (RFC 9457).

`GET /api/v1/espacos`: espaços do usuário atual, Casa primeiro, depois Empresa.

```json
[
  { "id": "019a0000-0000-7000-8000-000000000011", "tipo": "HOME", "nome": "Casa", "papel": "OWNER" },
  { "id": "019a0000-0000-7000-8000-000000000012", "tipo": "COMPANY", "nome": "Empresa", "papel": "OWNER" }
]
```

`GET /api/v1/espacos/{id}`: um espaço. `200` com o mesmo objeto; `404` se não existe **ou** se o usuário atual não é membro (não revelar que existe); `400` se o id não for um UUID.

Os nomes de campo ficam em português (`tipo`, `nome`, `papel`) e os valores dos enums em inglês, como na decisão 5.

## Backend

Módulo `identidade`:

```
identidade/
  IdentidadeApi.java                      porta dos outros módulos: usuarioAtualId(), papelNoEspaco(espacoId)
  domain/        TipoEspaco, Papel, Espaco, UsuarioId, EspacoNaoEncontrado
  application/   ConsultarEspacos (casos de uso), portas: EspacosRepository, UsuarioAtual
  adapter/in/web/        EspacosController, TratadorDeErros, EspacoResponse
  adapter/out/persistence/  JdbcEspacosRepository
  adapter/out/local/        DonoLocal (implementa UsuarioAtual com o id fixo)
```

- `ConsultarEspacos` ordena por `TipoEspaco` (HOME antes de COMPANY), regra em Java e não em SQL.
- `IdentidadeApi.papelNoEspaco(espacoId)` devolve o papel ou lança `EspacoNaoEncontrado`. É o guarda que as features 003 em diante chamam antes de tocar em qualquer dado de um espaço.
- Nenhuma classe de outro módulo é alterada.

## Banco e migrações

`V2__identidade_usuario_e_espacos.sql`

| Tabela | Colunas |
| --- | --- |
| `identity.app_user` | `id uuid PK`, `email citext NOT NULL UNIQUE`, `email_verified_at timestamptz`, `display_name text NOT NULL`, `status text NOT NULL CHECK (status IN ('ACTIVE','DISABLED'))`, `created_at timestamptz NOT NULL DEFAULT now()` |
| `identity.workspace` | `id uuid PK`, `kind text NOT NULL CHECK (kind IN ('HOME','COMPANY'))`, `name text NOT NULL`, `created_at timestamptz NOT NULL DEFAULT now()` |
| `identity.workspace_member` | PK (`workspace_id`, `user_id`), FKs `ON DELETE RESTRICT`, `role text NOT NULL CHECK (role IN ('OWNER','EDITOR','VIEWER'))`; índice em (`user_id`) |

`V3__dono_local_e_espacos_iniciais.sql`: insere o usuário local (`dono@bolso.local`, nome `Dono local`, `ACTIVE`, e-mail não verificado), os espaços `Casa` (HOME) e `Empresa` (COMPANY) e os dois vínculos `OWNER`. Ids fixos, no formato de UUID v7:

| O quê | Id |
| --- | --- |
| Dono local | `019a0000-0000-7000-8000-000000000001` |
| Casa | `019a0000-0000-7000-8000-000000000011` |
| Empresa | `019a0000-0000-7000-8000-000000000012` |

Na 007, o dono local reivindica a conta: o e-mail dele é trocado pelo e-mail verificado do login, e os espaços e vínculos ficam como estão. Por isso o e-mail inicial (`@bolso.local`) é só um marcador. Nada de dado real entra na migração (o repositório é público).

## Infra

`docker-compose.yml`:
- `api`: `127.0.0.1:8080:8080`
- `web`: `127.0.0.1:5173:80`
- `db` já está em `127.0.0.1:5432`.

Consequência: o app deixa de abrir pelo IP do computador na rede local (por exemplo, pelo celular). Para testar no celular, abra a porta do `web` à mão e só enquanto testa, sabendo que sem login qualquer pessoa da rede entra como dono.

## Frontend

- `features/identidade/api/useEspacos.ts`: hook TanStack Query para `GET /api/v1/espacos`, com os tipos vindos do `schema.d.ts`. Exportado pela fachada `index.ts`.
- `shared/lib/espacoAtual.ts`: contexto `EspacoAtual { id, tipo, nome }` e hook `useEspacoAtual()`. Fica em `shared` porque as features 003+ precisam do id do espaço e uma feature não pode importar outra. Quem preenche o contexto é o layout em `app`.
- `routes/$espaco/route.tsx` busca os espaços, acha o do tipo da URL (`casa` → HOME, `empresa` → COMPANY) e entrega ao `AppShell`. Estados:
  - carregando: aviso com `role="status"`;
  - erro de rede ou 5xx: mensagem com `role="alert"` e botão "Tentar de novo";
  - API respondeu mas o tipo não existe: mesma mensagem de erro (não é 404 de página).
- `SeletorEspaco` lista os espaços recebidos e mostra o `nome` da API. As URLs `/casa/...` e `/empresa/...` não mudam.
- `shared/api/schema.d.ts`: atualizado à mão para o contrato acima e confirmado com `npm run gen:api` na sua máquina (a diferença deve ser nenhuma, fora ordem ou comentário).

## Telas

Nenhuma nova. O shell e o seletor da 001 continuam iguais; ganham os estados de carregando e erro, no mesmo visual (fundo, borda e tipografia atuais).

## Critérios de aceite

**Backend (`cd backend && mvnw.cmd verify`, com o Docker aberto)**

1. `ModularityTest` e `ArquiteturaHexagonalTest` seguem verdes, com `identidade` com classes de verdade.
2. `MigracoesIT`: V2 e V3 aplicam do zero em PostgreSQL 18; as 3 tabelas existem; existe 1 usuário `ACTIVE`, 2 espaços (um `HOME`, um `COMPANY`) e 2 vínculos `OWNER` com os ids da tabela acima; inserir `kind` ou `role` fora da lista é rejeitado pelo banco.
3. `ConsultarEspacosTest` (unidade, portas falsas): devolve Casa antes de Empresa mesmo que a porta devolva o inverso; usuário sem vínculo recebe lista vazia.
4. `EspacosControllerIT` (MockMvc + Testcontainers, cada teste em transação que desfaz):
   - `GET /api/v1/espacos` → 200 com exatamente os dois espaços, na ordem Casa e Empresa, com `papel` `OWNER`;
   - `GET /api/v1/espacos/{id da Casa}` → 200;
   - `GET` de um id que não existe → 404 `problem+json`;
   - `GET` de um espaço criado no teste para **outro** usuário → 404, igual ao anterior;
   - `GET /api/v1/espacos/abc` → 400.
5. `DonoLocal` devolve `019a0000-0000-7000-8000-000000000001`, e esse usuário existe no banco (teste de integração).

**Frontend (`cd frontend && npm test && npm run lint && npm run typecheck && npm run build`)**

6. Com a API simulada (MSW), `/casa` mostra "Casa" no seletor, e abrir a lista mostra Casa e Empresa vindas da resposta.
7. Escolher Empresa no seletor leva a `/empresa`, e a página inicial da Empresa aparece.
8. Enquanto a requisição não responde, aparece o aviso de carregando; se responde 500, aparece o alerta com "Tentar de novo", e clicar nele refaz a chamada e mostra o shell.
9. Se a API devolve só a Casa, `/empresa` mostra o alerta de erro, e `/casa` segue funcionando.
10. `useEspacoAtual()` fora do layout lança erro claro; dentro, devolve o id do espaço da URL.
11. axe-core sem violações no shell, no carregando e no erro. As fronteiras do ESLint seguem verdes.

**Docker (na sua máquina)**

12. `docker compose up --build` sobe os 4 serviços; `http://localhost:5173/casa` mostra o shell com os espaços vindos da API; `GET http://localhost:8080/api/v1/espacos` responde os dois.
13. `docker compose ps` mostra `api` e `web` publicados em `127.0.0.1`, e o app não abre pelo IP da rede local.
14. Subir de novo com o volume existente não duplica usuário nem espaços.

## Fora de escopo

Login, sessão, Spring Security e CSRF (007); criar, renomear ou arquivar espaços; convites e papéis além de `OWNER` em uso (RF12); Row Level Security (003); gerador de UUID v7 (003); `auth_identity`, `email_login_code`, `refresh_token`; qualquer tela de configuração; Playwright.

## Riscos

- **Backend não compila nesta sessão** (Maven Central bloqueado): tudo do backend só está confirmado depois do `mvnw.cmd verify` na sua máquina. Fica como pendência na seção "Implementação".
- **Sem autenticação**, quem alcançar a porta é o dono. A trava é o `127.0.0.1`; o critério 13 existe para garantir isso.
- **Um espaço por tipo** (decisão 6) é uma simplificação consciente que o RF12 vai obrigar a rever.
- **Reivindicação do dono na 007**: se o login Google voltar um e-mail diferente do que o João Pedro espera, a conta local não é reaproveitada. A 007 decide o fluxo (por exemplo, confirmar o vínculo no primeiro login).

## Implementação (09/10/2026)

**Verificado na sessão**
- Front, numa cópia com npm 11 (igual ao Docker): `npm test` (50 testes, 12 novos), `lint`, `typecheck`, `build` e Prettier passando. Cobre os critérios 6 a 11.
- `V1` a `V3` aplicadas em sequência num PostgreSQL 16 real (o 18 não estava disponível aqui): sobem sem erro, o dono local e os dois espaços ficam com os ids da tabela, e o banco rejeita `kind` e `role` fora da lista, e-mail repetido com outra caixa e a exclusão de usuário que tem espaço. O SQL do repositório devolve o espaço certo.
- Núcleo do backend (domínio, casos de uso, `IdentidadeApi`) compilado com `javac` e a lógica do `ConsultarEspacosTest` executada à mão (Casa antes de Empresa, lista vazia sem vínculo, 404 para espaço alheio ou inexistente). Os demais arquivos Java só tiveram a sintaxe conferida.

**Verificado na máquina do João Pedro em 09/10/2026** (Maven Central bloqueado nas sessões; ele confirmou que ficou tudo certo)
- [x] `cd backend && mvnw.cmd verify` com o Docker aberto: critérios 1 a 5. Pontos de maior risco: o Boot 4.1 aceitar o `ResponseEntityExceptionHandler` do jeito escrito, o MockMvc montado com `webAppContextSetup` e o `JdbcClient` lendo `UUID`.
- [x] `docker compose up --build` e os critérios 12 a 14. Para o 14, suba de novo sem apagar o volume. Para o 13, `docker compose ps` deve mostrar `127.0.0.1:8080` e `127.0.0.1:5173`.
- [x] `cd frontend && npm run gen:api` com a API no ar: o `schema.d.ts` escrito à mão deve ficar igual ao gerado (ordem e comentários à parte). Se vier `?` em algum campo, falta o `@NotNull` do `EspacoResponse` fazer efeito no springdoc.
- [x] Pedir a revisão do subagente `revisor`.

**Mudanças em relação ao texto acima**
- `Papel` e `EspacoNaoEncontrado` ficam na **raiz** do módulo, junto de `IdentidadeApi`, em vez de em `domain`: são a linguagem publicada, e assim o Modulith deixa as outras features usarem o guarda sem enxergar o domínio. O `domain` importa `Papel` da raiz.
- `TratadorDeErros` estende `ResponseEntityExceptionHandler`, então os erros padrão do MVC (por exemplo id que não é UUID) também saem em `problem+json`. Como é um `@RestControllerAdvice`, o 404 de `EspacoNaoEncontrado` vale para todos os módulos.
- O controller declara `produces = application/json` e `EspacoResponse` usa `@NotNull` nos campos, para o OpenAPI sair com `application/json` e campos obrigatórios (tipos do front sem `?`).
- O cliente da API (`shared/api/client.ts`) passou a usar a origem da página como URL base e a ler o `fetch` a cada chamada: sem isso o MSW dos testes não intercepta, e o `fetch` do Node não aceita URL relativa.
- O servidor MSW dos testes já nasce respondendo `/api/v1/espacos` (`src/test/msw/handlers.ts`), com os ids fixos da V3.
- Estado de erro do layout: só aparece sem dados em cache; se a revalidação falhar com a lista já carregada, a tela em uso continua. Durante "Tentar de novo" o botão mostra "Tentando…".
- O critério 10 ("dentro do layout devolve o id do espaço da URL") ficou dividido em dois testes: `useEspacoAtual` com e sem contexto, e `acharEspaco`, que liga o trecho da URL ao id vindo da API.
- Favicon provisório (`frontend/public/favicon.svg`, um "B" na cor primária) e o `<link rel="icon">` no `index.html`. Sem ele, o navegador reaproveitava o ícone de outro projeto que já usou a porta 5173. Não está nos critérios; o logo de verdade fica para quando houver um.
