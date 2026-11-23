package br.com.barbearia.util;

/**
 * Escapa texto antes de escrever no HTML. Todo dado digitado por usuarios
 * (nome do cliente, nome do servico...) passa por aqui para evitar XSS.
 */
public final class Html {

    private Html() {
    }

    public static String esc(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        StringBuilder sb = new StringBuilder(texto.length() + 16);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Codifica um valor para ser usado dentro de uma URL (query string). */
    public static String url(Object valor) {
        if (valor == null) {
            return "";
        }
        return java.net.URLEncoder.encode(valor.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Retorna " checked" quando a condicao for verdadeira (para radios/checkboxes). */
    public static String checked(boolean condicao) {
        return condicao ? " checked" : "";
    }

    public static String selected(boolean condicao) {
        return condicao ? " selected" : "";
    }
}
