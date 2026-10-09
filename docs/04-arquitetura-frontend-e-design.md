# Arquitetura do front-end e design

08/10/2026 · João Pedro

## Resumo

Recomendamos **uma única aplicação React com TypeScript, organizada por funcionalidade**, espelhando os módulos do back-end. **Micro front-end não vale a pena para este projeto.** Ele resolve um problema de organização (várias equipes publicando telas de forma independente). As fontes consultadas sugerem considerar só a partir de umas 3 a 4 equipes de front-end e apontam custo alto de entrada (contratos, roteamento, CSS isolado), peso extra de carregamento e, em um caso relatado, volta ao monolito. Com um desenvolvedor, só trariam custo.

O que ganhamos com a organização por funcionalidade é a mesma separação em fatias, sem o custo: cada pasta de funcionalidade tem as suas telas, chamadas à API, regras e testes, e uma regra de lint impede que uma importe o interior da outra. Se um dia houver equipes separadas, essas fatias podem ser extraídas.

- **Stack:** React 19, TypeScript estrito, Vite, TanStack Router e Query, React Hook Form com Zod, Tailwind CSS.
- **Tipos sincronizados com o back-end:** o cliente da API é gerado a partir do contrato OpenAPI do Spring, então uma mudança de campo quebra a compilação em vez de quebrar a tela.
- **Testes:** Vitest para regras, Testing Library para componentes, MSW para simular a API, Playwright para poucos fluxos completos.
- **Design:** telas desenhadas primeiro no recurso de design do Claude, antes de codar.

## Telas e navegação

No topo fica o seletor de espaço (**Casa** ou **Empresa**), e o menu muda conforme o espaço escolhido. O espaço vai no endereço da página, então cada tela tem link próprio e voltar/avançar do navegador funcionam.

| Espaço | Tela | O que mostra e permite |
| --- | --- | --- |
| Todos | Login | código por e-mail ou Google |
| Casa | Início | mês atual: as 6 metas com barra de gasto e saldo, gastos recentes, atalho para lançar |
| Casa | Lançamentos | lista do mês com busca por texto, filtro por meta, tag e forma de pagamento; formulário rápido de gasto com meta, tag, cartão ou PIX/débito e parcelas |
| Casa | Presets | custos mensais (luz, água, internet) para usar ao abrir o mês, com o último valor editável |
| Casa | Cartões | cada cartão com limite usado e disponível, fatura do mês (aberta, fechada, paga) |
| Casa | Mês | percentuais das metas com barra somando 100%, fechar mês, histórico dos meses fechados |
| Casa | Tags | cadastro de tags e se são custo fixo ou variável |
| Empresa | Painel do teto | faturamento do ano contra R$ 81.000 e R$ 97.200 |
| Empresa | Notas | enviar o PDF da nota, ver a lista e os dados lidos para confirmar |
| Empresa | Custos e repasse | DAS e o repasse fixo para a Casa |
| Todos | Configurações | espaços, membros, auditoria, backup |

**Prioridade:** Lançamentos e Início são as telas de uso diário e devem ser as mais rápidas de usar, inclusive no celular, já que você lança gastos no dia a dia.

## Arquitetura

**Por funcionalidade, não por tipo de arquivo.** Em vez de uma pasta gigante de componentes e outra de hooks, cada funcionalidade tem a sua pasta, com os mesmos nomes dos módulos do back-end.

```
src/
  app/                 roteador, provedores, layout com o seletor Casa/Empresa
  features/
    identidade/        login, espaços
    lancamentos/       lista, formulário, presets, tags, parcelas
    cartoes/           cartões, faturas, limite
    orcamento/         metas, percentuais, fechar mês
    faturamento/       notas, painel do teto
      api/             chamadas e hooks de dados (TanStack Query)
      model/           tipos, esquemas Zod e funções puras de cálculo
      ui/              componentes da funcionalidade
      pages/           telas
  shared/
    ui/                botões, campos, tabela (sem regra de negócio)
    api/               cliente gerado do OpenAPI
    lib/               dinheiro, datas, formatação em pt-BR
```

(`api/`, `model/`, `ui/` e `pages/` existem dentro de cada pasta de funcionalidade.)

**Regras que o lint verifica:**

- uma funcionalidade não importa o interior de outra, só o que ela expõe no `index.ts`;
- `shared` não importa de `features`;
- componentes de interface não chamam a API direto; usam os hooks da pasta `api`.

**Estado.** Dados que vêm do servidor (lançamentos, faturas) ficam no TanStack Query, com cache e atualização após gravação. O resto é estado local do componente. O espaço atual e o mês vivem no endereço, não numa biblioteca de estado global.

**Dinheiro.** Nunca em `number` com vírgula flutuante. O back-end envia o valor como texto (`"110.00"`); o front calcula em centavos inteiros e formata com `Intl.NumberFormat` em pt-BR. Quem decide totais e saldos é o back-end; o front só exibe.

**Contrato com o back-end.** O Spring publica o contrato OpenAPI e o cliente TypeScript é gerado dele. Isso casa com o SDD: a spec define o contrato, o contrato gera os tipos dos dois lados.

