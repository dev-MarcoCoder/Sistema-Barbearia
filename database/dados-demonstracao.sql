-- =====================================================================
--  BarberAgenda - DADOS DE DEMONSTRACAO (opcional)
--
--  Barbearias, barbeiros, servicos e agendamentos ficticios, para testar
--  o sistema sem cadastrar nada. Rode DEPOIS do barbearia.sql:
--     mysql -u root -p < database/dados-demonstracao.sql
--
--  Para comecar do zero, rode so o barbearia.sql e nao rode este arquivo.
-- =====================================================================

USE barbearia;

-- =====================================================================
--  DADOS DE DEMONSTRACAO
--  Login do painel (apenas para teste local):
--    admin@navalhadeouro.demo / demo1234
--    contato@dobairro.demo    / demo1234
-- =====================================================================

INSERT INTO barbearia (id, nome, slug, telefone, endereco, hora_abertura, hora_fechamento, intervalo_min, dias_funcionamento) VALUES
(1, 'Navalha de Ouro Barbearia', 'navalha-de-ouro', '11987654321', 'Rua Augusta, 1500 - Consolação, São Paulo/SP', '09:00:00', '20:00:00', 30, '1,2,3,4,5,6'),
(2, 'Barbearia do Bairro',       'barbearia-do-bairro', '11912345678', 'Av. Brasil, 300 - Centro', '10:00:00', '18:00:00', 30, '2,3,4,5,6');

INSERT INTO usuario (barbearia_id, nome, email, senha_hash) VALUES
(1, 'Rafael Souza', 'admin@navalhadeouro.demo', '120000:YacXG963xMEdpAvh7rwYtA==:B0f82V/yaO5e/nrFdjOgD+4SmLeIUNPormCqLBrDO+Q='),
(2, 'Carlos Lima',  'contato@dobairro.demo',    '120000:YacXG963xMEdpAvh7rwYtA==:B0f82V/yaO5e/nrFdjOgD+4SmLeIUNPormCqLBrDO+Q=');

INSERT INTO barbeiro (id, barbearia_id, nome, ativo) VALUES
(1, 1, 'Rafael Souza', TRUE),
(2, 1, 'Diego Martins', TRUE),
(3, 1, 'Bruno Oliveira', TRUE),
(4, 2, 'Carlos Lima', TRUE);

INSERT INTO servico (id, barbearia_id, nome, descricao, duracao_min, preco) VALUES
(1, 1, 'Corte masculino',       'Tesoura ou máquina, com lavagem e finalização', 30, 45.00),
(2, 1, 'Barba completa',        'Barba com toalha quente e navalha',              30, 35.00),
(3, 1, 'Corte + barba',         'O combo mais pedido da casa',                    60, 70.00),
(4, 1, 'Pezinho / acabamento',  'Contorno e acabamento entre um corte e outro',   15, 15.00),
(5, 1, 'Sobrancelha',           'Design na navalha',                              15, 20.00),
(6, 2, 'Corte',                 'Corte tradicional',                              30, 35.00),
(7, 2, 'Barba',                 'Barba na navalha',                               30, 25.00);

-- Historico dos ultimos 30 dias (gera os numeros dos relatorios).
-- Para cada dia (exceto domingo) cria 6 atendimentos, um por hora, alternando barbeiro e servico.
INSERT INTO agendamento (barbearia_id, barbeiro_id, servico_id, cliente_nome, cliente_telefone, data,
                         hora_inicio, hora_fim, status, valor, codigo)
WITH RECURSIVE dias AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM dias WHERE n < 30
), horarios AS (
    SELECT 0 AS k UNION ALL SELECT k + 1 FROM horarios WHERE k < 5
)
SELECT 1,
       1 + ((d.n + h.k) % 3),
       s.id,
       ELT(1 + ((d.n * 7 + h.k * 3) % 12), 'João Pedro', 'Lucas Almeida', 'Mateus Ferreira', 'Gabriel Costa',
           'Pedro Henrique', 'Thiago Ribeiro', 'Felipe Santos', 'André Gomes', 'Rodrigo Nunes', 'Vinícius Rocha',
           'Gustavo Pereira', 'Leonardo Dias'),
       CONCAT('119', LPAD(((d.n * 7 + h.k * 3) % 12) * 1234567 % 100000000, 8, '0')),
       DATE_SUB(CURDATE(), INTERVAL d.n DAY),
       ADDTIME('09:00:00', SEC_TO_TIME(h.k * 7200)),
       ADDTIME(ADDTIME('09:00:00', SEC_TO_TIME(h.k * 7200)), SEC_TO_TIME(s.duracao_min * 60)),
       CASE WHEN (d.n + h.k) % 11 = 0 THEN 'CANCELADO'
            WHEN (d.n * 3 + h.k) % 13 = 0 THEN 'FALTOU'
            ELSE 'CONCLUIDO' END,
       s.preco,
       CONCAT('H', LPAD(d.n, 3, '0'), LPAD(h.k, 2, '0'))
FROM dias d
CROSS JOIN horarios h
JOIN servico s ON s.id = 1 + ((d.n * 3 + h.k * 2) % 5)
WHERE DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL d.n DAY)) <> 1;

-- Agenda de hoje e de amanha (para a demonstracao do painel)
INSERT INTO agendamento (barbearia_id, barbeiro_id, servico_id, cliente_nome, cliente_telefone, data, hora_inicio, hora_fim, status, valor, codigo) VALUES
(1, 1, 3, 'Marcelo Araújo',  '11991234567', CURDATE(), '09:00:00', '10:00:00', 'CONCLUIDO', 70.00, 'DEMO01'),
(1, 2, 1, 'Henrique Prado',  '11992345678', CURDATE(), '10:00:00', '10:30:00', 'CONCLUIDO', 45.00, 'DEMO02'),
(1, 3, 2, 'Caio Mendes',     '11993456789', CURDATE(), '11:00:00', '11:30:00', 'FALTOU',    35.00, 'DEMO03'),
(1, 1, 1, 'Eduardo Lopes',   '11994567890', CURDATE(), '14:00:00', '14:30:00', 'AGENDADO',  45.00, 'DEMO04'),
(1, 2, 3, 'Renato Farias',   '11995678901', CURDATE(), '15:00:00', '16:00:00', 'AGENDADO',  70.00, 'DEMO05'),
(1, 3, 4, 'Igor Batista',    '11996789012', CURDATE(), '16:30:00', '16:45:00', 'AGENDADO',  15.00, 'DEMO06'),
(1, 1, 2, 'Samuel Torres',   '11997890123', CURDATE(), '18:00:00', '18:30:00', 'AGENDADO',  35.00, 'DEMO07'),
(1, 1, 1, 'Fábio Cardoso',   '11998901234', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:30:00', '10:00:00', 'AGENDADO', 45.00, 'DEMO08'),
(1, 2, 3, 'Otávio Reis',     '11999012345', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '11:00:00', 'AGENDADO', 70.00, 'DEMO09'),
(1, 3, 1, 'Daniel Moura',    '11990123456', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:00:00', '13:30:00', 'AGENDADO', 45.00, 'DEMO10'),
(1, 1, 5, 'Júlio César',     '11981234567', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '17:00:00', '17:15:00', 'AGENDADO', 20.00, 'DEMO11'),
(2, 4, 6, 'Paulo Vieira',    '11982345678', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '10:30:00', 'AGENDADO', 35.00, 'DEMO12');
