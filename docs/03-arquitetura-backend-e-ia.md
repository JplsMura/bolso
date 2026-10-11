# Arquitetura do back-end e uso de IA

08/10/2026 · João Pedro

## Resumo

Para um sistema que começa com um usuário e precisa crescer sem reescrever, recomendamos um **monolito modular com arquitetura hexagonal**: uma aplicação só, dividida em módulos que correspondem aos contextos do domínio, cada um com o domínio puro no centro e o banco, a web e a IA nas bordas. Microsserviços não se justificam aqui: seriam custo de operação sem ganho.

- **Domínio:** cinco contextos (Identidade, Lançamentos, Cartões, Orçamento, Faturamento PJ), que conversam por interfaces públicas e eventos, sem chave estrangeira entre contextos.
- **Fronteiras verificadas por teste:** Spring Modulith e ArchUnit quebram o build se um módulo acessar o interior de outro.
- **IA no desenvolvimento:** specs escritas antes do código (SDD), um arquivo curto de contexto do projeto, e ciclos de agente que só terminam quando os testes passam.
- **IA no produto:** só onde ajuda de verdade, que é ler o PDF da nota e sugerir categoria. RAG não entra no MVP: seus dados cabem numa consulta SQL.

## Domínios (DDD)

Cada contexto tem a sua própria linguagem: "fatura" no contexto de Cartões é o que o banco cobra, e "nota" no Faturamento PJ é o que você emite. Separar evita que uma palavra com dois significados contamine o código. Os contextos abaixo seguem os esquemas do banco já modelado.

| Contexto | O que é dono | Regras que moram aqui |
| --- | --- | --- |
| Identidade | usuário, espaço (Casa ou Empresa), membro, papel | login por código no e-mail e Google; quem vê qual espaço |
| Lançamentos | lançamento, tag, preset, parcelamento | valor positivo; forma de pagamento; cartão só se for crédito; parcelas criadas nos meses seguintes |
| Cartões | cartão, fatura do mês, limite | fatura aberta, fechada ou paga; limite usado; dia de fechamento |
| Orçamento | as 6 metas, percentual por mês, fechamento do mês | percentuais somam 100%; fechar congela o mês; só o dono reabre |
| Faturamento PJ | empresa, regime, nota emitida, tomador, parâmetros fiscais | teto do MEI (R$ 81.000 e tolerância de R$ 97.200); Fator R; cálculo do imposto |

**Como se falam.** Um contexto nunca lê a tabela de outro. Conversam por duas vias:

- **Chamada síncrona** a uma interface pública do outro módulo, quando precisam da resposta na hora. Exemplo: Lançamentos pergunta a Cartões em qual fatura cai uma compra feita nesta data.
- **Evento** quando só avisam que algo aconteceu. Exemplo: Faturamento PJ publica "nota emitida" e Lançamentos cria a receita correspondente, guardando só uma referência de origem, sem chave estrangeira.

**Agregados.** Dentro de cada contexto, o que precisa ser consistente junto fica num agregado, e só a raiz é acessada de fora: a fatura com os seus totais, o mês de orçamento com as suas 6 metas, o parcelamento com as suas parcelas. Isso casa com o bloqueio otimista (coluna `version`) já previsto no banco.

## Arquitetura do back-end

**Monolito modular + hexagonal.** Um único deploy no Docker, mas com fronteiras internas rígidas. Se um dia um módulo precisar virar serviço separado, a fronteira já existe. Dentro de cada módulo o domínio fica no centro e não conhece Spring, banco nem HTTP; tudo que é externo entra e sai por portas.

> [Diagrama no app: um módulo em camadas e os cinco módulos do sistema]

Estrutura de pacotes de um módulo (exemplo do módulo de Lançamentos):

```
lancamentos/
  LancamentosApi.java      interface pública do módulo (o que os outros enxergam)
  domain/                  entidades, objetos de valor, regras. Java puro
  application/             casos de uso e portas (interfaces) de entrada e saída
  adapter/in/web/          controllers REST e DTOs
  adapter/out/persistence/ repositório sobre o PostgreSQL
```

**Regras que o build verifica:**

- `domain` não importa nada de `adapter`, de Spring nem de outro módulo (ArchUnit).
- Um módulo só acessa a raiz pública de outro, nunca os pacotes internos (`ApplicationModules.verify()` do Spring Modulith).
- Eventos entre módulos são publicados e ouvidos pelo mecanismo do Modulith, que pode registrá-los no banco para não perder nenhum se a aplicação cair.
- Cada módulo pode ser testado sozinho, com o banco real em contêiner (Testcontainers).

## Stack e versões

