<%--
    Painel principal: agenda do dia em forma de lista, com acoes para concluir, marcar falta ou cancelar.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jspf/admin-init.jspf" %>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.util.List"%>
<%@page import="br.com.barbearia.model.Agendamento"%>
<%@page import="br.com.barbearia.model.Barbeiro"%>
<%@page import="br.com.barbearia.model.StatusAgendamento"%>
<%@page import="br.com.barbearia.dao.AgendamentoDAO"%>
<%@page import="br.com.barbearia.dao.BarbeiroDAO"%>
<%@page import="br.com.barbearia.service.AgendaService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Csrf"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%@page import="br.com.barbearia.util.Param"%>
<%
    String tituloPagina = "Agenda";
    String paginaAtiva = "painel";

    LocalDate hoje = LocalDate.now();
    LocalDate data = Param.data(request.getParameter("data"));
    if (data == null) {
        data = hoje;
    }
    int filtroBarbeiro = Param.inteiro(request.getParameter("barbeiro"), 0);
    String filtroQuery = filtroBarbeiro > 0 ? "&barbeiro=" + filtroBarbeiro : "";

    if ("POST".equals(request.getMethod())) {
        try {
            new AgendaService().alterarStatus(barbearia, Param.inteiro(request.getParameter("id"), 0),
                    request.getParameter("status"));
            Flash.sucesso(session, "Agendamento atualizado.");
        } catch (RegraNegocioException e) {
            Flash.erro(session, e.getMessage());
        }
        response.sendRedirect("painel.jsp?data=" + data + filtroQuery);
        return;
    }

    List<Barbeiro> barbeiros = new BarbeiroDAO().listar(barbearia.getId(), true);
    List<Agendamento> agenda = new AgendamentoDAO().listarPorData(barbearia.getId(), data, filtroBarbeiro);

    int validos = 0;
    BigDecimal faturado = BigDecimal.ZERO;
    BigDecimal previsto = BigDecimal.ZERO;
    for (Agendamento a : agenda) {
        if (a.getStatus() != StatusAgendamento.CANCELADO) {
            validos++;
        }
        if (a.getStatus() == StatusAgendamento.CONCLUIDO) {
            faturado = faturado.add(a.getValor());
        }
        if (a.getStatus() == StatusAgendamento.CONCLUIDO || a.getStatus() == StatusAgendamento.AGENDADO) {
            previsto = previsto.add(a.getValor());
        }
    }

    String porta = (request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort();
    String linkPublico = request.getScheme() + "://" + request.getServerName() + porta + request.getContextPath()
            + "/agendar.jsp?b=" + barbearia.getSlug();
    String tituloDia = data.equals(hoje) ? "Hoje" : data.equals(hoje.plusDays(1)) ? "Amanhã"
            : data.equals(hoje.minusDays(1)) ? "Ontem" : Formato.dataExtenso(data);
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <%@include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>
<%@include file="/WEB-INF/jspf/admin-menu.jspf" %>

<div class="admin-cabecalho">
    <div>
        <h1><%= Html.esc(tituloDia) %></h1>
        <p class="suave sem-margem"><%= Html.esc(Formato.dataExtenso(data)) %></p>
    </div>
    <div class="acoes">
        <span id="link-publico" hidden><%= Html.esc(linkPublico) %></span>
        <button class="btn btn-secundario btn-pequeno" type="button" data-copiar="link-publico">Copiar link de agendamento</button>
    </div>
</div>

<%= Flash.exibir(session) %>

<div class="barra-ferramentas">
    <a class="btn btn-secundario btn-pequeno" href="painel.jsp?data=<%= data.minusDays(1) %><%= filtroQuery %>" aria-label="Dia anterior">←</a>
    <a class="btn btn-secundario btn-pequeno" href="painel.jsp?data=<%= hoje %><%= filtroQuery %>">Hoje</a>
    <a class="btn btn-secundario btn-pequeno" href="painel.jsp?data=<%= data.plusDays(1) %><%= filtroQuery %>" aria-label="Próximo dia">→</a>
    <span class="data-atual"></span>
    <% if (barbeiros.size() > 1) { %>
    <form method="get" action="painel.jsp" style="margin:0">
        <input type="hidden" name="data" value="<%= data %>">
        <select name="barbeiro" aria-label="Filtrar por barbeiro" onchange="this.form.submit()">
            <option value="0">Todos os barbeiros</option>
            <% for (Barbeiro b : barbeiros) { %>
                <option value="<%= b.getId() %>"<%= Html.selected(b.getId() == filtroBarbeiro) %>><%= Html.esc(b.getNome()) %></option>
            <% } %>
        </select>
        <noscript><button class="btn btn-secundario btn-pequeno" type="submit">Ver</button></noscript>
    </form>
    <% } %>
</div>

<div class="stats stats-3">
    <div class="stat"><div class="stat-rotulo">Atendimentos</div><div class="stat-valor"><%= validos %></div></div>
    <div class="stat"><div class="stat-rotulo">Já recebido</div><div class="stat-valor dourado"><%= Formato.moeda(faturado) %></div></div>
    <div class="stat"><div class="stat-rotulo">Previsto</div><div class="stat-valor"><%= Formato.moeda(previsto) %></div></div>
</div>

<% if (agenda.isEmpty()) { %>
    <div class="vazio">
        Nenhum horário marcado<%= barbearia.abreNo(data.getDayOfWeek()) ? "" : " (a barbearia não abre neste dia)" %>.<br>
        Divulgue seu link para os clientes começarem a agendar.
    </div>
<% } else { %>
<div class="agenda-lista">
    <% for (Agendamento a : agenda) {
           String lembrete = "Olá, " + a.getClienteNome() + "! Passando para lembrar do seu horário na "
                   + barbearia.getNome() + ": " + Formato.dataExtenso(a.getData()) + " às " + Formato.hora(a.getHoraInicio())
                   + " (" + a.getServicoNome() + ").";
    %>
    <div class="agenda-item status-<%= a.getStatus().name().toLowerCase() %>">
        <div class="agenda-hora"><%= Formato.hora(a.getHoraInicio()) %><small>até <%= Formato.hora(a.getHoraFim()) %></small></div>
        <div>
            <div class="agenda-cliente"><%= Html.esc(a.getClienteNome()) %></div>
            <div class="agenda-detalhe"><%= Html.esc(a.getServicoNome()) %> · <%= Html.esc(a.getBarbeiroNome()) %> · <%= Formato.moeda(a.getValor()) %></div>
            <a class="pequeno" href="https://wa.me/55<%= Html.esc(a.getClienteTelefone()) %>?text=<%= Html.url(lembrete) %>" target="_blank" rel="noopener"
               title="Enviar lembrete pelo WhatsApp">WhatsApp <%= Html.esc(Formato.telefone(a.getClienteTelefone())) %></a>
        </div>
        <div class="agenda-acoes">
            <span class="badge badge-<%= a.getStatus().name().toLowerCase() %>"><%= Html.esc(a.getStatus().getDescricao()) %></span>
            <form method="post" action="painel.jsp?data=<%= data %><%= filtroQuery %>">
                <%= Csrf.campo(session) %>
                <input type="hidden" name="id" value="<%= a.getId() %>">
                <% if (a.getStatus() == StatusAgendamento.AGENDADO) { %>
                    <button class="btn btn-sucesso btn-pequeno" name="status" value="CONCLUIDO">Concluir</button>
                    <button class="btn btn-secundario btn-pequeno" name="status" value="FALTOU">Faltou</button>
                    <button class="btn btn-perigo btn-pequeno" name="status" value="CANCELADO"
                            onclick="return confirm('Cancelar este horário?');">Cancelar</button>
                <% } else { %>
                    <button class="btn btn-secundario btn-pequeno" name="status" value="AGENDADO">Desfazer</button>
                <% } %>
            </form>
        </div>
    </div>
    <% } %>
</div>
<% } %>

<%@include file="/WEB-INF/jspf/admin-fim.jspf" %>
<script src="<%= ctx %>/js/painel.js"></script>
</body>
</html>
