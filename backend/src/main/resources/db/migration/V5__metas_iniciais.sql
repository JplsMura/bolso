-- Feature 003: as 6 metas do espaço Casa (as mesmas do AUVP), com ids fixos e as cores da paleta.
-- A Empresa não tem metas. Os percentuais por mês entram na feature 005.
-- Ids fixos no formato de UUID v7, como o dono local e os espaços (V3).

INSERT INTO finance.budget_goal (id, workspace_id, name, color, sort_order) VALUES
    ('019a0000-0000-7000-8000-000000000101', '019a0000-0000-7000-8000-000000000011', 'Custos Fixos',         '#6EA8FE', 1),
    ('019a0000-0000-7000-8000-000000000102', '019a0000-0000-7000-8000-000000000011', 'Conforto',             '#A3E06B', 2),
    ('019a0000-0000-7000-8000-000000000103', '019a0000-0000-7000-8000-000000000011', 'Metas',                '#C79BFF', 3),
    ('019a0000-0000-7000-8000-000000000104', '019a0000-0000-7000-8000-000000000011', 'Prazeres',             '#FF9F5A', 4),
    ('019a0000-0000-7000-8000-000000000105', '019a0000-0000-7000-8000-000000000011', 'Liberdade Financeira', '#FF8FB1', 5),
    ('019a0000-0000-7000-8000-000000000106', '019a0000-0000-7000-8000-000000000011', 'Conhecimento',         '#5CD6E8', 6);
