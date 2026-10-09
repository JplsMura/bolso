-- Feature 002: dono local fixo e os dois espaços, criados quando o banco nasce.
-- Ids fixos no formato de UUID v7; o DonoLocal (adapter.out.local) usa o mesmo id do dono.
-- Na 007 o dono reivindica esta conta trocando o e-mail pelo verificado no login.
-- Sem dado real aqui: o repositório é público.

INSERT INTO identity.app_user (id, email, display_name, status)
VALUES ('019a0000-0000-7000-8000-000000000001', 'dono@bolso.local', 'Dono local', 'ACTIVE');

INSERT INTO identity.workspace (id, kind, name) VALUES
    ('019a0000-0000-7000-8000-000000000011', 'HOME', 'Casa'),
    ('019a0000-0000-7000-8000-000000000012', 'COMPANY', 'Empresa');

INSERT INTO identity.workspace_member (workspace_id, user_id, role) VALUES
    ('019a0000-0000-7000-8000-000000000011', '019a0000-0000-7000-8000-000000000001', 'OWNER'),
    ('019a0000-0000-7000-8000-000000000012', '019a0000-0000-7000-8000-000000000001', 'OWNER');
