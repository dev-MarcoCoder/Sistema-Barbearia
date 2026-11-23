<%--
    Dados da barbearia e horario de funcionamento (usado no calculo dos horarios livres).
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jspf/admin-init.jspf" %>
<%@page import="br.com.barbearia.service.GestaoService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Csrf"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%@page import="br.com.barbearia.util.Param"%>
<%
    String tituloPagina = "Configurações";
    String paginaAtiva = "configuracoes";
    String erro = null;

    if ("POST".equals(request.getMethod())) {
        GestaoService gestao = new GestaoService();
        String acao = Param.texto(request.getParameter("acao"));
        try {
            if ("dados".equals(acao)) {
                gestao.salvarDados(barbearia, request.getParameter("nome"), request.getParameter("telefone"),
                        request.getParameter("endereco"));
                Flash.sucesso(session, "Dados da barbearia salvos.");
            } else if ("horarios".equals(acao)) {
                gestao.salvarHorarios(barbearia, request.getParameter("abertura"), request.getParameter("fechamento"),
                        request.getParameter("intervalo"), request.getParameterValues("dias"));
                Flash.sucesso(session, "Horário de funcionamento salvo. A agenda online já usa os novos horários.");
            }
            response.sendRedirect("configuracoes.jsp");
            return;
        } catch (RegraNegocioException e) {
            erro = e.getMessage();
        }
    }
    int[] intervalos = {10, 15, 20, 30, 45, 60};
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
        <h1>Configurações</h1>
        <p class="suave sem-margem">Essas informações aparecem na sua página de agendamento.</p>
    </div>
</div>

<%= Flash.exibir(session) %>
<%= Flash.alerta("erro", erro) %>

<div class="grid grid-2">
    <form class="card" method="post" action="configuracoes.jsp">
        <h3>Dados da barbearia</h3>
        <%= Csrf.campo(session) %>
        <input type="hidden" name="acao" value="dados">
        <div class="campo">
            <label for="nome">Nome</label>
            <input type="text" id="nome" name="nome" required maxlength="100" value="<%= Html.esc(barbearia.getNome()) %>">
        </div>
        <div class="campo">
            <label for="telefone">Telefone / WhatsApp</label>
            <input type="tel" id="telefone" name="telefone" required value="<%= Html.esc(Formato.telefone(barbearia.getTelefone())) %>">
        </div>
        <div class="campo">
            <label for="endereco">Endereço</label>
            <input type="text" id="endereco" name="endereco" maxlength="200" value="<%= Html.esc(barbearia.getEndereco()) %>">
        </div>
        <div class="campo">
            <span class="rotulo">Link de agendamento</span>
            <% String portaLink = (request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort(); %>
            <span class="suave pequeno"><%= Html.esc(request.getScheme() + "://" + request.getServerName() + portaLink + ctx) %>/agendar.jsp?b=<strong><%= Html.esc(barbearia.getSlug()) %></strong></span>
        </div>
        <button class="btn btn-primario" type="submit">Salvar dados</button>
    </form>

    <form class="card" method="post" action="configuracoes.jsp">
        <h3>Horário de funcionamento</h3>
        <%= Csrf.campo(session) %>
        <input type="hidden" name="acao" value="horarios">
        <div class="linha-campos" style="grid-template-columns:1fr 1fr">
            <div class="campo">
                <label for="abertura">Abre às</label>
                <input type="time" id="abertura" name="abertura" required value="<%= Formato.hora(barbearia.getHoraAbertura()) %>">
            </div>
            <div class="campo">
                <label for="fechamento">Fecha às</label>
                <input type="time" id="fechamento" name="fechamento" required value="<%= Formato.hora(barbearia.getHoraFechamento()) %>">
            </div>
        </div>
        <div class="campo">
            <label for="intervalo">Oferecer horários a cada</label>
            <select id="intervalo" name="intervalo">
                <% for (int i : intervalos) { %>
                    <option value="<%= i %>"<%= Html.selected(i == barbearia.getIntervaloMin()) %>><%= i %> minutos</option>
                <% } %>
            </select>
            <span class="dica">Ex.: a cada 30 minutos → 09:00, 09:30, 10:00...</span>
        </div>
        <div class="campo">
            <span class="rotulo">Dias de funcionamento</span>
            <div class="checks">
                <% for (int d = 1; d <= 7; d++) { %>
                <label><input type="checkbox" name="dias" value="<%= d %>"<%= Html.checked(barbearia.getDiasFuncionamento().contains(d)) %>>
                    <span style="text-transform:capitalize"><%= Html.esc(Formato.diaSemana(d).replace("-feira", "")) %></span></label>
                <% } %>
            </div>
        </div>
        <button class="btn btn-primario" type="submit">Salvar horários</button>
    </form>
</div>

<%@include file="/WEB-INF/jspf/admin-fim.jspf" %>
</body>
</html>