| Peça | Escolha | Por quê |
| --- | --- | --- |
| Linguagem | Java 25 (LTS) | Spring Boot 4.1 aceita Java 17 a 26; a LTS mais nova dá mais anos de suporte |
| Framework | Spring Boot 4.1 | Lançado em 30/06/2026, com suporte aberto até 31/07/2027; a 3.5 já saiu do suporte aberto |
| Fronteiras | Spring Modulith e ArchUnit | Verificam os módulos no build e dão eventos entre eles |
| Banco | PostgreSQL | Já modelado em cinco esquemas |
| Migrações | Flyway | Versão do banco no Git, aplicada do zero em teste |
| Acesso a dados | JPA nos agregados; jOOQ nas telas de consulta | Ver abaixo |
| Testes | JUnit 5, Testcontainers, ArchUnit | Banco real em contêiner; nada de H2 fingindo ser Postgres |
| Login | Spring Security com código por e-mail e Google (OIDC) | Os dois métodos que você pediu |

**Acesso a dados em dois modos.** Para gravar, o agregado é carregado, a regra roda no domínio e ele é salvo: JPA resolve bem e já traz o bloqueio otimista. Para telas de leitura pesadas (fatura do mês, painel do teto do MEI, fechamento), usar consulta SQL direta, tipada com jOOQ ou com `JdbcClient`, evita carregar objetos só para somar. Começamos só com JPA e `JdbcClient` e adotamos jOOQ se as consultas crescerem; a decisão fica reversível porque está atrás da porta de saída.

**Verificar antes de codar:** a versão do Spring Modulith compatível com o Boot 4.1 (a documentação atual é da linha 2.x) e o suporte do Testcontainers ao Boot 4.1.

## IA no desenvolvimento

A ideia central é que a IA trabalhe em pequenos passos que se verificam sozinhos, em vez de receber um pedido grande e responder com código que ninguém confere.

**1. Spec antes do código (SDD).** Cada funcionalidade nasce como uma spec curta em `specs/`: o problema, as regras, exemplos de entrada e saída e os critérios de aceite. A spec vira os testes, e os testes dirigem o código. Para este projeto, os documentos de requisitos e de banco já são a base; a primeira spec seria Lançamentos e fechamento do mês.

**2. Arquivo de contexto enxuto (AGENTS.md ou CLAUDE.md).** Ele diz ao agente o que ele não adivinha: comandos exatos de build, teste e lint, as regras de arquitetura ("domínio não importa Spring"), nomes do domínio e o que nunca fazer. As recomendações consultadas convergem em:

- manter o arquivo curto e dividir por pasta (um `CLAUDE.md` em `backend/`, outro em `frontend/`);
- incluir comandos de validação exatos e anti-padrões do código, com exemplo;
- não repetir o que o modelo já faz por padrão, nem princípios genéricos como "escreva código legível";
- um estudo citado na pesquisa indica que arquivos de contexto longos aumentaram o custo por sessão em mais de 20% com ganho mínimo. Começar pequeno e crescer só quando o agente errar repetidamente.

**3. Loops de agente (loop engineering).** Em vez de pedir e conferir a cada passo, você desenha um ciclo: objetivo, ação, observação, ajuste, repetido até uma condição de parada que a máquina consegue medir. Pontos que as fontes repetem:

- **A verificação é objetiva:** testes passando, ArchUnit e Modulith verdes, lint limpo. O agente não dá nota ao próprio trabalho; um verificador separado costuma dar resultado melhor que a autoavaliação.
- **Memória em arquivo:** um registro do que já foi feito e decidido, no Git, para não repetir erro e para a conversa poder recomeçar limpa.
- **Padrão Ralph:** cada rodada começa com contexto limpo, recarrega a spec e executa uma tarefa. Serve bem para backlog de tickets com TDD e para subir cobertura de testes. Fique com loops que só mexem no repositório: o risco real está em ações fora dele (um caso citado apagou um banco de produção).
- **Gates de humano:** nada entra na `main` sem testes verdes e sem você ler o diff.

**4. Economia de tokens.** O custo cresce porque cada chamada reenvia todo o histórico. O que mais reduz, segundo os estudos citados:

- cortar saídas de ferramentas (a maior parte do crescimento vem delas; até cerca de 40% a 60% era removível sem perda de desempenho em um benchmark);
- recomeçar a conversa por fase, passando o estado por arquivos, em vez de uma sessão enorme;
- cache do prefixo fixo (leitura em cache custa cerca de um décimo do valor sem cache);
- planejar com um modelo maior e executar com um menor;
- subagentes para tarefas isoladas, de modo que o histórico delas não infle a conversa principal;
- alertar quando o gasto por tarefa passar do dobro do normal.

