<%--
    Login do dono da barbearia.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="br.com.barbearia.model.Usuario"%>
<%@page import="br.com.barbearia.service.ContaService"%>
<%@page import="br.com.barbearia.util.Flash"%>
<%@page import="br.com.barbearia.util.Param"%>
<%@page import="br.com.barbearia.web.AuthFilter"%>
<%
    String tituloPagina = "Entrar";
    String erro = null;

    if ("POST".equals(request.getMethod())) {
        Usuario u = new ContaService().autenticar(request.getParameter("email"), request.getParameter("senha"));
        if (u != null) {
            session.invalidate();
            HttpSession nova = request.getSession(true);
            nova.setAttribute(AuthFilter.USUARIO, u);
            response.sendRedirect(request.getContextPath() + "/admin/painel.jsp");
            return;
        }
        erro = "E-mail ou senha incorretos.";
    } else if (session.getAttribute(AuthFilter.USUARIO) != null) {
        response.sendRedirect(request.getContextPath() + "/admin/painel.jsp");
        return;
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
    <div class="auth">
        <h1>Entrar no painel</h1>
        <p class="suave">Acesso para donos e administradores de barbearias.</p>

        <%= Flash.exibir(session) %>
        <%= Flash.alerta("erro", erro) %>

        <form class="card" method="post" action="login.jsp">
            <div class="campo">
                <label for="email">E-mail</label>
                <input type="email" id="email" name="email" required autocomplete="username" autofocus
                       value="<%= Html.esc(Param.texto(request.getParameter("email"))) %>">
            </div>
            <div class="campo">
                <label for="senha">Senha</label>
                <input type="password" id="senha" name="senha" required autocomplete="current-password">
            </div>
            <button class="btn btn-primario btn-bloco" type="submit">Entrar</button>
            <p class="suave pequeno centro mt sem-margem">Ainda não usa o BarberAgenda? <a href="<%= ctx %>/cadastro.jsp">Cadastre sua barbearia</a></p>
        </form>
    </div>
</main>

<%@include file="/WEB-INF/jspf/rodape.jspf" %>
</body>
</html>
