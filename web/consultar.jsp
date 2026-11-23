<%--
    O cliente consulta ou cancela o agendamento pelo codigo: consultar.jsp?b=<slug>&c=<codigo>
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="br.com.barbearia.model.Agendamento"%>
<%@page import="br.com.barbearia.model.Barbearia"%>
<%@page import="br.com.barbearia.dao.BarbeariaDAO"%>
<%@page import="br.com.barbearia.service.AgendaService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%@page import="br.com.barbearia.util.Param"%>
<%
    String tituloPagina = "Consultar agendamento";
    Barbearia barbearia = new BarbeariaDAO().buscarPorSlug(request.getParameter("b"));
    if (barbearia == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }
    String slug = barbearia.getSlug();
    AgendaService agenda = new AgendaService();
    String codigo = Param.texto(request.getParameter("c")).toUpperCase();

    if ("POST".equals(request.getMethod())) {
        try {
            agenda.cancelarPeloCliente(barbearia, codigo);
            Flash.sucesso(session, "Agendamento cancelado. O horário foi liberado para outros clientes.");
        } catch (RegraNegocioException e) {
            Flash.erro(session, e.getMessage());
        }
        response.sendRedirect("consultar.jsp?b=" + Html.url(slug) + "&c=" + Html.url(codigo));
        return;
    }

    Agendamento ag = codigo.isEmpty() ? null : agenda.buscarPorCodigo(barbearia, codigo);
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <%@include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>
<%@include file="/WEB-INF/jspf/topo-publico.jspf" %>

<main class="container">
    <div class="auth">
        <p class="suave sem-margem"><a href="agendar.jsp?b=<%= Html.url(slug) %>">← <%= Html.esc(barbearia.getNome()) %></a></p>
        <h1>Consultar agendamento</h1>

        <%= Flash.exibir(session) %>

        <form class="card" method="get" action="consultar.jsp">
            <input type="hidden" name="b" value="<%= Html.esc(slug) %>">
            <div class="campo">
                <label for="c">Código do agendamento</label>
                <input type="text" id="c" name="c" required maxlength="10" placeholder="Ex.: K7M2QX"
                       style="text-transform:uppercase;letter-spacing:.12em" value="<%= Html.esc(codigo) %>">
                <span class="dica">O código aparece na tela de confirmação.</span>
            </div>
            <button class="btn btn-primario btn-bloco" type="submit">Buscar</button>
        </form>

        <% if (!codigo.isEmpty() && ag == null) { %>
            <div class="alerta alerta-erro mt">Nenhum agendamento encontrado com esse código nesta barbearia.</div>
        <% } else if (ag != null) { %>
            <div class="card">
                <div class="card-titulo">
                    <h3><%= Html.esc(ag.getServicoNome()) %></h3>
                    <span class="badge badge-<%= ag.getStatus().name().toLowerCase() %>"><%= Html.esc(ag.getStatus().getDescricao()) %></span>
                </div>
                <dl class="detalhes">
                    <dt>Cliente</dt><dd><%= Html.esc(ag.getClienteNome()) %></dd>
                    <dt>Barbeiro</dt><dd><%= Html.esc(ag.getBarbeiroNome()) %></dd>
                    <dt>Dia</dt><dd><%= Html.esc(Formato.dataExtenso(ag.getData())) %></dd>
                    <dt>Horário</dt><dd><%= Formato.hora(ag.getHoraInicio()) %> às <%= Formato.hora(ag.getHoraFim()) %></dd>
                    <dt>Valor</dt><dd><%= Formato.moeda(ag.getValor()) %></dd>
                </dl>
                <% if (ag.podeSerCanceladoPeloCliente()) { %>
                <form method="post" action="consultar.jsp?b=<%= Html.url(slug) %>"
                      onsubmit="return confirm('Cancelar este agendamento?');">
                    <input type="hidden" name="c" value="<%= Html.esc(ag.getCodigo()) %>">
                    <button class="btn btn-perigo btn-bloco" type="submit">Cancelar agendamento</button>
                </form>
                <% } %>
            </div>
        <% } %>
    </div>
</main>

<%@include file="/WEB-INF/jspf/rodape.jspf" %>
</body>
</html>
