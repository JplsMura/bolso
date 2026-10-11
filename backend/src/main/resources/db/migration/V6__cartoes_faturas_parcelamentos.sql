-- Feature 004: cartões de crédito, faturas por mês e compras parceladas (schema finance).
-- credit_card e card_invoice são do módulo Cartões; installment_plan e as colunas novas de transaction são
-- de Lançamentos. As chaves estrangeiras compostas cruzam os dois módulos de propósito (mesmo schema): o
-- banco garante que a fatura é daquele cartão e daquele espaço, mesmo que um bug passe pela aplicação.
-- Dias e números de parcela são integer (não smallint) para casar direto com Integer no JPA.

CREATE TABLE finance.credit_card (
    id           uuid          PRIMARY KEY,
    workspace_id uuid          NOT NULL REFERENCES identity.workspace (id) ON DELETE RESTRICT,
    name         text          NOT NULL CHECK (char_length(name) BETWEEN 1 AND 40),
    closing_day  integer       NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
    due_day      integer       NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    credit_limit numeric(14,2) NOT NULL CHECK (credit_limit > 0),
    archived_at  timestamptz,
    created_at   timestamptz   NOT NULL DEFAULT now(),
    updated_at   timestamptz   NOT NULL DEFAULT now(),
    version      bigint        NOT NULL DEFAULT 0,
    UNIQUE (id, workspace_id)
);
-- nome único entre os cartões ativos do espaço, sem diferenciar maiúsculas (um arquivado libera o nome)
CREATE UNIQUE INDEX ux_credit_card_workspace_name ON finance.credit_card (workspace_id, lower(name))
    WHERE archived_at IS NULL;

CREATE TABLE finance.card_invoice (
    id              uuid          PRIMARY KEY,
    workspace_id    uuid          NOT NULL,
    card_id         uuid          NOT NULL,
    reference_month date          NOT NULL CHECK (EXTRACT(DAY FROM reference_month) = 1),
    closing_date    date          NOT NULL,
    due_date        date          NOT NULL,
    status          text          NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'PAID')),
    total_amount    numeric(14,2) CHECK (total_amount >= 0),
    paid_on         date,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    version         bigint        NOT NULL DEFAULT 0,
    UNIQUE (id, card_id, workspace_id),
    CONSTRAINT ux_card_invoice_card_month UNIQUE (card_id, reference_month),
    CONSTRAINT fk_card_invoice_card FOREIGN KEY (card_id, workspace_id)
        REFERENCES finance.credit_card (id, workspace_id) ON DELETE RESTRICT,
    -- a fatura leva o nome do mês em que fecha, e vence depois de fechar
    CONSTRAINT ck_card_invoice_closing_month CHECK (date_trunc('month', closing_date)::date = reference_month),
    CONSTRAINT ck_card_invoice_due_after_closing CHECK (due_date > closing_date),
    -- aberta não tem total congelado; só a paga tem data de pagamento, e nunca antes de fechar
    CONSTRAINT ck_card_invoice_total CHECK ((status = 'OPEN') = (total_amount IS NULL)),
    CONSTRAINT ck_card_invoice_paid CHECK ((status = 'PAID') = (paid_on IS NOT NULL)),
    CONSTRAINT ck_card_invoice_paid_after_closing CHECK (paid_on IS NULL OR paid_on >= closing_date)
);
-- limite usado: só as faturas não pagas
CREATE INDEX ix_card_invoice_unpaid ON finance.card_invoice (card_id) WHERE status <> 'PAID';
-- lembrete de vencimento (depois)
CREATE INDEX ix_card_invoice_due ON finance.card_invoice (workspace_id, due_date);

CREATE TABLE finance.installment_plan (
    id                uuid          PRIMARY KEY,
    workspace_id      uuid          NOT NULL,
    card_id           uuid          NOT NULL,
    description       text          NOT NULL CHECK (char_length(description) BETWEEN 1 AND 140),
    total_amount      numeric(14,2) NOT NULL CHECK (total_amount > 0),
    installments      integer       NOT NULL CHECK (installments BETWEEN 2 AND 48),
    first_installment integer       NOT NULL DEFAULT 1,
    purchase_date     date          NOT NULL,
    created_at        timestamptz   NOT NULL DEFAULT now(),
    updated_at        timestamptz   NOT NULL DEFAULT now(),
    deleted_at        timestamptz,
    version           bigint        NOT NULL DEFAULT 0,
    UNIQUE (id, workspace_id),
    CONSTRAINT fk_installment_plan_card FOREIGN KEY (card_id, workspace_id)
        REFERENCES finance.credit_card (id, workspace_id) ON DELETE RESTRICT,
    CONSTRAINT ck_installment_plan_first CHECK (first_installment BETWEEN 1 AND installments),
    -- pelo menos 1 centavo por parcela
    CONSTRAINT ck_installment_plan_cents CHECK (total_amount * 100 >= installments)
);

ALTER TABLE finance.transaction
    ADD COLUMN card_id             uuid,
    ADD COLUMN invoice_id          uuid,
    ADD COLUMN installment_plan_id uuid,
    ADD COLUMN installment_no      integer CHECK (installment_no BETWEEN 1 AND 48),
    ADD CONSTRAINT fk_transaction_card FOREIGN KEY (card_id, workspace_id)
        REFERENCES finance.credit_card (id, workspace_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_transaction_invoice FOREIGN KEY (invoice_id, card_id, workspace_id)
        REFERENCES finance.card_invoice (id, card_id, workspace_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_transaction_installment_plan FOREIGN KEY (installment_plan_id, workspace_id)
        REFERENCES finance.installment_plan (id, workspace_id) ON DELETE RESTRICT,
    -- cartão se e só se for crédito (IS NOT DISTINCT FROM: entrada tem payment_method nulo)
    ADD CONSTRAINT ck_transaction_card_only_credit
        CHECK ((payment_method IS NOT DISTINCT FROM 'CREDIT') = (card_id IS NOT NULL)),
    -- todo gasto no cartão está numa fatura
    ADD CONSTRAINT ck_transaction_card_invoice CHECK ((card_id IS NULL) = (invoice_id IS NULL)),
    -- parcela tem número, e só existe no cartão
    ADD CONSTRAINT ck_transaction_installment CHECK ((installment_plan_id IS NULL) = (installment_no IS NULL)),
    ADD CONSTRAINT ck_transaction_installment_card CHECK (installment_plan_id IS NULL OR card_id IS NOT NULL);

-- itens e total de uma fatura, somados só pelo índice
CREATE INDEX ix_transaction_invoice ON finance.transaction (invoice_id) INCLUDE (amount)
    WHERE deleted_at IS NULL AND invoice_id IS NOT NULL;
-- uma parcela por número dentro do parcelamento
CREATE UNIQUE INDEX ux_transaction_installment ON finance.transaction (installment_plan_id, installment_no)
    WHERE deleted_at IS NULL AND installment_plan_id IS NOT NULL;
