<%--
    Pagina publica de agendamento de uma barbearia: agendar.jsp?b=<slug>
    Mostra uma decisao por vez: servico -> barbeiro -> dia e horario -> dados do cliente.
    As escolhas ja feitas viram uma linha-resumo com o link "Trocar".
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.LocalTime"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="br.com.barbearia.model.Agendamento"%>
<%@page import="br.com.barbearia.model.Barbearia"%>
<%@page import="br.com.barbearia.model.Barbeiro"%>
<%@page import="br.com.barbearia.model.Servico"%>
<%@page import="br.com.barbearia.dao.BarbeariaDAO"%>
<%@page import="br.com.barbearia.dao.BarbeiroDAO"%>
<%@page import="br.com.barbearia.dao.ServicoDAO"%>
<%@page import="br.com.barbearia.service.AgendaService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%@page import="br.com.barbearia.util.Param"%>
<%
    String tituloPagina = "Agendar horário";
    Barbearia barbearia = new BarbeariaDAO().buscarPorSlug(request.getParameter("b"));
    if (barbearia == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }
    tituloPagina = "Agendar em " + barbearia.getNome();
    String slug = barbearia.getSlug();
    String base = "agendar.jsp?b=" + Html.url(slug);

    List<Servico> servicos = new ServicoDAO().listar(barbearia.getId(), true);
    List<Barbeiro> barbeiros = new BarbeiroDAO().listar(barbearia.getId(), true);
    AgendaService agenda = new AgendaService();

    int servicoId = Param.inteiro(request.getParameter("servico"), 0);
    int barbeiroId = Param.inteiro(request.getParameter("barbeiro"), 0);
    LocalDate data = Param.data(request.getParameter("data"));
    LocalTime horaEscolhida = Param.hora(request.getParameter("hora"));
    String erro = null;

    // Confirmacao do agendamento
    if ("POST".equals(request.getMethod())) {
        try {
            Agendamento novo = agenda.agendar(barbearia, servicoId, barbeiroId, data, horaEscolhida,
                    request.getParameter("nome"), request.getParameter("telefone"));
            response.sendRedirect("confirmacao.jsp?b=" + Html.url(slug) + "&c=" + Html.url(novo.getCodigo()));
            return;
        } catch (RegraNegocioException e) {
            erro = e.getMessage();
        }
    }

    Servico servico = null;
    for (Servico s : servicos) {
        if (s.getId() == servicoId) {
            servico = s;
        }
    }
    Barbeiro barbeiro = null;
    for (Barbeiro b : barbeiros) {
        if (b.getId() == barbeiroId) {
            barbeiro = b;
        }
    }
    boolean barbeiroUnico = barbeiros.size() == 1;
    if (barbeiro == null && barbeiroUnico) {
        barbeiro = barbeiros.get(0);
    }

    LocalDate hoje = LocalDate.now();
    LocalDate ultimoDia = hoje.plusDays(AgendaService.DIAS_ANTECEDENCIA);
    if (data != null && (data.isBefore(hoje) || data.isAfter(ultimoDia))) {
        data = null;
    }
    List<LocalDate> dias = new ArrayList<>();
    for (LocalDate d = hoje; !d.isAfter(ultimoDia); d = d.plusDays(1)) {
        dias.add(d);
    }

    List<LocalTime> horarios = null;
    if (servico != null && barbeiro != null && data != null) {
        horarios = agenda.horariosDisponiveis(barbearia, barbeiro, servico, data);
    }
    boolean abrirChat = "1".equals(request.getParameter("chat"));
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <%@include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>
<%@include file="/WEB-INF/jspf/topo-publico.jspf" %>

