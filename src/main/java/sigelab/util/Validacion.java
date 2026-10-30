package sigelab.util;

import java.util.regex.Pattern;

/** Validaciones simples de campos (mensajes orientados al usuario). */
public final class Validacion {
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validacion() { }

    public static String obligatorio(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) throw new BusinessException("El campo " + campo + " es obligatorio.");
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new BusinessException("El campo " + campo + " no puede superar " + maximo + " caracteres.");
        }
        return limpio;
    }

    public static String opcional(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) return null;
        return obligatorio(valor, campo, maximo);
    }

    public static String correo(String valor) {
        String limpio = obligatorio(valor, "correo", 150);
        if (!CORREO.matcher(limpio).matches()) throw new BusinessException("Ingrese un correo válido.");
        return limpio;
    }

    public static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new BusinessException(mensaje);
    }
}