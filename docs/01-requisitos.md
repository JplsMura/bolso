# Requisitos: sistema de finanças pessoais e PJ

Atualizado em 08/10/2026 · João Pedro

## Visão geral

O sistema substitui o bloco de notas mensal por um controle próprio de finanças da casa e da empresa: lançamentos do mês, faturas de cartão, teto do MEI e simulação de faturamento como ME. Ele roda local em Docker, nasce para um usuário e já é modelado para vários. Serve também de projeto-escola para praticar Spec-Driven Development, arquitetura hexagonal, DDD e uso de IA no desenvolvimento.

## Escopo e premissas

O MVP cobre o que hoje vive no bloco de notas e nas planilhas: o fluxo mensal, os cartões e o teto do MEI.

- **Execução:** local, em Docker Compose (API, front e banco).
- **Usuários:** só o João Pedro no início, com o modelo de dados já preparado para vários usuários.
- **AUVP:** sistema fechado, sem integração. O novo sistema complementa e permite exportar dados em CSV para conferência.
- **Dados:** valores reais ficam só no banco local. Seeds e testes usam dados fictícios.
- **Idioma e moeda:** português do Brasil e reais (R$), fuso America/Sao_Paulo.
- **Ponto de partida:** as planilhas de custos, cartões e o planejamento de 2027 definem os campos e as regras iniciais.

## Requisitos funcionais

O MVP tem dezenove requisitos; cenários, reserva, importação e histórico ficam para a versão seguinte.

| ID | Requisito | Fase |
| --- | --- | --- |
| RF01 | Lançar entradas e saídas do mês com categoria e tipo (fixo, variável, parcela, investimento) | MVP |
| RF02 | Fechar o mês: sobra do mês e sobra acumulada, como na aba Mês a mês | MVP |
| RF03 | Cartões: fatura por cartão e mês, parcelas com mês de término e previsão do mês seguinte | MVP |
| RF04 | Lançamentos recorrentes (luz, condomínio, internet, assinaturas) gerados a cada mês | MVP |
| RF05 | Registrar notas emitidas e mostrar o faturamento acumulado do ano | MVP |
| RF06 | Teto do MEI: acumulado contra R$ 81.000 e R$ 97.200, folga, alerta antes de uma nota que passa do limite e estimativa da guia complementar | MVP |
| RF07 | Simulador de ME: dado o líquido desejado, calcula o faturamento necessário com Simples, Fator R, contador e INSS | MVP |
| RF08 | Cadastro de usuário e login (e-mail com código de verificação e conta Google), com todos os dados isolados por usuário | MVP |
| RF09 | Cenários com premissas editáveis (benefícios da família, carro financiado, reserva) e comparativo | Depois |
| RF10 | Reserva e investimentos: saldo, aportes e metas | Depois |
| RF11 | Importar e exportar CSV | Depois |
| RF12 | Compartilhar o orçamento com outro usuário (convite) | Depois |
| RF13 | Histórico de alterações dos lançamentos | Depois |
| RF14 | Subir o arquivo da nota emitida (PDF ou XML): o sistema lê mês, valor e tomador, mostra os dados para conferência e só salva depois da confirmação | MVP |
| RF15 | Lançamentos diários: tela principal com o mês corrente, lançamento rápido do dia e total gasto até agora | MVP |
| RF16 | Saldos iniciais: o histórico começa em outubro de 2026, com faturas, parcelas, reserva e faturamento do ano informados à mão na primeira configuração | MVP |
| RF17 | Metas por categoria, como no AUVP: a renda do mês é dividida em seis categorias com percentual editável (Custos Fixos, Conforto, Metas, Prazeres, Conhecimento, Liberdade Financeira), e cada lançamento pertence a uma delas. A tela mostra gasto, previsto, restante e número de transações por categoria | MVP |
| RF18 | Sincronização das contas e cartões via Open Finance (Pluggy), por um adaptador de saída trocável; até lá, lançamento manual e importação de extrato | Depois |
| RF19 | Dois espaços (Casa e Empresa): o faturamento PJ vive na Empresa e o repasse para a pessoa física entra na Casa como receita. Saldo por conta fica fora do MVP; só gasto por categoria. | MVP |
| RF20 | Presets de lançamentos mensais (luz, água, internet, repasse): ao abrir o mês, usar o preset com o último valor e editar antes de confirmar. | MVP |
| RF21 | Compra parcelada: ao informar o número de parcelas, o sistema cria os lançamentos dos meses seguintes. | MVP |
| RF22 | Percentuais das metas por mês; ao fechar, o mês congela percentual, receita, orçamento e gasto de cada meta. Só o dono reabre, com auditoria. Tags livres (custo fixo ou variável) além das 6 metas. | MVP |
| RF23 | Cadastro de cartões de crédito (BTG, Nubank etc.) e forma de pagamento em todo gasto: cartão escolhido ou PIX, débito, dinheiro, boleto. A forma de pagamento é independente da meta e das tags. | MVP |
| RF24 | Painel do teto do MEI no espaço Empresa: a cada nota cadastrada mostra quanto falta para R$ 81.000 e para a tolerância de R$ 97.200. Auditoria de alterações e backup automático do banco. | MVP |
| RF25 | Limite do cartão e fatura por mês (aberta, fechada, paga): mostra limite usado e disponível, congela o total ao fechar e registra o pagamento sem duplicar o gasto no orçamento. | MVP |

