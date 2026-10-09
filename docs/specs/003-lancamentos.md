# 003 · Lançamentos

Status: **implementada, aguardando verificação na máquina** (aprovada em 09/10/2026) · 09/10/2026 · João Pedro

## Objetivo

Lançar entradas e saídas, ver o mês, filtrar, buscar, editar e excluir. É a primeira feature que grava dado financeiro, então ela também fixa as bases das próximas: gerador de UUID v7, dinheiro como valor no domínio, entidade JPA separada do domínio e o guarda de acesso ao espaço em todo endpoint.

Escopo escolhido: **só o núcleo**. Cartão, parcelas, presets, fatura e fechamento do mês ficam para 004 e seguintes.

## Requisitos cobertos

- RF01: lançar entradas e saídas do mês (a "natureza" fixo/variável/parcela/investimento sai pela meta e pelas tags, ver decisão 2).
- RF15 (parcial): lista do mês corrente, lançamento rápido, total gasto até agora. O painel da tela inicial é da 005.
- RF17 (parcial): cada gasto da Casa pertence a uma das 6 metas. Percentuais, previsto e restante ficam na 005.
- RF22 (parcial): tags livres, com tipo de custo fixo ou variável. O fechamento do mês é da 005.
- RF23 (parcial): forma de pagamento em todo gasto. Cartão cadastrado entra na 004; até lá a API recusa `CREDIT`.
- RNF02: todo endpoint passa pelo guarda `IdentidadeApi` e toda consulta filtra por `workspace_id`.
- RNF04: dinheiro em `numeric(14,2)`, `BigDecimal` no back, string na API, centavos no front.
- Pendência de requisitos: **busca por texto entra no MVP** (decidido nesta spec).

Fora desta feature: RF03, RF04, RF14, RF16, RF20, RF21, RF25 e RF13 (histórico por lançamento).

## Decisões desta spec

Confirmadas por você em 09/10/2026: escopo só o núcleo; metas por migração; busca no MVP; sem Row Level Security por enquanto (entra com o login, na 007).

Propostas por mim, para você confirmar ou trocar:

1. **O lançamento aponta direto para a meta, sem a tabela `category`.** A modelagem previa `category` dentro de cada meta, mas o design (NovoGasto) pede só Meta e Tag, e as tags já cobrem o detalhe (Luz, Alimentação, Uber). Menos uma tabela e uma tela. Se aprovar, atualizo `docs/02-modelagem-banco.md`.
2. **Sem coluna `nature`.** Fixo ou variável vem da tag (`cost_type`); parcela vem do parcelamento (004); investimento é a meta Liberdade Financeira.
3. **As 6 metas existem só na Casa.** A Empresa não usa metas do AUVP (o design tem Teto do MEI, Notas e Custos e repasse). Na Casa toda saída tem meta; na Empresa nenhum lançamento tem. Entrada nunca tem meta.
4. **Entrada não tem forma de pagamento; toda saída tem.** O banco garante com `CHECK`.
5. **Módulo `orcamento` mínimo**, dono da tabela `budget_goal`: só leitura das 6 metas (`GET /metas` e a porta `OrcamentoApi`). A 005 acrescenta percentuais e fechamento nele. Assim o `lancamentos` nunca lê tabela de outro módulo.
6. **Módulo `compartilhado` aberto** (`@ApplicationModule(type = OPEN)`), com `UuidV7` e o valor `Dinheiro`. Qualquer módulo pode usá-lo. O `ModularityTest` passa a esperar 6 módulos.
7. **UUID v7 escrito à mão** (48 bits de milissegundos, versão 7, variante 10, resto aleatório), com teste de versão, variante, ordem e unicidade. Evita uma dependência para 15 linhas.
8. **Domínio sem anotação JPA.** `domain` segue Java puro; a entidade JPA vive em `adapter/out/persistence` com um mapeador para o domínio. Custa um mapeador por agregado, mas deixa as regras testáveis sem Spring e fecha a pergunta da 001. Entra uma regra ArchUnit: `domain` não depende de `jakarta.persistence`.
9. **`IdentidadeApi` ganha `tipoDoEspaco(espacoId)`** (HOME ou COMPANY), e `TipoEspaco` sobe para a raiz do módulo, como `Papel`. É uma adição; nada da 002 muda.
10. **`CREDIT` já está na lista permitida do banco** (para a 004 não precisar alterar o `CHECK`), mas a API responde 422 enquanto não houver cartão.
11. **Exclusão lógica** (`deleted_at`), sem lixeira nem restauração por enquanto.
12. **Isolamento reforçado no banco:** chaves estrangeiras compostas `(id, workspace_id)` impedem que um lançamento use meta ou tag de outro espaço, mesmo que um bug passe pela aplicação.

