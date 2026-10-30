package sigelab.util;

import sigelab.enums.RolUsuario;
import sigelab.model.Usuario;

/** RN-20: los permisos se validan en la lógica, no solo ocultando botones. */
public final class Permisos {
    private Permisos() { }

    public static void requerir(Usuario actor, RolUsuario... permitidos) {
        if (actor == null || !actor.isActivo()) {
            throw new BusinessException("Debe iniciar sesión con un usuario activo.");
        }
        for (RolUsuario rol : permitidos) {
            if (actor.getRol() == rol) return;
        }
        throw new BusinessException("Su rol no tiene permiso para realizar esta operación.");
    }

    public static boolean tiene(Usuario actor, RolUsuario... permitidos) {
        try {
            requerir(actor, permitidos);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}