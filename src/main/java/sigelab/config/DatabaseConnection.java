package sigelab.config;

import sigelab.repository.AuditoriaRepository;
import sigelab.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // Ajusta la contraseña según tu configuración de SQL Server
    private static final String URL_MASTER = "jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true;";
    private static final String URL_SIGELAB = "jdbc:sqlserver://localhost:1433;databaseName=SIGELAB;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "admin1234";

    public static Connection getConnection() throws SQLException {
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

        // Insertar/actualizar el usuario admin asegurando el hash correcto con BCrypt
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String passHash = BCrypt.hashpw("1234", BCrypt.gensalt());
            stmt.execute("DELETE FROM SIGELAB.dbo.Usuario WHERE usuario = 'admin'");
            stmt.execute("INSERT INTO SIGELAB.dbo.Usuario (usuario, password_hash, rol, activo) VALUES ('admin', '" + passHash + "', 'ADMIN', 1)");

        } catch (Exception e) {
            System.out.println("Error preparando usuario admin: " + e.getMessage());
        }

        // Prueba de autenticación y auditoría
        UsuarioRepository repo = new UsuarioRepository();
        var optUsuario = repo.autenticar("admin", "1234");

        if (optUsuario.isPresent()) {
            var user = optUsuario.get();
            System.out.println("¡Autenticación exitosa! Usuario: " + user.getUsuario());
            System.out.println("Rol: " + user.getRol());

            // Registro en bitácora de auditoría
            AuditoriaRepository auditoriaRepo = new AuditoriaRepository();
            auditoriaRepo.registrarAccion(user.getIdUsuario(), user.getUsuario(), "INICIO_SESION", "El usuario inició sesión exitosamente.");
        } else {
            System.out.println("Prueba de Login: No se encontró el usuario o la contraseña no coincide.");
        }
    }
}