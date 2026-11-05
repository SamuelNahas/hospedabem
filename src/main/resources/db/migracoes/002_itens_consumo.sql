CREATE TABLE IF NOT EXISTS item_consumo (
    id        SERIAL PRIMARY KEY,
    codigo    VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(120) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    preco     NUMERIC(10,2) NOT NULL CHECK (preco > 0)
);

-- O preço aplicado pertence ao lançamento, não ao preço atual do catálogo (UC11).
CREATE TABLE IF NOT EXISTS consumo (
    id                   SERIAL PRIMARY KEY,
    conta_hospedagem_id   INTEGER NOT NULL REFERENCES conta_hospedagem (id) ON DELETE RESTRICT,
    item_consumo_id       INTEGER NOT NULL REFERENCES item_consumo (id) ON DELETE RESTRICT,
    quantidade           INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario       NUMERIC(10,2) NOT NULL CHECK (preco_unitario > 0),
    data_hora            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS consumo_item ON consumo (item_consumo_id);
CREATE INDEX IF NOT EXISTS consumo_conta ON consumo (conta_hospedagem_id);
