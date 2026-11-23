package br.com.barbearia.dao;

import br.com.barbearia.model.ItemRanking;
import br.com.barbearia.model.ResumoPeriodo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas de agregacao (GROUP BY / SUM / COUNT) para o painel e os relatorios. */
public class RelatorioDAO {

    /** Totais por status entre duas datas (inclusive). */
    public ResumoPeriodo resumo(int barbeariaId, LocalDate inicio, LocalDate fim) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS qtd, COALESCE(SUM(valor), 0) AS total FROM agendamento "
                + "WHERE barbearia_id = ? AND data BETWEEN ? AND ? GROUP BY status";
        ResumoPeriodo r = new ResumoPeriodo();
        BigDecimal agendadoValor = BigDecimal.ZERO;
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, barbeariaId);
            ps.setObject(2, inicio);
            ps.setObject(3, fim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qtd = rs.getInt("qtd");
                    BigDecimal total = rs.getBigDecimal("total");
                    switch (rs.getString("status")) {
                        case "AGENDADO":
                            r.setAgendados(qtd);
                            agendadoValor = total;
                            break;
                        case "CONCLUIDO":
                            r.setConcluidos(qtd);
                            r.setFaturado(total);
                            break;
                        case "CANCELADO":
                            r.setCancelados(qtd);
                            break;
                        case "FALTOU":
                            r.setFaltas(qtd);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        r.setPrevisto(r.getFaturado().add(agendadoValor));
        return r;
    }

    public List<ItemRanking> rankingServicos(int barbeariaId, LocalDate inicio, LocalDate fim) throws SQLException {
        return ranking("SELECT s.nome, COUNT(*) AS qtd, SUM(a.valor) AS total FROM agendamento a "
                + "JOIN servico s ON s.id = a.servico_id "
                + "WHERE a.barbearia_id = ? AND a.data BETWEEN ? AND ? AND a.status = 'CONCLUIDO' "
                + "GROUP BY s.id, s.nome ORDER BY qtd DESC, total DESC", barbeariaId, inicio, fim);
    }

    public List<ItemRanking> rankingBarbeiros(int barbeariaId, LocalDate inicio, LocalDate fim) throws SQLException {
        return ranking("SELECT b.nome, COUNT(*) AS qtd, SUM(a.valor) AS total FROM agendamento a "
                + "JOIN barbeiro b ON b.id = a.barbeiro_id "
                + "WHERE a.barbearia_id = ? AND a.data BETWEEN ? AND ? AND a.status = 'CONCLUIDO' "
                + "GROUP BY b.id, b.nome ORDER BY total DESC, qtd DESC", barbeariaId, inicio, fim);
    }

    /** Faturamento de cada dia do periodo (para o grafico de barras do relatorio). */
    public List<ItemRanking> faturamentoPorDia(int barbeariaId, LocalDate inicio, LocalDate fim) throws SQLException {
        return ranking("SELECT DATE_FORMAT(data, '%Y-%m-%d') AS nome, COUNT(*) AS qtd, SUM(valor) AS total "
                + "FROM agendamento WHERE barbearia_id = ? AND data BETWEEN ? AND ? AND status = 'CONCLUIDO' "
                + "GROUP BY data ORDER BY data", barbeariaId, inicio, fim);
    }

    private List<ItemRanking> ranking(String sql, int barbeariaId, LocalDate inicio, LocalDate fim) throws SQLException {
        List<ItemRanking> lista = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, barbeariaId);
            ps.setObject(2, inicio);
            ps.setObject(3, fim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ItemRanking(rs.getString("nome"), rs.getInt("qtd"), rs.getBigDecimal("total")));
                }
            }
        }
        return lista;
    }
}
