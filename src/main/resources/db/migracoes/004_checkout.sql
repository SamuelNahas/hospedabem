-- Iteração 2, UC12 (check-out e pagamento). Idempotente; não apaga dados.
ALTER TABLE conta_hospedagem ADD COLUMN IF NOT EXISTS fechada_em TIMESTAMP;

-- Um único pagamento por conta (UNIQUE impede cobrança duplicada mesmo entre estações).
CREATE TABLE IF NOT EXISTS pagamento (
    id                  SERIAL PRIMARY KEY,
    conta_hospedagem_id INTEGER       NOT NULL UNIQUE REFERENCES conta_hospedagem (id) ON DELETE RESTRICT,
    forma               VARCHAR(20)   NOT NULL CHECK (forma IN ('DINHEIRO', 'PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO')),
    quantidade_diarias  INTEGER       NOT NULL CHECK (quantidade_diarias > 0),
    valor_diarias       NUMERIC(12,2) NOT NULL CHECK (valor_diarias >= 0),
    valor_consumos      NUMERIC(12,2) NOT NULL CHECK (valor_consumos >= 0),
    valor_total         NUMERIC(12,2) NOT NULL CHECK (valor_total > 0),
    confirmado_em       TIMESTAMP     NOT NULL,
    funcionario_id      INTEGER       NOT NULL REFERENCES funcionario (id) ON DELETE RESTRICT,
    CHECK (valor_total = valor_diarias + valor_consumos)
);