<main class="container agendar">
    <header class="agendar-topo">
        <h1><%= Html.esc(barbearia.getNome()) %></h1>
        <p class="suave sem-margem">
            <% if (barbearia.getEndereco() != null && !barbearia.getEndereco().isEmpty()) { %><%= Html.esc(barbearia.getEndereco()) %> · <% } %>
            Aberto das <%= Formato.hora(barbearia.getHoraAbertura()) %> às <%= Formato.hora(barbearia.getHoraFechamento()) %>
        </p>
        <div class="acoes mt">
            <button class="btn btn-secundario" type="button" data-abrir-chat>💬 Agendar conversando com o assistente</button>
            <a class="link-discreto" href="consultar.jsp?b=<%= Html.url(slug) %>">Já agendou? Ver ou cancelar</a>
        </div>
    </header>

    <%= Flash.alerta("erro", erro) %>

    <% if (servicos.isEmpty() || barbeiros.isEmpty()) { %>
        <div class="vazio">Esta barbearia ainda não abriu a agenda online. Ligue para <%= Html.esc(Formato.telefone(barbearia.getTelefone())) %>.</div>
    <% } else { %>

    <form method="get" action="agendar.jsp" id="form-escolha" data-auto-submit>
        <input type="hidden" name="b" value="<%= Html.esc(slug) %>">

        <%-- 1. Servico --%>
        <% if (servico == null) { %>
        <section class="etapa" id="etapa-servico">
            <h2 class="etapa-pergunta">Qual serviço você quer?</h2>
            <div class="lista-opcoes">
                <% for (Servico s : servicos) { %>
                <label class="linha-opcao">
                    <input type="radio" name="servico" value="<%= s.getId() %>" required>
                    <span class="linha-opcao-corpo">
                        <span>
                            <span class="linha-opcao-nome"><%= Html.esc(s.getNome()) %></span>
                            <span class="linha-opcao-detalhe"><%= Formato.duracao(s.getDuracaoMin()) %></span>
                        </span>
                        <span class="linha-opcao-preco"><%= Formato.moeda(s.getPreco()) %></span>
                    </span>
                </label>
                <% } %>
            </div>
        </section>
        <% } else { %>
        <input type="hidden" name="servico" value="<%= servico.getId() %>">
        <div class="escolhido">
            <span class="escolhido-check" aria-hidden="true">✓</span>
            <span class="escolhido-texto"><strong><%= Html.esc(servico.getNome()) %></strong>
                <span class="suave"><%= Formato.duracao(servico.getDuracaoMin()) %> · <%= Formato.moeda(servico.getPreco()) %></span></span>
            <a href="<%= base %>">Trocar</a>
        </div>

        <%-- 2. Barbeiro --%>
        <% if (barbeiro == null) { %>
        <section class="etapa" id="etapa-barbeiro">
            <h2 class="etapa-pergunta">Com quem?</h2>
            <div class="lista-opcoes lista-opcoes-grade">
                <% for (Barbeiro b : barbeiros) { %>
                <label class="linha-opcao">
                    <input type="radio" name="barbeiro" value="<%= b.getId() %>" required>
                    <span class="linha-opcao-corpo">
                        <span class="com-avatar-linha">
                            <span class="avatar"><%= Html.esc(b.getNome().substring(0, 1).toUpperCase()) %></span>
                            <span class="linha-opcao-nome"><%= Html.esc(b.getNome()) %></span>
                        </span>
                    </span>
                </label>
                <% } %>
            </div>
        </section>
        <% } else { %>
        <input type="hidden" name="barbeiro" value="<%= barbeiro.getId() %>">
        <div class="escolhido">
            <span class="escolhido-check" aria-hidden="true">✓</span>
            <span class="escolhido-texto">com <strong><%= Html.esc(barbeiro.getNome()) %></strong></span>
            <% if (!barbeiroUnico) { %><a href="<%= base %>&servico=<%= servico.getId() %>">Trocar</a><% } %>
        </div>

        <%-- 3. Dia --%>
        <section class="etapa" id="etapa-data">
            <h2 class="etapa-pergunta">Que dia?</h2>
            <div class="chips chips-rolagem">
                <% for (LocalDate d : dias) {
                       boolean aberto = barbearia.abreNo(d.getDayOfWeek());
                       String rotulo = d.equals(hoje) ? "hoje" : d.equals(hoje.plusDays(1)) ? "amanhã" : Formato.diaSemanaCurto(d);
                %>
                <label class="chip" title="<%= aberto ? Html.esc(Formato.dataExtenso(d)) : "Fechado" %>">
                    <input type="radio" name="data" value="<%= d %>"<%= Html.checked(d.equals(data)) %><%= aberto ? "" : " disabled" %> required>
                    <span><small><%= Html.esc(rotulo) %></small><%= Formato.dataCurta(d) %></span>
                </label>
                <% } %>
            </div>
        </section>
        <% } %>
        <% } %>

        <noscript><button class="btn btn-secundario" type="submit">Continuar</button></noscript>
    </form>

    <%-- 4. Horario + dados --%>
    <% if (horarios != null) { %>
    <form method="post" action="<%= base %>" id="horarios" class="etapa">
        <input type="hidden" name="servico" value="<%= servico.getId() %>">
        <input type="hidden" name="barbeiro" value="<%= barbeiro.getId() %>">
        <input type="hidden" name="data" value="<%= data %>">

        <h2 class="etapa-pergunta">Que horas?</h2>
        <% if (horarios.isEmpty()) { %>
            <div class="vazio">Não há horários livres com <%= Html.esc(barbeiro.getNome()) %> neste dia. Escolha outro dia acima.</div>
        <% } else { %>
            <div class="chips">
                <% for (LocalTime h : horarios) { %>
                <label class="chip chip-hora">
                    <input type="radio" name="hora" value="<%= Formato.hora(h) %>"<%= Html.checked(h.equals(horaEscolhida)) %> required>
                    <span><%= Formato.hora(h) %></span>
                </label>
                <% } %>
            </div>

            <div class="card mt dados-cliente">
                <p class="resumo-linha">
                    <strong><%= Html.esc(servico.getNome()) %></strong> com <%= Html.esc(barbeiro.getNome()) %><br>
                    <span class="suave"><%= Html.esc(Formato.dataExtenso(data)) %> às
                        <span id="resumo-hora"><%= horaEscolhida == null ? "—" : Formato.hora(horaEscolhida) %></span> ·
                        <%= Formato.moeda(servico.getPreco()) %>, pago na barbearia</span>
                </p>
                <div class="linha-campos">
                    <div class="campo">
                        <label for="nome">Seu nome</label>
                        <input type="text" id="nome" name="nome" required maxlength="100" autocomplete="name"
                               value="<%= Html.esc(Param.texto(request.getParameter("nome"))) %>">
                    </div>
                    <div class="campo">
                        <label for="telefone">Celular (WhatsApp)</label>
                        <input type="tel" id="telefone" name="telefone" required autocomplete="tel" placeholder="(11) 98765-4321"
                               value="<%= Html.esc(Param.texto(request.getParameter("telefone"))) %>">
                    </div>
                </div>
                <button class="btn btn-primario btn-grande btn-bloco" type="submit">Confirmar agendamento</button>
            </div>
        <% } %>
    </form>
    <% } %>
    <% } %>
</main>

<%@include file="/WEB-INF/jspf/chat.jspf" %>
<%@include file="/WEB-INF/jspf/rodape.jspf" %>
<script src="<%= ctx %>/js/agendar.js"></script>
</body>
</html>
