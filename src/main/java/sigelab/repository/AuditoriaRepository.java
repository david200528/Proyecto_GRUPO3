package sigelab.repository;

import sigelab.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditoriaRepository {

    public void registrarAccion(Integer idUsuario, String usuario, String accion, String detalle) {
        String sql = "INSERT INTO SIGELAB.dbo.bitacora_auditoria (id_usuario, usuario, accion, detalle) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (idUsuario != null) {
                stmt.setInt(1, idUsuario);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }

            stmt.setString(2, usuario);
            stmt.setString(3, accion);
            stmt.setString(4, detalle);

            stmt.executeUpdate();
            System.out.println("[Auditoría] Evento registrado: " + accion + " por " + usuario);

        } catch (SQLException e) {
            System.out.println("Error al registrar auditoría: " + e.getMessage());
        }
    }
}