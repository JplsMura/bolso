# 004 · Cartões

Status: **implementada, aguardando verificação na máquina do João Pedro** (aprovada e implementada em 11/10/2026) · João Pedro

## Objetivo

Cadastrar cartões de crédito, lançar gastos no cartão (à vista e parcelados), ver a fatura de cada cartão por mês com o ciclo aberta → fechada → paga, e o limite usado e disponível. Libera o `CREDIT` que a 003 deixou reservado.

A regra central, já decidida na modelagem: **o orçamento conta pela data da compra; a fatura mostra o caixa.** Pagar a fatura não cria lançamento, então nada é somado duas vezes.

## Requisitos cobertos

- RF23: cadastro de cartões e `CREDIT` com cartão escolhido em todo gasto no crédito.
- RF25: limite usado e disponível; fatura por mês (aberta, fechada, paga); total congelado ao fechar; pagamento registrado sem duplicar o gasto.
- RF03: fatura por cartão e mês, parcelas com a parcela atual e a última, e a fatura do mês seguinte já visível (parcelas futuras).
- RF21: compra parcelada cria as parcelas dos meses seguintes ao cadastrar (só no cartão, ver decisão 5).
- RF16 (parcial): parcelamento já em andamento antes de outubro/2026 entra informando a parcela atual (decisão 6).

Fora desta feature: RF20 (presets), RF13 (histórico por lançamento), RF18 (Open Finance) e o resto do RF16 (reserva e faturamento do ano).

## Decisões desta spec

Propostas por mim, para você confirmar ou trocar:

