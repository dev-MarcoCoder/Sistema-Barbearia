<%--
    Cadastro de servicos da barbearia (nome, duracao, preco).
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jspf/admin-init.jspf" %>
<%@page import="java.util.List"%>
<%@page import="br.com.barbearia.model.Servico"%>
<%@page import="br.com.barbearia.dao.ServicoDAO"%>
<%@page import="br.com.barbearia.service.GestaoService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Csrf"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%@page import="br.com.barbearia.util.Param"%>
<%
    String tituloPagina = "Serviços";
    String paginaAtiva = "servicos";
    GestaoService gestao = new GestaoService();
    ServicoDAO servicoDAO = new ServicoDAO();
    String erro = null;

    if ("POST".equals(request.getMethod())) {
        String acao = Param.texto(request.getParameter("acao"));
        int id = Param.inteiro(request.getParameter("id"), 0);
        try {
            if ("salvar".equals(acao)) {
                gestao.salvarServico(barbearia, id, request.getParameter("nome"), request.getParameter("descricao"),
                        request.getParameter("duracao"), request.getParameter("preco"));
                Flash.sucesso(session, id > 0 ? "Serviço atualizado." : "Serviço adicionado.");
            } else if ("ativar".equals(acao) || "desativar".equals(acao)) {
                gestao.definirServicoAtivo(barbearia, id, "ativar".equals(acao));
                Flash.sucesso(session, "ativar".equals(acao) ? "Serviço reativado." : "Serviço desativado. Ele não aparece mais para os clientes.");
            }
            response.sendRedirect("servicos.jsp");
            return;
        } catch (RegraNegocioException e) {
            erro = e.getMessage();
        }
    }

    List<Servico> servicos = servicoDAO.listar(barbearia.getId(), false);

    // formulario: edicao (?editar=id), reexibicao apos erro, ou novo
    Servico emEdicao = null;
    int editarId = Param.inteiro(request.getParameter(erro != null ? "id" : "editar"), 0);
    if (editarId > 0) {
        emEdicao = servicoDAO.buscar(editarId, barbearia.getId());
    }
    String fNome, fDescricao, fDuracao, fPreco;
    if (erro != null) {
        fNome = Param.texto(request.getParameter("nome"));
        fDescricao = Param.texto(request.getParameter("descricao"));
        fDuracao = Param.texto(request.getParameter("duracao"));
        fPreco = Param.texto(request.getParameter("preco"));
    } else if (emEdicao != null) {
        fNome = emEdicao.getNome();
        fDescricao = emEdicao.getDescricao() == null ? "" : emEdicao.getDescricao();
        fDuracao = String.valueOf(emEdicao.getDuracaoMin());
        fPreco = Formato.decimal(emEdicao.getPreco());
    } else {
        fNome = "";
        fDescricao = "";
        fDuracao = "30";
        fPreco = "";
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
        <h1>Serviços</h1>
        <p class="suave sem-margem">O que a sua barbearia oferece. A duração define quanto tempo o horário fica reservado na agenda.</p>
    </div>
</div>

<%= Flash.exibir(session) %>
<%= Flash.alerta("erro", erro) %>

<div class="grid grid-lateral">
    <div class="tabela-wrap">
        <table class="tabela">
            <thead>
                <tr><th>Serviço</th><th>Duração</th><th class="num">Preço</th><th>Situação</th><th>Ações</th></tr>
            </thead>
            <tbody>
            <% for (Servico s : servicos) { %>
                <tr class="<%= s.isAtivo() ? "" : "inativo" %>">
                    <td><strong><%= Html.esc(s.getNome()) %></strong><br><span class="suave pequeno"><%= Html.esc(s.getDescricao()) %></span></td>
                    <td><%= Formato.duracao(s.getDuracaoMin()) %></td>
                    <td class="num"><%= Formato.moeda(s.getPreco()) %></td>
                    <td><span class="badge <%= s.isAtivo() ? "badge-ativo" : "badge-inativo" %>"><%= s.isAtivo() ? "Ativo" : "Inativo" %></span></td>
                    <td>
                        <div class="acoes">
                            <a class="btn btn-secundario btn-pequeno" href="servicos.jsp?editar=<%= s.getId() %>">Editar</a>
                            <form method="post" action="servicos.jsp">
                                <%= Csrf.campo(session) %>
                                <input type="hidden" name="id" value="<%= s.getId() %>">
                                <% if (s.isAtivo()) { %>
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

    <form class="card" method="post" action="servicos.jsp" id="form-servico">
        <h3><%= emEdicao != null ? "Editar serviço" : "Novo serviço" %></h3>
        <%= Csrf.campo(session) %>
        <input type="hidden" name="acao" value="salvar">
        <input type="hidden" name="id" value="<%= emEdicao != null ? emEdicao.getId() : 0 %>">
        <div class="campo">
            <label for="nome">Nome</label>
            <input type="text" id="nome" name="nome" required maxlength="80" value="<%= Html.esc(fNome) %>" placeholder="Ex.: Corte degradê">
        </div>
        <div class="campo">
            <label for="descricao">Descrição <span class="suave">(opcional)</span></label>
            <input type="text" id="descricao" name="descricao" maxlength="200" value="<%= Html.esc(fDescricao) %>">
        </div>
        <div class="linha-campos" style="grid-template-columns:1fr 1fr">
            <div class="campo">
                <label for="duracao">Duração (min)</label>
                <input type="number" id="duracao" name="duracao" required min="5" max="480" step="5" value="<%= Html.esc(fDuracao) %>">
            </div>
            <div class="campo">
                <label for="preco">Preço (R$)</label>
                <input type="text" id="preco" name="preco" required inputmode="decimal" placeholder="45,00" value="<%= Html.esc(fPreco) %>">
            </div>
        </div>
        <div class="acoes">
            <button class="btn btn-primario" type="submit"><%= emEdicao != null ? "Salvar alterações" : "Adicionar serviço" %></button>
            <% if (emEdicao != null) { %><a class="btn btn-secundario" href="servicos.jsp">Cancelar</a><% } %>
        </div>
    </form>
</div>

<%@include file="/WEB-INF/jspf/admin-fim.jspf" %>
</body>
</html>
