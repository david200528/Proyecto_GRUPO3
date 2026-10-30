package sigelab.repository;

import sigelab.util.TablaResultado;

import java.time.LocalDateTime;

/** Consultas de dashboard y reportes (SELECT, JOIN, WHERE, ORDER BY, BETWEEN, COUNT, GROUP BY). */
public class ReporteRepository {

    public int totalEquipos() {
        return ConsultaRepository.contar("SELECT COUNT(*) FROM dbo.Equipo");
    }

    public int equiposEnEstado(String estado) {
        return ConsultaRepository.contar("SELECT COUNT(*) FROM dbo.Equipo WHERE estado = ?", estado);
    }

    public int prestamosActivos() {
        return ConsultaRepository.contar("SELECT COUNT(*) FROM dbo.Prestamo WHERE estado = 'ACTIVO'");
    }

    public int prestamosVencidos() {
        return ConsultaRepository.contar("SELECT COUNT(*) FROM dbo.Prestamo p WHERE p.estado = 'ACTIVO' "
                + "AND p.fecha_esperada < GETDATE() AND EXISTS (SELECT 1 FROM dbo.DetallePrestamo d "
                + "WHERE d.id_prestamo = p.id_prestamo AND d.estado_devolucion = 'PENDIENTE')");
    }

    public TablaResultado equiposPorEstado() {
        return ConsultaRepository.consultar("SELECT estado AS [Estado], COUNT(*) AS [Cantidad] FROM dbo.Equipo "
                + "GROUP BY estado ORDER BY COUNT(*) DESC");
    }

    public TablaResultado equiposDeEstado(String estado) {
        return ConsultaRepository.consultar("SELECT e.codigo_inventario AS [Código], e.nombre AS [Nombre], "
                + "t.nombre AS [Tipo], l.nombre AS [Laboratorio], e.estado AS [Estado] "
                + "FROM dbo.Equipo e JOIN dbo.TipoEquipo t ON t.id_tipo_equipo = e.id_tipo_equipo "
                + "JOIN dbo.Laboratorio l ON l.id_laboratorio = e.id_laboratorio "
                + "WHERE e.estado = ? ORDER BY e.codigo_inventario", estado);
    }

    /** filtro: ACTIVO, FINALIZADO, VENCIDO o null para todos. */
    public TablaResultado prestamos(String filtro) {
        String base = "SELECT p.id_prestamo AS [ID], p.solicitante AS [Solicitante], p.fecha_prestamo AS [Fecha préstamo], "
                + "p.fecha_esperada AS [Fecha esperada], p.estado AS [Estado], "
                + "(SELECT COUNT(*) FROM dbo.DetallePrestamo d WHERE d.id_prestamo = p.id_prestamo "
                + "AND d.estado_devolucion = 'PENDIENTE') AS [Pendientes], u.usuario AS [Registró] "
                + "FROM dbo.Prestamo p JOIN dbo.Usuario u ON u.id_usuario = p.id_usuario_registra ";
        String where = switch (filtro == null ? "" : filtro) {
            case "ACTIVO" -> "WHERE p.estado = 'ACTIVO' ";
            case "FINALIZADO" -> "WHERE p.estado = 'FINALIZADO' ";
            case "VENCIDO" -> "WHERE p.estado = 'ACTIVO' AND p.fecha_esperada < GETDATE() AND EXISTS ("
                    + "SELECT 1 FROM dbo.DetallePrestamo d WHERE d.id_prestamo = p.id_prestamo "
                    + "AND d.estado_devolucion = 'PENDIENTE') ";
            default -> "";
        };
        return ConsultaRepository.consultar(base + where + "ORDER BY p.fecha_prestamo DESC");
    }

    public TablaResultado prestamosEntre(LocalDateTime desde, LocalDateTime hasta) {
        return ConsultaRepository.consultar("SELECT p.id_prestamo AS [ID], p.solicitante AS [Solicitante], "
                + "p.fecha_prestamo AS [Fecha préstamo], p.fecha_esperada AS [Fecha esperada], p.estado AS [Estado], "
                + "COUNT(d.id_detalle) AS [Equipos] FROM dbo.Prestamo p "
                + "JOIN dbo.DetallePrestamo d ON d.id_prestamo = p.id_prestamo "
                + "WHERE p.fecha_prestamo BETWEEN ? AND ? "
                + "GROUP BY p.id_prestamo, p.solicitante, p.fecha_prestamo, p.fecha_esperada, p.estado "
                + "ORDER BY p.fecha_prestamo DESC", desde, hasta);
    }

    public TablaResultado historialEquipo(String codigo) {
        return ConsultaRepository.consultar("SELECT p.id_prestamo AS [Préstamo], p.solicitante AS [Solicitante], "
                + "p.fecha_prestamo AS [Fecha préstamo], p.fecha_esperada AS [Fecha esperada], "
                + "dv.fecha_devolucion AS [Fecha devolución], ISNULL(dv.condicion, 'PENDIENTE') AS [Condición], "
                + "ISNULL(dv.observaciones, '') AS [Observaciones] FROM dbo.DetallePrestamo d "
                + "JOIN dbo.Equipo e ON e.id_equipo = d.id_equipo "
                + "JOIN dbo.Prestamo p ON p.id_prestamo = d.id_prestamo "
                + "LEFT JOIN dbo.Devolucion dv ON dv.id_detalle = d.id_detalle "
                + "WHERE e.codigo_inventario = ? ORDER BY p.fecha_prestamo DESC", codigo);
    }

    /** codigo null = todos los equipos. */
    public TablaResultado historialMantenimiento(String codigo) {
        String base = "SELECT e.codigo_inventario AS [Equipo], m.tipo_mantenimiento AS [Tipo], "
                + "m.fecha_ingreso AS [Ingreso], m.fecha_salida AS [Salida], m.responsable AS [Responsable], "
                + "m.descripcion AS [Descripción], m.estado AS [Estado], ISNULL(m.resultado, '') AS [Resultado] "
                + "FROM dbo.Mantenimiento m JOIN dbo.Equipo e ON e.id_equipo = m.id_equipo ";
        return codigo == null
                ? ConsultaRepository.consultar(base + "ORDER BY m.fecha_ingreso DESC")
                : ConsultaRepository.consultar(base + "WHERE e.codigo_inventario = ? ORDER BY m.fecha_ingreso DESC", codigo);
    }
}