<%--
    Equipe da barbearia. A quantidade de barbeiros ativos e limitada pelo plano.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jspf/admin-init.jspf" %>
<%@page import="java.util.List"%>
<%@page import="br.com.barbearia.model.Barbeiro"%>
<%@page import="br.com.barbearia.model.Plano"%>
<%@page import="br.com.barbearia.dao.BarbeiroDAO"%>
<%@page import="br.com.barbearia.service.GestaoService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Csrf"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Param"%>
<%
    String tituloPagina = "Barbeiros";
    String paginaAtiva = "barbeiros";
    GestaoService gestao = new GestaoService();
    BarbeiroDAO barbeiroDAO = new BarbeiroDAO();
    String erro = null;

    if ("POST".equals(request.getMethod())) {
        String acao = Param.texto(request.getParameter("acao"));
        int id = Param.inteiro(request.getParameter("id"), 0);
        try {
            switch (acao) {
                case "adicionar":
                    gestao.adicionarBarbeiro(barbearia, request.getParameter("nome"));
                    Flash.sucesso(session, "Barbeiro adicionado. Ele já aparece na página de agendamento.");
                    break;
                case "renomear":
                    gestao.renomearBarbeiro(barbearia, id, request.getParameter("nome"));
                    Flash.sucesso(session, "Nome atualizado.");
                    break;
                case "ativar":
                    gestao.ativarBarbeiro(barbearia, id);
                    Flash.sucesso(session, "Barbeiro reativado.");
                    break;
                case "desativar":
                    gestao.desativarBarbeiro(barbearia, id);
                    Flash.sucesso(session, "Barbeiro desativado. O histórico de atendimentos foi mantido.");
                    break;
                default:
                    break;
            }
            response.sendRedirect("barbeiros.jsp");
            return;
        } catch (RegraNegocioException e) {
            erro = e.getMessage();
        }
    }

    List<Barbeiro> barbeiros = barbeiroDAO.listar(barbearia.getId(), false);
    int ativos = 0;
    for (Barbeiro b : barbeiros) {
        if (b.isAtivo()) {
            ativos++;
        }
    }
    Plano plano = barbearia.getPlano();
    boolean noLimite = ativos >= plano.getLimiteBarbeiros();
    Barbeiro emEdicao = null;
    int editarId = Param.inteiro(request.getParameter("editar"), 0);
    if (editarId > 0) {
        emEdicao = barbeiroDAO.buscar(editarId, barbearia.getId());
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
        <h1>Barbeiros</h1>
        <p class="suave sem-margem">Cada barbeiro tem a própria agenda. O cliente escolhe com quem quer cortar.</p>
    </div>
</div>

<%= Flash.exibir(session) %>
<%= Flash.alerta("erro", erro) %>

<div class="grid grid-lateral">
    <div class="tabela-wrap">
        <table class="tabela" style="min-width:480px">
            <thead><tr><th>Nome</th><th>Situação</th><th>Ações</th></tr></thead>
            <tbody>
            <% for (Barbeiro b : barbeiros) { %>
                <tr class="<%= b.isAtivo() ? "" : "inativo" %>">
                    <td>
                        <% if (emEdicao != null && emEdicao.getId() == b.getId()) { %>
                        <form method="post" action="barbeiros.jsp" class="acoes">
                            <%= Csrf.campo(session) %>
                            <input type="hidden" name="acao" value="renomear">
                            <input type="hidden" name="id" value="<%= b.getId() %>">
                            <input type="text" name="nome" value="<%= Html.esc(b.getNome()) %>" required maxlength="100" aria-label="Nome do barbeiro" style="max-width:240px" autofocus>
                            <button class="btn btn-primario btn-pequeno" type="submit">Salvar</button>
                            <a class="btn btn-secundario btn-pequeno" href="barbeiros.jsp">Cancelar</a>
                        </form>
                        <% } else { %>
                            <span class="avatar" style="width:30px;height:30px;font-size:.85rem;margin-right:8px"><%= Html.esc(b.getNome().substring(0, 1).toUpperCase()) %></span>
                            <strong><%= Html.esc(b.getNome()) %></strong>
                        <% } %>
                    </td>
                    <td><span class="badge <%= b.isAtivo() ? "badge-ativo" : "badge-inativo" %>"><%= b.isAtivo() ? "Ativo" : "Inativo" %></span></td>
                    <td>
                        <div class="acoes">
                            <a class="btn btn-secundario btn-pequeno" href="barbeiros.jsp?editar=<%= b.getId() %>">Renomear</a>
                            <form method="post" action="barbeiros.jsp">
                                <%= Csrf.campo(session) %>
                                <input type="hidden" name="id" value="<%= b.getId() %>">
                                <% if (b.isAtivo()) { %>
                                    <button class="btn btn-perigo btn-pequeno" name="acao" value="desativar">Desativar</button>
                                <% } else { %>
                                    <button class="btn btn-sucesso btn-pequeno" name="acao" value="ativar">Reativar</button>
                                <% } %>
                            </form>
                        </div>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>

    <div>
        <div class="card">
            <h3>Uso do plano</h3>
            <div class="suave pequeno">Plano <%= Html.esc(plano.getNome()) %> · <%= Html.esc(plano.getLimiteBarbeirosTexto()) %></div>
            <div class="stat-valor"><%= ativos %><span class="suave" style="font-size:1rem"><%= plano.barbeirosIlimitados() ? " ativos" : " de " + plano.getLimiteBarbeiros() %></span></div>
            <% if (!plano.barbeirosIlimitados()) { %>
                <div class="progresso"><div style="width:<%= Math.min(100, ativos * 100 / plano.getLimiteBarbeiros()) %>%"></div></div>
            <% } %>
        </div>

        <form class="card" method="post" action="barbeiros.jsp">
            <h3>Adicionar barbeiro</h3>
            <%= Csrf.campo(session) %>
            <input type="hidden" name="acao" value="adicionar">
            <% if (noLimite) { %>
                <p class="suave">Você atingiu o limite do plano <%= Html.esc(plano.getNome()) %>.</p>
                <a class="btn btn-primario btn-bloco" href="plano.jsp">Ver plano Profissional</a>
            <% } else { %>
                <div class="campo">
                    <label for="nome">Nome</label>
                    <input type="text" id="nome" name="nome" required maxlength="100">
                </div>
                <button class="btn btn-primario btn-bloco" type="submit">Adicionar</button>
            <% } %>
        </form>
    </div>
</div>

<%@include file="/WEB-INF/jspf/admin-fim.jspf" %>
</body>
</html>
