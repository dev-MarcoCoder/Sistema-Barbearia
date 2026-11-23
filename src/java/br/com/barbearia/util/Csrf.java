package br.com.barbearia.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Protecao contra CSRF: todo formulario do painel envia um token guardado na sessao.
 * Um site malicioso nao consegue descobrir esse token, entao nao consegue
 * disparar acoes (ex.: cancelar agendamentos) em nome do dono logado.
 */
public final class Csrf {

    private static final String CHAVE = "csrfToken";

    private Csrf() {
    }

    public static String token(HttpSession sessao) {
        String token = (String) sessao.getAttribute(CHAVE);
        if (token == null) {
            token = Seguranca.gerarToken();
            sessao.setAttribute(CHAVE, token);
        }
        return token;
    }

    /** Campo hidden pronto para colocar dentro do form. */
    public static String campo(HttpSession sessao) {
        return "<input type=\"hidden\" name=\"csrf\" value=\"" + Html.esc(token(sessao)) + "\">";
    }

    public static boolean valido(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao == null) {
            return false;
        }
        String esperado = (String) sessao.getAttribute(CHAVE);
        String recebido = request.getParameter("csrf");
        if (esperado == null || recebido == null) {
            return false;
        }
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }
}
