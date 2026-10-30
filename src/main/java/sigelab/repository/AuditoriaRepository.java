package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.util.JdbcSupport;
import sigelab.util.TablaResultado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaRepository {

    /** Compatibilidad con llamadas anteriores. */
    public void registrarAccion(Integer idUsuario, String usuario, String accion, String detalle) {
        registrar(idUsuario, usuario, accion, null, null, detalle);
    }

    /**
     * Deja rastro de una operación (RN-18). Un fallo al auditar no debe romper la operación ya
     * confirmada, por eso solo se informa por consola.
     */
    public void registrar(Integer idUsuario, String usuario, String accion, String entidad,
                          Integer idRegistro, String detalle) {
        String sql = "INSERT INTO dbo.bitacora_auditoria (id_usuario, usuario, accion, entidad, id_registro, detalle) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, idUsuario, usuario == null || usuario.isBlank() ? "(desconocido)" : usuario,
                    accion, entidad, idRegistro, detalle);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("No se pudo registrar la auditoría: " + e.getMessage());
        }
    }

    public TablaResultado listar(int limite) {
        String sql = "SELECT TOP (" + Math.max(1, limite) + ") id_auditoria AS [ID], fecha_hora AS [Fecha y hora], "
                + "usuario AS [Usuario], accion AS [Acción], ISNULL(entidad, '') AS [Entidad], "
                + "id_registro AS [ID registro], ISNULL(detalle, '') AS [Descripción] "
                + "FROM dbo.bitacora_auditoria ORDER BY fecha_hora DESC, id_auditoria DESC";
        return ConsultaRepository.consultar(sql);
    }

    public List<String[]> listarBitacora() {
        List<String[]> lista = new ArrayList<>();
        for (Object[] fila : listar(500).filas()) {
            String[] texto = new String[fila.length];
            for (int i = 0; i < fila.length; i++) texto[i] = fila[i] == null ? "" : fila[i].toString();
            lista.add(texto);
        }
        return lista;
    }
}