## Regras de negócio

- **Valor:** maior que zero, até `999999999999.99`, sempre com 2 casas. Na API é string (`"110.00"`); `"110"`, `"110.5"` e `"-1.00"` são recusados no texto, e `"0.00"` é recusado pelo lançamento (não pelo `Dinheiro`, que aceita zero e negativos porque `sobra` e os totais precisam).
- **Descrição:** de 1 a 140 caracteres depois de aparar os espaços.
- **Data (`data`):** qualquer data. O `mesReferencia` (dia 1 do mês) é calculado pelo servidor a partir dela.
- **Direção:** `IN` (entrada) ou `OUT` (saída).
- **Saída na Casa:** meta obrigatória e forma de pagamento obrigatória (`PIX`, `DEBIT`, `CASH`, `BOLETO`, `TRANSFER`, `OTHER`). `CREDIT` é recusado até a 004.
- **Saída na Empresa:** forma de pagamento obrigatória, meta proibida.
- **Entrada:** sem meta e sem forma de pagamento, em qualquer espaço.
- **Tags:** de 0 a 10 por lançamento, do mesmo espaço. Nome de tag único no espaço sem diferenciar maiúsculas (`citext`), de 1 a 40 caracteres.
- **Permissões:** `OWNER` e `EDITOR` gravam; `VIEWER` só lê (403). Quem não é membro do espaço recebe 404, como na 002.
- **Edição concorrente:** o `PUT` leva a `versao` lida; se mudou, 409.
- **Totais do mês:** entradas, saídas e sobra (entradas menos saídas), calculados no banco sobre os lançamentos não excluídos do mês.
- **Busca:** o texto procura (sem diferenciar maiúsculas) na descrição e no nome das tags. `%` e `_` digitados valem como texto. Acento conta como letra diferente nesta versão.

## Contrato da API

Prefixo `/api/v1/espacos/{espacoId}`. Erros em `application/problem+json`: 400 formato inválido, 403 papel sem permissão, 404 espaço ou lançamento inexistente, 409 versão desatualizada, 422 regra de negócio.

| Método e rota | O que faz |
| --- | --- |
| `GET /lancamentos?mes=2026-10&q=&metaId=&tagId=&pagamento=&direcao=` | Lançamentos do mês, mais recentes primeiro, e os totais. `mes` ausente = mês atual em America/Sao_Paulo. Sem paginação (um mês cabe numa resposta). |
| `GET /lancamentos/{id}` | Um lançamento. |
| `POST /lancamentos` | Cria. 201 com `Location`. |
| `PUT /lancamentos/{id}` | Substitui os campos editáveis; exige `versao`. |
| `DELETE /lancamentos/{id}` | Exclusão lógica. 204. |
| `GET /tags` | Tags do espaço, ativas, por nome. |
| `POST /tags` | Cria tag (`nome`, `tipoCusto` `FIXED`, `VARIABLE` ou nulo). 201. 422 se o nome já existe. |
| `GET /metas` | As metas do espaço, na ordem de exibição (vazio na Empresa). |

Corpo de `POST` e `PUT` de lançamento:

```json
{ "direcao": "OUT", "valor": "110.00", "descricao": "Mercado", "data": "2026-10-08",
  "metaId": "019a0000-0000-7000-8000-000000000101", "formaPagamento": "PIX",
  "tagIds": ["..."], "versao": 3 }
```

(`versao` só no `PUT`.) Resposta de um lançamento: os mesmos campos, mais `id`, `mesReferencia` (`"2026-10"`), `tags` (`[{id, nome}]`) e `versao`. Resposta da lista:

```json
{ "mes": "2026-10",
  "totais": { "entradas": "5000.00", "saidas": "110.00", "sobra": "4890.00" },
  "itens": [ { "id": "...", "direcao": "OUT", "valor": "110.00", "descricao": "Mercado",
               "data": "2026-10-08", "metaId": "...", "formaPagamento": "PIX",
               "tags": [{ "id": "...", "nome": "Alimentação" }], "versao": 0 } ] }
```

Os valores desses exemplos são fictícios.

## Banco e migrações

`V4__financas_metas_tags_lancamentos.sql` (schema `finance`):

