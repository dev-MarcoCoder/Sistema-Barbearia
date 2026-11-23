<%--
    Confirmacao exibida ao cliente depois de agendar: confirmacao.jsp?b=<slug>&c=<codigo>
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="br.com.barbearia.model.Agendamento"%>
<%@page import="br.com.barbearia.model.Barbearia"%>
<%@page import="br.com.barbearia.dao.BarbeariaDAO"%>
<%@page import="br.com.barbearia.service.AgendaService"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%
    String tituloPagina = "Agendamento confirmado";
    Barbearia barbearia = new BarbeariaDAO().buscarPorSlug(request.getParameter("b"));
    Agendamento ag = barbearia == null ? null : new AgendaService().buscarPorCodigo(barbearia, request.getParameter("c"));
    if (ag == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }
    String slug = barbearia.getSlug();
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <%@include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>
<%@include file="/WEB-INF/jspf/topo-publico.jspf" %>

<main class="container">
    <div class="confirmacao card centro">
        <div class="icone-ok" aria-hidden="true">✓</div>
        <h1>Horário reservado!</h1>
        <p class="suave">Te esperamos na <strong><%= Html.esc(barbearia.getNome()) %></strong>.</p>

        <dl class="detalhes" style="text-align:left">
            <dt>Serviço</dt><dd><%= Html.esc(ag.getServicoNome()) %></dd>
            <dt>Barbeiro</dt><dd><%= Html.esc(ag.getBarbeiroNome()) %></dd>
            <dt>Dia</dt><dd><%= Html.esc(Formato.dataExtenso(ag.getData())) %></dd>
            <dt>Horário</dt><dd><%= Formato.hora(ag.getHoraInicio()) %> às <%= Formato.hora(ag.getHoraFim()) %></dd>
            <dt>Valor</dt><dd><%= Formato.moeda(ag.getValor()) %> <span class="suave pequeno">(pago na barbearia)</span></dd>
            <% if (barbearia.getEndereco() != null && !barbearia.getEndereco().isEmpty()) { %>
            <dt>Endereço</dt><dd><%= Html.esc(barbearia.getEndereco()) %></dd>
            <% } %>
        </dl>

        <div class="card" style="background:var(--superficie-2)">
            <div class="suave pequeno">Seu código de agendamento</div>
            <div class="codigo-grande"><%= Html.esc(ag.getCodigo()) %></div>
            <div class="suave pequeno">Guarde este código. Com ele você consulta ou cancela o horário.</div>
        </div>

        <div class="acoes mt" style="justify-content:center">
            <a class="btn btn-secundario" href="consultar.jsp?b=<%= Html.url(slug) %>&c=<%= Html.url(ag.getCodigo()) %>">Ver ou cancelar</a>
            <a class="btn btn-primario" href="agendar.jsp?b=<%= Html.url(slug) %>">Fazer outro agendamento</a>
        </div>
    </div>
</main>

<%@include file="/WEB-INF/jspf/rodape.jspf" %>
</body>
</html>
