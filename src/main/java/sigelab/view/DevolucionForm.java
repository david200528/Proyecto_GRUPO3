package sigelab.view;

import sigelab.controller.DevolucionController;
import sigelab.controller.PrestamoController;
import sigelab.enums.CondicionDevolucion;
import sigelab.model.DetallePrestamo;
import sigelab.model.Prestamo;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Devoluciones (parciales permitidas) de préstamos activos. */
public class DevolucionForm {
    private final JPanel panelPrincipal = Ui.contenedor("DEVOLUCIONES");
    private final PrestamoController prestamos;
    private final DevolucionController devoluciones;
    private final JTable tablaPrestamos = Ui.tabla(Ui.modelo("ID", "Solicitante", "Fecha esperada", "Pendientes", "Vencido"));
    private final JTable tablaDetalle = Ui.tabla(Ui.modelo("ID detalle", "Código", "Equipo", "Estado", "Condición", "Fecha devolución"));
    private final JComboBox<CondicionDevolucion> cmbCondicion = new JComboBox<>(CondicionDevolucion.values());
    private final JTextField txtObs = new JTextField(30);
    private List<DetallePrestamo> detalles = List.of();
    private int idPrestamo = -1;

    public DevolucionForm(Usuario actor) {
        prestamos = new PrestamoController(actor);
        devoluciones = new DevolucionController(actor);
        ComboBoxRenderers.configurarEnum(cmbCondicion);

        JPanel superior = new JPanel(new BorderLayout(4, 4));
        superior.setBorder(BorderFactory.createTitledBorder("1. Seleccione un préstamo activo"));
        superior.add(new JScrollPane(tablaPrestamos), BorderLayout.CENTER);
        JButton btnActualizar = new JButton("Actualizar lista");
        superior.add(Ui.botones(btnActualizar), BorderLayout.SOUTH);

        JPanel inferior = new JPanel(new BorderLayout(4, 4));
        inferior.setBorder(BorderFactory.createTitledBorder("2. Equipos del préstamo (seleccione uno pendiente)"));
        inferior.add(new JScrollPane(tablaDetalle), BorderLayout.CENTER);
        JPanel accion = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton btnDevolver = new JButton("Registrar devolución");
        accion.add(new JLabel("Condición:"));
        accion.add(cmbCondicion);
        accion.add(new JLabel("Observación:"));
        accion.add(txtObs);
        accion.add(btnDevolver);
        inferior.add(accion, BorderLayout.SOUTH);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, superior, inferior);
        division.setResizeWeight(0.45);
        panelPrincipal.add(division, BorderLayout.CENTER);

        btnActualizar.addActionListener(e -> cargarPrestamos());
        btnDevolver.addActionListener(e -> devolver());
        tablaPrestamos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) seleccionarPrestamo();
        });
        cargarPrestamos();
    }

    private void cargarPrestamos() {
        try {
            DefaultTableModel m = Ui.modelo("ID", "Solicitante", "Fecha esperada", "Pendientes", "Vencido");
            for (Prestamo p : prestamos.listarActivos()) {
                m.addRow(new Object[]{p.getIdPrestamo(), p.getSolicitante(), ComboBoxRenderers.fechaHora(p.getFechaEsperada()),
                        p.getPendientes(), p.isVencido() ? "Sí" : "No"});
            }
            tablaPrestamos.setModel(m);
            idPrestamo = -1;
            detalles = List.of();
            tablaDetalle.setModel(Ui.modelo("ID detalle", "Código", "Equipo", "Estado", "Condición", "Fecha devolución"));
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void seleccionarPrestamo() {
        int fila = tablaPrestamos.getSelectedRow();
        if (fila < 0) return;
        idPrestamo = Integer.parseInt(tablaPrestamos.getValueAt(fila, 0).toString());
        cargarDetalle();
    }

    private void cargarDetalle() {
        try {
            detalles = prestamos.detalles(idPrestamo);
            DefaultTableModel m = Ui.modelo("ID detalle", "Código", "Equipo", "Estado", "Condición", "Fecha devolución");
            for (DetallePrestamo d : detalles) {
                m.addRow(new Object[]{d.getIdDetalle(), d.getEquipo().getCodigoInventario(), d.getEquipo().getNombre(),
                        d.isDevuelto() ? "Devuelto" : "Pendiente",
                        d.isDevuelto() ? ComboBoxRenderers.texto(d.getDevolucion().getCondicion()) : "",
                        d.isDevuelto() ? ComboBoxRenderers.fechaHora(d.getFechaDevolucion()) : ""});
            }
            tablaDetalle.setModel(m);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void devolver() {
        try {
            int fila = tablaDetalle.getSelectedRow();
            if (idPrestamo < 0 || fila < 0) throw new IllegalArgumentException("Seleccione un préstamo y el equipo que se devuelve.");
            int idDetalle = Integer.parseInt(tablaDetalle.getValueAt(fila, 0).toString());
            boolean finalizado = devoluciones.devolver(idPrestamo, idDetalle,
                    (CondicionDevolucion) cmbCondicion.getSelectedItem(), txtObs.getText());
            Ui.info(panelPrincipal, finalizado
                    ? "Devolución registrada. El préstamo quedó finalizado."
                    : "Devolución registrada. El préstamo sigue activo con equipos pendientes.");
            txtObs.setText("");
            int seleccionado = idPrestamo;
            cargarPrestamos();
            if (!finalizado) {
                idPrestamo = seleccionado;
                cargarDetalle();
            }
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}