package br.com.barbearia.web;

import br.com.barbearia.util.Csrf;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Protege todas as paginas de /admin/*:
 * - sem login, manda para a tela de login;
 * - em requisicoes POST, confere o token CSRF.
 */
@WebFilter(urlPatterns = "/admin/*")
public class AuthFilter implements Filter {

    public static final String USUARIO = "usuario";

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        HttpSession sessao = request.getSession(false);
        if (sessao == null || sessao.getAttribute(USUARIO) == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) && !Csrf.valido(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Formulário expirado. Recarregue a página e tente de novo.");
            return;
        }

        // paginas do painel nao devem ficar em cache (dados mudam o tempo todo)
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, resp);
    }
}
