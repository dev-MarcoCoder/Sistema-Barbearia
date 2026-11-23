package br.com.barbearia.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Agendamento {

    private int id;
    private int barbeariaId;
    private int barbeiroId;
    private int servicoId;
    private String clienteNome;
    private String clienteTelefone;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private StatusAgendamento status = StatusAgendamento.AGENDADO;
    /** Preco do servico no momento do agendamento (nao muda se o preco do servico mudar depois). */
    private BigDecimal valor;
    /** Codigo curto que o cliente usa para consultar/cancelar sem precisar de login. */
    private String codigo;

    // Dados vindos de JOIN, apenas para exibicao
    private String barbeiroNome;
    private String servicoNome;

    public LocalDateTime getInicio() {
        return LocalDateTime.of(data, horaInicio);
    }

    /** O cliente so pode cancelar um agendamento que ainda nao aconteceu. */
    public boolean podeSerCanceladoPeloCliente() {
        return status == StatusAgendamento.AGENDADO && getInicio().isAfter(LocalDateTime.now());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBarbeariaId() { return barbeariaId; }
    public void setBarbeariaId(int barbeariaId) { this.barbeariaId = barbeariaId; }
    public int getBarbeiroId() { return barbeiroId; }
    public void setBarbeiroId(int barbeiroId) { this.barbeiroId = barbeiroId; }
    public int getServicoId() { return servicoId; }
    public void setServicoId(int servicoId) { this.servicoId = servicoId; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public String getClienteTelefone() { return clienteTelefone; }
    public void setClienteTelefone(String clienteTelefone) { this.clienteTelefone = clienteTelefone; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getBarbeiroNome() { return barbeiroNome; }
    public void setBarbeiroNome(String barbeiroNome) { this.barbeiroNome = barbeiroNome; }
    public String getServicoNome() { return servicoNome; }
    public void setServicoNome(String servicoNome) { this.servicoNome = servicoNome; }
}
