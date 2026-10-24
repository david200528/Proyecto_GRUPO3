package sigelab.service;

import sigelab.enums.MedioMarcaje;
import sigelab.model.Dispositivo;
import sigelab.repository.DispositivoRepository;
import sigelab.util.BusinessException;

import java.util.List;
import java.util.Optional;

public class DispositivoService {
    private final DispositivoRepository repository;
    public DispositivoService() { this(new DispositivoRepository()); }

    public DispositivoService(DispositivoRepository repository) { this.repository = repository; }
    public boolean guardar(Dispositivo d) { validar(d); if (repository.existeCodigo(d.getCodigo())) throw new BusinessException("El código del dispositivo ya está registrado."); d.setActivo(true); return repository.guardar(d); }
    public List<Dispositivo> listar() { return repository.listar(); }
    public List<Dispositivo> listarActivos() { return repository.listarActivos(); }
    public Optional<Dispositivo> buscar(int id) { return repository.buscarPorId(id); }
    public Optional<Dispositivo> buscarPorCodigo(String codigo) { return repository.buscarPorCodigo(codigo); }
    public boolean actualizar(Dispositivo d) { validar(d); if (d.getIdDispositivo() <= 0) throw new BusinessException("El dispositivo no tiene un ID válido."); if (repository.existeCodigo(d.getCodigo(), d.getIdDispositivo())) throw new BusinessException("El código del dispositivo ya está registrado."); return repository.actualizar(d); }
    public boolean desactivar(int id) { return repository.desactivar(id); }
    private void validar(Dispositivo d) { if (d == null) throw new BusinessException("El dispositivo es obligatorio."); if (d.getCodigo() == null || d.getCodigo().isBlank()) throw new BusinessException("El código del dispositivo es obligatorio."); if (d.getNombre() == null || d.getNombre().isBlank()) throw new BusinessException("El nombre del dispositivo es obligatorio."); if (d.getTipo() == null) throw new BusinessException("El tipo del dispositivo es obligatorio."); try { MedioMarcaje.valueOf(d.getTipo()); } catch (IllegalArgumentException e) { throw new BusinessException("El tipo de dispositivo no es válido."); } }

    public Dispositivo buscarDispositivoPorQr(String rutaImagenQr) {
        String codigoInventario = sigelab.util.QrUtil.leerQr(rutaImagenQr);
        if (codigoInventario == null || codigoInventario.isBlank()) {
            throw new BusinessException("No se pudo leer ningún código QR válido en la imagen seleccionada.");
        }

        Optional<Dispositivo> dispositivoOpt = buscarPorCodigo(codigoInventario);
        if (dispositivoOpt.isPresent()) {
            return dispositivoOpt.get();
        } else {
            throw new BusinessException("El código QR (" + codigoInventario + ") no está registrado en la base de datos.");
        }
    }

}

