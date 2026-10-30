package sigelab.repository;

import sigelab.config.DatabaseConnection;
import sigelab.util.JdbcSupport;
import sigelab.util.TablaResultado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Ejecuta consultas de solo lectura y devuelve el resultado como tabla (para reportes). */
public final class ConsultaRepository {
    private ConsultaRepository() { }

    public static TablaResultado consultar(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            JdbcSupport.asignar(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int n = meta.getColumnCount();
                String[] columnas = new String[n];
                for (int i = 0; i < n; i++) columnas[i] = meta.getColumnLabel(i + 1);
                List<Object[]> filas = new ArrayList<>();
                while (rs.next()) {
                    Object[] fila = new Object[n];
                    for (int i = 0; i < n; i++) fila[i] = rs.getObject(i + 1);
                    filas.add(fila);
                }
                return new TablaResultado(columnas, filas);
            }
        } catch (SQLException e) {
            throw JdbcSupport.error("consultar la información", e);
        }
    }

    public static int contar(String sql, Object... params) {
        TablaResultado tabla = consultar(sql, params);
        return tabla.filas().isEmpty() ? 0 : ((Number) tabla.filas().get(0)[0]).intValue();
    }
}