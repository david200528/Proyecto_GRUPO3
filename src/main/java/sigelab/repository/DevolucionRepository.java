package sigelab.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DevolucionRepository {

    public static class DetalleInfo {
        public int idDetalle, idPrestamo, idEquipo;
        public String estadoPrestamo;
    }

    public DetalleInfo buscarDetalle(Connection con, int idDetalle) throws SQLException {
        String sql = "SELECT d.id_detalle, d.id_prestamo, d.id_equipo, p.estado " +
                "FROM DetallePrestamo d JOIN Prestamo p ON p.id_prestamo = d.id_prestamo " +
                "WHERE d.id_detalle = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                DetalleInfo i = new DetalleInfo();
                i.idDetalle = rs.getInt(1);
                i.idPrestamo = rs.getInt(2);
                i.idEquipo = rs.getInt(3);
                i.estadoPrestamo = rs.getString(4);
                return i;
            }
        }
    }

    public boolean existeDevolucion(Connection con, int idDetalle) throws SQLException {

        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM Devolucion WHERE id_detalle = ?")) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public void insertar(Connection con, int idDetalle, String condicion, String obs, int idUsuario) throws SQLException {
        String sql = "INSERT INTO Devolucion (id_detalle, fecha_devolucion, condicion, observaciones, id_usuario_registra) " +
                "VALUES (?, GETDATE(), ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDetalle);
            ps.setString(2, condicion);
            ps.setString(3, obs);
            ps.setInt(4, idUsuario);
            ps.executeUpdate();
        }
    }

    public void marcarDetalleDevuelto(Connection con, int idDetalle, String valor) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE DetallePrestamo SET estado_devolucion = ? WHERE id_detalle = ?")) {
            ps.setString(1, valor);
            ps.setInt(2, idDetalle);
            ps.executeUpdate();
        }
    }

    public int contarPendientes(Connection con, int idPrestamo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM DetallePrestamo d WHERE d.id_prestamo = ? " +
                "AND NOT EXISTS (SELECT 1 FROM Devolucion v WHERE v.id_detalle = d.id_detalle)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);

            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public void finalizarPrestamo(Connection con, int idPrestamo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Prestamo SET estado = ? WHERE id_prestamo = ?")) {
            ps.setString(1, ValoresBd.PRESTAMO_FINALIZADO);
            ps.setInt(2, idPrestamo);
            ps.executeUpdate();
        }
    }

    public List<Object[]> listarPendientes(Connection con, int idPrestamo) throws SQLException {
        String sql = "SELECT d.id_detalle, e.codigo_inventario, e.nombre, e.estado " +
                "FROM DetallePrestamo d JOIN Equipo e ON e.id_equipo = d.id_equipo " +
                "WHERE d.id_prestamo = ? " +
                "AND NOT EXISTS (SELECT 1 FROM Devolucion v WHERE v.id_detalle = d.id_detalle)";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    filas.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            }
        }
        return filas;
    }
}