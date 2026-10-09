# Bolso: documento de contexto

Use este arquivo como instrução do Projeto no app do Claude e como base do CLAUDE.md. Detalhes completos estão nos 4 documentos (links no fim).

## O que é
Sistema web para o João Pedro (dev, PJ/MEI) controlar finanças pessoais e da empresa. Substitui o bloco de notas mensal. A AUVP é plataforma fechada, sem integração.

## Como conversar com ele
Português do Brasil, direto, tom casual-profissional. Ele dita por voz, então tolere erros de transcrição. Respostas curtas; detalhe só quando pedir.

## Produto
- Dois espaços, Casa e Empresa, trocados por menu.
- Empresa: recebe notas (PDF do portal do MEI é o único anexo), paga o DAS (conta PJ, vence dia 20), faz repasse fixo mensal para a Casa (entra como receita lá). Contador, INSS e Simples só a partir de jan/2027 (ME).
- Painel do teto do MEI: teto R$ 81.000, tolerância 20% (R$ 97.200). Exemplo (valores fictícios): R$ 91.000,00 no ano sem a 14ª nota; emitir a 14ª (R$ 7.000,00) no mesmo ano daria R$ 98.000,00 (desenquadramento retroativo), então emitir em janeiro do ano seguinte.
- Seis metas no estilo AUVP: Custos Fixos, Conforto, Metas, Prazeres, Liberdade Financeira, Conhecimento. Percentuais editáveis por mês, somando 100%. Cada mês guarda os seus.
- Fechamento do mês congela (percentual, base de receita, orçamento, gasto, saldo) em `month_closing_goal`. Só o dono reabre, com auditoria.
- Tags livres (custo fixo / variável) além das seis metas.
- Presets (`recurring_rule`): custos fixos mensais, usam o último valor, editáveis, nada lançado sozinho.
- Parcelamento cria os meses futuros ao cadastrar.
- Todo gasto tem forma de pagamento: PIX, DEBIT, CREDIT, CASH, BOLETO, TRANSFER, OTHER. `card_id` obrigatório só se CREDIT.
- Fatura por (cartão, mês de referência): OPEN / CLOSED / PAID. Total congela ao fechar. Limite usado = faturas não pagas, incluindo parcelas futuras. Pagar a fatura não cria lançamento. Orçamento conta pela data da compra.
- MVP controla só despesa por categoria; saldo de conta fica para depois.
- Auditoria de alterações e backup diário automático.
- Busca por texto: sugerida no MVP, ainda não confirmada.

## Arquitetura
- Back: Java 25 LTS, Spring Boot 4.1, monólito modular (Spring Modulith) + hexagonal. 5 contextos: Identidade, Lançamentos, Cartões, Orçamento, Faturamento PJ. Verificação com `ApplicationModules.verify()` e ArchUnit.
- Banco: PostgreSQL, schemas identity/finance/billing/planning/platform, centrado em workspace, UUID v7, numeric(14,2), `reference_month`, locking otimista, Flyway, Testcontainers.
- Persistência: JPA nos agregados, JdbcClient nas leituras.
- Auth: código por e-mail + Google OIDC, Spring Security, cookie HttpOnly.
- Front: React 19 + TypeScript strict + Vite, um app só (sem micro-frontend), organizado por feature espelhando o back. TanStack Router + Query, React Hook Form + Zod, Tailwind, shadcn/ui. Cliente gerado do OpenAPI.
- Dinheiro: string na API, centavos no front, Intl.NumberFormat pt-BR.

## Visual
Modo escuro principal, sóbrio, cor só para significado, responsivo (celular é a referência de uso diário), fonte DM Sans.
Paleta escura: fundo #0E1315, superfície #161C1F, elevado #1E2629, borda #2B353A, texto #E9EEEC, apagado #9BA8A5, primária #2DD4BF (texto sobre ela #04231F), aviso #F2C94C, perigo #F27A72, info #6EA8FE.
Metas: Custos Fixos #6EA8FE, Conforto #A3E06B, Metas #C79BFF, Prazeres #FF9F5A, Liberdade Financeira #FF8FB1, Conhecimento #5CD6E8.

## Como trabalhamos com IA
- SDD: spec em `docs/specs/` antes do código.
- Uma feature por conversa. Contexto limpo a cada fase; memória em arquivos.
- Verificação objetiva (testes, ArchUnit, lint). Quem confere não é quem escreveu. Merge humano.
- Economia de tokens: saída de ferramenta enxuta, planejar com modelo maior e executar com menor, subagentes.
- Skills previstas: `nova-spec`, `novo-modulo`, `nova-migracao`. Hooks: rodar testes/ArchUnit após editar, bloquear comandos destrutivos.
- IA no produto: leitor de PDF por regras primeiro, IA só como fallback com confirmação; sugestão de categoria por regras; sem RAG no MVP; dados mínimos, desligado por padrão.

## Ordem das features
1. Base do projeto (Docker, Boot 4.1 + checagem Modulith, React/Vite, CLAUDE.md, testes de arquitetura)
2. Identidade (login por código + Google, espaços)
3. Lançamentos
4. Cartões
5. Orçamento (metas, fechamento)
6. Faturamento PJ

## Pendências
- ~~Confirmar compatibilidade Spring Modulith 2.1.1 com Boot 4.1 e Testcontainers~~: confirmada na 001 (`mvnw verify` verde).
- Confirmar busca por texto no MVP.
- Telas de estado vazio e versão clara (referência).

## Onde está o resto
Documentos no app (Requisitos, Modelagem do banco, Arquitetura do back-end e IA, Arquitetura do front-end e design) e o canvas "Sistema de finanças". Cópias em Markdown vão em `docs/` e as telas em `design/`.
