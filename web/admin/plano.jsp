<%--
    Assinatura da barbearia (modelo de cobranca do SaaS).
    Nesta versao a troca de plano e simulada: nao ha integracao com meio de pagamento.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jspf/admin-init.jspf" %>
<%@page import="br.com.barbearia.model.Plano"%>
<%@page import="br.com.barbearia.service.GestaoService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Csrf"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%
    String tituloPagina = "Meu plano";
    String paginaAtiva = "plano";

    if ("POST".equals(request.getMethod())) {
        try {
            new GestaoService().alterarPlano(barbearia, request.getParameter("plano"));
            Flash.sucesso(session, "Plano alterado para " + barbearia.getPlano().getNome() + ". A nova mensalidade vale a partir da próxima fatura.");
        } catch (RegraNegocioException e) {
            Flash.erro(session, e.getMessage());
        }
        response.sendRedirect("plano.jsp");
        return;
    }
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
        <h1>Meu plano</h1>
        <p class="suave sem-margem">Você está no plano <strong><%= Html.esc(barbearia.getPlano().getNome()) %></strong>
            · <%= Formato.moeda(barbearia.getPlano().getPrecoMensal()) %> por mês.</p>
    </div>
</div>

<%= Flash.exibir(session) %>

<div class="grid grid-2" style="max-width:860px">
    <% for (Plano p : Plano.values()) {
           boolean atual = p == barbearia.getPlano();
    %>
    <div class="card plano<%= atual ? " destaque" : "" %>">
        <% if (atual) { %><span class="selo" style="align-self:flex-start;margin-bottom:8px">Plano atual</span><% } %>
        <h3 class="sem-margem"><%= Html.esc(p.getNome()) %></h3>
        <div class="plano-preco"><%= Formato.moeda(p.getPrecoMensal()) %> <small>/mês</small></div>
        <p class="suave sem-margem"><%= Html.esc(p.getDescricao()) %></p>
        <ul>
            <li>Link de agendamento online 24h</li>
            <li>Agenda com controle de status</li>
            <li><%= Html.esc(p.getLimiteBarbeirosTexto()) %></li>
            <li class="<%= p.temRelatorios() ? "" : "nao" %>">Relatórios de faturamento e ranking</li>
        </ul>
        <% if (atual) { %>
            <button class="btn btn-secundario btn-bloco" type="button" disabled>Seu plano atual</button>
        <% } else { %>
            <form method="post" action="plano.jsp" data-confirmar="Mudar para o plano <%= Html.esc(p.getNome()) %> por <%= Formato.moeda(p.getPrecoMensal()) %>/mês?">
                <%= Csrf.campo(session) %>
                <input type="hidden" name="plano" value="<%= p.name() %>">
                <button class="btn btn-primario btn-bloco" type="submit">Mudar para <%= Html.esc(p.getNome()) %></button>
            </form>
        <% } %>
    </div>
    <% } %>
</div>

<div class="alerta alerta-info mt" style="max-width:860px">
    Versão de demonstração: a troca de plano é registrada no sistema, mas nenhuma cobrança é feita.
    Na versão comercial a mensalidade seria cobrada automaticamente por cartão de crédito ou Pix recorrente,
    através de um gateway de pagamento.
</div>

<%@include file="/WEB-INF/jspf/admin-fim.jspf" %>
<script src="<%= ctx %>/js/painel.js"></script>
</body>
</html>
