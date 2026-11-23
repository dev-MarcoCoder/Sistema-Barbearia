package br.com.barbearia.service;

import br.com.barbearia.dao.AgendamentoDAO;
import br.com.barbearia.dao.BarbeiroDAO;
import br.com.barbearia.dao.ConnectionFactory;
import br.com.barbearia.dao.ServicoDAO;
import br.com.barbearia.model.Agendamento;
import br.com.barbearia.model.Barbearia;
import br.com.barbearia.model.Barbeiro;
import br.com.barbearia.model.Servico;
import br.com.barbearia.model.StatusAgendamento;
import br.com.barbearia.util.Param;
import br.com.barbearia.util.Seguranca;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras da agenda: quais horarios estao livres e como um agendamento e confirmado.
 */
public class AgendaService {

    /** Quantos dias para frente o cliente pode agendar. */
    public static final int DIAS_ANTECEDENCIA = 14;

    private final AgendamentoDAO agendamentoDAO = new AgendamentoDAO();
    private final BarbeiroDAO barbeiroDAO = new BarbeiroDAO();
    private final ServicoDAO servicoDAO = new ServicoDAO();

    public List<LocalTime> horariosDisponiveis(Barbearia barbearia, Barbeiro barbeiro, Servico servico, LocalDate data)
            throws SQLException {
        try (Connection c = ConnectionFactory.getConnection()) {
            List<LocalTime[]> ocupados = agendamentoDAO.listarOcupados(c, barbeiro.getId(), data);
            return gerarHorariosLivres(barbearia, servico.getDuracaoMin(), data, ocupados, LocalDateTime.now());
        }
    }

    /**
     * Algoritmo central da agenda.
     * Percorre o expediente de "intervalo em intervalo" (ex.: de 30 em 30 min) e devolve
     * cada horario em que o servico inteiro cabe sem encostar em outro atendimento do barbeiro.
     *
     * Um horario [inicio, fim) conflita com um ocupado [oInicio, oFim) quando
     * inicio < oFim E fim > oInicio.
     */
    public static List<LocalTime> gerarHorariosLivres(Barbearia barbearia, int duracaoMin, LocalDate data,
            List<LocalTime[]> ocupados, LocalDateTime agora) {

        List<LocalTime> livres = new ArrayList<>();
        if (data == null || duracaoMin <= 0 || data.isBefore(agora.toLocalDate())
                || !barbearia.abreNo(data.getDayOfWeek())) {
            return livres;
        }

        LocalTime fechamento = barbearia.getHoraFechamento();
        int passo = Math.max(5, barbearia.getIntervaloMin());
        LocalTime inicio = barbearia.getHoraAbertura();

        while (inicio.isBefore(fechamento)) {
            LocalTime fim = inicio.plusMinutes(duracaoMin);
            if (fim.isAfter(fechamento) || !fim.isAfter(inicio)) {
                break; // o servico nao cabe antes de fechar (ou passaria da meia-noite)
            }

            boolean jaPassou = data.equals(agora.toLocalDate()) && !inicio.isAfter(agora.toLocalTime());
            if (!jaPassou && !conflita(inicio, fim, ocupados)) {
                livres.add(inicio);
            }

            LocalTime proximo = inicio.plusMinutes(passo);
            if (!proximo.isAfter(inicio)) {
                break; // virou o dia
            }
            inicio = proximo;
        }
        return livres;
    }

