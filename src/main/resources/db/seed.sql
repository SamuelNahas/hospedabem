-- HospedaBem - Iteração 1 - Grupo N
-- CARGA INICIAL FICTÍCIA PARA DEMONSTRAÇÃO E TESTE. Nenhum dado corresponde a pessoa real.
-- Credenciais de teste (troque antes de qualquer uso real):
--   admin01 / Admin@123  (ADMINISTRADOR)
--   recep01 / Recep@123  (RECEPCIONISTA)

INSERT INTO funcionario (matricula, cpf, nome, cidade, estado, telefone, email, data_contratacao, perfil, senha_hash) VALUES
('admin01', '000.000.001-91', 'Ana Administradora (TESTE)', 'Campo Grande', 'MS', '(67) 90000-0001', 'admin@exemplo.test', '2025-01-10', 'ADMINISTRADOR',
 'pbkdf2$210000$EuKRwqhvNbinWt4hkPOEKQ==$29pX8s+3q3jWx5MzTDEaTvrPqbwMLyt23oKiOEcX6lA='),
('recep01', '000.000.002-72', 'Rui Recepcionista (TESTE)', 'Campo Grande', 'MS', '(67) 90000-0002', 'recep@exemplo.test', '2025-03-01', 'RECEPCIONISTA',
 'pbkdf2$210000$rz5ofFRoIhrFNle37uXwLA==$cV3BwJNQhZgL/jaedC1URtm22dlfaUE9OCvTSdE+12g=');

-- Hóspedes fictícios (UC06 Gerenciar Hóspede é da iteração 2)
INSERT INTO hospede (cpf_ou_passaporte, nome, data_nascimento, endereco, cidade, estado, telefone, email) VALUES
('11111111111', 'Carlos Teste Silva',    '1985-04-12', 'Rua Fictícia, 100', 'Campo Grande', 'MS', '(67) 91111-1111', 'carlos@exemplo.test'),
('22222222222', 'Beatriz Teste Souza',   '1990-09-30', NULL, NULL, NULL, '(67) 92222-2222', NULL),
('33333333333', 'Diego Teste Lima',      '2001-01-05', NULL, 'Dourados', 'MS', '(67) 93333-3333', NULL),
('44444444444', 'Elisa Teste Rocha',     '1978-07-21', NULL, NULL, NULL, '(11) 94444-4444', 'elisa@exemplo.test'),
('XP1234567',   'John Test Foreigner',   '1982-11-02', NULL, NULL, NULL, '+1 555 0100', NULL);

INSERT INTO tipo_quarto (codigo, descricao, capacidade_maxima, valor_diaria) VALUES
('STD', 'Standard',    2, 180.00),
('LUX', 'Luxo',        3, 320.00),
('SUI', 'Suíte',       4, 550.00);

INSERT INTO quarto (numero, andar, tipo_quarto_id, situacao)
SELECT v.numero, v.andar, t.id, v.situacao
FROM (VALUES (101, 1, 'STD', 'DISPONIVEL'), (102, 1, 'STD', 'DISPONIVEL'), (103, 1, 'STD', 'MANUTENCAO'),
             (201, 2, 'LUX', 'DISPONIVEL'), (202, 2, 'LUX', 'DISPONIVEL'),
             (301, 3, 'SUI', 'DISPONIVEL')) AS v(numero, andar, tipo, situacao)
JOIN tipo_quarto t ON t.codigo = v.tipo;

-- Reserva com entrada hoje, pronta para demonstrar o check-in com reserva (código 1001)
INSERT INTO reserva (hospede_responsavel_id, tipo_quarto_id, data_entrada_prevista, data_saida_prevista,
                     quantidade_hospedes, situacao, funcionario_id)
SELECT h.id, t.id, CURRENT_DATE, CURRENT_DATE + 3, 2, 'CONFIRMADA', f.id
FROM hospede h, tipo_quarto t, funcionario f
WHERE h.cpf_ou_passaporte = '11111111111' AND t.codigo = 'LUX' AND f.matricula = 'recep01';
