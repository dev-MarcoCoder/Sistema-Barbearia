<%--
    Pagina de erro configurada no web.xml (excecoes e 404).
    O detalhe tecnico vai para o log do Tomcat; o usuario ve uma mensagem amigavel.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<%@page import="br.com.barbearia.web.AppListener"%>
<%
    String tituloPagina = "Ops";
    int status = pageContext.getErrorData() == null ? 500 : pageContext.getErrorData().getStatusCode();
    String erroConfig = (String) application.getAttribute(AppListener.ERRO_CONFIG);
    boolean naoEncontrado = status == 404;

    if (exception != null) {
        application.log("Erro ao processar " + request.getAttribute("jakarta.servlet.error.request_uri"), exception);
    }
    Throwable causa = exception;
    while (causa != null && !(causa instanceof java.sql.SQLException)) {
        causa = causa.getCause();
    }
    boolean erroBanco = causa != null || erroConfig != null;
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <%@include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>
<main class="container">
    <div class="confirmacao card centro">
        <% if (naoEncontrado) { %>
            <h1>Página não encontrada</h1>
            <p class="suave">O endereço que você acessou não existe ou a barbearia não está mais cadastrada.</p>
        <% } else if (status == 403) { %>
            <h1>Formulário expirado</h1>
            <p class="suave">Por segurança, recarregue a página e tente novamente.</p>
        <% } else if (erroBanco) { %>
            <h1>Não foi possível acessar o banco de dados</h1>
            <p class="suave"><%= Html.esc(erroConfig != null ? erroConfig
                    : "Verifique se o MySQL está ligado, se o banco \"barbearia\" foi criado (database/barbearia.sql) e se o arquivo WEB-INF/db.properties está correto.") %></p>
        <% } else { %>
            <h1>Algo deu errado</h1>
            <p class="suave">Tivemos um problema ao processar sua solicitação. Tente novamente em instantes.</p>
        <% } %>
        <a class="btn btn-primario mt" href="<%= ctx %>/index.jsp">Voltar ao início</a>
    </div>
</main>
</body>
</html>
