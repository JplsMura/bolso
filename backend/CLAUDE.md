# Back-end (Bolso)

- Java 25 LTS, Spring Boot 4.1, Spring Modulith 2.1, PostgreSQL 18, Flyway, Testcontainers 2, ArchUnit.
- Pacote base `dev.bolso`. Um pacote por módulo: identidade, lancamentos, cartoes, orcamento, faturamento.
- Dentro do módulo: `<Modulo>Api` na raiz (única porta para os outros), `domain` (Java puro), `application` (casos de uso e portas), `adapter/in/web`, `adapter/out/persistence`.
- Sem login até a 007: `UsuarioAtual` (porta de saída da identidade) devolve o dono local, criado pela migração V3 com id fixo. Toda feature que mexe em dado de um espaço chama `IdentidadeApi.papelNoEspaco(espacoId)` antes (lança `EspacoNaoEncontrado`, que vira 404).
- Linguagem publicada da identidade na raiz do módulo: `IdentidadeApi`, `Papel`, `EspacoNaoEncontrado`.
- JPA nos agregados; JdbcClient nas consultas de leitura. Hibernate em `validate`: o schema é só do Flyway.
- Schemas: identity, finance, billing, planning, platform. IDs UUID v7. Locking otimista (`version`).
- Boot 4 é modular: starter próprio por tecnologia (`spring-boot-starter-webmvc`, `-flyway`, `-data-jpa`). Jackson 3 (`tools.jackson`). Testcontainers 2: `org.testcontainers.postgresql.PostgreSQLContainer`.

## Comandos (rodar dentro de `backend/`)
- Tudo (unidade, arquitetura e integração): `./mvnw verify` (Windows: `mvnw.cmd verify`). Precisa do Docker rodando.
- Só unidade e arquitetura, sem Docker: `./mvnw test`
- API local com banco em contêiner: `./mvnw spring-boot:test-run`

## Testes
- `*Test` = unidade e arquitetura (surefire). `*IT` = integração com Testcontainers (failsafe).
- `ModularityTest` (`ApplicationModules.verify()`) e `ArquiteturaHexagonalTest` (ArchUnit) nunca podem ser desligados.
- Domínio: JUnit 5 + AssertJ, sem Spring. Nada de H2.
