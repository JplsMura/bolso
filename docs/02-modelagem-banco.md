# Modelagem do banco de dados

2026-10-08 · @João Pedro

## Decisões de modelagem

O banco é PostgreSQL, organizado em cinco schemas que espelham os contextos do sistema, e todo dado pertence a um **espaço** (workspace), nunca direto a um usuário. Assim, compartilhar o orçamento com outra pessoa depois é só adicionar um membro, sem migrar dados.

- **Schemas:** `identity` (usuários e espaços), `finance` (lançamentos, categorias, metas, contas e cartões), `billing` (empresa, notas e tomadores), `planning` (cenários e metas de reserva, depois) e `platform` (parâmetros fiscais e auditoria).
- **Tabelas pequenas e estreitas:** o que muda pouco (cadastros) fica separado do que cresce (lançamentos), e arquivos de nota ficam no disco, não no banco.
- **Chaves:** UUID v7 gerado pela aplicação. Ele ordena por data de criação, o que mantém os índices compactos, ao contrário do UUID aleatório.
- **Dinheiro:** `numeric(14,2)` em reais, sempre positivo, com a direção (entrada ou saída) em coluna própria. Nunca `float`.
- **Datas:** `date` para dia do lançamento e `reference_month` (sempre o dia 1) para a competência do mês. Instantes em `timestamptz`, em UTC.
- **Texto:** `citext` para e-mail e nomes que precisam ser únicos sem diferenciar maiúsculas.
- **Exclusão:** lógica (`deleted_at`) só em lançamentos e notas; cadastros são arquivados (`archived_at`).
- **Concorrência:** coluna `version` nas tabelas editadas pela tela, para detectar duas edições ao mesmo tempo.
- **Regras no banco também:** `CHECK`, `UNIQUE` e chaves estrangeiras garantem o que a aplicação promete, mesmo que um bug passe.

## MER: entidades e relacionamentos

> [Diagrama/widget no app: MER · entidades e relacionamentos]

O espaço é a raiz: usuários entram nele como membros, com um papel, e os outros contextos guardam o `espaco_id`. O lançamento é o centro das finanças; categoria, conta, recorrência, fatura e parcelamento dizem de onde ele vem e como é agrupado.

## DER: núcleo financeiro

> [Diagrama/widget no app: DER · núcleo financeiro, com colunas e chaves estrangeiras]

A tabela `transaction` guarda só ids e valores, sem texto repetido: nome de categoria, conta ou cartão vive no cadastro correspondente. Por isso ela cresce em linhas, mas continua estreita, e os cadastros ficam pequenos o bastante para ficar sempre em memória.

## Dicionário de tabelas

São 29 tabelas em cinco schemas, e a maior parte é de cadastro com poucas linhas. Só `finance.transaction` e `platform.audit_log` crescem de verdade.

### identity

| Tabela | Guarda | Colunas e chaves principais |
| --- | --- | --- |
| `app_user` | Pessoa que entra no sistema | `id`, `email` (citext, único), `email_verified_at`, `display_name`, `status` |
| `auth_identity` | Como a pessoa entra | `user_id`, `provider` (EMAIL_CODE ou GOOGLE), `provider_subject`; único em (`provider`, `provider_subject`) |
| `email_login_code` | Código de verificação por e-mail, de vida curta | `id`, `email`, `code_hash`, `expires_at`, `consumed_at`, `attempts` |
| `refresh_token` | Sessões renováveis | `id`, `user_id`, `token_hash`, `expires_at`, `revoked_at` |
| `workspace` | O espaço, dono dos dados: Casa ou Empresa (campo `kind`) | `id`, `name`, `created_at` |
| `workspace_member` | Quem participa do espaço e com qual papel | PK (`workspace_id`, `user_id`), `role` (OWNER, EDITOR ou VIEWER) |
| `workspace_invite` (depois) | Convite por e-mail | `id`, `workspace_id`, `email`, `role`, `token_hash`, `expires_at`, `accepted_at` |

### finance

