package br.com.barbearia.ia;

import br.com.barbearia.dao.ConfigIADAO;
import br.com.barbearia.model.Barbearia;
import br.com.barbearia.util.Formato;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.beta.agents.AgentCreateParams;
import com.anthropic.models.beta.agents.AgentUpdateParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsAgent;
import com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolParams;
import com.anthropic.models.beta.agents.BetaManagedAgentsModel;
import com.anthropic.models.beta.environments.BetaCloudConfigParams;
import com.anthropic.models.beta.environments.BetaLimitedNetworkParams;
import com.anthropic.models.beta.environments.EnvironmentCreateParams;
import com.anthropic.models.beta.sessions.SessionCreateParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsAgentCustomToolUseEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionStatusIdleEvent;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsStreamSessionEvents;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEventParams;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEventParams;
import com.anthropic.models.beta.sessions.events.EventSendParams;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assistente de agendamento com IA, usando o Managed Agents da Anthropic.
 *
 * Como funciona:
 * 1. O AGENTE (modelo, instrucoes e ferramentas) e criado uma unica vez na Anthropic; o ID fica no banco.
 * 2. Cada conversa do site abre uma SESSAO desse agente.
 * 3. A cada mensagem, o servidor envia o texto para a sessao e acompanha os eventos:
 *    quando a IA pede uma ferramenta (agent.custom_tool_use), o servidor executa no nosso banco
 *    e devolve o resultado (user.custom_tool_result); quando a IA termina, a resposta vai para o site.
 */
public final class AssistenteIA {

    private static final Logger LOG = Logger.getLogger(AssistenteIA.class.getName());

