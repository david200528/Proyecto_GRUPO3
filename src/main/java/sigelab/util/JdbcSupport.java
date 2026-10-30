package sigelab.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Utilidades pequeñas para que los repositorios mantengan el mismo manejo de errores. */
public final class JdbcSupport {
    private JdbcSupport() { }

    public static DataAccessException error(String operation, SQLException cause) {
        return new DataAccessException("No se pudo " + operation + ".", cause);
    }

    /** Error de SQL Server por violación de UNIQUE / PRIMARY KEY (2627 / 2601). */
    public static boolean esDuplicado(SQLException e) {
        return e.getErrorCode() == 2627 || e.getErrorCode() == 2601;
    }

    /** Asigna parámetros tipados (null, fechas y enums incluidos). */
    public static void asignar(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) stmt.setObject(i + 1, null);
            else if (p instanceof Enum<?> e) stmt.setString(i + 1, e.name());
            else stmt.setObject(i + 1, p);
        }
    }

    public static LocalDate localDate(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, LocalDate.class);
    }

    public static LocalTime localTime(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, LocalTime.class);
    }

    public static LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, LocalDateTime.class);
    }

    public static void rollback(java.sql.Connection conn) {
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException ignored) {
            // nada más que hacer: la conexión se cierra a continuación
        }
    }
}