1. **A fatura leva o nome do mês em que fecha.** Fatura de outubro do Nubank = a que fecha em 25/10 (e vence em 05/11). É o que o design de Cartões já mostra (em meados de outubro, Nubank "aberta" e BTG "fechada"). A tela sempre mostra as duas datas ("fecha 25/10 · vence 05/11") para não confundir com o app do banco, que costuma usar o mês do vencimento. **Se preferir o mês do vencimento, troco agora**: muda só a regra de cálculo e os textos.
2. **Compra no dia do fechamento já cai na fatura seguinte** (é o "melhor dia de compra" dos bancos). Compra antes do dia de fechamento cai na fatura do mesmo mês.
3. **Fechar e pagar são manuais**, pelos botões do design ("Fechar fatura", "Marcar como paga"). A tela avisa quando uma fatura aberta já passou do dia de fechamento e quando uma fechada já passou do vencimento. Fechamento automático por job fica para depois.
4. **Fatura fechada ou paga não muda de valor.** Compra nova, troca de valor, data, cartão ou forma de pagamento e exclusão que caiam numa fatura fechada ou paga são recusadas (422, "reabra a fatura"). Descrição, meta e tags continuam editáveis, porque não mudam o total. Para corrigir: "Reabrir" (fechada → aberta) e "Desfazer pagamento" (paga → fechada).
5. **Parcelamento só no cartão de crédito**, de 2 a 48 parcelas, informando o **valor total**. Os centavos que sobram da divisão vão na primeira parcela (R$ 100,00 em 3 = 33,34 + 33,33 + 33,33). Cada parcela é um lançamento: a parcela N cai na fatura N−1 meses depois da primeira e conta no orçamento do mês dela (data da compra + N−1 meses). Assim a parcela pesa todo mês nas metas, como no AUVP.
6. **Parcelamento em andamento:** campo opcional "parcela atual" (padrão 1). Uma compra de julho em 6x cadastrada em outubro como "parcela atual 4" cria só as parcelas 4, 5 e 6. Resolve o ponto de partida do histórico sem tela de saldos iniciais.
7. **Parcela se edita pelo parcelamento.** `PUT` e `DELETE` em `/lancamentos/{id}` de uma parcela respondem 422. O parcelamento edita descrição, meta e tags de todas as parcelas em faturas abertas; valor, número de parcelas, cartão e data não mudam (exclui e cadastra de novo). Excluir o parcelamento apaga as parcelas que estão em faturas abertas; as de faturas fechadas ou pagas ficam (é o caso do estorno ou da quitação antecipada).
8. **Limite usado = soma dos lançamentos em faturas não pagas** (abertas e fechadas, inclusive as futuras das parcelas). Passar do limite **não bloqueia** a compra (o banco às vezes deixa); a tela mostra o disponível negativo em vermelho e o formulário avisa antes de salvar.
9. **Cartão com o mínimo:** nome (único no espaço, sem diferenciar maiúsculas), dia de fechamento, dia de vencimento e limite. Sem bandeira, final do cartão e cor (cor só para significado: a barra usa primária, aviso acima de 80% e perigo acima de 100%). Arquivar em vez de excluir; arquivar com fatura não paga é recusado.
10. **Dia 29, 30 ou 31** vira o último dia nos meses mais curtos. O vencimento fica no mesmo mês do fechamento se o dia de vencimento for maior que o de fechamento, senão no mês seguinte. A fatura guarda as suas datas ao ser criada; mudar o dia de fechamento do cartão vale para as faturas ainda não criadas.
11. **A fatura nasce na primeira compra** que cai nela (ou na parcela futura), como na modelagem. Mês sem compra não tem fatura; a tela mostra "sem gastos".
12. **Dependência entre módulos em um sentido só: `cartoes` → `lancamentos`.** O Lançamentos precisa saber em qual fatura cai a compra e se ela está aberta; o Cartões precisa somar os lançamentos da fatura. Para não criar ciclo (o Modulith recusa), o `lancamentos` publica na raiz uma porta `FaturasDoCartao` e o `cartoes` a implementa; o `cartoes` lê somas e itens pela `LancamentosApi`. Nenhum módulo lê tabela do outro.
13. **Chaves estrangeiras compostas entre `transaction` e as tabelas de cartão**, mesmo sendo de módulos diferentes: estão no mesmo schema `finance`, e a FK `(invoice_id, card_id, workspace_id)` garante no banco que a fatura é daquele cartão e daquele espaço. O código continua sem ler a tabela do outro módulo. (A regra "sem FK entre contextos" fica para `billing` × `finance`.)
14. **Cartões existem em qualquer espaço na API**, mas a tela e o item de menu entram só na Casa agora (o design da Empresa não tem cartões). Sem regra extra para a Empresa.
15. **Pagamento é integral** (requisitos: "fatura paga integralmente todo mês"): guarda só a data (`paid_on`), sem `paid_amount`. Rotativo e pagamento parcial ficam fora.
16. **Reabrir e desfazer pagamento:** `OWNER` e `EDITOR`, sem auditoria por enquanto (a tabela `audit_log` ainda não existe; entra com RF13).

Se aprovar, atualizo `docs/02-modelagem-banco.md` (colunas de `credit_card`, `card_invoice` e `installment_plan`) e `docs/03` (porta `FaturasDoCartao`).

## Regras de negócio

