package br.com.barbearia.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Guarda os IDs do agente e do ambiente criados na Anthropic (Managed Agents),
 * para que sejam criados uma unica vez e reaproveitados em todas as conversas.
 */
public class ConfigIADAO {

    private static volatile boolean tabelaVerificada;

    private void garantirTabela(Connection c) throws SQLException {
        if (tabelaVerificada) {
            return;
        }
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS config_ia ("
                    + "chave VARCHAR(50) PRIMARY KEY, valor VARCHAR(255) NOT NULL)");
        }
        tabelaVerificada = true;
    }

    public String ler(String chave) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection()) {
            garantirTabela(c);
            try (PreparedStatement ps = c.prepareStatement("SELECT valor FROM config_ia WHERE chave = ?")) {
                ps.setString(1, chave);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        }
    }

    public void salvar(String chave, String valor) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection()) {
            garantirTabela(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO config_ia (chave, valor) VALUES (?, ?) ON DUPLICATE KEY UPDATE valor = VALUES(valor)")) {
                ps.setString(1, chave);
                ps.setString(2, valor);
                ps.executeUpdate();
            }
        }
    }
}
