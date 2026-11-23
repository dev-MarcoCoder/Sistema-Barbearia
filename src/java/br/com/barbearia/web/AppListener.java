package br.com.barbearia.web;

import br.com.barbearia.dao.ConnectionFactory;
import br.com.barbearia.ia.ConfigIA;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.InputStream;
import java.util.Properties;

/**
 * Roda uma vez quando o Tomcat sobe a aplicacao: le /WEB-INF/db.properties
 * e configura a conexao com o banco. Se faltar o arquivo, guarda o erro
 * para as paginas mostrarem uma mensagem clara.
 */
@WebListener
public class AppListener implements ServletContextListener {

    public static final String ERRO_CONFIG = "erroConfiguracao";

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        ServletContext contexto = evento.getServletContext();
        try (InputStream entrada = contexto.getResourceAsStream("/WEB-INF/db.properties")) {
            if (entrada == null && configurarPorAmbiente(contexto)) {
                carregarConfigIA(contexto);
                return;
            }
            if (entrada == null) {
                registrarErro(contexto, "Arquivo /WEB-INF/db.properties não encontrado. "
                        + "Copie db.properties.example para db.properties e preencha os dados do MySQL.");
                return;
            }
            Properties config = new Properties();
            config.load(entrada);
            ConnectionFactory.configurar(config);
            contexto.log("BarberAgenda: conexão com o banco configurada.");
        } catch (Exception e) {
            registrarErro(contexto, "Não foi possível configurar o banco: " + e.getMessage());
        }
        carregarConfigIA(contexto);
    }

    /**
     * Na nuvem (Docker/AWS) o banco e configurado pelas variaveis de ambiente
     * DB_URL, DB_USUARIO e DB_SENHA, em vez do arquivo db.properties.
     */
    private boolean configurarPorAmbiente(ServletContext contexto) throws ClassNotFoundException {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) {
            return false;
        }
        Properties config = new Properties();
        config.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        config.setProperty("db.url", url.trim());
        config.setProperty("db.usuario", valorOuVazio(System.getenv("DB_USUARIO")));
        config.setProperty("db.senha", valorOuVazio(System.getenv("DB_SENHA")));
        ConnectionFactory.configurar(config);
        contexto.log("BarberAgenda: conexão com o banco configurada pelas variáveis de ambiente.");
        return true;
    }

    private static String valorOuVazio(String valor) {
        return valor == null ? "" : valor;
    }

    /** Le a chave da API da Anthropic de /WEB-INF/ia.properties (opcional). */
    private void carregarConfigIA(ServletContext contexto) {
        try (InputStream entrada = contexto.getResourceAsStream("/WEB-INF/ia.properties")) {
            if (entrada != null) {
                Properties config = new Properties();
                config.load(entrada);
                ConfigIA.configurar(config);
            }
        } catch (Exception e) {
            contexto.log("BarberAgenda: não foi possível ler /WEB-INF/ia.properties: " + e.getMessage());
        }
        contexto.log("BarberAgenda: assistente de IA " + (ConfigIA.disponivel()
                ? "ativado." : "desativado (sem chave da API; veja WEB-INF/ia.properties.example)."));
    }

    private void registrarErro(ServletContext contexto, String mensagem) {
        contexto.setAttribute(ERRO_CONFIG, mensagem);
        contexto.log("BarberAgenda: " + mensagem);
    }
}