| Tabela | Guarda | Colunas e chaves principais |
| --- | --- | --- |
| `budget_goal` | As 6 metas do orçamento (Custos Fixos, Conforto etc.) | `id`, `workspace_id`, `name`, `color`, `sort_order` |
| `budget_goal_percent` | Percentual de cada meta por mês, com histórico | PK (`goal_id`, `reference_month`), `percent` numeric(5,2) |
| month_closing_goal | Foto de cada meta no fechamento do mês: o histórico não muda quando você altera percentuais depois | PK (workspace_id, reference_month, goal_id), percent, income_base (receita do mês), budget_amount (percent × receita), spent_amount, balance |
| `category` | Categorias de lançamento, dentro de uma meta | `id`, `workspace_id`, `goal_id` (nulo para entradas), `name`, `kind` (INCOME ou EXPENSE), `archived_at` |
| tag | Etiqueta livre além das 6 metas (Luz, Alimentação, Uber), ligada ao lançamento por transaction_tag (N:N) | id, workspace_id, name (único por espaço), cost_type (FIXED, VARIABLE ou nulo), archived_at |
| `account` | Conta bancária ou carteira (fica para depois: no MVP só gasto por categoria) | `id`, `workspace_id`, `name`, `type`, `institution`, `opening_balance`, `opening_date`, `archived_at` |
| `credit_card` | Cartão de crédito | `id`, `workspace_id`, `name`, `brand`, `last4`, `closing_day`, `due_day`, `credit_limit`, `archived_at` |
| `card_invoice` | Fatura de um cartão em um mês (OPEN, CLOSED ou PAID). Única por (card_id, reference_month), criada na primeira compra do mês. Ao fechar guarda total_amount congelado; ao pagar guarda paid_amount e paid_on. O limite usado do cartão é a soma das faturas não pagas, incluindo parcelas futuras já lançadas | `id`, `workspace_id`, `card_id`, `reference_month`, `closing_date`, `due_date`, `status` (OPEN, CLOSED ou PAID), `paid_on` |
| `installment_plan` | Compra parcelada | `id`, `workspace_id`, `card_id`, `category_id`, `description`, `total_amount`, `installments`, `first_month` |
| `recurring_rule` | Preset de lançamento mensal (luz, água, internet, assinaturas): ao abrir o mês você usa o preset, vem com o último valor lançado e edita antes de confirmar | `id`, `workspace_id`, `description`, `amount`, `direction`, `category_id`, `account_id` ou `card_id`, `day_of_month`, `starts_on`, `ends_on` |
| `transaction` | O lançamento: a tabela que cresce | Ver o DER abaixo. Todo lançamento de saída tem forma de pagamento (payment_method: PIX, DEBIT, CREDIT, CASH, BOLETO, TRANSFER ou OTHER) e, quando for CREDIT, o cartão (card_id). A fatura (invoice_id) é calculada pelo dia de fechamento do cartão. A meta e as tags continuam independentes da forma de pagamento |
| `import_batch` | Cada arquivo importado, para não importar duas vezes | `id`, `workspace_id`, `source`, `file_sha256`, `imported_at`, `row_count` |
| `month_closing` | Totais congelados de um mês fechado (as metas congeladas ficam em month_closing_goal) | PK (`workspace_id`, `reference_month`), `status`, `total_in`, `total_out`, `surplus`, `closed_at` |

### billing

| Tabela | Guarda | Colunas e chaves principais |
| --- | --- | --- |
| `company` | Sua empresa | `id`, `workspace_id`, `legal_name`, `cnpj` (único por espaço) |
| `company_regime` | Em que regime a empresa esteve e quando | `company_id`, `regime` (MEI ou ME_SIMPLES), `valid_from`, `valid_to`; períodos não podem se sobrepor |
| `customer` | Tomador da nota | `id`, `workspace_id`, `name`, `document`; único em (`workspace_id`, `document`) |
| `issued_invoice` | Nota emitida | `id`, `workspace_id`, `company_id`, `customer_id`, `number`, `access_key`, `issue_date`, `competence_month`, `received_on`, `gross_amount`, `kind` (MONTHLY, THIRTEENTH, FOURTEENTH ou OTHER), `status`, `deleted_at`, `version` |
| `invoice_document` | Arquivo da nota e o que foi lido dele | `id`, `workspace_id`, `invoice_id` (nulo até confirmar), `file_name`, `sha256`, `storage_path`, `parse_status`, `parsed_data` jsonb |

### planning (depois)

| Tabela | Guarda | Colunas e chaves principais |
| --- | --- | --- |
| `scenario` | Um cenário (A, B, C) | `id`, `workspace_id`, `name`, `description` |
| `scenario_assumption` | Premissas do cenário | PK (`scenario_id`, `key`), `value_numeric`, `value_text` |
| `reserve_snapshot` | Saldo da reserva por mês | PK (`workspace_id`, `reference_month`), `liquid_amount`, `stocks_amount` |
| `savings_goal` | Meta de poupar, como a entrada do carro | `id`, `workspace_id`, `name`, `target_amount`, `target_date` |

### platform

| Tabela | Guarda | Colunas e chaves principais |
| --- | --- | --- |
| `tax_parameter` | Alíquotas e limites fiscais com vigência, globais | PK (`key`, `valid_from`), `value`, `unit`, `source_note` |
| `audit_log` | Quem mudou o quê e quando | `id` bigint, `workspace_id`, `actor_user_id`, `entity`, `entity_id`, `action`, `diff` jsonb, `changed_at` |