    static final String MODELO = "claude-opus-5-5";
    /** Tempo maximo de espera por uma resposta antes de desistir. */
    private static final long LIMITE_MS = 120_000;

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
            "Estilo: mensagens curtas, como no WhatsApp, com tom simpático e direto. Escreva em texto simples, sem",
            "tabelas, listas longas ou títulos; use **negrito** só para destacar horário e código. Se uma ferramenta",
            "devolver erro, explique o problema em palavras simples e ofereça outra opção.");

    private static volatile AssistenteIA instancia;

    private final AnthropicClient client;
    private volatile String agenteId;
    private volatile String ambienteId;

    private AssistenteIA(String chave) {
        this.client = AnthropicOkHttpClient.builder().apiKey(chave).build();
    }

    /** Retorna o assistente, ou null se a chave da API nao foi configurada. */
    public static AssistenteIA obter() {
        if (!ConfigIA.disponivel()) {
            return null;
        }
        if (instancia == null) {
            synchronized (AssistenteIA.class) {
                if (instancia == null) {
                    instancia = new AssistenteIA(ConfigIA.chave());
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
        /** true quando a sessao nao pode mais ser usada e uma nova deve ser aberta. */
        public final boolean sessaoEncerrada;

        Resposta(String texto, String codigoCriado, boolean sessaoEncerrada) {
            this.texto = texto;
            this.codigoCriado = codigoCriado;
            this.sessaoEncerrada = sessaoEncerrada;
        }
    }

    // ------------------------------------------------------------------ configuracao (uma vez)

    /**
     * Garante que o agente e o ambiente existem na Anthropic.
     * Cria na primeira vez e guarda os IDs no banco; se as instrucoes ou ferramentas mudarem,
     * atualiza o agente (gera uma nova versao) em vez de criar outro.
     */
    private synchronized void prepararAgente() throws SQLException {
        if (agenteId != null && ambienteId != null) {
            return;
        }
        ConfigIADAO config = new ConfigIADAO();
        String assinatura = sha256(MODELO + "\n" + INSTRUCOES + "\n" + FerramentasIA.assinatura());

        String id = config.ler("agente_id");
        if (id == null) {
            AgentCreateParams.Builder novo = AgentCreateParams.builder()
                    .name("BarberAgenda - assistente de agendamento")
                    .model(BetaManagedAgentsModel.of(MODELO))
                    .system(INSTRUCOES);
            for (BetaManagedAgentsCustomToolParams f : FerramentasIA.paraAgente()) {
                novo.addTool(f);
            }
            id = client.beta().agents().create(novo.build()).id();
            config.salvar("agente_id", id);
            config.salvar("agente_assinatura", assinatura);
            LOG.info("BarberAgenda: agente criado na Anthropic: " + id);
        } else if (!assinatura.equals(config.ler("agente_assinatura"))) {
            BetaManagedAgentsAgent atual = client.beta().agents().retrieve(id);
            AgentUpdateParams.Builder mudanca = AgentUpdateParams.builder()
                    .version(atual.version())
                    .model(BetaManagedAgentsModel.of(MODELO))
                    .system(INSTRUCOES);
            for (BetaManagedAgentsCustomToolParams f : FerramentasIA.paraAgente()) {
                mudanca.addTool(f);
            }
            client.beta().agents().update(id, mudanca.build());
            config.salvar("agente_assinatura", assinatura);
            LOG.info("BarberAgenda: agente atualizado na Anthropic: " + id);
        }

        String ambiente = config.ler("ambiente_id");
        if (ambiente == null) {
            ambiente = client.beta().environments().create(EnvironmentCreateParams.builder()
                    .name("barberagenda-chat")
                    .config(BetaCloudConfigParams.builder()
                            .networking(BetaLimitedNetworkParams.builder()
                                    .allowPackageManagers(false)
                                    .allowMcpServers(false)
                                    .build())
                            .build())
                    .build()).id();
            config.salvar("ambiente_id", ambiente);
            LOG.info("BarberAgenda: ambiente criado na Anthropic: " + ambiente);
        }

        this.agenteId = id;
        this.ambienteId = ambiente;
    }

    // ------------------------------------------------------------------ conversa

    /** Abre uma nova sessao (uma conversa) para um cliente da barbearia. */
    public String novaSessao(Barbearia barbearia) throws SQLException {
        prepararAgente();
        return client.beta().sessions().create(SessionCreateParams.builder()
                .agent(agenteId)
                .environmentId(ambienteId)
                .title("Chat - " + barbearia.getNome())
                .build()).id();
    }

    /**
     * Envia a mensagem do cliente e acompanha a sessao ate a IA terminar de responder,
     * executando as ferramentas que ela pedir.
     */
    public Resposta conversar(String sessaoId, Barbearia barbearia, String mensagem) {
        LocalDateTime agora = LocalDateTime.now();
        String contexto = "[site: agora é " + Formato.dataExtenso(agora.toLocalDate()) + " de " + agora.getYear()
                + ", " + Formato.hora(agora.toLocalTime()) + "]\n";

        StringBuilder texto = new StringBuilder();
        String codigoCriado = null;
        boolean encerrada = false;
        boolean comecou = false;
        long limite = System.currentTimeMillis() + LIMITE_MS;

        // Abre o stream ANTES de enviar a mensagem, para nao perder nenhum evento
        try (var stream = client.beta().sessions().events().streamStreaming(sessaoId)) {
            client.beta().sessions().events().send(sessaoId, EventSendParams.builder()
                    .addEvent(BetaManagedAgentsUserMessageEventParams.builder()
                            .addTextContent(contexto + mensagem)
                            .build())
                    .build());

            for (BetaManagedAgentsStreamSessionEvents evento : stream) {
                if (evento.isUserMessage() || evento.isSessionStatusRunning()) {
                    comecou = true;
                } else if (evento.isAgentMessage()) {
                    comecou = true;
                    evento.asAgentMessage().content().forEach(bloco -> bloco.text().ifPresent(t -> {
                        if (texto.length() > 0) {
                            texto.append("\n\n");
                        }
                        texto.append(t.text().trim());
                    }));
                } else if (evento.isAgentCustomToolUse()) {
                    comecou = true;
                    BetaManagedAgentsAgentCustomToolUseEvent chamada = evento.asAgentCustomToolUse();
                    FerramentasIA.Resultado r = FerramentasIA.executar(
                            chamada.name(), chamada.input()._additionalProperties(), barbearia);
                    if (r.codigoCriado != null) {
                        codigoCriado = r.codigoCriado;
                    }
                    client.beta().sessions().events().send(sessaoId, EventSendParams.builder()
                            .addEvent(BetaManagedAgentsUserCustomToolResultEventParams.builder()
                                    .customToolUseId(chamada.id())
                                    .addTextContent(r.texto)
                                    .isError(r.erro)
                                    .build())
                            .build());
                } else if (evento.isSessionStatusIdle() && comecou) {
                    BetaManagedAgentsSessionStatusIdleEvent.StopReason motivo = evento.asSessionStatusIdle().stopReason();
                    if (motivo.isRequiresAction()) {
                        continue; // esperando o resultado de uma ferramenta que acabamos de enviar
                    }
                    if (motivo.isRefusal()) {
                        texto.setLength(0);
                        texto.append("Desculpe, não posso ajudar com isso. Posso marcar, consultar ou cancelar um horário?");
                    }
                    break;
                } else if (evento.isSessionStatusTerminated()) {
                    encerrada = true;
                    break;
                } else if (evento.isSessionError()) {
                    LOG.warning("BarberAgenda: erro na sessao " + sessaoId + ": " + evento.asSessionError().error().message());
                }
                if (System.currentTimeMillis() > limite) {
                    LOG.warning("BarberAgenda: tempo esgotado esperando a sessao " + sessaoId);
                    encerrada = true;
                    break;
                }
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "BarberAgenda: falha ao conversar com a sessao " + sessaoId, e);
            return new Resposta("Tive um problema para responder agora. Pode repetir a mensagem?", codigoCriado, true);
        }

        if (texto.length() == 0) {
            texto.append(codigoCriado != null
                    ? "Pronto! Seu horário foi marcado. Código: **" + codigoCriado + "**."
                    : "Desculpe, não consegui responder agora. Pode repetir a mensagem?");
        }
        return new Resposta(texto.toString(), codigoCriado, encerrada);
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(texto.hashCode());
        }
    }
}
