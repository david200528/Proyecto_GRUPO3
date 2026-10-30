package sigelab.view;

import sigelab.util.BusinessException;
import sigelab.util.TablaResultado;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/** Ayudas comunes de interfaz: mensajes, tablas y formularios en rejilla. */
final class Ui {
    private static final java.util.Set<String> ESTADOS = java.util.Set.of(
            "DISPONIBLE", "PRESTADO", "DANADO", "MANTENIMIENTO", "BAJA", "ACTIVO", "FINALIZADO",
            "PENDIENTE", "BUENO", "PREVENTIVO", "CORRECTIVO", "EN_PROCESO", "REPARADO", "NO_REPARABLE");

    private Ui() { }

    /** Muestra el mensaje de negocio tal cual; los errores técnicos se ocultan tras un mensaje genérico. */
    static void error(Component padre, Throwable e) {
        String mensaje;
        if (e instanceof BusinessException || e instanceof IllegalArgumentException) {
            mensaje = e.getMessage();
        } else {
            e.printStackTrace();
            mensaje = "Ocurrió un problema al procesar la operación. Verifique la conexión con la base de datos "
                    + "e inténtelo de nuevo.";
        }
        JOptionPane.showMessageDialog(padre, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "SIGELAB", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
        return t;
    }

    static void llenar(JTable tabla, TablaResultado datos) {
        DefaultTableModel modelo = modelo(datos.columnas());
        for (Object[] fila : datos.filas()) {
            Object[] visible = new Object[fila.length];
            for (int i = 0; i < fila.length; i++) visible[i] = formato(fila[i]);
            modelo.addRow(visible);
        }
        tabla.setModel(modelo);
    }

    static Object formato(Object valor) {
        if (valor instanceof Timestamp ts) return ComboBoxRenderers.fechaHora(ts.toLocalDateTime());
        if (valor instanceof LocalDateTime ldt) return ComboBoxRenderers.fechaHora(ldt);
        if (valor instanceof java.sql.Date d) return ComboBoxRenderers.fecha(d.toLocalDate());
        if (valor instanceof Enum<?> e) return ComboBoxRenderers.texto(e);
        if (valor instanceof String s && ESTADOS.contains(s)) {
            // estados guardados como texto (DISPONIBLE, DANADO...) se muestran legibles
            String t = s.replace('_', ' ').toLowerCase();
            if (t.equals("danado")) t = "dañado";
            return Character.toUpperCase(t.charAt(0)) + t.substring(1);
        }
        return valor;
    }

    /** Fila etiqueta + campo en un GridBagLayout. */
    static void campo(JPanel panel, int fila, String etiqueta, JComponent componente) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = fila;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 3, 3, 8);
        panel.add(new JLabel(etiqueta), c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = fila;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 3);
        panel.add(componente, c);
    }

    static JPanel botones(JButton... botones) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (JButton b : botones) p.add(b);
        return p;
    }

    static JPanel contenedor(String titulo) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        p.add(lbl, BorderLayout.NORTH);
        return p;
    }

    static JSpinner selectorFechaHora(LocalDateTime inicial) {
        JSpinner s = new JSpinner(new SpinnerDateModel(
                Date.from(inicial.atZone(ZoneId.systemDefault()).toInstant()), null, null, java.util.Calendar.MINUTE));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy HH:mm"));
        return s;
    }

    static JSpinner selectorFecha(LocalDate inicial) {
        JSpinner s = new JSpinner(new SpinnerDateModel(
                Date.from(inicial.atStartOfDay(ZoneId.systemDefault()).toInstant()), null, null, java.util.Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        return s;
    }

    static LocalDateTime valorFechaHora(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    static LocalDate valorFecha(JSpinner s) {
        return valorFechaHora(s).toLocalDate();
    }

    static String texto(javax.swing.text.JTextComponent campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }
}