- **Cartão:** nome de 1 a 40 caracteres; dias de 1 a 31; limite maior que zero, até `999999999999.99`, texto com 2 casas como em todo dinheiro.
- **Gasto no crédito (`CREDIT`):** `cartaoId` obrigatório, de um cartão ativo do mesmo espaço; qualquer outra forma de pagamento proíbe `cartaoId`. Entrada nunca tem cartão. Meta segue a 003 (obrigatória na Casa, proibida na Empresa).
- **Em qual fatura cai:** `fechamento(mês) = min(diaFechamento, último dia do mês)`. Se `data < fechamento(mês da data)`, fatura do mês da data; senão, do mês seguinte. A fatura precisa estar aberta (decisão 4).
- **Vencimento:** o primeiro dia de vencimento (`min(diaVencimento, último dia)`) depois do fechamento: no mesmo mês se cair depois, senão no mês seguinte. (Na implementação: fecha 30 e vence 31 em fevereiro viram os dois dia 28, então vence em março.)
- **Ciclo da fatura:** `OPEN → CLOSED` (só a partir da data de fechamento; congela `total`), `CLOSED → PAID` (data de pagamento opcional, padrão hoje, não antes da data de fechamento nem no futuro), `CLOSED → OPEN` (reabrir; limpa o total), `PAID → CLOSED` (desfazer pagamento). Qualquer outra transição: 422. Todas exigem `versao` (409 se mudou).
- **Total da fatura:** aberta = soma ao vivo dos lançamentos não excluídos dela; fechada ou paga = o total congelado.
- **Parcelamento:** valor total maior que zero e de pelo menos 1 centavo por parcela; parcelas de 2 a 48; parcela atual de 1 ao total; todas as faturas que recebem parcela precisam estar abertas (ou ainda não existir). Descrição na lista e na fatura aparece como "Curso online 2/6".
- **Orçamento:** gasto no cartão conta no mês da data da compra (parcela: no mês dela). Totais da lista de lançamentos (003) não mudam ao pagar uma fatura.
- **Permissões, 404 e 409:** como na 003.

## Contrato da API

Prefixo `/api/v1/espacos/{espacoId}`. Mesmos códigos de erro da 003. Valores nos exemplos são fictícios.

**Cartões (módulo `cartoes`)**

| Método e rota | O que faz |
| --- | --- |
| `GET /cartoes` | Cartões ativos, por nome, com `limite`, `usado`, `disponivel` (pode ser negativo), `diaFechamento`, `diaVencimento`, `versao`. |
| `POST /cartoes` | Cria. 201 com `Location`. 422 se o nome já existe. |
| `PUT /cartoes/{id}` | Edita nome, dias e limite; exige `versao`. |
| `DELETE /cartoes/{id}` | Arquiva. 204. 422 se houver fatura não paga. |
| `GET /cartoes/{id}/fatura-da-compra?data=2026-10-08` | Em qual fatura cai uma compra nessa data: `mes`, `fechamento`, `vencimento`, `status` (nulo se ainda não existe). Alimenta o aviso do formulário. |
| `GET /faturas?mes=2026-10` | Para cada cartão ativo, a fatura daquele mês (ou `null`) com `status`, `fechamento`, `vencimento`, `total`, `pagaEm`, `versao` e os `itens` (descrição, meta, valor, data, parcela `2/6`). `mes` ausente = mês atual em America/Sao_Paulo. |
| `POST /faturas/{id}/fechar` · `/pagar` · `/reabrir` · `/desfazer-pagamento` | Transições. Corpo `{ "versao": 2 }`; `pagar` aceita também `"pagaEm": "2026-10-15"`. 200 com a fatura. |

```json
{ "mes": "2026-10",
  "faturas": [ { "cartao": { "id": "...", "nome": "Nubank", "limite": "4500.00", "usado": "1260.00", "disponivel": "3240.00" },
                 "fatura": { "id": "...", "mes": "2026-10", "status": "OPEN", "fechamento": "2026-10-25",
                             "vencimento": "2026-11-05", "total": "1260.00", "pagaEm": null, "versao": 0,
                             "itens": [ { "lancamentoId": "...", "descricao": "Curso online", "parcela": { "numero": 2, "total": 6 },
                                          "metaId": "...", "valor": "25.00", "data": "2026-10-03" } ] } } ] }
```

**Lançamentos (módulo `lancamentos`, mudanças)**

