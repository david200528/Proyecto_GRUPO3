package sigelab.view;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/** Formato de textos y renderizado de combos compartido por las pantallas. */
public final class ComboBoxRenderers {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ComboBoxRenderers() { }

    /** Muestra un texto legible para cada elemento del combo; null se ve como "(seleccione)". */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> void configurar(JComboBox<T> combo, Function<T, String> formato) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                   boolean selected, boolean focus) {
                String texto = value == null ? "(seleccione)" : formato.apply((T) value);
                return super.getListCellRendererComponent(list, texto, index, selected, focus);
            }
        });
    }

    public static <E extends Enum<E>> void configurarEnum(JComboBox<E> combo) {
        configurar(combo, ComboBoxRenderers::texto);
    }

    public static void configurarRoles(JComboBox<sigelab.enums.RolUsuario> combo) {
        configurarEnum(combo);
    }

    public static String texto(Enum<?> valor) {
        if (valor == null) return "";
        String texto = valor.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        if ("danado".equals(texto)) texto = "dañado";
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public static String fecha(LocalDate valor) {
        return valor == null ? "" : valor.format(FECHA);
    }

    public static String fechaHora(LocalDateTime valor) {
        return valor == null ? "" : valor.format(FECHA_HORA);
    }
}