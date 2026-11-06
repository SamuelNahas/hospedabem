-- Iteração 2, UC11 (lançamento de consumo). Idempotente; não apaga dados.
-- Total persistido junto com o preço aplicado, para a cobrança não depender do catálogo.
ALTER TABLE consumo ADD COLUMN IF NOT EXISTS valor_total NUMERIC(12,2);
UPDATE consumo SET valor_total = preco_unitario * quantidade WHERE valor_total IS NULL;
ALTER TABLE consumo ALTER COLUMN valor_total SET NOT NULL;
ALTER TABLE consumo ADD COLUMN IF NOT EXISTS funcionario_id INTEGER REFERENCES funcionario (id) ON DELETE RESTRICT;
