package br.com.barbearia.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fabrica de conexoes com o MySQL.
 * Os dados de acesso vem de /WEB-INF/db.properties (fora do Git),
 * carregados uma unica vez quando a aplicacao sobe (AppListener).
 */
public final class ConnectionFactory {

    private static volatile Properties config;

    private ConnectionFactory() {
    }

    public static synchronized void configurar(Properties propriedades) throws ClassNotFoundException {
        Class.forName(propriedades.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        config = propriedades;
    }

    public static boolean configurado() {
        return config != null;
    }

    public static Connection getConnection() throws SQLException {
        Properties p = config;
        if (p == null) {
            throw new SQLException("Banco de dados não configurado. Copie /WEB-INF/db.properties.example "
                    + "para /WEB-INF/db.properties e preencha usuário e senha do MySQL.");
        }
        return DriverManager.getConnection(p.getProperty("db.url"), p.getProperty("db.usuario"), p.getProperty("db.senha"));
    }
}
