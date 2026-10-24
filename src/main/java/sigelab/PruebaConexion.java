package sigelab;

import sigelab.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class PruebaConexion {
    public static void main(String[] args) {
        try (Connection con = DatabaseConnection.getConnection()) {
            System.out.println("Conexión exitosa a: " + con.getCatalog());
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}