> Nota de consistência: RF13 (histórico de alterações) está como "Depois", mas RF22 e RF24 já exigem auditoria no MVP. A auditoria de fechamento e reabertura entra no MVP; o histórico completo por lançamento fica para depois.

## Regras de negócio

Os valores abaixo vêm do planejamento atual e ficam como parâmetros com vigência por ano, nunca fixos no código, porque a lei e o contador podem mudar.

- **Teto do MEI:** R$ 81.000 por ano, com tolerância de 20% (R$ 97.200).
- **Até 20% acima do teto:** continua MEI até 31/12, paga a guia complementar sobre o excedente e vira ME em 1º de janeiro.
- **Mais de 20% acima:** exclusão retroativa a 1º de janeiro, com ano recalculado, juros e multa. O sistema avisa antes de registrar uma nota que cruza esse ponto.
- **Simples (Anexo III):** 6% só se o Fator R for de 28% ou mais, ou seja, pró-labore de pelo menos 28% do faturamento. Abaixo disso a empresa cai no Anexo V, a partir de 15,5%.
- **INSS do pró-labore:** 11% do pró-labore.
- **Líquido como ME:** fator × faturamento − custo mensal do contador (os dois são parâmetros). O fator vem da combinação de Simples e INSS e é recalculado quando a alíquota muda.
- **Cartão:** fatura paga integralmente todo mês. A fatura de um mês soma parcelas ativas e gastos avulsos, e uma parcela some após o mês de término.
- **Sobra do mês:** entradas menos todas as saídas, incluindo investimento. A 13ª nota entra como entrada de dezembro.
- **Fora do fluxo:** lançamentos marcados como pagos por terceiros (como a bateria do carro) ou já quitados em outro sistema não entram na sobra.

## Requisitos não funcionais

| ID | Requisito | Meta |
| --- | --- | --- |
| RNF01 | Execução com um único `docker compose up` (API, front e PostgreSQL) | Obrigatório |
| RNF02 | Isolamento por usuário: todo dado carrega o dono e toda consulta filtra por ele | Obrigatório |
| RNF03 | Login sem senha: código por e-mail e Google (OIDC), sessão por cookie HttpOnly com expiração | Obrigatório |
| RNF04 | Valores monetários em `BigDecimal` com 2 casas e arredondamento definido; nunca `double` | Obrigatório |
| RNF05 | Migrações de banco versionadas (Flyway) | Obrigatório |
| RNF06 | Backup: dump diário do banco para uma pasta local e restauração documentada | Obrigatório |
| RNF07 | Testes automatizados no back e no front rodando no CI local e no Docker | Obrigatório |
| RNF08 | Cobertura de pelo menos 80% no domínio e nos casos de uso | Meta |
| RNF09 | Resposta das telas principais abaixo de 500 ms em uso local | Meta |
| RNF10 | Contrato da API em OpenAPI, com tipos TypeScript gerados a partir dele | Meta |
| RNF11 | Segredos fora do repositório (arquivo `.env` ignorado pelo Git) | Obrigatório |

