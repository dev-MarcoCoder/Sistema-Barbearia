package br.com.barbearia.web;

import br.com.barbearia.dao.BarbeariaDAO;
import br.com.barbearia.ia.AssistenteIA;
import br.com.barbearia.model.Barbearia;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Endpoint do chat: POST /chat com os parametros b (barbearia) e mensagem.
 * Responde em JSON: { ok, resposta, agendamento }.
 *
 * Cada navegador tem a sua conversa (o historico de mensagens com a IA), guardada na HttpSession
 * e separada por barbearia.
 */
@WebServlet(urlPatterns = "/chat")
public class ChatServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ChatServlet.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Limites simples para evitar abuso (cada mensagem custa tokens na API). */
    private static final int MAX_CARACTERES = 1000;
    private static final int MAX_MENSAGENS_POR_CONVERSA = 40;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        Barbearia barbearia;
        try {
            barbearia = new BarbeariaDAO().buscarPorSlug(request.getParameter("b"));
        } catch (SQLException e) {
            responder(response, false, "Não foi possível acessar o sistema agora.", null);
            return;
        }
        if (barbearia == null) {
            responder(response, false, "Barbearia não encontrada.", null);
            return;
        }

        HttpSession sessao = request.getSession(true);
        String chaveSessao = "chat:" + barbearia.getId();
        String chaveContador = "chatContador:" + barbearia.getId();

        if ("1".equals(request.getParameter("reiniciar"))) {
            sessao.removeAttribute(chaveSessao);
            sessao.removeAttribute(chaveContador);
            responder(response, true, null, null);
            return;
        }

        String mensagem = request.getParameter("mensagem");
        mensagem = mensagem == null ? "" : mensagem.trim();
        if (mensagem.isEmpty()) {
            responder(response, false, "Escreva uma mensagem.", null);
            return;
        }
        if (mensagem.length() > MAX_CARACTERES) {
            mensagem = mensagem.substring(0, MAX_CARACTERES);
        }

        AssistenteIA assistente = AssistenteIA.obter();
        if (assistente == null) {
            responder(response, false, "O assistente ainda não foi ativado nesta barbearia. "
                    + "Você pode agendar normalmente pela página.", null);
            return;
        }

        // Uma mensagem por vez em cada navegador (o historico precisa ficar em ordem)
        synchronized (trava(sessao)) {
            Integer contador = (Integer) sessao.getAttribute(chaveContador);
            int enviadas = contador == null ? 0 : contador;
            if (enviadas >= MAX_MENSAGENS_POR_CONVERSA) {
                responder(response, false, "Esta conversa ficou longa. Clique em ↺ para começar uma nova.", null);
                return;
            }
            sessao.setAttribute(chaveContador, enviadas + 1);

            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> historico = (List<Map<String, Object>>) sessao.getAttribute(chaveSessao);
                if (historico == null) {
                    historico = new ArrayList<>();
                }
                AssistenteIA.Resposta r = assistente.conversar(historico, barbearia, mensagem);
                sessao.setAttribute(chaveSessao, historico); // regrava para o Tomcat saber que mudou
                if (r.sessaoEncerrada) {
                    sessao.removeAttribute(chaveSessao); // a proxima mensagem abre uma conversa nova
                }
                String link = r.codigoCriado == null ? null
                        : request.getContextPath() + "/confirmacao.jsp?b=" + enc(barbearia.getSlug()) + "&c=" + enc(r.codigoCriado);
                responder(response, true, r.texto, link);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "BarberAgenda: falha no chat", e);
                sessao.removeAttribute(chaveSessao);
                responder(response, false, "O assistente está indisponível no momento. "
                        + "Tente de novo em instantes ou agende pela página.", null);
            }
        }
    }

    private static Object trava(HttpSession sessao) {
        Object trava = sessao.getAttribute("chatTrava");
        if (trava == null) {
            synchronized (ChatServlet.class) {
                trava = sessao.getAttribute("chatTrava");
                if (trava == null) {
                    trava = new java.io.Serializable() {
                        private static final long serialVersionUID = 1L;
                    };
                    sessao.setAttribute("chatTrava", trava);
                }
            }
        }
        return trava;
    }

    private static String enc(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static void responder(HttpServletResponse response, boolean ok, String texto, String agendamento)
            throws IOException {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("ok", ok);
        corpo.put("resposta", texto);
        corpo.put("agendamento", agendamento);
        JSON.writeValue(response.getWriter(), corpo);
    }
}
