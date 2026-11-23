package br.com.barbearia.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Totais de um periodo (dia ou mes), usados no painel e nos relatorios. */
public class ResumoPeriodo {

    private int agendados;
    private int concluidos;
    private int cancelados;
    private int faltas;
    /** Soma dos atendimentos concluidos. */
    private BigDecimal faturado = BigDecimal.ZERO;
    /** Soma dos atendimentos concluidos + ainda agendados. */
    private BigDecimal previsto = BigDecimal.ZERO;

    /** Atendimentos que ocuparam a agenda (tudo menos cancelados). */
    public int getTotalValidos() {
        return agendados + concluidos + faltas;
    }

    /** Percentual de faltas sobre os atendimentos que ja deveriam ter acontecido. */
    public int getTaxaFaltas() {
        int base = concluidos + faltas;
        return base == 0 ? 0 : Math.round(faltas * 100f / base);
    }

    public BigDecimal getTicketMedio() {
        if (concluidos == 0) {
            return BigDecimal.ZERO;
        }
        return faturado.divide(BigDecimal.valueOf(concluidos), 2, RoundingMode.HALF_UP);
    }

    public int getAgendados() { return agendados; }
    public void setAgendados(int agendados) { this.agendados = agendados; }
    public int getConcluidos() { return concluidos; }
    public void setConcluidos(int concluidos) { this.concluidos = concluidos; }
    public int getCancelados() { return cancelados; }
    public void setCancelados(int cancelados) { this.cancelados = cancelados; }
    public int getFaltas() { return faltas; }
    public void setFaltas(int faltas) { this.faltas = faltas; }
    public BigDecimal getFaturado() { return faturado; }
    public void setFaturado(BigDecimal faturado) { this.faturado = faturado; }
    public BigDecimal getPrevisto() { return previsto; }
    public void setPrevisto(BigDecimal previsto) { this.previsto = previsto; }
}