| Método e rota | O que muda |
| --- | --- |
| `POST` e `PUT /lancamentos` | Campo `cartaoId`. `CREDIT` passa a ser aceito. 422 se a parcela for editada por aqui. |
| `GET /lancamentos` | Filtro `cartaoId`. Cada item ganha `cartaoId`, `faturaMes` e `parcela` (`{ parcelamentoId, numero, total }` ou nulo). |
| `DELETE /lancamentos/{id}` | 422 se for parcela ou se a fatura estiver fechada ou paga. |
| `POST /parcelamentos` | Cria o parcelamento e as parcelas. 201 com `Location` e as parcelas criadas. |
| `GET /parcelamentos/{id}` | O parcelamento e as suas parcelas (com o status da fatura de cada uma). |
| `PUT /parcelamentos/{id}` | Edita `descricao`, `metaId` e `tagIds` das parcelas em faturas abertas; exige `versao`. |
| `DELETE /parcelamentos/{id}` | Exclui as parcelas em faturas abertas. 204. |

```json
{ "valorTotal": "150.00", "parcelas": 6, "parcelaAtual": 1, "descricao": "Curso online",
  "data": "2026-09-03", "cartaoId": "...", "metaId": "...", "tagIds": [] }
```

## Banco e migrações

`V6__cartoes_faturas_parcelamentos.sql` (schema `finance`):

| Tabela | Colunas e restrições |
| --- | --- |
| `credit_card` | `id uuid PK`, `workspace_id` FK, `name citext` (1 a 40), `closing_day smallint CHECK 1..31`, `due_day smallint CHECK 1..31`, `credit_limit numeric(14,2) CHECK > 0`, `archived_at`, `created_at`, `updated_at`, `version`; `UNIQUE (id, workspace_id)`; único parcial (`workspace_id`, `name`) onde `archived_at IS NULL` |
| `card_invoice` | `id uuid PK`, `workspace_id`, `card_id`, `reference_month date CHECK (dia 1)`, `closing_date date`, `due_date date CHECK (due_date > closing_date)`, `status text CHECK IN ('OPEN','CLOSED','PAID')`, `total_amount numeric(14,2)`, `paid_on date`, `created_at`, `updated_at`, `version`; FK (`card_id`, `workspace_id`) → `credit_card`; `UNIQUE (card_id, reference_month)`; `UNIQUE (id, card_id, workspace_id)`; `CHECK ((status = 'OPEN') = (total_amount IS NULL))`; `CHECK ((status = 'PAID') = (paid_on IS NOT NULL))` |
| `installment_plan` | `id uuid PK`, `workspace_id`, `card_id`, `description text`, `total_amount numeric(14,2) CHECK > 0`, `installments smallint CHECK 2..48`, `first_installment smallint CHECK (1 ≤ first_installment ≤ installments)`, `purchase_date date`, `created_at`, `updated_at`, `deleted_at`, `version`; FK (`card_id`, `workspace_id`) → `credit_card`; `UNIQUE (id, workspace_id)` |
| `transaction` (alterada) | novas colunas `card_id`, `invoice_id`, `installment_plan_id`, `installment_no smallint`; FKs (`card_id`, `workspace_id`) → `credit_card`, (`invoice_id`, `card_id`, `workspace_id`) → `card_invoice (id, card_id, workspace_id)`, (`installment_plan_id`, `workspace_id`) → `installment_plan`; `CHECK ((payment_method = 'CREDIT') = (card_id IS NOT NULL))`; `CHECK ((card_id IS NULL) = (invoice_id IS NULL))`; `CHECK ((installment_plan_id IS NULL) = (installment_no IS NULL))`; `CHECK (installment_plan_id IS NULL OR card_id IS NOT NULL)` |

Índices: `transaction (invoice_id) INCLUDE (amount)` parcial em `deleted_at IS NULL` (soma da fatura só pelo índice); único `transaction (installment_plan_id, installment_no)` parcial em `deleted_at IS NULL`; `card_invoice (card_id)` parcial em `status <> 'PAID'` (limite usado); `card_invoice (workspace_id, due_date)` (lembrete, depois). A V4 não é tocada; como a API recusava `CREDIT`, nenhuma linha existente quebra os `CHECK` novos.

## Backend

