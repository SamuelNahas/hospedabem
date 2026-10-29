-- HospedaBem - Iteração 1 - Grupo N
-- Criação do esquema (PostgreSQL 12+). Não apaga dados: falha se as tabelas já existirem.

CREATE TABLE funcionario (
    id                SERIAL PRIMARY KEY,
    matricula         VARCHAR(20)  NOT NULL UNIQUE,
    cpf               VARCHAR(14)  NOT NULL UNIQUE,
    nome              VARCHAR(120) NOT NULL,
    endereco          VARCHAR(200),
    cidade            VARCHAR(80),
    estado            CHAR(2),
    telefone          VARCHAR(20),
    email             VARCHAR(120),
    data_contratacao  DATE         NOT NULL,
    perfil            VARCHAR(20)  NOT NULL CHECK (perfil IN ('ADMINISTRADOR', 'RECEPCIONISTA')),
    -- PBKDF2-HMAC-SHA256: "pbkdf2$<iteracoes>$<salt base64>$<hash base64>"
    senha_hash        VARCHAR(200) NOT NULL,
    senha_alterada_em TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE hospede (
    id                SERIAL PRIMARY KEY,
    cpf_ou_passaporte VARCHAR(20)  NOT NULL UNIQUE,
    nome              VARCHAR(120) NOT NULL,
    data_nascimento   DATE         NOT NULL,
    endereco          VARCHAR(200),
    cidade            VARCHAR(80),
    estado            CHAR(2),
    telefone          VARCHAR(20)  NOT NULL,
    email             VARCHAR(120)
);

CREATE TABLE tipo_quarto (
    id                SERIAL PRIMARY KEY,
    codigo            VARCHAR(20)   NOT NULL UNIQUE,
    descricao         VARCHAR(120)  NOT NULL,
    capacidade_maxima INTEGER       NOT NULL CHECK (capacidade_maxima > 0),
    valor_diaria      NUMERIC(10,2) NOT NULL CHECK (valor_diaria > 0)
);

CREATE TABLE quarto (
    id             SERIAL PRIMARY KEY,
    numero         INTEGER     NOT NULL UNIQUE CHECK (numero > 0),
    andar          INTEGER     NOT NULL,
    tipo_quarto_id INTEGER     NOT NULL REFERENCES tipo_quarto (id) ON DELETE RESTRICT,
    situacao       VARCHAR(20) NOT NULL CHECK (situacao IN ('DISPONIVEL', 'OCUPADO', 'RESERVADO', 'MANUTENCAO'))
);

CREATE SEQUENCE reserva_codigo_seq START 1001;

CREATE TABLE reserva (
    id                     SERIAL PRIMARY KEY,
    codigo                 INTEGER     NOT NULL UNIQUE DEFAULT nextval('reserva_codigo_seq'),
    hospede_responsavel_id INTEGER     NOT NULL REFERENCES hospede (id) ON DELETE RESTRICT,
    tipo_quarto_id         INTEGER     NOT NULL REFERENCES tipo_quarto (id) ON DELETE RESTRICT,
    data_entrada_prevista  DATE        NOT NULL,
    data_saida_prevista    DATE        NOT NULL,
    quantidade_hospedes    INTEGER     NOT NULL CHECK (quantidade_hospedes > 0),
    situacao               VARCHAR(20) NOT NULL CHECK (situacao IN ('CONFIRMADA', 'CANCELADA', 'CONVERTIDA')),
    funcionario_id         INTEGER     NOT NULL REFERENCES funcionario (id) ON DELETE RESTRICT,
    registrada_em          TIMESTAMP   NOT NULL DEFAULT now(),
    -- incrementada a cada alteração; o check-in a usa para detectar mudanças após o resumo
    versao                 INTEGER     NOT NULL DEFAULT 0,
    CHECK (data_saida_prevista > data_entrada_prevista)
);

CREATE SEQUENCE hospedagem_codigo_seq START 5001;

CREATE TABLE hospedagem (
    id                  SERIAL PRIMARY KEY,
    codigo              INTEGER     NOT NULL UNIQUE DEFAULT nextval('hospedagem_codigo_seq'),
    data_hora_entrada   TIMESTAMP   NOT NULL,
    data_saida_prevista DATE        NOT NULL,
    data_hora_saida     TIMESTAMP,
    situacao            VARCHAR(20) NOT NULL CHECK (situacao IN ('ATIVA', 'ENCERRADA')),
    quarto_id           INTEGER     NOT NULL REFERENCES quarto (id) ON DELETE RESTRICT,
    -- "origina" 0..1 : 0..1 - UNIQUE impede converter a mesma reserva duas vezes
    reserva_id          INTEGER     UNIQUE REFERENCES reserva (id) ON DELETE RESTRICT,
    funcionario_id      INTEGER     NOT NULL REFERENCES funcionario (id) ON DELETE RESTRICT,
    CHECK (data_saida_prevista > data_hora_entrada::date),
    CHECK (data_hora_saida IS NULL OR data_hora_saida >= data_hora_entrada)
);

-- No máximo uma hospedagem ativa por quarto, mesmo com várias instâncias do desktop.
CREATE UNIQUE INDEX hospedagem_uma_ativa_por_quarto ON hospedagem (quarto_id) WHERE situacao = 'ATIVA';

CREATE TABLE hospedagem_hospede (
    hospedagem_id INTEGER NOT NULL REFERENCES hospedagem (id) ON DELETE RESTRICT,
    hospede_id    INTEGER NOT NULL REFERENCES hospede (id) ON DELETE RESTRICT,
    PRIMARY KEY (hospedagem_id, hospede_id)
);

CREATE TABLE conta_hospedagem (
    id            SERIAL PRIMARY KEY,
    hospedagem_id INTEGER       NOT NULL UNIQUE REFERENCES hospedagem (id) ON DELETE RESTRICT,
    situacao      VARCHAR(20)   NOT NULL CHECK (situacao IN ('ABERTA', 'FECHADA')),
    valor_diaria  NUMERIC(10,2) NOT NULL CHECK (valor_diaria > 0),
    aberta_em     TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX reserva_tipo_periodo ON reserva (tipo_quarto_id, data_entrada_prevista, data_saida_prevista)
    WHERE situacao = 'CONFIRMADA';