**5. Testes como rede de segurança.** Pirâmide: muitos testes de domínio (rápidos, sem Spring), alguns de módulo com Testcontainers e poucos de ponta a ponta. Teste de arquitetura roda sempre.

### Agentes, skills e hooks no projeto

Sim, mas pequenos e no processo de desenvolvimento, não dentro do produto. A regra é só criar um quando o mesmo erro ou o mesmo passo a passo se repetir.

| Peça | Para que serve | Exemplo neste projeto |
| --- | --- | --- |
| `CLAUDE.md` | regras fixas, sempre carregadas | comandos de build e teste, regras hexagonais |
| Skill (`.claude/skills/<nome>/SKILL.md`) | receita que só carrega quando a tarefa pede | `nova-spec`, `novo-modulo`, `nova-migracao` |
| Subagente (`.claude/agents/<nome>.md`) | assistente com contexto e permissões próprios | `revisor-de-arquitetura`, somente leitura, confere as regras hexagonais e de módulos |
| Hook | comando automático, sem depender do modelo | rodar testes e ArchUnit depois de editar; bloquear comandos destrutivos |

**Ordem para adotar:** primeiro o `CLAUDE.md`; depois duas ou três skills (spec, módulo, migração); depois um subagente revisor, separando quem faz de quem confere; por fim os hooks. Skills ajudam na economia de tokens porque só a descrição fica na conversa e o corpo carrega quando necessário, e subagentes mantêm o histórico da revisão fora da conversa principal. Servidor MCP (por exemplo, leitura do banco) fica para depois.

## IA no produto

Usar IA dentro do sistema só vale onde um código comum não resolve bem. Para este sistema, a ordem de prioridade é esta:

- **Ler a nota emitida (RF14).** O PDF do portal do MEI tem formato estável, então o primeiro caminho é um leitor comum, que extrai o texto e acha número, data, tomador e valor por regras. A IA entra só como plano B quando o leitor falha, e o resultado sempre aparece para você confirmar antes de gravar.
- **Sugerir categoria e tag.** Primeiro regras simples ("Uber" vira Transporte). A IA sugere só o que as regras não cobrem, e cada sugestão aceita vira uma regra nova.
- **Perguntas sobre o seu histórico** ("quanto gastei com Uber em 2026?"). Isso se resolve com consulta ao banco, não com RAG.

**Onde o RAG não vale a pena (por enquanto).** RAG serve para achar trechos relevantes em muito texto solto. Seus dados são tabelas com valores e datas, e a busca por texto da descrição já resolve com o próprio PostgreSQL. RAG traria um banco vetorial, custo e risco de resposta errada sobre números, sem ganho. Faria sentido mais tarde só se houvesse documentos longos, como contratos ou manuais.

**Cuidados.**

- Dados financeiros vão para uma API externa quando a IA é usada: enviar o mínimo (texto da nota, nunca CPF, conta ou saldos) e deixar isso configurável e desligado por padrão.
- A IA sugere, o cálculo de dinheiro e de imposto fica sempre em código testado.
- Chamada de IA atrás de uma porta de saída, para trocar de provedor ou rodar sem ela.

## Decisões, riscos e próximos passos

**Decisões tomadas**

- [x] Monolito modular com hexagonal e Spring Modulith (recomendado) ou camadas simples?
- [x] Começar com JPA e `JdbcClient`, deixando jOOQ para depois (recomendado)?
- [x] IA no produto: só leitor de nota com plano B de IA e sugestão de categoria, sem RAG no MVP?
- [x] Loops de agente só dentro do repositório, com merge manual?

**Riscos**

- Compatibilidade do Spring Modulith e do Testcontainers com o Boot 4.1, que é recente: conferir antes das migrações.
- Arquitetura demais para um sistema pequeno: manter um módulo por contexto e nada além disso.
- Loop sem condição de parada objetiva gasta tokens sem convergir.

**Próximos passos**

1. Levantamento do front-end e do design (próximo documento).
2. Definir a camada de IA do desenvolvimento: `CLAUDE.md`, pasta `specs/` e o primeiro loop.
3. Migrações do banco e depois o back-end, começando por Lançamentos.

**Fontes consultadas:** [Spring Modulith, fundamentos](https://docs.spring.io/spring-modulith/reference/fundamentals.html) · [IBM, o que é loop engineering](https://www.ibm.com/think/topics/loop-engineering) · [Augment Code, custo de tokens em loops](https://www.augmentcode.com/guides/ai-agent-loop-token-cost-context-constraints) · [Packmind, boas práticas de contexto](https://packmind.com/context-engineering-ai-coding/context-engineering-best-practices/) · [Decoding AI, Ralph loops](https://www.decodingai.com/p/ralph-loops)
