package br.com.barbearia.ia;

import java.util.Properties;

/**
 * Chave da API da Anthropic usada pelo assistente.
 * Vem de /WEB-INF/ia.properties (fora do Git) ou da variavel de ambiente ANTHROPIC_API_KEY.
 * A chave nunca aparece no codigo nem no repositorio.
 */
public final class ConfigIA {

    private static volatile String chave;

    private ConfigIA() {
    }

    public static void configurar(Properties propriedades) {
        String valor = propriedades.getProperty("anthropic.api_key", "").trim();
        chave = valor.isEmpty() ? null : valor;
    }

    public static String chave() {
        if (chave != null) {
            return chave;
        }
        String ambiente = System.getenv("ANTHROPIC_API_KEY");
        return ambiente == null || ambiente.isBlank() ? null : ambiente.trim();
    }

    public static boolean disponivel() {
        return chave() != null;
    }
}