**Login.** Sessão em cookie `HttpOnly`, sem guardar token no `localStorage`.

## Stack

| Peça | Escolha | Observação |
| --- | --- | --- |
| Biblioteca | React 19 | |
| Linguagem | TypeScript em modo estrito | |
| Build | Vite | |
| Rotas | TanStack Router | tipagem das rotas; React Router é a alternativa mais comum |
| Dados do servidor | TanStack Query | cache, recarga, estados de carregando e erro |
| Formulários | React Hook Form | formulário de gasto é o mais usado do sistema |
| Validação | Zod | o mesmo esquema valida o formulário e a resposta da API |
| Estilo | Tailwind CSS | você já usa |
| Componentes base | shadcn/ui (Radix) | acessíveis, código fica no projeto |
| Gráficos | Recharts, só se precisar | barras das metas dão conta com CSS |
| Qualidade | ESLint, Prettier, axe-core | acessibilidade checada nos testes |

A lista se apoia em um modelo de projeto React 2026 que usa React 19, Vite, TypeScript estrito, TanStack Router e Query, Vitest e Playwright. Formulários, validação, estilo e componentes base são escolha nossa, não da fonte, e podem ser trocados sem refazer o resto. As versões exatas devem ser conferidas ao criar o projeto.

## Testes

Mesma ideia do back-end: muitos testes rápidos na base, poucos lentos no topo.

| Nível | Ferramenta | O que testa | Quantidade |
| --- | --- | --- | --- |
| Unidade | Vitest | funções puras: dinheiro em centavos, soma dos percentuais das metas em 100%, cálculo de parcelas | muitos |
| Componente | Testing Library | como o usuário usa: preencher o formulário de gasto, ver erro de validação, trocar de espaço | bastante |
| API simulada | MSW | respostas da API de mentira, com os tipos do contrato, sem subir o back-end | em apoio ao anterior |
| Acessibilidade | axe-core | contraste, rótulos de campo, navegação por teclado | nos testes de componente |
| Ponta a ponta | Playwright | poucos fluxos completos: entrar, lançar um gasto no cartão, fechar o mês | poucos |

**Regras de bolso:** testar o que o usuário vê, não detalhes internos do componente; cada spec de funcionalidade já lista os exemplos que viram teste; o loop de agente do front usa `vitest`, `tsc` e `eslint` como condição de parada, assim como o ArchUnit no back-end.

## Design

O Claude tem um recurso de design que monta telas como pranchas (artboards) numa tela de desenho, que você vê, comenta e pede ajustes, sem escrever código. Para quem nunca usou, o caminho é:

1. **Definir o visual base primeiro:** cores, tipografia, espaçamento, forma dos botões e dos cartões, modo claro e escuro. Pode ser um mini sistema de design simples, que depois vira os tokens do Tailwind.
2. **Desenhar as telas de uso diário:** Início (as 6 metas) e Lançamentos, em versão celular e computador, porque o celular é onde você lança gastos.
3. **Revisar e ajustar** pedindo mudanças em linguagem normal, até aprovar.
4. **Só depois desenhar as demais telas**, reaproveitando o mesmo visual.
5. **Passar para código:** as telas aprovadas viram referência para os componentes React, e os tokens viram a configuração do Tailwind.

**Referência que você já tem:** as capturas da AUVP (metas com barra, transações recentes) mostram o que funciona para você e o que falta. Podemos partir delas, sem copiar a marca nem o visual exato.

**O que decidir antes de desenhar:** o clima do visual (sóbrio e limpo, ou mais colorido), se haverá modo escuro e se o celular ou o computador é o foco.

> Atualização: todas as telas do MVP já foram desenhadas. Os arquivos estão em `design/project/` e a paleta (modo escuro) está em `docs/CONTEXTO.md`.

## Decisões, riscos e próximos passos

**Decisões tomadas**

- [x] Uma aplicação por funcionalidade, sem micro front-end
- [x] TanStack Router, pelas rotas tipadas e por combinar com o TanStack Query; React Router fica como alternativa
- [x] Visual: modo escuro como principal, sóbrio e limpo, com cor só para dar significado (metas e alertas)
- [x] Responsivo: celular, tablet e computador, com o celular como referência de uso diário

**Riscos**

- Montar a estrutura por funcionalidade com regras de lint toma um dia de configuração; compensa porque o código gerado por IA tende a misturar camadas sem essa trava.
- Cliente gerado do OpenAPI exige que o back-end publique o contrato desde o início.
- Desenhar todas as telas antes de codar atrasa; desenhar só as duas principais e seguir reduz o risco.

**Próximos passos**

1. Responder as quatro decisões acima.
2. Definir o visual base e desenhar Início e Lançamentos no recurso de design.
3. Escrever o `CLAUDE.md` do front-end e a primeira spec de tela.

**Fontes consultadas:** [Micro-frontends em 2026: quando funcionam](https://blog.codercops.com/blog/micro-frontends-module-federation-architecture-2026) · [Modelo de projeto React para 2026](https://dev.to/asudbury/modern-react-template-for-2026-4clc)