> RNF03 foi ajustado ao exportar para o projeto: no app ainda constava "senhas com hash forte (BCrypt ou Argon2) e sessão por token", que ficou desatualizado depois da decisão de login por código e Google.

## Arquitetura

[Diagrama no app: arquitetura hexagonal, com entrada, núcleo e saída. Veja `02-arquitetura-backend-e-ia.md`.]

O front chama a API REST, que aciona casos de uso; o domínio decide as regras e grava pelo adaptador de persistência. Cada contexto vira um pacote com seu modelo e seus casos de uso, e o DDD entra só onde há regra de negócio (MEI, ME, cartões). A stack é Java 25 (LTS) com Spring Boot 4.1, PostgreSQL, React, TypeScript e Vite.

## Spec-Driven Development e testes

A especificação vem antes do código e é a fonte de verdade: cada funcionalidade nasce como uma spec, e os testes saem dos cenários dela.

1. **Spec:** o que e por quê, com regras de negócio e cenários em Dado/Quando/Então.
2. **Plano:** decisões técnicas, portas e adaptadores afetados, mudanças no banco.
3. **Tarefas:** passos pequenos, cada um com um teste que falha primeiro.
4. **Implementação:** código mínimo para passar, revisão do diff e refatoração.

Cada funcionalidade fica em `docs/specs/NNN-nome/` com `spec.md`, `plan.md` e `tasks.md`. Decisões de arquitetura vão em ADRs curtos.

| Camada | Ferramentas | O que testa |
| --- | --- | --- |
| Domínio (Java puro) | JUnit 5, AssertJ | Regras do MEI, Simples, Fator R, fechamento do mês, sem Spring e sem banco |
| Casos de uso | JUnit 5, Mockito | Orquestração com portas simuladas |
| Adaptadores | Spring Boot Test, Testcontainers (PostgreSQL) | Repositórios, REST e segurança |
| Arquitetura | ArchUnit | O domínio não depende de Spring nem de adaptadores |
| Front | Vitest, React Testing Library, MSW | Componentes, hooks e chamadas à API simuladas |
| Ponta a ponta (depois) | Playwright | Fluxos completos: lançar, fechar o mês, simular ME |

## Boas práticas de IA e economia de tokens

Entendi "RAG" como as boas práticas de uso de IA no desenvolvimento. Se você quis dizer RAG de verdade (busca em documentos), isso entra como item opcional futuro, por exemplo perguntar às suas specs em linguagem natural.

- **Contexto fixo e curto:** um `CLAUDE.md` na raiz com stack, comandos de build e teste, regras de arquitetura e convenções. A IA lê isso toda vez, então cada linha precisa valer.
- **Spec como contexto:** passar o caminho da spec e da tarefa, não colar arquivos inteiros na conversa.
- **Uma tarefa por sessão:** limpar o contexto entre tarefas e resumir o estado quando a conversa ficar longa.
- **Planejar antes de codar:** pedir o plano, revisar e só então implementar.
- **Testes como verificador:** o teste que falha primeiro define "pronto" e evita idas e vindas.
- **Saída enxuta:** pedir diffs e só os trechos alterados, não arquivos completos.
- **Modelo certo para a tarefa:** modelo menor para código repetitivo, testes e refatorações; modelo maior para arquitetura e depuração difícil.
- **Prefixo estável:** manter instruções e contexto fixos no começo do prompt para aproveitar cache.
- **Revisão humana:** ler todo diff, rodar os testes e commitar em passos pequenos.
- **Privacidade:** dados financeiros reais não vão para a IA; usar seeds fictícios e deixar segredos fora do repositório.