| Tabela | Colunas e restrições |
| --- | --- |
| `budget_goal` | `id uuid PK`, `workspace_id` FK `identity.workspace`, `name text`, `color text` (`#RRGGBB`), `sort_order int`, `UNIQUE (id, workspace_id)`, único por (`workspace_id`, `lower(name)`) |
| `tag` | `id uuid PK`, `workspace_id` FK, `name citext`, `cost_type text CHECK IN ('FIXED','VARIABLE')` nulo permitido, `archived_at timestamptz`, `UNIQUE (id, workspace_id)`, `UNIQUE (workspace_id, name)` |
| `transaction` | `id uuid PK`, `workspace_id` FK, `direction text CHECK IN ('IN','OUT')`, `amount numeric(14,2) CHECK (amount > 0)`, `description text`, `occurred_on date`, `reference_month date CHECK (dia 1)`, `goal_id uuid`, `payment_method text CHECK IN ('PIX','DEBIT','CREDIT','CASH','BOLETO','TRANSFER','OTHER')`, `created_at`, `updated_at`, `deleted_at`, `version bigint`; FK composta (`goal_id`, `workspace_id`) → `budget_goal (id, workspace_id)`; `CHECK (direction = 'OUT' OR goal_id IS NULL)`; `CHECK ((direction = 'OUT') = (payment_method IS NOT NULL))` |
| `transaction_tag` | PK (`transaction_id`, `tag_id`), `workspace_id`, FKs compostas para `transaction` e `tag` pelo `workspace_id` |

Índices (parciais em `deleted_at IS NULL` onde couber): (`workspace_id`, `reference_month`, `goal_id`) `INCLUDE (amount, direction)`; (`workspace_id`, `occurred_on` DESC); `transaction_tag (tag_id)`. Chaves estrangeiras com `ON DELETE RESTRICT`, exceto `transaction_tag → transaction`.

`V5__metas_iniciais.sql`: insere as 6 metas do espaço Casa com ids fixos (`019a0000-0000-7000-8000-000000000101` a `...106`), na ordem e com as cores da paleta: Custos Fixos `#6EA8FE`, Conforto `#A3E06B`, Metas `#C79BFF`, Prazeres `#FF9F5A`, Liberdade Financeira `#FF8FB1`, Conhecimento `#5CD6E8`. A Empresa não recebe metas (decisão 3).

## Backend

```
compartilhado/  package-info (OPEN), UuidV7, Dinheiro
orcamento/      OrcamentoApi (metaDoEspaco, metasDoEspaco) · domain Meta · application ConsultarMetas
                adapter/in/web MetasController · adapter/out/persistence JdbcMetasRepository
lancamentos/    domain: Lancamento (regras), Tag, Direcao, FormaPagamento, TotaisDoMes
                application: casos de uso (Lancar, Editar, Excluir, ConsultarMes, CriarTag, ListarTags)
                             e portas (LancamentosRepository, TagsRepository, Relogio)
                adapter/in/web: LancamentosController, TagsController, DTOs, TratadorDeErros
                adapter/out/persistence: entidades JPA + mapeador, repositórios JPA (gravação)
                                         e JdbcClient (lista do mês, totais, busca)
identidade/     IdentidadeApi.tipoDoEspaco(espacoId); TipoEspaco passa para a raiz
```

- Todo caso de uso começa chamando `IdentidadeApi.papelNoEspaco` (e `tipoDoEspaco` quando a regra depende dele).
- Gravação: agregado carregado, regra no domínio, salvo com JPA e `@Version`. Leitura da lista e dos totais: `JdbcClient`.
- `TratadorDeErros` mapeia as exceções de domínio para 403, 409 e 422; o 404 de espaço já existe (002).
- ArchUnit ganha a regra "domain não depende de `jakarta.persistence`".

## Frontend

- **Tela Lançamentos** (`/casa/lancamentos`), no visual de `design/project/LancamentosCelular.dc.html`: mês com setas, totais, campo de busca e filtros (todos, meta, tag, pagamento), lista agrupada por dia.
- **Novo gasto e nova entrada** (`/casa/lancamentos/novo`): formulário do design `NovoGasto.dc.html` (valor, descrição, meta, tag, forma de pagamento, data); entrada só com valor, descrição e data. Sem o campo de parcelas e sem o aviso de fatura (004). Tags podem ser criadas no próprio formulário.
- **Editar e excluir** (`/casa/lancamentos/$id`), com confirmação antes de excluir e mensagem clara no 409 ("alguém alterou este lançamento, recarregue").
- Estados: carregando, erro com "Tentar de novo", mês sem lançamentos.
- O item "Lançamentos" do menu da Casa passa a ser um link (o menu ganha um campo `rota` por item).
- `shared/lib/money.ts` ganha centavos → texto da API (`11000` → `"110.00"`), com testes.
- `useMetas` e `useTags` ficam em `features/lancamentos/api` (uma feature não importa outra; a 005 pode mover a lista de metas para `shared`).
- Tipos novos do `schema.d.ts`: o ideal é gerar com `npm run gen:api` depois da API no ar; nas sessões sem Maven foram escritos à mão no estilo do gerado e precisam ser conferidos (ver "Implementação").

