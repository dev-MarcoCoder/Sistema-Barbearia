package br.com.barbearia.ia;

import br.com.barbearia.dao.BarbeiroDAO;
import br.com.barbearia.dao.ServicoDAO;
import br.com.barbearia.model.Agendamento;
import br.com.barbearia.model.Barbearia;
import br.com.barbearia.model.Barbeiro;
import br.com.barbearia.model.Servico;
import br.com.barbearia.service.AgendaService;
import br.com.barbearia.service.RegraNegocioException;
import br.com.barbearia.util.Formato;
import br.com.barbearia.util.Param;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ferramentas que o assistente de IA pode usar (function calling, via OpenRouter).
 *
 * A IA so decide QUAL ferramenta chamar e com quais dados. Quem executa e o nosso servidor,
 * reaproveitando as mesmas regras de negocio da pagina (AgendaService). Por isso a IA nao consegue
 * marcar um horario ocupado nem mexer em outra barbearia: a barbearia vem da sessao do site,
 * nunca do texto da conversa.
 */
public final class FerramentasIA {

    private static final ObjectMapper JSON = new ObjectMapper();

    private FerramentasIA() {
    }

    /** Definicao de uma ferramenta: nome, descricao e parametros (JSON Schema). */
    static final class Ferramenta {
        final String nome;
        final String descricao;
        final Map<String, Object> propriedades = new LinkedHashMap<>();
        final List<String> obrigatorios = new ArrayList<>();

        Ferramenta(String nome, String descricao) {
            this.nome = nome;
            this.descricao = descricao;
        }

        Ferramenta param(String nome, String tipo, String descricao, boolean obrigatorio) {
            propriedades.put(nome, Map.of("type", tipo, "description", descricao));
            if (obrigatorio) {
                obrigatorios.add(nome);
            }
            return this;
        }
    }

    static final List<Ferramenta> TODAS = List.of(
            new Ferramenta("ver_barbearia",
                    "Mostra os dados da barbearia: nome, endereço, telefone, horário e dias de funcionamento, "
                    + "a data e a hora de agora e até quantos dias à frente dá para agendar."),
            new Ferramenta("listar_servicos",
                    "Lista os serviços da barbearia com id, duração e preço."),
            new Ferramenta("listar_barbeiros",
                    "Lista os barbeiros da barbearia com id e nome."),
            new Ferramenta("ver_horarios_livres",
                    "Mostra os horários livres de um serviço em um dia. Sem barbeiro_id, mostra os horários de cada barbeiro.")
                    .param("servico_id", "integer", "Id do serviço (de listar_servicos).", true)
                    .param("data", "string", "Dia no formato AAAA-MM-DD.", true)
                    .param("barbeiro_id", "integer", "Id do barbeiro (opcional).", false),
            new Ferramenta("criar_agendamento",
                    "Marca o horário. Use só depois que o cliente confirmar serviço, barbeiro, dia, hora, nome e celular.")
                    .param("servico_id", "integer", "Id do serviço.", true)
                    .param("barbeiro_id", "integer", "Id do barbeiro.", true)
                    .param("data", "string", "Dia no formato AAAA-MM-DD.", true)
                    .param("hora", "string", "Horário no formato HH:MM, um dos horários livres.", true)
                    .param("nome_cliente", "string", "Nome do cliente.", true)
                    .param("telefone", "string", "Celular do cliente com DDD.", true),
            new Ferramenta("consultar_agendamento",
                    "Busca um agendamento pelo código de 6 caracteres que o cliente recebeu.")
                    .param("codigo", "string", "Código do agendamento.", true),
            new Ferramenta("cancelar_agendamento",
                    "Cancela um agendamento pelo código. Use só depois que o cliente confirmar que quer cancelar.")
                    .param("codigo", "string", "Código do agendamento.", true)
    );

    private static final List<Map<String, Object>> FORMATO_OPENROUTER = montarFormatoOpenRouter();

    /** As ferramentas no formato "tools" da API Chat Completions (OpenRouter). */
    static List<Map<String, Object>> paraOpenRouter() {
        return FORMATO_OPENROUTER;
    }

