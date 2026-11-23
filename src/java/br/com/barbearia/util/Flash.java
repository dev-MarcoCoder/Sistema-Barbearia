package br.com.barbearia.util;

import jakarta.servlet.http.HttpSession;

/**
 * Mensagem que sobrevive a um redirect (padrao Post/Redirect/Get):
 * a pagina que processa o formulario grava a mensagem e a proxima pagina exibe uma vez.
 */
public final class Flash {

    private static final String CHAVE = "flash";

    private Flash() {
    }

    public static void sucesso(HttpSession sessao, String mensagem) {
        sessao.setAttribute(CHAVE, new String[]{"sucesso", mensagem});
    }

    public static void erro(HttpSession sessao, String mensagem) {
        sessao.setAttribute(CHAVE, new String[]{"erro", mensagem});
    }

    /** Devolve o HTML do aviso (ou vazio) e remove a mensagem da sessao. */
    public static String exibir(HttpSession sessao) {
        if (sessao == null) {
            return "";
        }
        Object valor = sessao.getAttribute(CHAVE);
        if (!(valor instanceof String[])) {
            return "";
        }
        sessao.removeAttribute(CHAVE);
        String[] flash = (String[]) valor;
        return alerta(flash[0], flash[1]);
    }

    public static String alerta(String tipo, String mensagem) {
        if (mensagem == null || mensagem.isEmpty()) {
            return "";
        }
        return "<div class=\"alerta alerta-" + Html.esc(tipo) + "\" role=\"status\">" + Html.esc(mensagem) + "</div>";
    }
}
