package sigelab.view;

import sigelab.controller.PrestamoController;
import sigelab.model.Equipo;
import sigelab.model.Prestamo;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Registro de préstamos: encabezado + equipos (por código o lectura de QR). */
public class PrestamoForm {
    private final JPanel panelPrincipal = Ui.contenedor("PRESTAMOS DE EQUIPOS");
    private final PrestamoController controller;
    private final JTextField txtSolicitante = new JTextField(25);
    private final JSpinner spnFechaEsperada = Ui.selectorFechaHora(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0));
    private final JTextField txtObs = new JTextField(25);
    private final JTextField txtCodigo = new JTextField(14);
    private final JTable tablaEquipos = Ui.tabla(Ui.modelo("Código", "Nombre", "Tipo", "Laboratorio"));
    private final JTable tablaActivos = Ui.tabla(Ui.modelo("ID", "Solicitante", "Fecha préstamo", "Fecha esperada", "Pendientes", "Vencido"));
    private final List<Equipo> agregados = new ArrayList<>();

    public PrestamoForm(Usuario actor) {
        controller = new PrestamoController(actor);

        JPanel encabezado = new JPanel(new GridBagLayout());
        Ui.campo(encabezado, 0, "Solicitante", txtSolicitante);
        Ui.campo(encabezado, 1, "Devolución esperada", spnFechaEsperada);
        Ui.campo(encabezado, 2, "Observaciones", txtObs);

        JButton btnAgregar = new JButton("Agregar equipo");
        JButton btnQr = new JButton("Leer QR desde imagen");
        JButton btnQuitar = new JButton("Quitar seleccionado");
        JPanel agregar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        agregar.add(new JLabel("Código de inventario:"));
        agregar.add(txtCodigo);
        agregar.add(btnAgregar);
        agregar.add(btnQr);
        agregar.add(btnQuitar);

        JButton btnConfirmar = new JButton("CONFIRMAR PRESTAMO");
        JButton btnLimpiar = new JButton("Limpiar");

        JPanel nuevo = new JPanel(new BorderLayout(4, 4));
        nuevo.setBorder(BorderFactory.createTitledBorder("Nuevo préstamo"));
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(encabezado, BorderLayout.CENTER);
        cabecera.add(agregar, BorderLayout.SOUTH);
        nuevo.add(cabecera, BorderLayout.NORTH);
        nuevo.add(new JScrollPane(tablaEquipos), BorderLayout.CENTER);
        nuevo.add(Ui.botones(btnConfirmar, btnLimpiar), BorderLayout.SOUTH);

        JPanel activos = new JPanel(new BorderLayout(4, 4));
        activos.setBorder(BorderFactory.createTitledBorder("Préstamos activos"));
        JButton btnActualizar = new JButton("Actualizar lista");
        activos.add(new JScrollPane(tablaActivos), BorderLayout.CENTER);
        activos.add(Ui.botones(btnActualizar), BorderLayout.SOUTH);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, nuevo, activos);
        division.setResizeWeight(0.6);
        panelPrincipal.add(division, BorderLayout.CENTER);

        btnAgregar.addActionListener(e -> agregar(txtCodigo.getText()));
        txtCodigo.addActionListener(e -> agregar(txtCodigo.getText()));
        btnQr.addActionListener(e -> leerQr());
        btnQuitar.addActionListener(e -> quitar());
        btnConfirmar.addActionListener(e -> confirmar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnActualizar.addActionListener(e -> cargarActivos());
        cargarActivos();
    }

    /** Un solo punto de entrada para código escrito o leído por QR: la validación vive en el servicio. */
    private void agregar(String codigo) {
        try {
            Equipo e = controller.validarEquipo(codigo, agregados);
            agregados.add(e);
            txtCodigo.setText("");
            txtCodigo.requestFocus();
            refrescarEquipos();
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    private void leerQr() {
        JFileChooser selector = new JFileChooser();
        if (selector.showOpenDialog(panelPrincipal) != JFileChooser.APPROVE_OPTION) return;
        try {
            File archivo = selector.getSelectedFile();
            agregar(controller.leerQr(archivo));
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    private void quitar() {
        int fila = tablaEquipos.getSelectedRow();
        if (fila < 0) return;
        agregados.remove(tablaEquipos.convertRowIndexToModel(fila));
        refrescarEquipos();
    }

    private void confirmar() {
        try {
            LocalDateTime fecha = Ui.valorFechaHora(spnFechaEsperada);
            if (!Ui.confirmar(panelPrincipal, "¿Confirmar el préstamo de " + agregados.size() + " equipo(s)?")) return;
            Prestamo p = controller.crear(txtSolicitante.getText(), fecha, txtObs.getText(), new ArrayList<>(agregados));
            Ui.info(panelPrincipal, "Préstamo " + p.getIdPrestamo() + " registrado. Los equipos quedaron como prestados.");
            limpiar();
            cargarActivos();
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    private void limpiar() {
        agregados.clear();
        txtSolicitante.setText("");
        txtObs.setText("");
        txtCodigo.setText("");
        spnFechaEsperada.setValue(java.util.Date.from(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)
                .atZone(java.time.ZoneId.systemDefault()).toInstant()));
        refrescarEquipos();
    }

    private void refrescarEquipos() {
        DefaultTableModel m = Ui.modelo("Código", "Nombre", "Tipo", "Laboratorio");
        for (Equipo e : agregados) {
            m.addRow(new Object[]{e.getCodigoInventario(), e.getNombre(), e.getTipo().getNombre(), e.getLaboratorio().getNombre()});
        }
        tablaEquipos.setModel(m);
    }

    private void cargarActivos() {
        try {
            DefaultTableModel m = Ui.modelo("ID", "Solicitante", "Fecha préstamo", "Fecha esperada", "Pendientes", "Vencido");
            for (Prestamo p : controller.listarActivos()) {
                m.addRow(new Object[]{p.getIdPrestamo(), p.getSolicitante(), ComboBoxRenderers.fechaHora(p.getFechaPrestamo()),
                        ComboBoxRenderers.fechaHora(p.getFechaEsperada()), p.getPendientes(), p.isVencido() ? "Sí" : "No"});
            }
            tablaActivos.setModel(m);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}