Pagar uma fatura não gera lançamento: os gastos do cartão já contam no mês da fatura, e assim nada é somado duas vezes. Ao confirmar o recebimento de uma nota, o sistema cria o lançamento de entrada e guarda o id da nota em `source_ref`, sem chave estrangeira entre `billing` e `finance`.

## Índices e restrições

A regra geral: toda chave composta começa por `workspace_id`, porque toda consulta filtra por espaço, e os índices mais usados são parciais (`WHERE deleted_at IS NULL`) para ignorar o que foi excluído.

| Índice | Tabela | Consulta que atende |
| --- | --- | --- |
| (`workspace_id`, `reference_month`, `category_id`) INCLUDE (`amount`, `direction`), parcial | `transaction` | A tela do mês e a soma por categoria e por meta, lida só no índice |
| (`workspace_id`, `occurred_on` DESC), parcial | `transaction` | Lançamentos do dia e transações recentes |
| (`invoice_id`), parcial | `transaction` | Itens e total de uma fatura |
| único (`installment_plan_id`, `installment_no`) | `transaction` | Uma parcela por número, sem duplicar |
| único (`recurring_rule_id`, `reference_month`), parcial | `transaction` | O job de recorrência pode rodar duas vezes sem duplicar |
| único (`workspace_id`, `source`, `source_ref`), parcial | `transaction` | Importação e sincronização sem duplicar lançamentos |
| único (`workspace_id`, `lower(name)`) | `category` | Sem duas categorias com o mesmo nome |
| único (`card_id`, `reference_month`) | `card_invoice` | Uma fatura por cartão e mês |
| (`workspace_id`, `due_date`) | `card_invoice` | Lembrete de vencimento |
| (`company_id`, `issue_date`) | `issued_invoice` | Faturamento acumulado do ano e teto do MEI |
| único (`company_id`, `number`) e único parcial em `access_key` | `issued_invoice` | Nota duplicada |
| único (`workspace_id`, `sha256`) | `invoice_document` | O mesmo arquivo subido duas vezes |
| (`user_id`) | `workspace_member` | Achar os espaços de um usuário (a PK já cobre o outro sentido) |
| único (`provider`, `provider_subject`) e único em `email` | `auth_identity`, `app_user` | Login |
| único (`token_hash`) e (`expires_at`) | `refresh_token` | Validar o token e limpar os vencidos |

**Restrições no banco**

- `CHECK (amount > 0)` e `CHECK (direction IN ('IN','OUT'))` em `transaction`.
- `CHECK (invoice_id IS NULL OR account_id IS NULL)`: compra no cartão não sai de uma conta até a fatura ser paga.
- `CHECK (direction = 'OUT' OR nature IS NULL)`: só saídas têm natureza (fixo, variável, parcela, investimento).
- `EXCLUDE USING gist` em `company_regime` para impedir períodos de regime sobrepostos.
- Chaves estrangeiras com `ON DELETE RESTRICT`, nunca `CASCADE`, em tudo que é dinheiro. `CASCADE` só em dependentes puros, como `scenario_assumption`.

**O que não indexar:** colunas de poucos valores, como `direction` e `status`, sozinhas. Cada índice a mais deixa a gravação mais lenta e ocupa disco, então novos índices entram quando um `EXPLAIN` mostrar a necessidade.

## Escala e longo prazo

O modelo atende bem um servidor só por muito tempo: com cerca de 100 lançamentos por mês por espaço e mil espaços em cinco anos, `transaction` chega a uns 6 milhões de linhas (estimativa), e o PostgreSQL com os índices acima dá conta sem partição. O que muda a cada fase:

| Fase | Quando | O que entra |
| --- | --- | --- |
| 1 | Agora, uso seu | Um PostgreSQL, os índices acima, `EXPLAIN` nas consultas do mês e backup diário |
| 2 | Alguns milhares de espaços | Pool de conexões (HikariCP e PgBouncer), réplica de leitura para relatórios, `pg_stat_statements` para achar consultas lentas |
| 3 | Dezenas de milhões de lançamentos | Partição de `transaction` por faixa de `reference_month`, arquivamento de anos antigos, resumos mensais atualizados por job |

**Decisões que precisam ser tomadas agora para não doer depois**

- **Partição:** em tabela particionada a chave primária precisa incluir a coluna de partição. Por isso `reference_month` já existe como coluna própria e as consultas já filtram por ela; migrar para partição depois fica possível sem mudar a aplicação.
- **Isolamento:** a aplicação sempre filtra por `workspace_id`. Row Level Security entra como segunda trava (política baseada em `current_setting('app.workspace_id')`), junto com um teste automatizado que tenta ler dados de outro espaço.
- **Meses fechados:** `month_closing` guarda os totais congelados. O histórico não recalcula, e uma correção em mês fechado exige reabrir o mês de forma explícita.
- **Arquivos:** as notas ficam em disco (ou em armazenamento de objetos, como MinIO, depois), e o banco guarda só o caminho e o hash.
- **Concorrência:** a coluna `version` evita que duas telas sobrescrevam uma edição uma da outra, e as transações são curtas.

