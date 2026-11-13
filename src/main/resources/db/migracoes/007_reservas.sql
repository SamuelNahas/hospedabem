-- Iteração 2, UC09 (consultar, alterar e cancelar reserva). Idempotente; não altera colunas nem apaga dados.
-- A tabela reserva já tem, desde a iteração 1, todas as colunas que o UC09 grava, inclusive versao.
-- O que falta é o índice: a alteração recalcula a disponibilidade do tipo de destino a cada gravação,
-- e essa consulta filtra reserva por tipo e situação (ServicoDisponibilidade/DisponibilidadeDAO).
CREATE INDEX IF NOT EXISTS reserva_tipo_situacao ON reserva (tipo_quarto_id, situacao);
