# Bolso

Finanças pessoais e PJ. Contexto: `CLAUDE.md` e `docs/CONTEXTO.md`.

## Rodar tudo (Docker)

1. Copie `.env.example` para `.env` e troque a senha.
2. `docker compose up --build`
3. Front em http://localhost:5173 · API em http://localhost:8080/actuator/health · contrato OpenAPI em http://localhost:8080/v3/api-docs

As portas abrem só em `127.0.0.1` (esta máquina). Ainda não há login: quem alcança a API é tratado como o dono local. Por isso não publique as portas na rede até a feature 007 (login).

## Desenvolvimento

- Back-end (`backend/`): `./mvnw verify` roda unidade, arquitetura e integração (precisa do Docker Desktop aberto). No Windows: `mvnw.cmd verify`. API local com banco em contêiner: `./mvnw spring-boot:test-run`.
- Front-end (`frontend/`): `npm install` uma vez, depois `npm run dev`, `npm test`, `npm run lint`, `npm run typecheck`.
- Tipos da API no front: com a API rodando, `npm run gen:api`.

## Backup e restauração

O serviço `backup` do compose grava `backups/bolso-AAAA-MM-DD.sql.gz` ao subir e a cada 24 h, e apaga os arquivos com mais de `BACKUP_RETENCAO_DIAS` (padrão 30). A pasta `backups/` fica fora do Git.

Para restaurar um dump (funciona no PowerShell e no bash):

```sh
docker compose stop api backup
docker compose exec db dropdb -U bolso --force bolso
docker compose exec db createdb -U bolso bolso
docker compose cp backups/bolso-2026-10-08.sql.gz db:/tmp/restaurar.sql.gz
docker compose exec db sh -c "gunzip -c /tmp/restaurar.sql.gz | psql -U bolso -d bolso -v ON_ERROR_STOP=1"
docker compose start api backup
```

Ajuste `bolso` se mudou `POSTGRES_USER` ou `POSTGRES_DB` no `.env`.
