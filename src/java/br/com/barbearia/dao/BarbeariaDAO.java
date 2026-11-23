package br.com.barbearia.dao;

import br.com.barbearia.model.Barbearia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BarbeariaDAO {

    private static final String COLUNAS = "id, nome, slug, telefone, endereco, hora_abertura, "
            + "hora_fechamento, intervalo_min, dias_funcionamento";

    public Barbearia buscarPorId(int id) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUNAS + " FROM barbearia WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public Barbearia buscarPorSlug(String slug) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUNAS + " FROM barbearia WHERE slug = ?")) {
            ps.setString(1, slug.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public List<Barbearia> listarTodas() throws SQLException {
        List<Barbearia> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUNAS + " FROM barbearia ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(montar(rs));
            }
        }
        return lista;
    }

    public boolean existeSlug(Connection c, String slug) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM barbearia WHERE slug = ?")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Insere usando a conexao recebida (para participar de uma transacao). */
    public int inserir(Connection c, Barbearia b) throws SQLException {
        String sql = "INSERT INTO barbearia (nome, slug, telefone, endereco, hora_abertura, hora_fechamento, "
                + "intervalo_min, dias_funcionamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getNome());
            ps.setString(2, b.getSlug());
            ps.setString(3, b.getTelefone());
            ps.setString(4, b.getEndereco());
            ps.setObject(5, b.getHoraAbertura());
            ps.setObject(6, b.getHoraFechamento());
            ps.setInt(7, b.getIntervaloMin());
            ps.setString(8, b.getDiasFuncionamentoTexto());
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                int id = chaves.getInt(1);
                b.setId(id);
                return id;
            }
        }
    }

    public void atualizarDados(Barbearia b) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE barbearia SET nome = ?, telefone = ?, endereco = ? WHERE id = ?")) {
            ps.setString(1, b.getNome());
            ps.setString(2, b.getTelefone());
            ps.setString(3, b.getEndereco());
            ps.setInt(4, b.getId());
            ps.executeUpdate();
        }
    }

    public void atualizarHorarios(Barbearia b) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE barbearia SET hora_abertura = ?, hora_fechamento = ?, "
                     + "intervalo_min = ?, dias_funcionamento = ? WHERE id = ?")) {
            ps.setObject(1, b.getHoraAbertura());
            ps.setObject(2, b.getHoraFechamento());
            ps.setInt(3, b.getIntervaloMin());
            ps.setString(4, b.getDiasFuncionamentoTexto());
            ps.setInt(5, b.getId());
            ps.executeUpdate();
        }
    }

    private Barbearia montar(ResultSet rs) throws SQLException {
        Barbearia b = new Barbearia();
        b.setId(rs.getInt("id"));
        b.setNome(rs.getString("nome"));
        b.setSlug(rs.getString("slug"));
        b.setTelefone(rs.getString("telefone"));
        b.setEndereco(rs.getString("endereco"));
        b.setHoraAbertura(rs.getObject("hora_abertura", LocalTime.class));
        b.setHoraFechamento(rs.getObject("hora_fechamento", LocalTime.class));
        b.setIntervaloMin(rs.getInt("intervalo_min"));
        b.setDiasFuncionamentoTexto(rs.getString("dias_funcionamento"));
        return b;
    }
}
