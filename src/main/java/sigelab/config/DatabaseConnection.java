package sigelab.config;

import org.mindrot.jbcrypt.BCrypt;
import sigelab.service.UsuarioService;
import sigelab.repository.AuditoriaRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnection {

    private static final Properties PROPS = cargarPropiedades();

    // Los datos de conexion se leen, en este orden, de: propiedad del sistema, variable de
    // entorno o src/main/resources/config.properties. Ya no hay contrasenas en el codigo.
    private static final String URL_SIGELAB = configuracion("sigelab.db.url", "DB_URL", "db.url",
            "jdbc:sqlserver://localhost:1433;databaseName=SIGELAB;encrypt=true;trustServerCertificate=true;");
    private static final String URL_MASTER = URL_SIGELAB.replace("databaseName=SIGELAB", "databaseName=master");
    private static final String USER = configuracion("sigelab.db.user", "DB_USER", "db.user", "sa");
    private static final String PASSWORD = configuracion("sigelab.db.password", "DB_PASSWORD", "db.password", null);

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null) {
            throw new SQLException("Falta la contraseña de la base de datos. "
                    + "Copia config.example.properties a config.properties y completa db.password.");
        }
        return DriverManager.getConnection(URL_SIGELAB, USER, PASSWORD);
    }

    public static void main(String[] args) {
        File schemaFile = new File("src/schema.sql");

        if (schemaFile.exists()) {
            try (Connection conn = DriverManager.getConnection(URL_MASTER, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {

                String contenido = Files.readString(schemaFile.toPath());
                String[] sentencias = contenido.split(";");

                for (String sql : sentencias) {
                    String limpia = sql.trim();
                    if (!limpia.isEmpty() && !limpia.equalsIgnoreCase("GO")) {
                        try {
                            stmt.execute(limpia);
                        } catch (Exception e) {
                            System.out.println("Error SQL: " + e.getMessage());
                        }
                    }
                }
                System.out.println("Esquema procesado correctamente.");
            } catch (Exception e) {
                System.out.println("Aviso al preparar esquema: " + e.getMessage());
            }
        }

        // Inserción o actualización segura del usuario admin sin romper las referencias de auditoría
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String passHash = BCrypt.hashpw("1234", BCrypt.gensalt());

            // Intentamos actualizar primero por si ya existe el usuario con registros asociados
            int filasActualizadas = stmt.executeUpdate("UPDATE SIGELAB.dbo.Usuario SET password_hash = '" + passHash + "', rol = 'ADMIN', activo = 1 WHERE usuario = 'admin'");

            // Si no existía, lo insertamos por primera vez
            if (filasActualizadas == 0) {
                stmt.execute("INSERT INTO SIGELAB.dbo.Usuario (nombre, apellido, usuario, password_hash, correo, rol, activo) VALUES ('Administrador', 'Sistema', 'admin', '" + passHash + "', 'admin@sigelab.edu', 'ADMIN', 1)");
            }
            System.out.println("Usuario admin preparado exitosamente.");

        } catch (Exception e) {
            System.out.println("Error preparando usuario admin: " + e.getMessage());
        }

        // Prueba de autenticación y auditoría
        UsuarioService repo = new UsuarioService();
        var optUsuario = repo.autenticar("admin", "1234");

        if (optUsuario.isPresent()) {
            var user = optUsuario.get();
            System.out.println("¡Autenticación exitosa! Usuario: " + user.getUsuario());
            System.out.println("Rol: " + user.getRol());

            AuditoriaRepository auditoriaRepo = new AuditoriaRepository();
            auditoriaRepo.registrarAccion(user.getIdUsuario(), user.getUsuario(), "INICIO_SESION", "El usuario inició sesión exitosamente.");
        } else {
            System.out.println("Prueba de Login: No se encontró el usuario o la contraseña no coincide.");
        }
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getResourceAsStream("/config.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            // si no se puede leer el archivo, se usan las otras fuentes
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