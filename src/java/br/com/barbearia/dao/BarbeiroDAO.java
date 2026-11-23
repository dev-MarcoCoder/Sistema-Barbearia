package br.com.barbearia.dao;

import br.com.barbearia.model.Barbeiro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Toda consulta filtra por barbearia_id: uma barbearia nunca enxerga
 * nem altera os dados de outra (isolamento entre clientes do SaaS).
 */
public class BarbeiroDAO {

    public List<Barbeiro> listar(int barbeariaId, boolean apenasAtivos) throws SQLException {
        String sql = "SELECT id, barbearia_id, nome, ativo FROM barbeiro WHERE barbearia_id = ?"
                + (apenasAtivos ? " AND ativo = TRUE" : "") + " ORDER BY ativo DESC, nome";
        List<Barbeiro> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, barbeariaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
        }
        return lista;
    }

    public Barbeiro buscar(int id, int barbeariaId) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, barbearia_id, nome, ativo FROM barbeiro WHERE id = ? AND barbearia_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, barbeariaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public int contarAtivos(int barbeariaId) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM barbeiro WHERE barbearia_id = ? AND ativo = TRUE")) {
            ps.setInt(1, barbeariaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Trava a linha do barbeiro ate o fim da transacao (SELECT ... FOR UPDATE).
     * Assim dois agendamentos simultaneos para o mesmo barbeiro sao processados um de cada vez.
     */
    public void travar(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM barbeiro WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
            }
        }
    }

    public void inserir(Connection c, Barbeiro b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO barbeiro (barbearia_id, nome, ativo) VALUES (?, ?, ?)")) {
            ps.setInt(1, b.getBarbeariaId());
            ps.setString(2, b.getNome());
            ps.setBoolean(3, b.isAtivo());
            ps.executeUpdate();
        }
    }

    public void inserir(Barbeiro b) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection()) {
            inserir(c, b);
        }
    }

    public void renomear(int id, int barbeariaId, String nome) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE barbeiro SET nome = ? WHERE id = ? AND barbearia_id = ?")) {
            ps.setString(1, nome);
            ps.setInt(2, id);
            ps.setInt(3, barbeariaId);
            ps.executeUpdate();
        }
    }

    /** Exclusao logica: o barbeiro sai da agenda, mas o historico de atendimentos continua. */
    public void definirAtivo(int id, int barbeariaId, boolean ativo) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE barbeiro SET ativo = ? WHERE id = ? AND barbearia_id = ?")) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, id);
            ps.setInt(3, barbeariaId);
            ps.executeUpdate();
        }
    }

    private Barbeiro montar(ResultSet rs) throws SQLException {
        Barbeiro b = new Barbeiro();
        b.setId(rs.getInt("id"));
        b.setBarbeariaId(rs.getInt("barbearia_id"));
        b.setNome(rs.getString("nome"));
        b.setAtivo(rs.getBoolean("ativo"));
        return b;
    }
}
