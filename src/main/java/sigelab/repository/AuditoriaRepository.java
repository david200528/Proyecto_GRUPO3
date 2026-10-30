package sigelab.repository;

import sigelab.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaRepository {

    // Método adaptado para recibir tanto los 2 como los 4 parámetros si se requiere
    public void registrarAccion(Integer idUsuario, String accion) {
        registrarAccion(idUsuario, null, accion, null);
    }

    public void registrarAccion(Integer idUsuario, String usuario, String accion, String detalle) {
        String sql = "INSERT INTO dbo.bitacora_auditoria (id_usuario, accion, fecha_hora) VALUES (?, ?, GETDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario != null ? idUsuario : 1); // fallback por seguridad si es sistema
            stmt.setString(2, accion);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar auditoría: " + e.getMessage());
        }
    }

    public List<String[]> listarBitacora() {
        List<String[]> lista = new ArrayList<>();
        // Se utiliza 'id_bitacora' siguiendo la convención de nombres del proyecto
        String sql = "SELECT b.id_bitacora, ISNULL(u.usuario, 'Sistema') as usuario, b.accion, b.fecha_hora " +
                "FROM dbo.bitacora_auditoria b " +
                "LEFT JOIN dbo.Usuario u ON b.id_usuario = u.id_usuario " +
                "ORDER BY b.fecha_hora DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new String[]{
                        String.valueOf(rs.getInt("id_bitacora")),
                        rs.getString("usuario"),
                        rs.getString("accion"),
                        rs.getString("fecha_hora")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}