## Critérios de aceite

**Backend (`mvnw.cmd verify`)**
1. `ModularityTest` (6 módulos) e `ArquiteturaHexagonalTest` (com a regra nova) verdes.
2. `UuidV7Test`: versão 7, variante correta, dois ids no mesmo milissegundo diferentes, ids gerados em milissegundos distintos ficam em ordem.
3. `DinheiroTest`: `"110.00"` e `"0.00"` valem, `"110"`, `"110.5"`, `"-1.00"` e `"1e3"` não (o valor positivo do lançamento é testado em `LancamentoTest`); soma e subtração mantêm 2 casas; nunca `double`.
4. Testes do domínio e dos casos de uso (portas falsas): cada regra da seção "Regras de negócio", incluindo meta obrigatória na Casa, proibida na Empresa e na entrada, `CREDIT` recusado, tag de outro espaço recusada, `VIEWER` recusado e versão desatualizada.
5. `MigracoesIT`: V4 e V5 aplicam do zero; existem as 6 metas da Casa e nenhuma na Empresa; o banco rejeita valor zero, direção e pagamento fora da lista, entrada com meta, saída sem pagamento e **meta ou tag de outro espaço** (FK composta).
6. `LancamentosControllerIT` (MockMvc e Testcontainers): criar (201 e `Location`), ler, editar com `versao` certa (200) e errada (409), excluir (204 e depois 404 no GET e fora da lista), lista do mês com totais certos, filtros por meta, tag, pagamento e direção, busca por descrição e por tag (inclusive `%` e `_` como texto), mês sem lançamentos (lista vazia e totais `"0.00"`), valor mal formado (400), regra de negócio (422), espaço de outro usuário e espaço inexistente (404), e `VIEWER` (403, com um vínculo criado no teste).
7. `TagsControllerIT`: criar, listar, nome repetido com outra caixa (422), tag ativa só do espaço.
8. `MetasControllerIT`: seis metas na Casa, na ordem e com as cores; lista vazia na Empresa.
9. `GET /v3/api-docs` traz os endpoints novos com `valor` como string e campos obrigatórios.

**Frontend (`npm test && npm run lint && npm run typecheck && npm run build`)**
10. `money.ts`: centavos → `"110.00"` e ida e volta com o texto da API, sem `parseFloat`.
11. Lista: mostra os itens do mês agrupados por dia, os totais, e o estado vazio; trocar de mês refaz a consulta; busca e filtros mandam os parâmetros certos (MSW confere a URL).
12. Criar gasto: o formulário valida (valor, descrição, meta e pagamento obrigatórios) e envia o valor como texto da API; criar tag no formulário; entrada sem meta e sem pagamento.
13. Editar manda a `versao`; o 409 mostra a mensagem de recarregar; excluir pede confirmação.
14. Erro de carregamento mostra o alerta com "Tentar de novo". axe-core sem violações na lista, no formulário e na confirmação de exclusão.
15. As fronteiras do ESLint seguem verdes (a feature `lancamentos` não importa outra).

**Docker (na sua máquina)**
16. `docker compose up --build`: criar, editar, buscar e excluir um lançamento na tela da Casa; os totais batem; a Empresa não mostra o item Lançamentos.
17. Subir de novo com o volume existente não duplica as 6 metas.
18. Um lançamento criado continua depois de `docker compose down` e `up` (sem `-v`) e aparece no backup do dia seguinte.

## Fora de escopo

Cartões, fatura e `card_id` (004); parcelas (RF21) e presets (RF20); percentuais, previsto, restante e fechamento do mês (005); Empresa: notas, teto do MEI, repasse e "Custos e repasse" (006); login e Row Level Security (007); auditoria por lançamento (RF13); lixeira e restauração; anexos; importação e exportação de CSV; busca sem acento; telas de gerenciar tags (renomear, arquivar); modo de edição em lote; saldo por conta.