```
lancamentos/   FaturasDoCartao (porta na raiz, implementada pelo cartoes): faturaDaCompra, faturaDoMes,
                 exigirAberta → FaturaResolvida(id, mes, status)
               LancamentosApi (nova, raiz): somaPorFatura(espaco, faturaIds), itensDaFatura(espaco, faturaId)
               domain: Lancamento ganha cartaoId, faturaId, Parcela(planoId, numero); Parcelamento (divisão
                       dos centavos, datas e meses de cada parcela)
               application: GravarLancamentos (CREDIT), GravarParcelamentos; ConsultarLancamentos (filtro cartão)
               adapter: ParcelamentosController, InstallmentPlanEntity, colunas novas em LancamentoEntity
cartoes/       CartoesApi não é necessária nesta feature (ninguém chama o cartoes)
               domain: Cartao, Fatura (ciclo e total congelado), CalendarioDoCartao (em qual fatura cai, datas)
               application: GerenciarCartoes, ConsultarFaturas, MudarStatusDaFatura, FaturasDoCartaoService
                            (implementa a porta do lancamentos)
               adapter/in/web: CartoesController, FaturasController, DTOs, TratadorDeErrosCartoes
               adapter/out/persistence: JPA para Cartao e Fatura (gravação), JdbcClient para listas e limite
```

- Todo caso de uso começa pelo guarda `IdentidadeApi`, como na 003.
- A fatura é criada dentro da mesma transação do lançamento, e a leitura dela usa `SELECT ... FOR SHARE` para uma compra não entrar numa fatura que está sendo fechada ao mesmo tempo.
- `CalendarioDoCartao` e `Parcelamento` são Java puro, com a maior parte dos testes.

## Frontend

- **Tela Cartões** (`/casa/cartoes`), no visual de `design/project/Cartoes.dc.html`: mês com setas; por cartão, datas de fechamento e vencimento, barra de limite usado e disponível, etapas Aberta/Fechada/Paga, total, itens da fatura e o botão da próxima ação (Fechar fatura, Marcar como paga). Ações secundárias (Reabrir, Desfazer pagamento) num menu do cartão, com confirmação. Avisos de "passou do fechamento" e "vencida". Texto fixo: "Pagar a fatura não cria um novo gasto".
- **Novo cartão e editar cartão**: nome, dia de fechamento, dia de vencimento, limite; arquivar com confirmação. Estado vazio: "Cadastre seu primeiro cartão".
- **Formulário de gasto** (`NovoGasto.dc.html`): opção Cartão na forma de pagamento; ao escolher, os cartões com o disponível; campo Parcelas (1 a 48) e, abaixo, "parcela atual" escondido atrás de "compra já em andamento?"; aviso "Entra na fatura de novembro do Nubank · fecha 25/11" vindo de `fatura-da-compra`; aviso de limite estourado; com parcelas > 1 o envio vai para `/parcelamentos` e mostra "6x de R$ 25,00".
- **Editar parcela** abre o formulário do parcelamento (descrição, meta, tags) com a lista de parcelas e o botão "Excluir parcelas restantes".
- **Lista de lançamentos**: nome do cartão e "2/6" nos itens; filtro por cartão.
- Item "Cartões" do menu da Casa vira link.
- `useCartoes` vai para `shared/api` (usado por `cartoes` e `lancamentos`; uma feature não importa outra).
- Erros 422 de fatura fechada com o texto do servidor e o atalho para a tela Cartões.

## Ajuste: formulário compacto (aprovado em 11/10/2026)

Pedido depois de testar a 004: o formulário de gasto ficou comprido, e data, parcelas e o botão Salvar só apareciam rolando. Só muda a arrumação; campos, regras e API ficam iguais. Esboço aprovado no canvas "Bolso · Formulário compacto".

