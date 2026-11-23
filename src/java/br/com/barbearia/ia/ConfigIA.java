package br.com.barbearia.ia;

import java.util.Properties;

/**
 * Configuracao do assistente de IA, que usa o OpenRouter (https://openrouter.ai).
 *
 * A chave vem de /WEB-INF/ia.properties (fora do Git) ou da variavel de ambiente OPENROUTER_API_KEY.
 * O modelo pode ser trocado por openrouter.modelo / OPENROUTER_MODEL (padrao: Claude Haiku 5.5).
 * A chave nunca aparece no codigo nem no repositorio.
 */
public final class ConfigIA {

    /** Modelo padrao: Claude Haiku 5.5, o mais barato da Anthropic, pelo OpenRouter. */
    public static final String MODELO_PADRAO = "anthropic/claude-haiku-5.5";

    private static volatile String chave;
    private static volatile String modelo;

    private ConfigIA() {
    }

    public static void configurar(Properties propriedades) {
        chave = vazioParaNulo(propriedades.getProperty("openrouter.api_key"));
        modelo = vazioParaNulo(propriedades.getProperty("openrouter.modelo"));
    }

    public static String chave() {
        return chave != null ? chave : vazioParaNulo(System.getenv("OPENROUTER_API_KEY"));
    }

    public static String modelo() {
        if (modelo != null) {
            return modelo;
        }
        String ambiente = vazioParaNulo(System.getenv("OPENROUTER_MODEL"));
        return ambiente != null ? ambiente : MODELO_PADRAO;
    }

    public static boolean disponivel() {
        return chave() != null;
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
