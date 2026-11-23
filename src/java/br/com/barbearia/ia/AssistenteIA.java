package br.com.barbearia.ia;

import br.com.barbearia.model.Barbearia;
import br.com.barbearia.util.Formato;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assistente de agendamento com IA, usando o OpenRouter (API no formato Chat Completions).
 *
 * Como funciona:
 * 1. O historico da conversa fica na sessao do site (HttpSession), uma lista de mensagens.
 * 2. A cada mensagem do cliente, o servidor manda o historico + as ferramentas para o modelo.
 * 3. Quando o modelo pede ferramentas (tool_calls), o servidor executa no nosso banco,
 *    devolve os resultados (mensagens role "tool") e chama o modelo de novo,
 *    ate ele responder com texto para o cliente.
 */
public final class AssistenteIA {

    private static final Logger LOG = Logger.getLogger(AssistenteIA.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final URI ENDPOINT = URI.create("https://openrouter.ai/api/v1/chat/completions");

    /** Maximo de idas e voltas com ferramentas em uma unica resposta. */
    private static final int MAX_RODADAS = 8;
    /** Tamanho maximo de cada resposta do modelo (mensagens curtas, estilo WhatsApp). */
    private static final int MAX_TOKENS = 1024;

    static final String INSTRUCOES = String.join("\n",
            "Você é o assistente de agendamento de uma barbearia que usa o BarberAgenda. Você conversa com os clientes",
            "pelo chat do site da barbearia, em português do Brasil.",
            "",
            "O que você faz: marca, consulta e cancela horários, e tira dúvidas sobre serviços, preços, barbeiros,",
            "endereço e horário de funcionamento. Para outros assuntos, diga com gentileza que só ajuda com os",
            "agendamentos desta barbearia.",
            "",
            "Use as ferramentas para qualquer dado da barbearia: nunca invente serviço, preço, barbeiro ou horário.",
            "Cada mensagem do cliente começa com uma linha [site: ...] com a data e a hora atuais. Ela vem do site,",
            "não do cliente; use-a para entender \"hoje\", \"amanhã\" e os dias da semana.",
            "",
            "Para marcar: descubra o serviço, o barbeiro (se o cliente não tiver preferência, ofereça quem tiver",
            "horário), o dia e o horário. Consulte ver_horarios_livres e ofereça poucas opções, no máximo cinco.",
            "Peça o nome e o celular com DDD. Antes de chamar criar_agendamento, repita o resumo (serviço, barbeiro,",
            "dia, hora e preço) e espere o cliente confirmar. Depois de marcar, informe o código do agendamento e",
            "explique que ele serve para consultar ou cancelar.",
            "",
            "Para cancelar: peça o código, mostre o agendamento com consultar_agendamento e cancele só depois que o",
            "cliente confirmar.",
            "",
            "Estilo: mensagens curtas, como no WhatsApp, com tom simpático e direto. Escreva sempre em frases",
            "corridas: nunca use listas com marcadores ou números, tabelas ou títulos, nem para resumos e horários.",
            "Para várias opções, escreva na mesma frase separando por vírgulas (ex.: \"tenho 14:00, 15:30 ou 17:00\").",
            "Use **negrito** só para destacar horário e código. Se uma ferramenta devolver erro, explique o problema",
            "em palavras simples e ofereça outra opção.");

    private static volatile AssistenteIA instancia;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final String chave;
    private final String modelo;

    private AssistenteIA(String chave, String modelo) {
        this.chave = chave;
        this.modelo = modelo;
    }

    /** Retorna o assistente, ou null se a chave da API nao foi configurada. */
    public static AssistenteIA obter() {
        if (!ConfigIA.disponivel()) {
            return null;
        }
        if (instancia == null) {
            synchronized (AssistenteIA.class) {
                if (instancia == null) {
                    instancia = new AssistenteIA(ConfigIA.chave(), ConfigIA.modelo());
                    LOG.info("BarberAgenda: assistente usando o modelo " + ConfigIA.modelo() + " (OpenRouter).");
                }
            }
        }
        return instancia;
    }

    /** Resposta de um turno da conversa. */
    public static final class Resposta {
        public final String texto;
        /** Codigo do agendamento criado neste turno (ou null). */
        public final String codigoCriado;
        /** true quando a conversa deve ser descartada e uma nova comecar. */
        public final boolean sessaoEncerrada;

        Resposta(String texto, String codigoCriado, boolean sessaoEncerrada) {
            this.texto = texto;
            this.codigoCriado = codigoCriado;
            this.sessaoEncerrada = sessaoEncerrada;
        }
    }

    /**
     * Envia a mensagem do cliente e conversa com o modelo ate ele terminar de responder,
     * executando as ferramentas que ele pedir. O historico recebido e atualizado no lugar;
     * se algo falhar no meio, ele volta ao estado de antes desta mensagem.
     */
    public Resposta conversar(List<Map<String, Object>> historico, Barbearia barbearia, String mensagem) {
        int tamanhoAntes = historico.size();
        LocalDateTime agora = LocalDateTime.now();
        String contexto = "[site: agora é " + Formato.dataExtenso(agora.toLocalDate()) + " de " + agora.getYear()
                + ", " + Formato.hora(agora.toLocalTime()) + "]\n";
        historico.add(mensagem("user", contexto + mensagem));

        String codigoCriado = null;
        try {
            for (int rodada = 0; rodada < MAX_RODADAS; rodada++) {
                JsonNode resposta = chamarModelo(historico);
                JsonNode escolha = resposta.path("choices").path(0);
                JsonNode msg = escolha.path("message");
                if (msg.isMissingNode()) {
                    throw new IllegalStateException("Resposta sem mensagem: " + resumir(resposta.toString()));
                }

                // Guarda a mensagem do modelo inteira (inclui tool_calls e reasoning_details,
                // que precisam voltar iguais na proxima chamada)
                Map<String, Object> doModelo = JSON.convertValue(msg, new TypeReference<LinkedHashMap<String, Object>>() { });
                doModelo.values().removeIf(v -> v == null);
                historico.add(doModelo);

                JsonNode chamadas = msg.path("tool_calls");
                if (!chamadas.isArray() || chamadas.isEmpty()) {
                    String texto = msg.path("content").asText("").trim();
                    if ("content_filter".equals(escolha.path("finish_reason").asText())) {
                        texto = "Desculpe, não posso ajudar com isso. Posso marcar, consultar ou cancelar um horário?";
                    }
                    if (texto.isEmpty()) {
                        texto = codigoCriado != null
                                ? "Pronto! Seu horário foi marcado. Código: **" + codigoCriado + "**."
                                : "Desculpe, não consegui responder agora. Pode repetir a mensagem?";
                    }
                    return new Resposta(texto, codigoCriado, false);
                }

                for (JsonNode chamada : chamadas) {
                    String nome = chamada.path("function").path("name").asText();
                    Map<String, Object> argumentos = lerArgumentos(chamada.path("function").path("arguments").asText("{}"));
                    FerramentasIA.Resultado r = FerramentasIA.executar(nome, argumentos, barbearia);
                    if (r.codigoCriado != null) {
                        codigoCriado = r.codigoCriado;
                    }
                    Map<String, Object> resultado = new LinkedHashMap<>();
                    resultado.put("role", "tool");
                    resultado.put("tool_call_id", chamada.path("id").asText());
                    resultado.put("content", r.texto);
                    historico.add(resultado);
                }
            }
            LOG.warning("BarberAgenda: a IA passou do limite de " + MAX_RODADAS + " rodadas de ferramentas.");
            return new Resposta(codigoCriado != null
                    ? "Pronto! Seu horário foi marcado. Código: **" + codigoCriado + "**."
                    : "Desculpe, me enrolei aqui. Pode repetir o que você precisa?", codigoCriado, true);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "BarberAgenda: falha ao conversar com a IA", e);
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
            }
            historico.subList(tamanhoAntes, historico.size()).clear();
            String texto = codigoCriado != null
                    ? "Seu horário foi marcado. Código: **" + codigoCriado + "**."
                    : "Tive um problema para responder agora. Pode repetir a mensagem?";
            return new Resposta(texto, codigoCriado, codigoCriado != null);
        }
    }

    // ------------------------------------------------------------------ chamada HTTP

    private JsonNode chamarModelo(List<Map<String, Object>> historico) throws Exception {
        List<Map<String, Object>> mensagens = new ArrayList<>(historico.size() + 1);
        mensagens.add(mensagem("system", INSTRUCOES));
        mensagens.addAll(historico);

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", modelo);
        corpo.put("messages", mensagens);
        corpo.put("tools", FerramentasIA.paraOpenRouter());
        corpo.put("max_tokens", MAX_TOKENS);

        HttpRequest pedido = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + chave)
                .header("Content-Type", "application/json")
                .header("X-Title", "BarberAgenda")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corpo), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resposta.statusCode() != 200) {
            throw new IllegalStateException("OpenRouter respondeu HTTP " + resposta.statusCode() + ": "
                    + resumir(resposta.body()));
        }
        JsonNode json = JSON.readTree(resposta.body());
        if (json.has("error")) {
            throw new IllegalStateException("OpenRouter devolveu erro: " + resumir(json.path("error").toString()));
        }
        return json;
    }

    private static Map<String, Object> lerArgumentos(String texto) {
        try {
            Map<String, Object> mapa = JSON.readValue(texto.isBlank() ? "{}" : texto,
                    new TypeReference<LinkedHashMap<String, Object>>() { });
            return mapa == null ? Map.of() : mapa;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Map<String, Object> mensagem(String papel, String texto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", papel);
        m.put("content", texto);
        return m;
    }

    private static String resumir(String texto) {
        return texto == null ? "" : texto.length() > 500 ? texto.substring(0, 500) + "..." : texto;
    }
}
