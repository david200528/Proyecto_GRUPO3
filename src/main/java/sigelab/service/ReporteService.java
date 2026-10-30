package sigelab.service;

import sigelab.enums.EstadoEquipo;
import sigelab.enums.RolUsuario;
import sigelab.model.Usuario;
import sigelab.repository.ReporteRepository;
import sigelab.util.BusinessException;
import sigelab.util.Permisos;
import sigelab.util.TablaResultado;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReporteService {
    private final ReporteRepository repository = new ReporteRepository();

    /** Cantidades del dashboard obtenidas desde la base de datos (6.11). */
    public Map<String, Integer> dashboard(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
        Map<String, Integer> datos = new LinkedHashMap<>();
        datos.put("Total de equipos", repository.totalEquipos());
        datos.put("Disponibles", repository.equiposEnEstado(EstadoEquipo.DISPONIBLE.name()));
        datos.put("Prestados", repository.equiposEnEstado(EstadoEquipo.PRESTADO.name()));
        datos.put("Dañados", repository.equiposEnEstado(EstadoEquipo.DANADO.name()));
        datos.put("En mantenimiento", repository.equiposEnEstado(EstadoEquipo.MANTENIMIENTO.name()));
        datos.put("De baja", repository.equiposEnEstado(EstadoEquipo.BAJA.name()));
        datos.put("Préstamos activos", repository.prestamosActivos());
        datos.put("Préstamos vencidos", repository.prestamosVencidos());
        return datos;
    }

    public TablaResultado equiposPorEstado(Usuario actor, EstadoEquipo estado) {
        permitir(actor);
        return estado == null ? repository.equiposPorEstado() : repository.equiposDeEstado(estado.name());
    }

    /** filtro: ACTIVO, FINALIZADO, VENCIDO o null (todos). */
    public TablaResultado prestamos(Usuario actor, String filtro) {
        permitir(actor);
        return repository.prestamos(filtro);
    }

    public TablaResultado prestamosEntre(Usuario actor, LocalDate desde, LocalDate hasta) {
        permitir(actor);
        if (desde == null || hasta == null) throw new BusinessException("Indique ambas fechas.");
        if (hasta.isBefore(desde)) throw new BusinessException("La fecha final no puede ser anterior a la inicial.");
        return repository.prestamosEntre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay().minusSeconds(1));
    }

    public TablaResultado historialEquipo(Usuario actor, String codigo) {
        permitir(actor);
        if (codigo == null || codigo.isBlank()) throw new BusinessException("Indique el código del equipo.");
        return repository.historialEquipo(codigo.trim());
    }

    public TablaResultado historialMantenimiento(Usuario actor, String codigoOpcional) {
        permitir(actor);
        return repository.historialMantenimiento(codigoOpcional == null || codigoOpcional.isBlank()
                ? null : codigoOpcional.trim());
    }

    private void permitir(Usuario actor) {
        Permisos.requerir(actor, RolUsuario.ADMIN, RolUsuario.ENCARGADO, RolUsuario.DOCENTE);
    }
}