-- Iteração 2, UC12 (check-out e pagamento). Não apaga dados.
-- O InicializadorBanco só executa este script enquanto pagamento.valor_total não tiver precisão 15:
-- ALTER TABLE trava a tabela inteira mesmo quando não muda nada.
ALTER TABLE conta_hospedagem ADD COLUMN IF NOT EXISTS fechada_em TIMESTAMP;

-- Um único pagamento por conta (UNIQUE impede cobrança duplicada mesmo entre estações).
-- NUMERIC(15,2) comporta muitos lançamentos do maior valor aceito (consumo.valor_total é NUMERIC(13,2));
-- acima disso o Extrato recusa com mensagem de negócio.
CREATE TABLE IF NOT EXISTS pagamento (
    id                  SERIAL PRIMARY KEY,
    conta_hospedagem_id INTEGER       NOT NULL UNIQUE REFERENCES conta_hospedagem (id) ON DELETE RESTRICT,
    forma               VARCHAR(20)   NOT NULL CHECK (forma IN ('DINHEIRO', 'PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO')),
    quantidade_diarias  INTEGER       NOT NULL CHECK (quantidade_diarias > 0),
    valor_diarias       NUMERIC(15,2) NOT NULL CHECK (valor_diarias >= 0),
    valor_consumos      NUMERIC(15,2) NOT NULL CHECK (valor_consumos >= 0),
    valor_total         NUMERIC(15,2) NOT NULL CHECK (valor_total > 0),
    confirmado_em       TIMESTAMP     NOT NULL,
    funcionario_id      INTEGER       NOT NULL REFERENCES funcionario (id) ON DELETE RESTRICT,
    CHECK (valor_total = valor_diarias + valor_consumos)
);

-- Bancos criados com a primeira versão deste script usavam NUMERIC(12,2).
ALTER TABLE pagamento ALTER COLUMN valor_diarias TYPE NUMERIC(15,2), ALTER COLUMN valor_consumos TYPE NUMERIC(15,2),
    ALTER COLUMN valor_total TYPE NUMERIC(15,2);