- **Computador (md+):** faixas da largura toda, uma por assunto. Faixa 1: Gasto/Entrada e, numa linha, valor, descrição e data. Faixa 2: formas de pagamento numa linha (todas visíveis); com Cartão, cartões e parcelas à esquerda, disponível e fatura à direita. Faixa 3: metas numa linha e tags. Rodapé fixo com o resumo (só com valor) e Salvar. (Primeiro testei duas colunas, como no esboço; na tela larga a coluna do pagamento ficava curta e solta antes de escolher a forma, então trocamos por faixas em 11/10.)
- **Celular:** uma coluna. Data ao lado do valor; forma de pagamento, cartões, metas e tags em linhas que rolam para o lado; cartão escolhido, parcelas e aviso num bloco só; botão Salvar fixo acima do menu.
- **Formas de pagamento:** no celular, Transferência e Outro ficam atrás de "Mais…" (aparecem sozinhas se uma delas já estiver escolhida, como na edição); no computador cabem todas.
- **Cartões:** o chip mostra só o nome; o disponível aparece embaixo, para o cartão escolhido.
- **Tags (pedido no mesmo dia):** em vez da fileira de chips e do campo "Nova tag", um campo só que busca as tags existentes e oferece "Criar “…”" (combobox com lista: setas, Enter, Esc). As escolhidas ficam embaixo, cada uma com × para tirar. Vale também na tela do parcelamento. Espaçamento entre blocos e campos mais folgado. `NovaTag.tsx` deixa de ser usado.
- Vale para novo gasto e edição. Os testes da 004 que procuravam o disponível no nome do chip passam a procurar no texto do bloco.

## Critérios de aceite

**Backend (`mvnw.cmd verify`)**
1. `ModularityTest` e `ArquiteturaHexagonalTest` verdes, sem ciclo entre `cartoes` e `lancamentos`.
2. `CalendarioDoCartaoTest`: compra um dia antes do fechamento (mesmo mês), no dia do fechamento e depois (mês seguinte); fechamento 31 em fevereiro e em ano bissexto; vencimento menor que fechamento (mês seguinte) e maior (mesmo mês); virada de dezembro para janeiro.
3. `ParcelamentoTest`: 100,00 em 3 = 33,34 + 33,33 + 33,33 e soma exata; 0,05 em 6 recusado; parcela atual 4 de 6 gera 3 parcelas com as datas e meses certos; data dia 31 vira o último dia nos meses curtos.
4. `FaturaTest`: todas as transições válidas e as inválidas (422); fechar antes da data de fechamento recusado; total congelado ao fechar e limpo ao reabrir; pagamento antes do fechamento recusado.
5. Casos de uso com portas falsas: `CREDIT` sem cartão, com cartão arquivado ou de outro espaço; `cartaoId` com PIX; compra, edição de valor/data/cartão e exclusão em fatura fechada (422) e edição só de descrição/meta/tags nela (200); parcela editada por `/lancamentos` (422); `VIEWER` (403).
6. `MigracoesIT`: V6 aplica do zero sobre a V5; o banco recusa `CREDIT` sem cartão, cartão com PIX, fatura de outro cartão (FK composta), cartão de outro espaço, duas faturas do mesmo cartão e mês, fatura aberta com total, fatura paga sem data, parcela sem número e número repetido.
7. `CartoesControllerIT`: criar, listar com usado e disponível, editar com versão certa e errada, nome repetido (422), arquivar com fatura não paga (422) e sem (204).
8. `FaturasControllerIT`: lista do mês com itens e totais; ciclo completo fechar → pagar → desfazer → reabrir; 409 com versão velha; **limite usado inclui parcelas futuras e exclui faturas pagas**; `fatura-da-compra` responde o mês certo.
9. `LancamentosControllerIT` e `ParcelamentosControllerIT`: gasto no crédito aparece na fatura e no total do mês da compra; **pagar a fatura não muda os totais do mês**; parcelamento cria N parcelas em N faturas; editar o parcelamento muda só as parcelas em faturas abertas; excluir apaga só essas; filtro por cartão.
10. `GET /v3/api-docs` traz os endpoints novos com dinheiro como string; `POST` documentados como 201.

