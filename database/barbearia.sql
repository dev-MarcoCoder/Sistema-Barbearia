-- =====================================================================
--  BarberAgenda - Sistema de agendamento para barbearias (SaaS)
--  Script de criacao do banco (MySQL 8) - cria as tabelas vazias
--
--  Como usar: abra no MySQL Workbench e execute tudo (raio),
--  ou pelo terminal:  mysql -u root -p < database/barbearia.sql
--
--  ATENCAO: o script apaga e recria o banco "barbearia".
-- =====================================================================

DROP DATABASE IF EXISTS barbearia;
CREATE DATABASE barbearia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE barbearia;

-- Cada barbearia que usa o sistema (uso gratuito, plano unico)
CREATE TABLE barbearia (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    nome               VARCHAR(100) NOT NULL,
    slug               VARCHAR(80)  NOT NULL UNIQUE,          -- usado no link publico de agendamento
    telefone           VARCHAR(11)  NOT NULL,
    endereco           VARCHAR(200),
    hora_abertura      TIME NOT NULL DEFAULT '09:00:00',
    hora_fechamento    TIME NOT NULL DEFAULT '19:00:00',
    intervalo_min      INT  NOT NULL DEFAULT 30,               -- de quanto em quanto tempo a agenda oferece horarios
    dias_funcionamento VARCHAR(20) NOT NULL DEFAULT '1,2,3,4,5,6', -- 1 = segunda ... 7 = domingo
    criado_em          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Dono/administrador que acessa o painel
CREATE TABLE usuario (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id INT NOT NULL,
    nome         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    senha_hash   VARCHAR(200) NOT NULL,                        -- PBKDF2 + salt, nunca a senha pura
    criado_em    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id) ON DELETE CASCADE
);

CREATE TABLE barbeiro (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id INT NOT NULL,
    nome         VARCHAR(100) NOT NULL,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_barbeiro_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id) ON DELETE CASCADE
);

CREATE TABLE servico (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id INT NOT NULL,
    nome         VARCHAR(80)  NOT NULL,
    descricao    VARCHAR(200),
    duracao_min  INT NOT NULL,
    preco        DECIMAL(10, 2) NOT NULL,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_servico_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id) ON DELETE CASCADE
);

CREATE TABLE agendamento (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    barbearia_id     INT NOT NULL,
    barbeiro_id      INT NOT NULL,
    servico_id       INT NOT NULL,
    cliente_nome     VARCHAR(100) NOT NULL,
    cliente_telefone VARCHAR(11)  NOT NULL,
    data             DATE NOT NULL,
    hora_inicio      TIME NOT NULL,
    hora_fim         TIME NOT NULL,
    status           ENUM('AGENDADO', 'CONCLUIDO', 'CANCELADO', 'FALTOU') NOT NULL DEFAULT 'AGENDADO',
    valor            DECIMAL(10, 2) NOT NULL,                  -- preco no momento do agendamento
    codigo           VARCHAR(10) NOT NULL UNIQUE,              -- o cliente usa para consultar/cancelar
    criado_em        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agendamento_barbearia FOREIGN KEY (barbearia_id) REFERENCES barbearia (id) ON DELETE CASCADE,
    CONSTRAINT fk_agendamento_barbeiro  FOREIGN KEY (barbeiro_id)  REFERENCES barbeiro (id),
    CONSTRAINT fk_agendamento_servico   FOREIGN KEY (servico_id)   REFERENCES servico (id),
    INDEX idx_agenda_barbeiro (barbeiro_id, data),
    INDEX idx_agenda_barbearia (barbearia_id, data)
);

-- IDs do agente e do ambiente do assistente de IA (Managed Agents da Anthropic).
-- O sistema preenche sozinho na primeira conversa; nao edite a mao.
CREATE TABLE config_ia (
    chave VARCHAR(50)  PRIMARY KEY,
    valor VARCHAR(255) NOT NULL
);
