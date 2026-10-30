package sigelab.view;

import sigelab.controller.LaboratorioController;
import sigelab.model.Laboratorio;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Catálogo de laboratorios (solo ADMIN). */
public class LaboratorioForm {
    private final JPanel panelPrincipal = Ui.contenedor("ADMINISTRACION DE LABORATORIOS");
    private final LaboratorioController controller;
    private final JTextField txtCodigo = new JTextField(15);
    private final JTextField txtNombre = new JTextField(25);
    private final JTextField txtUbicacion = new JTextField(25);
    private final JSpinner spnCapacidad = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
    private final JCheckBox chkActivo = new JCheckBox("Activo", true);
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnGuardar = new JButton("Guardar");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JTable tabla = Ui.tabla(Ui.modelo("ID", "Código", "Nombre", "Ubicación", "Capacidad", "Activo"));
    private Laboratorio seleccionado;

    public LaboratorioForm(Usuario actor) {
        controller = new LaboratorioController(actor);
        JPanel formulario = new JPanel(new GridBagLayout());
        Ui.campo(formulario, 0, "Código", txtCodigo);
        Ui.campo(formulario, 1, "Nombre", txtNombre);
        Ui.campo(formulario, 2, "Ubicación", txtUbicacion);
        Ui.campo(formulario, 3, "Capacidad", spnCapacidad);
        Ui.campo(formulario, 4, "Estado", chkActivo);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(formulario, BorderLayout.CENTER);
        arriba.add(Ui.botones(btnNuevo, btnGuardar, btnActualizar), BorderLayout.SOUTH);
        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        panelPrincipal.add(centro, BorderLayout.CENTER);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) seleccionar();
        });
        limpiar();
        cargar();
    }

    private Laboratorio leer() {
        Laboratorio l = seleccionado != null ? seleccionado : new Laboratorio();
        l.setCodigo(txtCodigo.getText());
        l.setNombre(txtNombre.getText());
        l.setUbicacion(txtUbicacion.getText());
        l.setCapacidad((Integer) spnCapacidad.getValue());
        l.setActivo(chkActivo.isSelected());
        return l;
    }

    private void guardar() {
        try {
            Laboratorio nuevo = controller.crear(leerNuevo());
            Ui.info(panelPrincipal, "Laboratorio creado: " + nuevo.getCodigo());
            limpiar();
            cargar();
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private Laboratorio leerNuevo() {
        seleccionado = null;
        return leer();
    }

    private void actualizar() {
        try {
            if (seleccionado == null) throw new IllegalArgumentException("Seleccione un laboratorio de la tabla.");
            controller.actualizar(leer());
            Ui.info(panelPrincipal, "Laboratorio actualizado.");
            limpiar();
            cargar();
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void seleccionar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) return;
        try {
            int id = Integer.parseInt(tabla.getValueAt(fila, 0).toString());
            for (Laboratorio l : controller.listar()) {
                if (l.getIdLaboratorio() == id) {
                    seleccionado = l;
                    txtCodigo.setText(l.getCodigo());
                    txtCodigo.setEditable(false);
                    txtNombre.setText(l.getNombre());
                    txtUbicacion.setText(l.getUbicacion());
                    spnCapacidad.setValue(l.getCapacidad());
                    chkActivo.setSelected(l.isActivo());
                    btnGuardar.setEnabled(false);
                    btnActualizar.setEnabled(true);
                }
            }
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void limpiar() {
        seleccionado = null;
        txtCodigo.setText("");
        txtCodigo.setEditable(true);
        txtNombre.setText("");
        txtUbicacion.setText("");
        spnCapacidad.setValue(0);
        chkActivo.setSelected(true);
        tabla.clearSelection();
        btnGuardar.setEnabled(true);
        btnActualizar.setEnabled(false);
    }

    private void cargar() {
        try {
            DefaultTableModel m = Ui.modelo("ID", "Código", "Nombre", "Ubicación", "Capacidad", "Activo");
            for (Laboratorio l : controller.listar()) {
                m.addRow(new Object[]{l.getIdLaboratorio(), l.getCodigo(), l.getNombre(), l.getUbicacion(),
                        l.getCapacidad(), l.isActivo() ? "Sí" : "No"});
            }
            tabla.setModel(m);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}