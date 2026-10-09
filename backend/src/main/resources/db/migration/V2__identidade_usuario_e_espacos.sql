-- Feature 002: quem é o dono e quais espaços existem. Login (auth_identity, códigos, sessões) entra na 007.

CREATE TABLE identity.app_user (
    id                uuid        PRIMARY KEY,
    email             citext      NOT NULL UNIQUE,
    email_verified_at timestamptz,
    display_name      text        NOT NULL,
    status            text        NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE identity.workspace (
    id         uuid        PRIMARY KEY,
    kind       text        NOT NULL CHECK (kind IN ('HOME', 'COMPANY')),
    name       text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE identity.workspace_member (
    workspace_id uuid NOT NULL REFERENCES identity.workspace (id) ON DELETE RESTRICT,
    user_id      uuid NOT NULL REFERENCES identity.app_user (id) ON DELETE RESTRICT,
    role         text NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    PRIMARY KEY (workspace_id, user_id)
);

-- achar os espaços de um usuário (a PK já cobre o outro sentido)
CREATE INDEX workspace_member_user_idx ON identity.workspace_member (user_id);
