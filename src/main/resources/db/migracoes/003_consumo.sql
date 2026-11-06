-- Iteração 2, UC11 (lançamento de consumo). Não apaga lançamentos.
-- O InicializadorBanco só executa este script enquanto consumo.valor_total não for coluna gerada:
-- ALTER TABLE trava a tabela inteira mesmo quando não muda nada.

-- Total calculado pelo banco: não existe lançamento com total que não confira com preço x quantidade,
-- e NUMERIC(13,2) comporta o maior total aceito (99.999.999,99 x 999).
ALTER TABLE consumo DROP COLUMN IF EXISTS valor_total;
ALTER TABLE consumo ADD COLUMN valor_total NUMERIC(13,2) GENERATED ALWAYS AS (preco_unitario * quantidade) STORED;
ALTER TABLE consumo DROP CONSTRAINT IF EXISTS consumo_quantidade_maxima;
ALTER TABLE consumo ADD CONSTRAINT consumo_quantidade_maxima CHECK (quantidade <= 999);
ALTER TABLE consumo ADD COLUMN IF NOT EXISTS funcionario_id INTEGER REFERENCES funcionario (id) ON DELETE RESTRICT;

-- Código e descrição cobrados pertencem ao lançamento, como o preço: renomear o item no catálogo
-- não altera contas já lançadas.
ALTER TABLE consumo ADD COLUMN IF NOT EXISTS codigo_item VARCHAR(20);
ALTER TABLE consumo ADD COLUMN IF NOT EXISTS descricao_item VARCHAR(120);
UPDATE consumo c SET codigo_item = i.codigo, descricao_item = i.descricao
FROM item_consumo i WHERE i.id = c.item_consumo_id AND c.codigo_item IS NULL;
ALTER TABLE consumo ALTER COLUMN codigo_item SET NOT NULL;
ALTER TABLE consumo ALTER COLUMN descricao_item SET NOT NULL;
