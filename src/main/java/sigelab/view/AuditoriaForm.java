package sigelab.view;

import sigelab.controller.AuditoriaController;
import sigelab.model.Usuario;

import javax.swing.*;
import java.awt.*;

/** Bitácora de auditoría (solo ADMIN). */
public class AuditoriaForm {
    private final JPanel panelPrincipal = Ui.contenedor("AUDITORIA Y BITACORA DEL SISTEMA");
    private final AuditoriaController controller = new AuditoriaController();
    private final Usuario actor;
    private final JTable tabla = Ui.tabla(Ui.modelo());

    public AuditoriaForm(Usuario actor) {
        this.actor = actor;
        panelPrincipal.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JButton btnActualizar = new JButton("Actualizar registros");
        btnActualizar.addActionListener(e -> cargar());
        panelPrincipal.add(Ui.botones(btnActualizar), BorderLayout.SOUTH);
        cargar();
    }

    private void cargar() {
        try {
            Ui.llenar(tabla, controller.listar(actor));
        } catch (RuntimeException e) {
            Ui.error(panelPrincipal, e);
        }
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}