**Frontend (`npm test && npm run lint && npm run typecheck && npm run build`)**
11. Tela Cartões: cartões, barra de limite (cor por faixa), status e botão certo por status; trocar de mês refaz a consulta; estado vazio; confirmação em reabrir e desfazer pagamento; 409 com mensagem de recarregar.
12. Formulário de gasto: Cartão exige escolher um cartão; o aviso da fatura chama `fatura-da-compra` com a data; parcelas > 1 envia para `/parcelamentos` com o valor total como texto da API; aviso de limite estourado.
13. Parcela: abre o formulário do parcelamento; excluir parcelas restantes pede confirmação.
14. axe-core sem violações na tela Cartões, no formulário de cartão e no formulário de gasto com cartão. Fronteiras do ESLint verdes.

**Docker (na sua máquina)**
15. `docker compose up --build`: cadastrar um cartão, lançar um gasto à vista e um parcelado em 3x, ver os dois na fatura, o limite usado e as parcelas nos meses seguintes; fechar e pagar a fatura e conferir que os totais da lista de lançamentos não mudaram.
16. Subir de novo com o volume existente não perde nada; a V6 aplica sobre um banco que já tem lançamentos da 003.

## Fora de escopo

Presets (RF20); fechamento automático da fatura por job e lembretes de vencimento; pagamento parcial, rotativo, juros, IOF, anuidade e crédito/estorno na fatura (estorno: excluir a compra se a fatura estiver aberta); importação de fatura (CSV/OFX) e Open Finance (RF18); conta de onde sai o pagamento e saldo por conta; editar valor, número de parcelas, cartão ou data de um parcelamento; parcelamento fora do cartão (carnê, boleto); bandeira, final, cor e cartão adicional/virtual; tela de cartões na Empresa; auditoria de reabertura (RF13); orçamento por meta e painel da tela inicial (005).

## Riscos

- **Porta invertida no Modulith** (`cartoes` implementa uma interface da raiz do `lancamentos`). É o padrão que o Modulith recomenda para evitar ciclo, mas o teste de módulo isolado do `lancamentos` vai precisar de um dublê da porta. Só o seu `mvnw verify` confirma.
- **Datas:** o cálculo de fatura é onde mora o erro silencioso (dia 31, fevereiro, dia do fechamento). Por isso fica em Java puro, com testes de tabela.
- **Decisão 1 (mês da fatura)** é a mais difícil de trocar depois que houver dado: muda o `reference_month` de todas as faturas. Melhor confirmar agora.
- **JPA:** colunas novas em `LancamentoEntity` e três entidades novas sob `ddl-auto: validate`; mesmo risco da 003.
- **Valor por parcela:** quando o banco cobra parcelas com juros e o total não divide igual, os centavos podem não bater com a fatura real. Contorno: informar o total que o banco mostra.

## Implementação (11/10/2026)

**Verificado na sessão**
- O backend foi compilado e testado com as dependências do seu repositório local do Maven (`~/.m2`, lido com sua permissão; Maven 3.9.11 offline), em **Java 21** (`-Djava.version=21`): a sessão não tem JDK 25.
- `mvn verify` completo: **146 testes de unidade e arquitetura e 139 de integração, todos verdes**, incluindo `ModularityTest` (sem ciclo entre `cartoes` e `lancamentos`), ArchUnit e o Hibernate em `validate` aceitando as entidades novas. Os ITs rodaram contra um **PostgreSQL 16 local** (sem Docker na sessão): numa cópia descartável, o `TestcontainersConfig` foi trocado por uma configuração vazia e o banco passado por propriedade. Nada disso voltou para o seu projeto.
- V6 aplicada sobre um banco que já tinha lançamentos da 003, e as 22 restrições novas conferidas uma a uma (cada uma recusa pelo motivo certo).
- API rodando de verdade: `schema.d.ts` **gerado** do `/v3/api-docs` (não escrito à mão). Telas conferidas no navegador, no celular (390 px) e no desktop, com dados fictícios.
- Front: 145 testes (incluindo axe na tela Cartões, nos formulários de cartão e de gasto com cartão, na confirmação e no parcelamento), lint, typecheck, Prettier e build. Rodado com npm 11, sem mexer no `package-lock.json`.

