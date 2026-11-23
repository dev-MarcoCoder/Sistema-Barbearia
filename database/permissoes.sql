-- Usado so no Docker/AWS: da ao usuario "barbearia" acesso ao banco criado por barbearia.sql.
-- (O usuario e criado automaticamente pelo container do MySQL.)
GRANT ALL PRIVILEGES ON barbearia.* TO 'barbearia'@'%';
FLUSH PRIVILEGES;
