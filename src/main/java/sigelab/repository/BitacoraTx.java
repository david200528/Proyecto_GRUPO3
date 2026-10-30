package sigelab.repository;

import java.sql.*;

public class BitacoraTx {
    public static void registrar(Connection con, int idUsuario, String usuario,
                                 String accion, String detalle) throws SQLException {
        String sql = "INSERT INTO bitacora_auditoria (id_usuario, usuario, fecha_hora, accion, detalle) " +
                "VALUES (?, ?, GETDATE(), ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, usuario);
            ps.setString(3, accion);
            ps.setString(4, detalle);
            ps.executeUpdate();
        }
    }
}