## Evolução e manutenção

O que torna o sistema fácil de manter em 2027 e depois, e as próximas specs candidatas.

**Manutenção**

- **Monólito modular:** um módulo por contexto, sem microsserviços. O Spring Modulith pode verificar as fronteiras entre módulos junto com o ArchUnit.
- **Parâmetros fiscais com vigência:** tela para editar alíquotas e limites por ano, com testes de regressão de cada ano.
- **CI:** build com testes do back e do front, ArchUnit, lint e formatação (Spotless, ESLint, Prettier) a cada commit.
- **Dependências:** atualização automática com Renovate ou Dependabot e alerta de vulnerabilidades, o que importa porque o Spring Boot 4.0 sai de suporte em dezembro de 2026.
- **Observabilidade:** logs estruturados, Spring Boot Actuator e healthcheck no Docker.
- **Dados:** backup com restauração testada, dump criptografado e exportação completa dos dados do usuário.
- **Segurança:** limite de tentativas no login, refresh token, autenticação em dois fatores opcional e segredos fora da imagem.
- **Documentação viva:** ADRs, specs versionadas junto do código, README de execução e diagramas em Mermaid.

**Leitura da nota (RF14)**

- Começar pelo XML da nota, que traz os campos estruturados; PDF por leitura de texto; OCR só como último recurso.
- Mostrar sempre os dados lidos para conferência e guardar o arquivo original.
- Evitar duplicidade pelo número e pela chave da nota.

> Atualização posterior: o comprovante é só o PDF da nota do portal do MEI; leitor por regras primeiro, IA só como fallback com confirmação.

**Specs candidatas, na ordem em que fazem sentido**

1. Cenários e comparativo, trazendo para o sistema as abas A, B e C da planilha.
2. Reserva, investimentos e metas, como a entrada do carro.
3. Orçamento por categoria, com alerta quando o gasto livre passa do previsto.
4. Lembretes: vencimento das faturas, do DAS e da declaração anual.
5. Relatório anual para o contador, com notas do ano e faturamento por mês.
6. Importação de extratos (OFX ou CSV) para conferir os lançamentos.
7. Compartilhar o orçamento com outra pessoa da casa, com convite.
8. Uso no celular como PWA.

## Fora de escopo, riscos e próximos passos

**Fora de escopo no MVP:** integração com a AUVP ou com bancos, emissão de notas, cálculo oficial de impostos (o sistema estima e o contador confirma), aplicativo móvel e hospedagem na nuvem.

**Riscos**

- Regras fiscais mudam; por isso ficam como parâmetros com vigência, e o Super MEI está parado no Senado.
- Falsa precisão: a guia complementar e o Simples são estimativas e precisam ser rotuladas assim na tela.
- Excesso de arquitetura para um sistema pequeno; o DDD entra só onde há regra de negócio (MEI, ME, cartões).

**Perguntas em aberto**

- [x] Login por e-mail com código de verificação e por conta Google (decidido)
- [ ] Categorias: lista padrão que você pode editar, mais a tela principal de lançamentos diários (proposta, confirmar)
- [x] Histórico a partir de outubro de 2026, com saldos iniciais informados à mão (decidido)
- [ ] Java 25 (LTS) e Spring Boot 4.1 no lugar de Java 21 e Spring Boot 3 (proposta, confirmar). Decidido depois nos documentos de arquitetura.
- [ ] Em que formato você recebe a nota emitida: PDF, XML ou os dois? Respondido depois: só o PDF do portal do MEI.

**Próximos passos**

1. Revisar este documento e responder as perguntas acima.
2. Escrever a primeira spec (lançamentos e fechamento do mês).
3. Criar o repositório com `CLAUDE.md`, Docker Compose e a estrutura hexagonal vazia.
