package sigelab.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final Properties PROPS = cargarPropiedades();

    private static final String URL = configuracion("sigelab.db.url", "DB_URL", "db.url",
            "jdbc:sqlserver://localhost:1433;databaseName=SIGELAB;encrypt=true;trustServerCertificate=true;");
    private static final String USER = configuracion("sigelab.db.user", "DB_USER", "db.user", "sa");
    private static final String PASSWORD = configuracion("sigelab.db.password", "DB_PASSWORD", "db.password", null);

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null) {
            throw new SQLException("Falta la contraseña de la base de datos. Revisa config.properties.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getResourceAsStream("/config.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            // si no se puede leer el archivo, se usan los otros orígenes
        }
        return props;
    }

    private static String configuracion(String propiedad, String variable, String clave, String porDefecto) {
        String valor = System.getProperty(propiedad);
        if (valor == null || valor.isBlank()) valor = System.getenv(variable);
        if (valor == null || valor.isBlank()) valor = PROPS.getProperty(clave);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}