    private static boolean conflita(LocalTime inicio, LocalTime fim, List<LocalTime[]> ocupados) {
        for (LocalTime[] o : ocupados) {
            if (inicio.isBefore(o[1]) && fim.isAfter(o[0])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Confirma um agendamento. Tudo roda em uma transacao e o barbeiro fica travado
     * (SELECT ... FOR UPDATE) enquanto a disponibilidade e conferida de novo,
     * para dois clientes nao conseguirem reservar o mesmo horario ao mesmo tempo.
     */
    public Agendamento agendar(Barbearia barbearia, int servicoId, int barbeiroId, LocalDate data, LocalTime hora,
            String nomeCliente, String telefoneCliente) throws RegraNegocioException, SQLException {

        String nome = Param.texto(nomeCliente);
        String telefone = Param.somenteDigitos(telefoneCliente);

        Servico servico = servicoDAO.buscar(servicoId, barbearia.getId());
        if (servico == null || !servico.isAtivo()) {
            throw new RegraNegocioException("Escolha um serviço.");
        }
        Barbeiro barbeiro = barbeiroDAO.buscar(barbeiroId, barbearia.getId());
        if (barbeiro == null || !barbeiro.isAtivo()) {
            throw new RegraNegocioException("Escolha um barbeiro.");
        }
        if (data == null) {
            throw new RegraNegocioException("Escolha o dia do atendimento.");
        }
        if (data.isAfter(LocalDate.now().plusDays(DIAS_ANTECEDENCIA))) {
            throw new RegraNegocioException("É possível agendar com no máximo " + DIAS_ANTECEDENCIA + " dias de antecedência.");
        }
        if (hora == null) {
            throw new RegraNegocioException("Escolha um horário.");
        }
        if (nome.length() < 2 || nome.length() > 100) {
            throw new RegraNegocioException("Informe seu nome.");
        }
        if (telefone.length() < 10 || telefone.length() > 11) {
            throw new RegraNegocioException("Informe um celular com DDD, por exemplo (11) 98765-4321.");
        }

        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                barbeiroDAO.travar(c, barbeiro.getId());

                List<LocalTime[]> ocupados = agendamentoDAO.listarOcupados(c, barbeiro.getId(), data);
                List<LocalTime> livres = gerarHorariosLivres(barbearia, servico.getDuracaoMin(), data, ocupados,
                        LocalDateTime.now());
                if (!livres.contains(hora)) {
                    throw new RegraNegocioException("Esse horário acabou de ficar indisponível. Escolha outro horário.");
                }

                Agendamento a = new Agendamento();
                a.setBarbeariaId(barbearia.getId());
                a.setBarbeiroId(barbeiro.getId());
                a.setServicoId(servico.getId());
                a.setClienteNome(nome);
                a.setClienteTelefone(telefone);
                a.setData(data);
                a.setHoraInicio(hora);
                a.setHoraFim(hora.plusMinutes(servico.getDuracaoMin()));
                a.setStatus(StatusAgendamento.AGENDADO);
                a.setValor(servico.getPreco());
                a.setCodigo(gerarCodigoUnico(c));
                agendamentoDAO.inserir(c, a);

                c.commit();
                a.setBarbeiroNome(barbeiro.getNome());
                a.setServicoNome(servico.getNome());
                return a;
            } catch (RegraNegocioException | SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private String gerarCodigoUnico(Connection c) throws SQLException {
        String codigo;
        do {
            codigo = Seguranca.gerarCodigo(6);
        } while (agendamentoDAO.existeCodigo(c, codigo));
        return codigo;
    }

    /** O proprio cliente cancela pelo codigo recebido na confirmacao. */
    public Agendamento cancelarPeloCliente(Barbearia barbearia, String codigo) throws RegraNegocioException, SQLException {
        Agendamento a = buscarPorCodigo(barbearia, codigo);
        if (a == null) {
            throw new RegraNegocioException("Agendamento não encontrado. Confira o código.");
        }
        if (!a.podeSerCanceladoPeloCliente()) {
            throw new RegraNegocioException("Este agendamento não pode mais ser cancelado.");
        }
        agendamentoDAO.atualizarStatus(a.getId(), barbearia.getId(), StatusAgendamento.CANCELADO);
        a.setStatus(StatusAgendamento.CANCELADO);
        return a;
    }

    public Agendamento buscarPorCodigo(Barbearia barbearia, String codigo) throws SQLException {
        String limpo = Param.texto(codigo).toUpperCase();
        if (limpo.isEmpty() || limpo.length() > 10) {
            return null;
        }
        return agendamentoDAO.buscarPorCodigo(barbearia.getId(), limpo);
    }

    /** Usado pelo painel do barbeiro: concluir, marcar falta ou cancelar. */
    public void alterarStatus(Barbearia barbearia, int agendamentoId, String novoStatus)
            throws RegraNegocioException, SQLException {
        StatusAgendamento status = StatusAgendamento.deTexto(novoStatus);
        if (status == null) {
            throw new RegraNegocioException("Situação inválida.");
        }
        if (!agendamentoDAO.atualizarStatus(agendamentoId, barbearia.getId(), status)) {
            throw new RegraNegocioException("Agendamento não encontrado.");
        }
    }
}
