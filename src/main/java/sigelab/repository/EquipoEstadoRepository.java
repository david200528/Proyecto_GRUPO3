package sigelab.repository;

import java.sql.*;

public class EquipoEstadoRepository {
    public String obtenerEstado(Connection con, int idEquipo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT estado FROM Equipo WHERE id_equipo = ?")) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public void actualizarEstado(Connection con, int idEquipo, String nuevoEstado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Equipo SET estado = ? WHERE id_equipo = ?")) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, idEquipo);
            ps.executeUpdate();
        }
    }
}