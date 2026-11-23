<%--
    Onboarding do SaaS: a barbearia cria a propria conta e ja entra no painel.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="br.com.barbearia.model.Usuario"%>
<%@page import="br.com.barbearia.service.ContaService"%>
<%@page import="br.com.barbearia.service.RegraNegocioException"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Param"%>
<%@page import="br.com.barbearia.web.AuthFilter"%>
<%
    String tituloPagina = "Cadastrar barbearia";
    String erro = null;

    if ("POST".equals(request.getMethod())) {
        try {
            Usuario novo = new ContaService().cadastrar(
                    request.getParameter("nomeBarbearia"),
                    request.getParameter("telefone"),
                    request.getParameter("endereco"),
                    request.getParameter("nome"),
                    request.getParameter("email"),
                    request.getParameter("senha"),
                    request.getParameter("confirmacao"));

            // nova sessao apos autenticar (evita fixacao de sessao)
            session.invalidate();
            HttpSession nova = request.getSession(true);
            nova.setAttribute(AuthFilter.USUARIO, novo);
            Flash.sucesso(nova, "Barbearia cadastrada! Confira seus serviços e horários e depois divulgue seu link.");
            response.sendRedirect(request.getContextPath() + "/admin/painel.jsp");
            return;
        } catch (RegraNegocioException e) {
            erro = e.getMessage();
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

<main class="container">
    <div class="auth largo">
        <h1>Cadastre sua barbearia</h1>
        <p class="suave">Comece no plano Básico com 14 dias grátis. Você pode mudar de plano quando quiser.</p>

        <%= Flash.alerta("erro", erro) %>

        <form class="card" method="post" action="cadastro.jsp">
            <h3>Dados da barbearia</h3>
            <div class="linha-campos">
                <div class="campo">
                    <label for="nomeBarbearia">Nome da barbearia</label>
                    <input type="text" id="nomeBarbearia" name="nomeBarbearia" required maxlength="100"
                           value="<%= Html.esc(Param.texto(request.getParameter("nomeBarbearia"))) %>">
                </div>
                <div class="campo">
                    <label for="telefone">Telefone / WhatsApp</label>
                    <input type="tel" id="telefone" name="telefone" required placeholder="(11) 98765-4321"
                           value="<%= Html.esc(Param.texto(request.getParameter("telefone"))) %>">
                </div>
            </div>
            <div class="campo">
                <label for="endereco">Endereço</label>
                <input type="text" id="endereco" name="endereco" maxlength="200" placeholder="Rua, número - bairro"
                       value="<%= Html.esc(Param.texto(request.getParameter("endereco"))) %>">
            </div>

            <h3 class="mt">Seus dados de acesso</h3>
            <div class="linha-campos">
                <div class="campo">
                    <label for="nome">Seu nome</label>
                    <input type="text" id="nome" name="nome" required maxlength="100" autocomplete="name"
                           value="<%= Html.esc(Param.texto(request.getParameter("nome"))) %>">
                </div>
                <div class="campo">
                    <label for="email">E-mail</label>
                    <input type="email" id="email" name="email" required maxlength="150" autocomplete="email"
                           value="<%= Html.esc(Param.texto(request.getParameter("email"))) %>">
                </div>
            </div>
            <div class="linha-campos">
                <div class="campo">
                    <label for="senha">Senha</label>
                    <input type="password" id="senha" name="senha" required minlength="8" autocomplete="new-password">
                    <span class="dica">Mínimo de 8 caracteres.</span>
                </div>
                <div class="campo">
                    <label for="confirmacao">Confirme a senha</label>
                    <input type="password" id="confirmacao" name="confirmacao" required minlength="8" autocomplete="new-password">
                </div>
            </div>

            <button class="btn btn-primario btn-bloco btn-grande" type="submit">Criar conta e entrar no painel</button>
            <p class="suave pequeno centro mt sem-margem">Já tem conta? <a href="<%= ctx %>/login.jsp">Entrar</a></p>
        </form>
    </div>
</main>

<%@include file="/WEB-INF/jspf/rodape.jspf" %>
</body>
</html>
