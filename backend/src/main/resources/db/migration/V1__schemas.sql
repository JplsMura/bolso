-- Base do projeto (spec 001): schemas por contexto e extensões.
-- As tabelas entram nas migrações de cada feature (V2 em diante).

CREATE EXTENSION IF NOT EXISTS citext;      -- e-mail e nomes únicos sem diferenciar maiúsculas
CREATE EXTENSION IF NOT EXISTS btree_gist;  -- EXCLUDE de períodos (billing.company_regime)

CREATE SCHEMA IF NOT EXISTS identity;  -- usuários, espaços, membros
CREATE SCHEMA IF NOT EXISTS finance;   -- lançamentos, metas, cartões, faturas
CREATE SCHEMA IF NOT EXISTS billing;   -- empresa, notas emitidas, tomadores
CREATE SCHEMA IF NOT EXISTS planning;  -- cenários e reserva (depois do MVP)
CREATE SCHEMA IF NOT EXISTS platform;  -- parâmetros fiscais e auditoria
