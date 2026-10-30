package sigelab.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MantenimientoRepository {

    public int insertar(Connection con, int idEquipo, String tipo, String descripcion,
                        String responsable, String obs) throws SQLException {
        String sql = "INSERT INTO Mantenimiento (id_equipo, fecha_ingreso, tipo_mantenimiento, descripcion, " +
                "responsable, estado, observaciones) VALUES (?, GETDATE(), ?, ?, ?, 'EN_PROCESO', ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idEquipo);
            ps.setString(2, tipo);
            ps.setString(3, descripcion);
            ps.setString(4, responsable);
            ps.setString(5, obs);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public boolean tieneAbierto(Connection con, int idEquipo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM Mantenimiento WHERE id_equipo = ? AND estado = 'EN_PROCESO'")) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public Object[] buscar(Connection con, int idMantenimiento) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_equipo, estado FROM Mantenimiento WHERE id_mantenimiento = ?")) {
            ps.setInt(1, idMantenimiento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Object[]{rs.getInt(1), rs.getString(2)} : null;
            }
        }
    }

    public void finalizar(Connection con, int idMantenimiento, String resultado, String obs) throws SQLException {
        String sql = "UPDATE Mantenimiento SET fecha_salida = GETDATE(), resultado = ?, " +
                "estado = 'FINALIZADO', observaciones = ? WHERE id_mantenimiento = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, resultado);
            ps.setString(2, obs);
            ps.setInt(3, idMantenimiento);
            ps.executeUpdate();
        }
    }

    public List<Object[]> listar(Connection con, boolean soloAbiertos) throws SQLException {
        String sql = "SELECT m.id_mantenimiento, e.codigo_inventario, m.tipo_mantenimiento, m.fecha_ingreso, " +
                "m.responsable, m.estado, m.resultado FROM Mantenimiento m " +
                "JOIN Equipo e ON e.id_equipo = m.id_equipo " +
                (soloAbiertos ? "WHERE m.estado = 'EN_PROCESO' " : "") +
                "ORDER BY m.fecha_ingreso DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                filas.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4), rs.getString(5), rs.getString(6), rs.getString(7)});
        }
        return filas;
    }
}