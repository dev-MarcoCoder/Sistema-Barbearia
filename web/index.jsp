<%--
    Pagina inicial do SaaS: curta e direta.
    Cliente -> escolhe a barbearia e agenda. Dono de barbearia -> cadastra.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="br.com.barbearia.model.Barbearia"%>
<%@page import="br.com.barbearia.model.Plano"%>
<%@page import="br.com.barbearia.dao.BarbeariaDAO"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%
    String tituloPagina = "Agende seu corte em poucos toques";
    List<Barbearia> barbearias = new BarbeariaDAO().listarTodas();
    Barbearia destaque = barbearias.isEmpty() ? null : barbearias.get(0);
    for (Barbearia b : barbearias) {
        if (b.getPlano() == Plano.PROFISSIONAL) {
            destaque = b;
            break;
        }
    }
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <%@include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>
<%@include file="/WEB-INF/jspf/topo-publico.jspf" %>

<main>
    <section class="hero hero-simples">
        <div class="container">
            <h1>Agende seu corte em poucos toques.</h1>
            <p class="lead">Escolha a barbearia, o serviço e um horário livre. Se preferir, converse com o assistente e ele marca para você.</p>
            <% if (destaque != null) { %>
            <div class="acoes">
                <a class="btn btn-primario btn-grande" href="<%= ctx %>/agendar.jsp?b=<%= Html.url(destaque.getSlug()) %>">Agendar horário</a>
                <a class="btn btn-secundario btn-grande" href="<%= ctx %>/agendar.jsp?b=<%= Html.url(destaque.getSlug()) %>&chat=1">💬 Falar com o assistente</a>
            </div>
            <% } %>
        </div>
    </section>

    <section class="secao" id="barbearias">
        <div class="container">
            <h2>Barbearias</h2>
            <% if (barbearias.isEmpty()) { %>
                <div class="vazio">Nenhuma barbearia cadastrada ainda. <a href="<%= ctx %>/cadastro.jsp">Seja a primeira.</a></div>
            <% } else { %>
            <div class="lista-barbearias">
                <% for (Barbearia b : barbearias) { %>
                <a class="barbearia-item" href="<%= ctx %>/agendar.jsp?b=<%= Html.url(b.getSlug()) %>">
                    <span class="avatar"><%= Html.esc(b.getNome().substring(0, 1).toUpperCase()) %></span>
                    <span class="barbearia-item-texto">
                        <strong><%= Html.esc(b.getNome()) %></strong>
                        <span class="suave pequeno"><%= Html.esc(b.getEndereco()) %></span>
                    </span>
                    <span class="barbearia-item-acao">Agendar →</span>
                </a>
                <% } %>
            </div>
            <% } %>
        </div>
    </section>

    <section class="secao secao-dono" id="planos">
        <div class="container">
            <div class="dono-chamada">
                <div>
                    <h2>Tem uma barbearia?</h2>
                    <p class="suave">Receba agendamentos 24h por um link próprio, com assistente de IA incluso.
                        Acompanhe a agenda do dia e o faturamento num painel simples. 14 dias grátis.</p>
                    <a class="btn btn-primario" href="<%= ctx %>/cadastro.jsp">Cadastrar minha barbearia</a>
                </div>
                <div class="planos-mini">
                    <% for (Plano p : Plano.values()) { %>
                    <div class="plano-mini<%= p == Plano.PROFISSIONAL ? " destaque" : "" %>">
                        <span class="suave pequeno"><%= Html.esc(p.getNome()) %></span>
                        <strong><%= Formato.moeda(p.getPrecoMensal()) %><small>/mês</small></strong>
                        <span class="pequeno"><%= Html.esc(p.getLimiteBarbeirosTexto()) %><%= p.temRelatorios() ? " · relatórios" : "" %></span>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </section>
</main>

<%@include file="/WEB-INF/jspf/rodape.jspf" %>
</body>
</html>