**Pendente na máquina do João Pedro**
- [ ] `cd backend && cmd //c mvnw.cmd verify` (Docker aberto): critérios 1 a 10 em Java 25 e PostgreSQL 18 (Testcontainers), que a sessão não tinha.
- [ ] `cd frontend && npm ci && npm test && npm run lint && npm run typecheck && npm run build`.
- [ ] `docker compose up --build`; com a API no ar, `npm run gen:api` e conferir que o `schema.d.ts` não muda.
- [ ] Critérios 15 e 16 à mão.
- [ ] Apagar `backend/target/claude-bolso.tgz` e `backend/target/claude-m2.tgz` (cópias que a sessão usou; `mvnw clean` também resolve, a pasta é ignorada pelo Git).

**Mudanças em relação ao texto acima**
- Dias e número de parcela são `integer`, não `smallint` (casa direto com `Integer` no JPA). Nome do cartão é `text` com índice único em `lower(name)` só entre os ativos (arquivar libera o nome), em vez de `citext`, para o Hibernate validar sem surpresa.
- `card_invoice` ganhou `CHECK`s além dos previstos: fechamento dentro do mês da fatura, pagamento não antes do fechamento; `installment_plan` exige pelo menos 1 centavo por parcela também no banco.
- `installment_plan` fica sem `goal_id` (meta e tags vivem nas parcelas) e é gravado com `JdbcClient` (linha pequena, versão conferida no `UPDATE`). Cartão e fatura usam JPA; a fatura é **criada** por SQL com `ON CONFLICT DO NOTHING` (duas compras ao mesmo tempo não criam a mesma fatura) e travada com `FOR SHARE`; fechar/pagar trava com `PESSIMISTIC_WRITE` antes de somar.
- A porta ficou com quatro métodos: `mesDaFaturaDaCompra` (não grava; usada também para não criar faturas vazias de meses passados numa compra em andamento), `faturaDoMes`, `faturaTravada` e `faturas` (leitura sem trava, porque `FOR SHARE` não roda em transação somente-leitura).
- Fatura fechada responde 422 com `type: urn:bolso:problema:fatura-fechada`; a tela usa isso para mostrar "Ir para Cartões".
- Excluir parcelamento sem nenhuma parcela em fatura aberta responde 422 (antes não estava dito).
- Revisão (agente separado, 11/10): aprovada. Corrigido: faixa de perigo da barra calculada pelos centavos (100,4% aparecia como aviso); mudar status de fatura de cartão arquivado não dá mais 404; nome repetido em dois cadastros simultâneos dá 422 (não 500); leitura do total de parcelas filtra pelo espaço. Ficou como risco aceito: arquivar o cartão no mesmo instante em que entra uma compra nele pode deixar uma fatura aberta escondida ou responder 500 (um usuário só; travar o cartão resolve se um dia importar).
- Front: editar uma parcela mostra o aviso e o link "Abrir o parcelamento" (`/casa/parcelamentos/$id`), em vez de embutir o formulário. Reabrir e desfazer pagamento ficam como botões secundários com confirmação, não num menu.
- Front: mês, datas, seletor de mês, estado de erro, chip, erro da API, metas e cartões foram para `shared` (as duas features usam); os arquivos antigos em `lancamentos` só reexportam, para não precisar apagar nada. `handlersPadrao` do MSW responde `/cartoes` com lista vazia.
