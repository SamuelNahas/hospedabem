-- CARGA EXTRA FICTÍCIA PARA DEMONSTRAÇÃO (java -jar target/hospedabem.jar --hb.demo=true). Executada depois de seed.sql.
-- Não altera os dados de seed.sql: tipos STD e LUX, quartos 101 a 202 e reserva 1001 continuam iguais.
-- Reservas e hospedagens usam códigos próprios (2001+ e 9001+), então as próximas geradas pelo sistema
-- continuam sendo a reserva 1002 e a hospedagem 5001.

INSERT INTO hospede (cpf_ou_passaporte, nome, data_nascimento, endereco, cidade, estado, telefone, email) VALUES
('55555555555', 'Fernanda Teste Alves',   '1995-02-14', 'Av. Fictícia, 250', 'Campo Grande', 'MS', '(67) 95555-5555', 'fernanda@exemplo.test'),
('66666666666', 'Gustavo Teste Pereira',  '1988-06-03', NULL, 'Campo Grande', 'MS', '(67) 96666-6666', NULL),
('77777777777', 'Helena Teste Costa',     '1972-12-25', NULL, 'Três Lagoas', 'MS', '(67) 97777-7777', 'helena@exemplo.test'),
('88888888888', 'Igor Teste Martins',     '1999-08-17', NULL, 'Cuiabá', 'MT', '(65) 98888-8888', NULL),
('12312312312', 'Juliana Teste Ribeiro',  '1993-03-09', NULL, 'São Paulo', 'SP', '(11) 91231-2312', 'juliana@exemplo.test'),
('AB9876543',   'Marie Test Dupont',      '1986-10-11', NULL, NULL, NULL, '+33 6 00 00 00 00', NULL);

INSERT INTO tipo_quarto (codigo, descricao, capacidade_maxima, valor_diaria) VALUES
('EXE', 'Executivo', 2, 260.00);

INSERT INTO quarto (numero, andar, tipo_quarto_id, situacao)
SELECT v.numero, v.andar, t.id, v.situacao
FROM (VALUES (302, 3, 'SUI', 'DISPONIVEL'),
             (501, 5, 'EXE', 'DISPONIVEL'), (502, 5, 'EXE', 'DISPONIVEL'), (503, 5, 'EXE', 'MANUTENCAO'))
     AS v(numero, andar, tipo, situacao)
JOIN tipo_quarto t ON t.codigo = v.tipo;

-- Reservas: futuras confirmadas, uma cancelada e uma já convertida em hospedagem (2006).
INSERT INTO reserva (codigo, hospede_responsavel_id, tipo_quarto_id, data_entrada_prevista, data_saida_prevista,
                     quantidade_hospedes, situacao, funcionario_id)
SELECT v.codigo, h.id, t.id, CURRENT_DATE + v.entrada, CURRENT_DATE + v.saida, v.quantidade, v.situacao, f.id
FROM (VALUES (2001, '88888888888', 'EXE',  1,  4, 2, 'CONFIRMADA'),
             (2002, 'AB9876543',   'SUI',  5,  8, 3, 'CONFIRMADA'),
             (2003, '33333333333', 'STD', 10, 12, 1, 'CONFIRMADA'),
             (2004, '44444444444', 'LUX', 15, 18, 2, 'CONFIRMADA'),
             (2005, '12312312312', 'EXE',  2,  5, 1, 'CANCELADA'),
             (2006, '77777777777', 'SUI', -2,  1, 1, 'CONVERTIDA'))
     AS v(codigo, documento, tipo, entrada, saida, quantidade, situacao)
JOIN hospede h ON h.cpf_ou_passaporte = v.documento
JOIN tipo_quarto t ON t.codigo = v.tipo
JOIN funcionario f ON f.matricula = 'recep01';

-- Hospedagens ativas: 9001 sem reserva (quarto 501) e 9002 originada da reserva 2006 (quarto 301).
INSERT INTO hospedagem (codigo, data_hora_entrada, data_saida_prevista, situacao, quarto_id, reserva_id, funcionario_id)
SELECT v.codigo, date_trunc('second', now()) - v.desde, CURRENT_DATE + v.saida, 'ATIVA', q.id, r.id, f.id
FROM (VALUES (9001, 501, NULL::integer, interval '1 day',  2),
             (9002, 301, 2006,          interval '2 days', 1))
     AS v(codigo, numero, reserva, desde, saida)
JOIN quarto q ON q.numero = v.numero
LEFT JOIN reserva r ON r.codigo = v.reserva
JOIN funcionario f ON f.matricula = 'recep01';

INSERT INTO hospedagem_hospede (hospedagem_id, hospede_id)
SELECT hg.id, h.id
FROM (VALUES (9001, '55555555555'), (9001, '66666666666'), (9002, '77777777777')) AS v(codigo, documento)
JOIN hospedagem hg ON hg.codigo = v.codigo
JOIN hospede h ON h.cpf_ou_passaporte = v.documento;

UPDATE quarto SET situacao = 'OCUPADO' WHERE numero IN (301, 501);

INSERT INTO conta_hospedagem (hospedagem_id, situacao, valor_diaria)
SELECT hg.id, 'ABERTA', t.valor_diaria
FROM hospedagem hg
JOIN quarto q ON q.id = hg.quarto_id
JOIN tipo_quarto t ON t.id = q.tipo_quarto_id
WHERE hg.codigo IN (9001, 9002);
