-- Feature 003: metas, tags e lançamentos (schema finance).
-- Chaves estrangeiras compostas (id, workspace_id) impedem que um lançamento use meta ou tag de
-- outro espaço, mesmo que um bug passe pela aplicação. Dinheiro sempre numeric(14,2).

CREATE TABLE finance.budget_goal (
    id           uuid    PRIMARY KEY,
    workspace_id uuid    NOT NULL REFERENCES identity.workspace (id) ON DELETE RESTRICT,
    name         text    NOT NULL,
    color        text    NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    sort_order   integer NOT NULL,
    UNIQUE (id, workspace_id)
);
CREATE UNIQUE INDEX ux_budget_goal_workspace_name ON finance.budget_goal (workspace_id, lower(name));

CREATE TABLE finance.tag (
    id           uuid        PRIMARY KEY,
    workspace_id uuid        NOT NULL REFERENCES identity.workspace (id) ON DELETE RESTRICT,
    name         citext      NOT NULL CHECK (char_length(name::text) BETWEEN 1 AND 40),
    cost_type    text        CHECK (cost_type IN ('FIXED', 'VARIABLE')),
    archived_at  timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, workspace_id),
    UNIQUE (workspace_id, name)
);

CREATE TABLE finance.transaction (
    id              uuid          PRIMARY KEY,
    workspace_id    uuid          NOT NULL REFERENCES identity.workspace (id) ON DELETE RESTRICT,
    direction       text          NOT NULL CHECK (direction IN ('IN', 'OUT')),
    amount          numeric(14,2) NOT NULL CHECK (amount > 0),
    description     text          NOT NULL CHECK (char_length(description) BETWEEN 1 AND 140),
    occurred_on     date          NOT NULL,
    reference_month date          NOT NULL CHECK (EXTRACT(DAY FROM reference_month) = 1),
    goal_id         uuid,
    payment_method  text          CHECK (payment_method IN ('PIX', 'DEBIT', 'CREDIT', 'CASH', 'BOLETO', 'TRANSFER', 'OTHER')),
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    deleted_at      timestamptz,
    version         bigint        NOT NULL DEFAULT 0,
    UNIQUE (id, workspace_id),
    CONSTRAINT fk_transaction_goal FOREIGN KEY (goal_id, workspace_id)
        REFERENCES finance.budget_goal (id, workspace_id) ON DELETE RESTRICT,
    -- entrada não tem meta; toda saída tem forma de pagamento e entrada não tem
    CONSTRAINT ck_transaction_goal_only_out CHECK (direction = 'OUT' OR goal_id IS NULL),
    CONSTRAINT ck_transaction_payment CHECK ((direction = 'OUT') = (payment_method IS NOT NULL))
);

CREATE INDEX ix_transaction_month_goal ON finance.transaction (workspace_id, reference_month, goal_id)
    INCLUDE (amount, direction) WHERE deleted_at IS NULL;
CREATE INDEX ix_transaction_occurred_on ON finance.transaction (workspace_id, occurred_on DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE finance.transaction_tag (
    transaction_id uuid NOT NULL,
    tag_id         uuid NOT NULL,
    workspace_id   uuid NOT NULL,
    PRIMARY KEY (transaction_id, tag_id),
    CONSTRAINT fk_transaction_tag_transaction FOREIGN KEY (transaction_id, workspace_id)
        REFERENCES finance.transaction (id, workspace_id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_tag_tag FOREIGN KEY (tag_id, workspace_id)
        REFERENCES finance.tag (id, workspace_id) ON DELETE RESTRICT
);
CREATE INDEX ix_transaction_tag_tag ON finance.transaction_tag (tag_id);