    private static List<Map<String, Object>> montarFormatoOpenRouter() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Ferramenta f : TODAS) {
            Map<String, Object> parametros = new LinkedHashMap<>();
            parametros.put("type", "object");
            parametros.put("properties", f.propriedades);
            parametros.put("required", f.obrigatorios);
            Map<String, Object> funcao = new LinkedHashMap<>();
            funcao.put("name", f.nome);
            funcao.put("description", f.descricao);
            funcao.put("parameters", parametros);
            lista.add(Map.of("type", "function", "function", funcao));
        }
        return List.copyOf(lista);
    }

    /** Resultado de uma ferramenta: texto (JSON) devolvido para a IA. */
    public static final class Resultado {
        final String texto;
        final boolean erro;
        /** Preenchido quando a ferramenta criou um agendamento. */
        final String codigoCriado;

        Resultado(String texto, boolean erro, String codigoCriado) {
            this.texto = texto;
            this.erro = erro;
            this.codigoCriado = codigoCriado;
        }
    }

    private static Resultado ok(Object dados) {
        try {
            return new Resultado(JSON.writeValueAsString(dados), false, null);
        } catch (Exception e) {
            return new Resultado(String.valueOf(dados), false, null);
        }
    }

    private static Resultado erro(String mensagem) {
        return new Resultado("{\"erro\": " + JSON.valueToTree(mensagem).toString() + "}", true, null);
    }

    /** Executa a ferramenta pedida pela IA, sempre dentro da barbearia desta conversa. */
    public static Resultado executar(String nome, Map<String, Object> entrada, Barbearia barbearia) {
        try {
            switch (nome) {
                case "ver_barbearia":
                    return verBarbearia(barbearia);
                case "listar_servicos":
                    return listarServicos(barbearia);
                case "listar_barbeiros":
                    return listarBarbeiros(barbearia);
                case "ver_horarios_livres":
                    return verHorarios(barbearia, entrada);
                case "criar_agendamento":
                    return criarAgendamento(barbearia, entrada);
                case "consultar_agendamento":
                    return consultar(barbearia, entrada);
                case "cancelar_agendamento":
                    return cancelar(barbearia, entrada);
                default:
                    return erro("Ferramenta desconhecida: " + nome);
            }
        } catch (RegraNegocioException e) {
            return erro(e.getMessage());
        } catch (SQLException e) {
            return erro("Falha ao acessar o banco de dados. Peça para o cliente tentar de novo em instantes.");
        }
    }

    // ------------------------------------------------------------------ ferramentas

    private static Resultado verBarbearia(Barbearia b) {
        LocalDateTime agora = LocalDateTime.now();
        List<String> dias = new ArrayList<>();
        for (Integer d : b.getDiasFuncionamento()) {
            dias.add(Formato.diaSemana(d));
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("nome", b.getNome());
        dados.put("endereco", b.getEndereco());
        dados.put("telefone", Formato.telefone(b.getTelefone()));
        dados.put("horario", Formato.hora(b.getHoraAbertura()) + " às " + Formato.hora(b.getHoraFechamento()));
        dados.put("dias_de_funcionamento", dias);
        dados.put("hoje", agora.toLocalDate().toString() + " (" + Formato.diaSemana(agora.getDayOfWeek().getValue()) + ")");
        dados.put("hora_atual", Formato.hora(agora.toLocalTime()));
        dados.put("agenda_ate", agora.toLocalDate().plusDays(AgendaService.DIAS_ANTECEDENCIA).toString());
        dados.put("pagamento", "Na barbearia, depois do atendimento.");
        return ok(dados);
    }

    private static Resultado listarServicos(Barbearia b) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Servico s : new ServicoDAO().listar(b.getId(), true)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("nome", s.getNome());
            item.put("descricao", s.getDescricao());
            item.put("duracao", Formato.duracao(s.getDuracaoMin()));
            item.put("preco", Formato.moeda(s.getPreco()));
            lista.add(item);
        }
        return ok(Map.of("servicos", lista));
    }

    private static Resultado listarBarbeiros(Barbearia b) throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Barbeiro br : new BarbeiroDAO().listar(b.getId(), true)) {
            lista.add(Map.of("id", br.getId(), "nome", br.getNome()));
        }
        return ok(Map.of("barbeiros", lista));
    }

    private static Resultado verHorarios(Barbearia b, Map<String, Object> entrada)
            throws SQLException, RegraNegocioException {
        Servico servico = new ServicoDAO().buscar(inteiro(entrada, "servico_id"), b.getId());
        if (servico == null || !servico.isAtivo()) {
            throw new RegraNegocioException("Serviço não encontrado. Use listar_servicos para ver os ids.");
        }
        LocalDate data = Param.data(texto(entrada, "data"));
        LocalDate hoje = LocalDate.now();
        if (data == null) {
            throw new RegraNegocioException("Data inválida. Use o formato AAAA-MM-DD.");
        }
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(AgendaService.DIAS_ANTECEDENCIA))) {
            throw new RegraNegocioException("Só é possível agendar de " + hoje + " até "
                    + hoje.plusDays(AgendaService.DIAS_ANTECEDENCIA) + ".");
        }
        if (!b.abreNo(data.getDayOfWeek())) {
            throw new RegraNegocioException("A barbearia não abre em " + Formato.diaSemana(data.getDayOfWeek().getValue()) + ".");
        }

        int barbeiroId = inteiro(entrada, "barbeiro_id");
        AgendaService agenda = new AgendaService();
        List<Map<String, Object>> porBarbeiro = new ArrayList<>();
        for (Barbeiro br : new BarbeiroDAO().listar(b.getId(), true)) {
            if (barbeiroId > 0 && br.getId() != barbeiroId) {
                continue;
            }
            List<String> horas = new ArrayList<>();
            for (LocalTime h : agenda.horariosDisponiveis(b, br, servico, data)) {
                horas.add(Formato.hora(h));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("barbeiro_id", br.getId());
            item.put("barbeiro", br.getNome());
            item.put("horarios_livres", horas);
            porBarbeiro.add(item);
        }
        if (porBarbeiro.isEmpty()) {
            throw new RegraNegocioException("Barbeiro não encontrado. Use listar_barbeiros para ver os ids.");
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("data", data.toString());
        dados.put("dia", Formato.dataExtenso(data));
        dados.put("servico", servico.getNome() + " (" + Formato.duracao(servico.getDuracaoMin()) + ")");
        dados.put("barbeiros", porBarbeiro);
        return ok(dados);
    }

    private static Resultado criarAgendamento(Barbearia b, Map<String, Object> entrada)
            throws SQLException, RegraNegocioException {
        Agendamento a = new AgendaService().agendar(b,
                inteiro(entrada, "servico_id"),
                inteiro(entrada, "barbeiro_id"),
                Param.data(texto(entrada, "data")),
                Param.hora(texto(entrada, "hora")),
                texto(entrada, "nome_cliente"),
                texto(entrada, "telefone"));
        Resultado r = ok(descrever(a));
        return new Resultado(r.texto, false, a.getCodigo());
    }

    private static Resultado consultar(Barbearia b, Map<String, Object> entrada) throws SQLException {
        Agendamento a = new AgendaService().buscarPorCodigo(b, texto(entrada, "codigo"));
        if (a == null) {
            return erro("Nenhum agendamento com esse código nesta barbearia.");
        }
        Map<String, Object> dados = descrever(a);
        dados.put("pode_cancelar", a.podeSerCanceladoPeloCliente());
        return ok(dados);
    }

    private static Resultado cancelar(Barbearia b, Map<String, Object> entrada)
            throws SQLException, RegraNegocioException {
        Agendamento a = new AgendaService().cancelarPeloCliente(b, texto(entrada, "codigo"));
        return ok(descrever(a));
    }

    private static Map<String, Object> descrever(Agendamento a) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("codigo", a.getCodigo());
        dados.put("situacao", a.getStatus().getDescricao());
        dados.put("servico", a.getServicoNome());
        dados.put("barbeiro", a.getBarbeiroNome());
        dados.put("dia", Formato.dataExtenso(a.getData()));
        dados.put("horario", Formato.hora(a.getHoraInicio()) + " às " + Formato.hora(a.getHoraFim()));
        dados.put("valor", Formato.moeda(a.getValor()));
        dados.put("cliente", a.getClienteNome());
        return dados;
    }

    // ------------------------------------------------------------------ leitura dos parametros

    /** Le um parametro como texto, aceitando que a IA mande numero ou texto. */
    private static Object bruto(Map<String, Object> entrada, String chave) {
        return entrada == null ? null : entrada.get(chave);
    }

    private static String texto(Map<String, Object> entrada, String chave) {
        Object valor = bruto(entrada, chave);
        if (valor instanceof Number) {
            return String.valueOf(((Number) valor).longValue());
        }
        return valor == null ? null : valor.toString().trim();
    }

    private static int inteiro(Map<String, Object> entrada, String chave) {
        Object valor = bruto(entrada, chave);
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        return valor == null ? 0 : Param.inteiro(valor.toString(), 0);
    }
}
