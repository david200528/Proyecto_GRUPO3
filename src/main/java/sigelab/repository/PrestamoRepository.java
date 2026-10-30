package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.enums.CondicionDevolucion;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoPrestamo;
import sigelab.model.*;
import sigelab.util.BusinessException;
import sigelab.util.JdbcSupport;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrestamoRepository {
    private static final String SELECT = "SELECT p.id_prestamo, p.solicitante, p.fecha_prestamo, p.fecha_esperada, "
            + "p.id_usuario_registra, u.usuario, p.estado, p.observaciones, "
            + "(SELECT COUNT(*) FROM dbo.DetallePrestamo d WHERE d.id_prestamo = p.id_prestamo "
            + "AND d.estado_devolucion = 'PENDIENTE') AS pendientes "
            + "FROM dbo.Prestamo p JOIN dbo.Usuario u ON u.id_usuario = p.id_usuario_registra ";

    /**
     * Crea el préstamo, sus detalles y pasa cada equipo a PRESTADO en una sola transacción (RN-09).
     * El UPDATE solo funciona si el equipo sigue DISPONIBLE, así dos préstamos simultáneos no pueden
     * llevarse el mismo equipo (RN-07/RN-08).
     */
    public void crear(Prestamo p, List<Integer> idsEquipo) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            p.setFechaPrestamo(LocalDateTime.now());
            String sqlPrestamo = "INSERT INTO dbo.Prestamo (solicitante, fecha_prestamo, fecha_esperada, "
                    + "id_usuario_registra, estado, observaciones) VALUES (?, ?, ?, ?, 'ACTIVO', ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPrestamo, Statement.RETURN_GENERATED_KEYS)) {
                JdbcSupport.asignar(stmt, p.getSolicitante(), p.getFechaPrestamo(), p.getFechaEsperada(),
                        p.getIdUsuarioRegistra(), p.getObservaciones());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) p.setIdPrestamo(keys.getInt(1));
                }
            }
            for (int idEquipo : idsEquipo) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE dbo.Equipo SET estado = 'PRESTADO' WHERE id_equipo = ? AND estado = 'DISPONIBLE'")) {
                    stmt.setInt(1, idEquipo);
                    if (stmt.executeUpdate() != 1) {
                        throw new BusinessException("Un equipo del préstamo ya no está disponible. "
                                + "Actualice la lista e inténtelo de nuevo.");
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO dbo.DetallePrestamo (id_prestamo, id_equipo) VALUES (?, ?)")) {
                    stmt.setInt(1, p.getIdPrestamo());
                    stmt.setInt(2, idEquipo);
                    stmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            JdbcSupport.rollback(conn);
            if (JdbcSupport.esDuplicado(e)) {
                throw new BusinessException("El mismo equipo no puede aparecer dos veces en el préstamo.");
            }
            throw JdbcSupport.error("registrar el préstamo", e);
        } catch (RuntimeException e) {
            JdbcSupport.rollback(conn);
            throw e;
        } finally {
            cerrar(conn);
        }
    }

    /**
     * Registra la devolución de un detalle: devolución, estado del detalle, estado del equipo y, si ya
     * no quedan pendientes, cierre del préstamo (RN-12, RN-13, RN-16). Devuelve true si el préstamo
     * quedó FINALIZADO.
     */
    public boolean devolver(Devolucion d, int idPrestamo, int idEquipo) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE dbo.DetallePrestamo SET estado_devolucion = 'DEVUELTO' "
                            + "WHERE id_detalle = ? AND estado_devolucion = 'PENDIENTE'")) {
                stmt.setInt(1, d.getIdDetalle());
                if (stmt.executeUpdate() != 1) throw new BusinessException("Ese equipo ya fue devuelto.");
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO dbo.Devolucion (id_detalle, fecha_devolucion, condicion, observaciones, "
                            + "id_usuario_registra) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                d.setFechaDevolucion(LocalDateTime.now());
                JdbcSupport.asignar(stmt, d.getIdDetalle(), d.getFechaDevolucion(), d.getCondicion(),
                        d.getObservaciones(), d.getIdUsuarioRegistra());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) d.setIdDevolucion(keys.getInt(1));
                }
            }
            EstadoEquipo nuevo = d.getCondicion() == CondicionDevolucion.BUENO
                    ? EstadoEquipo.DISPONIBLE : EstadoEquipo.DANADO;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE dbo.Equipo SET estado = ? WHERE id_equipo = ? AND estado = 'PRESTADO'")) {
                JdbcSupport.asignar(stmt, nuevo, idEquipo);
                if (stmt.executeUpdate() != 1) {
                    throw new BusinessException("El equipo no figura como prestado; no se puede devolver.");
                }
            }
            boolean finalizado;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM dbo.DetallePrestamo WHERE id_prestamo = ? AND estado_devolucion = 'PENDIENTE'")) {
                stmt.setInt(1, idPrestamo);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    finalizado = rs.getInt(1) == 0;
                }
            }
            if (finalizado) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE dbo.Prestamo SET estado = 'FINALIZADO' WHERE id_prestamo = ?")) {
                    stmt.setInt(1, idPrestamo);
                    stmt.executeUpdate();
                }
            }
            conn.commit();
            return finalizado;
        } catch (SQLException e) {
            JdbcSupport.rollback(conn);
            if (JdbcSupport.esDuplicado(e)) throw new BusinessException("Ese equipo ya fue devuelto.");
            throw JdbcSupport.error("registrar la devolución", e);
        } catch (RuntimeException e) {
            JdbcSupport.rollback(conn);
            throw e;
        } finally {
            cerrar(conn);
        }
    }

    public Optional<Prestamo> buscarPorId(int id) {
        List<Prestamo> lista = consultar(SELECT + "WHERE p.id_prestamo = ?", id);
        if (lista.isEmpty()) return Optional.empty();
        Prestamo p = lista.get(0);
        p.getDetalles().addAll(detalles(id));
        return Optional.of(p);
    }

    public List<Prestamo> listarActivos() {
        return consultar(SELECT + "WHERE p.estado = 'ACTIVO' ORDER BY p.fecha_prestamo DESC");
    }

    public List<Prestamo> listarFinalizados() {
        return consultar(SELECT + "WHERE p.estado = 'FINALIZADO' ORDER BY p.fecha_prestamo DESC");
    }

    /** RN-17: fecha esperada pasada y equipos pendientes. */
    public List<Prestamo> listarVencidos() {
        return consultar(SELECT + "WHERE p.estado = 'ACTIVO' AND p.fecha_esperada < GETDATE() AND EXISTS ("
                + "SELECT 1 FROM dbo.DetallePrestamo d WHERE d.id_prestamo = p.id_prestamo "
                + "AND d.estado_devolucion = 'PENDIENTE') ORDER BY p.fecha_esperada");
    }

    public List<Prestamo> listarEntre(LocalDateTime desde, LocalDateTime hasta) {
        return consultar(SELECT + "WHERE p.fecha_prestamo BETWEEN ? AND ? ORDER BY p.fecha_prestamo DESC", desde, hasta);
    }

    public List<DetallePrestamo> detalles(int idPrestamo) {
        String sql = "SELECT d.id_detalle, d.id_prestamo, d.estado_devolucion, "
                + "e.id_equipo, e.codigo_inventario, e.nombre, e.estado, "
                + "dv.id_devolucion, dv.fecha_devolucion, dv.condicion, dv.observaciones "
                + "FROM dbo.DetallePrestamo d JOIN dbo.Equipo e ON e.id_equipo = d.id_equipo "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE d.id_prestamo = ? ORDER BY e.codigo_inventario";
        List<DetallePrestamo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPrestamo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DetallePrestamo d = new DetallePrestamo();
                    d.setIdDetalle(rs.getInt("id_detalle"));
                    d.setIdPrestamo(rs.getInt("id_prestamo"));
                    d.setDevuelto("DEVUELTO".equals(rs.getString("estado_devolucion")));
                    Equipo e = new Equipo();
                    e.setIdEquipo(rs.getInt("id_equipo"));
                    e.setCodigoInventario(rs.getString("codigo_inventario"));
                    e.setNombre(rs.getString("nombre"));
                    e.setEstado(EstadoEquipo.valueOf(rs.getString("estado")));
                    d.setEquipo(e);
                    if (d.isDevuelto()) {
                        Devolucion dv = new Devolucion();
                        dv.setIdDevolucion(rs.getInt("id_devolucion"));
                        dv.setIdDetalle(d.getIdDetalle());
                        dv.setFechaDevolucion(JdbcSupport.localDateTime(rs, "fecha_devolucion"));
                        dv.setCondicion(CondicionDevolucion.valueOf(rs.getString("condicion")));
                        dv.setObservaciones(rs.getString("observaciones"));
                        d.setDevolucion(dv);
                    }
                    lista.add(d);
                }
            }
        } catch (SQLException ex) {
            throw JdbcSupport.error("consultar el detalle del préstamo", ex);
        }
        return lista;
    }

    private List<Prestamo> consultar(String sql, Object... params) {
        List<Prestamo> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Prestamo p = new Prestamo();
                    p.setIdPrestamo(rs.getInt("id_prestamo"));
                    p.setSolicitante(rs.getString("solicitante"));
                    p.setFechaPrestamo(JdbcSupport.localDateTime(rs, "fecha_prestamo"));
                    p.setFechaEsperada(JdbcSupport.localDateTime(rs, "fecha_esperada"));
                    p.setIdUsuarioRegistra(rs.getInt("id_usuario_registra"));
                    p.setUsuarioRegistra(rs.getString("usuario"));
                    p.setEstado(EstadoPrestamo.valueOf(rs.getString("estado")));
                    p.setObservaciones(rs.getString("observaciones"));
                    p.setPendientes(rs.getInt("pendientes"));
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar los préstamos", e);
        }
        return lista;
    }

    private static void cerrar(Connection conn) {
        if (conn == null) return;
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException ignored) {
            // la conexión ya no se usa
        }
    }
}