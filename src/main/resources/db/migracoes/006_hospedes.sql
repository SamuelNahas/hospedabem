-- Iteração 2, UC06 (gerenciar hóspede). Idempotente; não altera colunas nem apaga dados.
-- A tabela hospede já existe desde a iteração 1. A identificação única continua garantida pela
-- restrição UNIQUE da coluna cpf_ou_passaporte, que impede cadastro duplicado também entre estações.
-- Vínculos conferidos antes de excluir um hóspede (reserva como responsável e hospedagem).
CREATE INDEX IF NOT EXISTS reserva_hospede_responsavel ON reserva (hospede_responsavel_id);
CREATE INDEX IF NOT EXISTS hospedagem_hospede_hospede ON hospedagem_hospede (hospede_id);