**Retenção e limpeza**

- Um job diário apaga códigos de login e refresh tokens vencidos.
- `audit_log` será particionada por mês, com prazo de descarte definido por você.
- Lançamentos e notas só saem por exclusão lógica; a exclusão física fica para o pedido de apagar a conta.
- Backup: `pg_dump` diário com restauração testada; com vários usuários, arquivamento contínuo (WAL) para recuperar até um instante.

## Migrações, convenções e dados de teste

- **Flyway:** uma migração por schema (`V1__identity.sql`, `V2__finance.sql`, `V3__billing.sql`, `V4__platform.sql`), e migração já aplicada nunca é editada.
- **Mudança destrutiva em duas etapas:** primeiro adiciona e passa a gravar nos dois lugares, depois remove o antigo em outra versão.
- **Nomes:** `snake_case`, tabelas no singular, `fk_<tabela>_<coluna>`, `ix_<tabela>_<colunas>` e `ux_<tabela>_<colunas>`.
- **Teste de migração:** a cada build, o Testcontainers sobe um PostgreSQL vazio e aplica todas as migrações do zero.
- **Seeds:** as 6 metas e as categorias padrão são criadas pela aplicação ao criar um espaço, não por migração. Os parâmetros fiscais de 2027 entram por migração, com a fonte anotada em `source_note`.
- **Saldos iniciais:** o histórico começa em outubro de 2026 e entra pela tela de primeira configuração (RF16), não por script.
- **Dados fictícios:** testes e demonstração usam um espaço de exemplo; seus valores reais ficam só no banco local.

## Perguntas em aberto e próximos passos

- [x] Teto do MEI pela data de emissão da nota (decidido). Você emite no dia 1 e recebe no mesmo mês, então só muda algo em nota de fim de ano paga em janeiro, como o 14º; o modelo guarda as duas datas, e vale conferir esse caso com o contador.
- [x] Percentuais das 6 metas editáveis por mês, com uma barra que obriga a soma a fechar 100% (decidido); o modelo já guarda por mês.
- [x] Só gastos por categoria no MVP; conta e saldo por conta ficam para depois (decidido).
- [ ] Dois espaços, Casa e Empresa, trocados por menu, com o repasse da empresa entrando na casa como entrada (decidido).
- [x] O DAS sai da conta PJ, no espaço Empresa; contador e INSS não existem este ano e entram em 2027 (decidido)
- [x] Repasse mensal de valor fixo, como preset na Empresa e entrada na Casa (decidido)
- [x] As mesmas 6 metas do AUVP mais tags livres (Luz, Alimentação, Uber), cada tag marcada como custo fixo ou variável (decidido)
- [x] Anexo é só o PDF da nota emitida no portal do MEI, ligado à nota (decidido)
- [ ] Busca por texto (digitar "uber" e ver todos os lançamentos com essa palavra) entra no MVP? Sugestão: sim, é barata
- [x] Limite e fatura por mês: cada cartão tem limite e uma fatura por mês (aberta, fechada, paga), que congela o total ao fechar, como o fechamento do mês (decidido)
- [x] Pagar a fatura não cria lançamento, para não contar o gasto duas vezes; o orçamento conta pela data da compra e a fatura mostra o caixa (decidido)
- [x] Índices para isso: único (card_id, reference_month); parcial em card_invoice onde status diferente de PAID (limite usado rápido); em transaction, (invoice_id) INCLUDE (amount) onde deleted_at é nulo, para somar a fatura só pelo índice, sem ler a tabela (decidido)
- [x] Forma de pagamento em todo gasto: cartão de crédito cadastrado (BTG, Nubank etc.) ou PIX, débito, dinheiro, boleto; CHECK garante que card_id existe se e só se for CREDIT (decidido)
- [x] Painel do teto do MEI: soma das notas emitidas no ano contra R$ 81.000 e R$ 97.200, calculada a cada nota cadastrada, sem guardar saldo (decidido)
- [x] Auditoria de quem mudou o quê e backup automático do banco no Docker (decidido)
- [x] Metas por mês: cada mês guarda os seus percentuais; mudar outubro não toca setembro, e o fechamento congela percentual, receita, orçamento e gasto (decidido)
- [x] Compra parcelada: ao cadastrar com N parcelas, o sistema cria os lançamentos dos meses seguintes (decidido)
- [x] Reabrir mês fechado: só o dono, com registro na auditoria (decidido)

**Próximos passos**

1. Revisar o MER e o DER e responder as perguntas acima.
2. Escrever as migrações V1 e V2 e o teste que aplica tudo do zero.
3. Escrever a primeira spec (lançamentos e fechamento do mês) apoiada neste modelo.
