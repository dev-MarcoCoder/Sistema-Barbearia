package br.com.barbearia.dao;

import br.com.barbearia.model.Servico;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServicoDAO {

    private static final String COLUNAS = "id, barbearia_id, nome, descricao, duracao_min, preco, ativo";

    public List<Servico> listar(int barbeariaId, boolean apenasAtivos) throws SQLException {
        String sql = "SELECT " + COLUNAS + " FROM servico WHERE barbearia_id = ?"
                + (apenasAtivos ? " AND ativo = TRUE" : "") + " ORDER BY ativo DESC, preco, nome";
        List<Servico> lista = new ArrayList<>();
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

    public Servico buscar(int id, int barbeariaId) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + COLUNAS + " FROM servico WHERE id = ? AND barbearia_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, barbeariaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public void inserir(Connection c, Servico s) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO servico (barbearia_id, nome, descricao, duracao_min, preco, ativo) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, s.getBarbeariaId());
            ps.setString(2, s.getNome());
            ps.setString(3, s.getDescricao());
            ps.setInt(4, s.getDuracaoMin());
            ps.setBigDecimal(5, s.getPreco());
            ps.setBoolean(6, s.isAtivo());
            ps.executeUpdate();
        }
    }

    public void inserir(Servico s) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection()) {
            inserir(c, s);
        }
    }

    public void atualizar(Servico s) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE servico SET nome = ?, descricao = ?, duracao_min = ?, "
                     + "preco = ? WHERE id = ? AND barbearia_id = ?")) {
            ps.setString(1, s.getNome());
            ps.setString(2, s.getDescricao());
            ps.setInt(3, s.getDuracaoMin());
            ps.setBigDecimal(4, s.getPreco());
            ps.setInt(5, s.getId());
            ps.setInt(6, s.getBarbeariaId());
            ps.executeUpdate();
        }
    }

    /** Exclusao logica, para nao perder o historico dos agendamentos ja feitos. */
    public void definirAtivo(int id, int barbeariaId, boolean ativo) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE servico SET ativo = ? WHERE id = ? AND barbearia_id = ?")) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, id);
            ps.setInt(3, barbeariaId);
            ps.executeUpdate();
        }
    }

    private Servico montar(ResultSet rs) throws SQLException {
        Servico s = new Servico();
        s.setId(rs.getInt("id"));
        s.setBarbeariaId(rs.getInt("barbearia_id"));
        s.setNome(rs.getString("nome"));
        s.setDescricao(rs.getString("descricao"));
        s.setDuracaoMin(rs.getInt("duracao_min"));
        s.setPreco(rs.getBigDecimal("preco"));
        s.setAtivo(rs.getBoolean("ativo"));
        return s;
    }
}