## Riscos

- **Primeiro uso de JPA no projeto.** `ddl-auto: validate` compara as entidades com o schema do Flyway; qualquer diferença de tipo (`numeric`, `date`, `citext`, enum em texto) derruba a aplicação ao subir. Só o seu `mvnw verify` confirma, porque o Maven não roda nas sessões.
- **Decisão 1 muda a modelagem.** Se você quiser manter `category`, a spec cresce (tabela, seed e tela de categorias).
- **Chaves compostas no JPA** exigem cuidado nos mapeamentos; se ficar pesado, a garantia continua no banco e a entidade guarda só os ids.
- **Busca sem acento:** "cafe" não acha "café". Aceitável no começo; a extensão `unaccent` resolve depois sem mudar a API.

## Implementação (09/10/2026)

**Verificado na sessão (nuvem, sem Maven Central)**
- `V1` a `V5` em sequência num PostgreSQL 16 real, com as constraints (valor zero, direção e pagamento fora da lista, entrada com meta, saída sem pagamento, FK composta de meta e tag de outro espaço) e as consultas de lista, totais e busca.
- Núcleo Java (`Dinheiro`, `UuidV7`, domínio e casos de uso) compilado com `javac` e exercitado por um `main` de fumaça. Isso achou um erro de ordem de inicialização estática em `Dinheiro` (NPE ao carregar a classe), já corrigido.
- Front: `npm test` (101 testes, incluindo axe na lista, no formulário e na confirmação de exclusão), `lint` sem avisos, `typecheck` e Prettier passando.
- Revisão por um agente separado (que não escreveu o código): primeira rodada reprovou; os bloqueios (lock explícito somando versão duas vezes, 409 para `OptimisticLockingFailureException`, `mes` mal documentado, transações de leitura, vazamento de dados ao trocar de espaço no front) foram corrigidos.

**Pendente na máquina do João Pedro** (só o `mvnw verify` e o Docker confirmam)
- [ ] `cd backend && mvnw.cmd verify` (Docker aberto): critérios 1 a 9. Maior risco: `ddl-auto: validate` contra `LancamentoEntity`, `ModularityTest` com 6 módulos, regra nova do ArchUnit, `LancamentosVersaoIT` (commits reais; confere versão 0 → 1 → 2 e 409 com versão velha), o 400 de `mes` e o OpenAPI (`required`, `mes` como string).
- [ ] `cd frontend && npm ci && npm test && npm run lint && npm run typecheck && npm run build`.
- [ ] `docker compose up --build`; com a API nova no ar, `npm run gen:api` e **conferir que `schema.d.ts` não muda** (rodar só com a API nova no ar, senão o arquivo sai vazio).
- [ ] Critérios 16 a 18 à mão.

**Mudanças em relação ao texto acima**
- `Dinheiro` aceita zero e negativos (totais e `sobra`); `deTexto` recusa `-`; o lançamento exige valor positivo. Critério 3 ajustado.
- Tags usam `JdbcClient` (não JPA); só o agregado `Lancamento` usa JPA, com a entidade separada no adapter e regra de ArchUnit proibindo JPA no `domain`.
- Casos de uso: `ConsultarLancamentos`, `GravarLancamentos` e `GerenciarTags`. `RelogioSistema` fica em `adapter/out/relogio`.
- `IdentidadeApi` ganhou `tipoDoEspaco` e `exigirPermissaoDeEscrita` (com `SemPermissao`, 403); `TipoEspaco` foi para a raiz do módulo.
- Totais ignoram os filtros (são do mês inteiro).
- `mes` é `String` validada (`yyyy-MM`, 400 se mal formada), pois `YearMonth` saía mal documentado no OpenAPI.
- A versão sobe sozinha pelo `@Version` porque `atualizadoEm` sempre muda; não há `em.lock`.
- `spring.jackson.default-property-inclusion: non_null`.
- `schema.d.ts` foi escrito à mão no estilo do gerado (sem Maven não havia API para gerar).
- Front: os tipos da API usados pelo `model` ficam em `model/tipos.ts`, e o ESLint ganhou a categoria `tipos-da-api` para o `schema.d.ts`. A lista descarta o cache anterior ao trocar de espaço.
- Os docs de requisito (`01-requisitos.md`, `CONTEXTO.md`) ainda dizem "categoria" (RF01, RF17, RF19); não foram editados de propósito. A modelagem (`02`) e a arquitetura (`03`) foram atualizadas.
