package sigelab.view;

import sigelab.controller.EquipoController;
import sigelab.controller.MantenimientoController;
import sigelab.enums.EstadoEquipo;
import sigelab.enums.EstadoMantenimiento;
import sigelab.enums.ResultadoMantenimiento;
import sigelab.enums.TipoMantenimiento;
import sigelab.model.Equipo;
import sigelab.model.Mantenimiento;
import sigelab.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Envío de equipos a mantenimiento y cierre del mantenimiento. */
public class MantenimientoForm {
    private final JPanel panelPrincipal = Ui.contenedor("MANTENIMIENTO DE EQUIPOS");
    private final MantenimientoController controller;
    private final EquipoController equipos;
    private final JComboBox<Equipo> cmbEquipo = new JComboBox<>();
    private final JComboBox<TipoMantenimiento> cmbTipo = new JComboBox<>(TipoMantenimiento.values());
    private final JTextField txtDescripcion = new JTextField(30);
    private final JTextField txtResponsable = new JTextField(20);
    private final JTextField txtObs = new JTextField(30);
    private final JComboBox<ResultadoMantenimiento> cmbResultado = new JComboBox<>(ResultadoMantenimiento.values());
    private final JTextField txtObsCierre = new JTextField(25);
    private final JTable tabla = Ui.tabla(Ui.modelo("ID", "Equipo", "Tipo", "Ingreso", "Salida", "Responsable", "Estado", "Resultado"));
    private List<Mantenimiento> lista = List.of();

    public MantenimientoForm(Usuario actor) {
        controller = new MantenimientoController(actor);
        equipos = new EquipoController(actor);
        ComboBoxRenderers.configurarEnum(cmbTipo);
        ComboBoxRenderers.configurarEnum(cmbResultado);
        ComboBoxRenderers.configurar(cmbEquipo, e -> e.getCodigoInventario() + " - " + e.getNombre()
                + " (" + ComboBoxRenderers.texto(e.getEstado()) + ")");

        JPanel envio = new JPanel(new GridBagLayout());
        envio.setBorder(BorderFactory.createTitledBorder("Enviar equipo a mantenimiento (dañado o disponible)"));
        Ui.campo(envio, 0, "Equipo", cmbEquipo);
        Ui.campo(envio, 1, "Tipo", cmbTipo);
        Ui.campo(envio, 2, "Descripción", txtDescripcion);
        Ui.campo(envio, 3, "Responsable", txtResponsable);
        Ui.campo(envio, 4, "Observaciones", txtObs);
        JButton btnEnviar = new JButton("Enviar a mantenimiento");
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(envio, BorderLayout.CENTER);
        arriba.add(Ui.botones(btnEnviar), BorderLayout.SOUTH);

        JPanel cierre = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton btnFinalizar = new JButton("Finalizar mantenimiento seleccionado");
        JButton btnActualizar = new JButton("Actualizar");
        cierre.add(new JLabel("Resultado:"));
        cierre.add(cmbResultado);
        cierre.add(new JLabel("Observaciones:"));
        cierre.add(txtObsCierre);
        cierre.add(btnFinalizar);
        cierre.add(btnActualizar);

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.setBorder(BorderFactory.createTitledBorder("Historial de mantenimiento"));
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(cierre, BorderLayout.SOUTH);

        JPanel contenido = new JPanel(new BorderLayout(6, 6));
        contenido.add(arriba, BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);
        panelPrincipal.add(contenido, BorderLayout.CENTER);

        btnEnviar.addActionListener(e -> enviar());
        btnFinalizar.addActionListener(e -> finalizar());
        btnActualizar.addActionListener(e -> recargar());
        recargar();
    }

    private void recargar() {
        try {
            cmbEquipo.removeAllItems();
            List<Equipo> candidatos = new ArrayList<>(equipos.listar(EstadoEquipo.DANADO));
            candidatos.addAll(equipos.listar(EstadoEquipo.DISPONIBLE));
            for (Equipo e : candidatos) cmbEquipo.addItem(e);
            lista = controller.listar(null);
            DefaultTableModel m = Ui.modelo("ID", "Equipo", "Tipo", "Ingreso", "Salida", "Responsable", "Estado", "Resultado");
            for (Mantenimiento x : lista) {
                m.addRow(new Object[]{x.getIdMantenimiento(), x.getEquipo().getCodigoInventario(), ComboBoxRenderers.texto(x.getTipo()),
                        ComboBoxRenderers.fechaHora(x.getFechaIngreso()), ComboBoxRenderers.fechaHora(x.getFechaSalida()),
                        x.getResponsable(), ComboBoxRenderers.texto(x.getEstado()), ComboBoxRenderers.texto(x.getResultado())});
            }
            tabla.setModel(m);
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    private void enviar() {
        try {
            Equipo e = (Equipo) cmbEquipo.getSelectedItem();
            controller.enviar(e, (TipoMantenimiento) cmbTipo.getSelectedItem(), txtDescripcion.getText(),
                    txtResponsable.getText(), txtObs.getText());
            Ui.info(panelPrincipal, "El equipo quedó en mantenimiento.");
            txtDescripcion.setText("");
            txtObs.setText("");
            recargar();
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    private void finalizar() {
        try {
            int fila = tabla.getSelectedRow();
            if (fila < 0) throw new IllegalArgumentException("Seleccione un mantenimiento de la tabla.");
            int id = Integer.parseInt(tabla.getValueAt(fila, 0).toString());
            controller.finalizar(id, (ResultadoMantenimiento) cmbResultado.getSelectedItem(), txtObsCierre.getText());
            Ui.info(panelPrincipal, cmbResultado.getSelectedItem() == ResultadoMantenimiento.REPARADO
                    ? "Mantenimiento finalizado. El equipo quedó disponible."
                    : "Mantenimiento finalizado. El equipo quedó dado de baja.");
            txtObsCierre.setText("");
            recargar();
        } catch (RuntimeException ex) {
            Ui.error(panelPrincipal, ex);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}