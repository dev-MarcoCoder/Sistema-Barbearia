package br.com.barbearia.dao;

import br.com.barbearia.model.Agendamento;
import br.com.barbearia.model.StatusAgendamento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AgendamentoDAO {

    private static final String SELECT_COMPLETO = "SELECT a.id, a.barbearia_id, a.barbeiro_id, a.servico_id, "
            + "a.cliente_nome, a.cliente_telefone, a.data, a.hora_inicio, a.hora_fim, a.status, a.valor, a.codigo, "
            + "b.nome AS barbeiro_nome, s.nome AS servico_nome "
            + "FROM agendamento a "
            + "JOIN barbeiro b ON b.id = a.barbeiro_id "
            + "JOIN servico s ON s.id = a.servico_id ";

    /** Agenda de um dia da barbearia, opcionalmente filtrada por barbeiro (0 = todos). */
    public List<Agendamento> listarPorData(int barbeariaId, LocalDate data, int barbeiroId) throws SQLException {
        String sql = SELECT_COMPLETO + "WHERE a.barbearia_id = ? AND a.data = ?"
                + (barbeiroId > 0 ? " AND a.barbeiro_id = ?" : "")
                + " ORDER BY a.hora_inicio, b.nome";
        List<Agendamento> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, barbeariaId);
            ps.setObject(2, data);
            if (barbeiroId > 0) {
                ps.setInt(3, barbeiroId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Horarios ja ocupados de um barbeiro em um dia (tudo que nao foi cancelado).
     * Recebe a conexao para rodar dentro da transacao do agendamento.
     */
    public List<LocalTime[]> listarOcupados(Connection c, int barbeiroId, LocalDate data) throws SQLException {
        List<LocalTime[]> ocupados = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT hora_inicio, hora_fim FROM agendamento "
                + "WHERE barbeiro_id = ? AND data = ? AND status <> 'CANCELADO'")) {
            ps.setInt(1, barbeiroId);
            ps.setObject(2, data);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ocupados.add(new LocalTime[]{
                        rs.getObject("hora_inicio", LocalTime.class),
                        rs.getObject("hora_fim", LocalTime.class)});
                }
            }
        }
        return ocupados;
    }

    public void inserir(Connection c, Agendamento a) throws SQLException {
        String sql = "INSERT INTO agendamento (barbearia_id, barbeiro_id, servico_id, cliente_nome, cliente_telefone, "
                + "data, hora_inicio, hora_fim, status, valor, codigo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getBarbeariaId());
            ps.setInt(2, a.getBarbeiroId());
            ps.setInt(3, a.getServicoId());
            ps.setString(4, a.getClienteNome());
            ps.setString(5, a.getClienteTelefone());
            ps.setObject(6, a.getData());
            ps.setObject(7, a.getHoraInicio());
            ps.setObject(8, a.getHoraFim());
            ps.setString(9, a.getStatus().name());
            ps.setBigDecimal(10, a.getValor());
            ps.setString(11, a.getCodigo());
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                a.setId(chaves.getInt(1));
            }
        }
    }

    public boolean existeCodigo(Connection c, String codigo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM agendamento WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Agendamento buscarPorCodigo(int barbeariaId, String codigo) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_COMPLETO + "WHERE a.barbearia_id = ? AND a.codigo = ?")) {
            ps.setInt(1, barbeariaId);
            ps.setString(2, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public boolean atualizarStatus(int id, int barbeariaId, StatusAgendamento status) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE agendamento SET status = ? WHERE id = ? AND barbearia_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.setInt(3, barbeariaId);
            return ps.executeUpdate() == 1;
        }
    }

    private Agendamento montar(ResultSet rs) throws SQLException {
        Agendamento a = new Agendamento();
        a.setId(rs.getInt("id"));
        a.setBarbeariaId(rs.getInt("barbearia_id"));
        a.setBarbeiroId(rs.getInt("barbeiro_id"));
        a.setServicoId(rs.getInt("servico_id"));
        a.setClienteNome(rs.getString("cliente_nome"));
        a.setClienteTelefone(rs.getString("cliente_telefone"));
        a.setData(rs.getObject("data", LocalDate.class));
        a.setHoraInicio(rs.getObject("hora_inicio", LocalTime.class));
        a.setHoraFim(rs.getObject("hora_fim", LocalTime.class));
        a.setStatus(StatusAgendamento.deTexto(rs.getString("status")));
        a.setValor(rs.getBigDecimal("valor"));
        a.setCodigo(rs.getString("codigo"));
        a.setBarbeiroNome(rs.getString("barbeiro_nome"));
        a.setServicoNome(rs.getString("servico_nome"));
        return a;
    }
}
