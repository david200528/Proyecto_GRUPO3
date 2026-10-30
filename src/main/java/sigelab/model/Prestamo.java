package sigelab.model;

import sigelab.enums.EstadoPrestamo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Prestamo {
    private int idPrestamo;
    private String solicitante;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaEsperada;
    private int idUsuarioRegistra;
    private String usuarioRegistra;
    private EstadoPrestamo estado = EstadoPrestamo.ACTIVO;
    private String observaciones;
    private int pendientes;
    private final List<DetallePrestamo> detalles = new ArrayList<>();

    public int getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(int idPrestamo) { this.idPrestamo = idPrestamo; }
    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }
    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(LocalDateTime fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }
    public LocalDateTime getFechaEsperada() { return fechaEsperada; }
    public void setFechaEsperada(LocalDateTime fechaEsperada) { this.fechaEsperada = fechaEsperada; }
    public int getIdUsuarioRegistra() { return idUsuarioRegistra; }
    public void setIdUsuarioRegistra(int idUsuarioRegistra) { this.idUsuarioRegistra = idUsuarioRegistra; }
    public String getUsuarioRegistra() { return usuarioRegistra; }
    public void setUsuarioRegistra(String usuarioRegistra) { this.usuarioRegistra = usuarioRegistra; }
    public EstadoPrestamo getEstado() { return estado; }
    public void setEstado(EstadoPrestamo estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public int getPendientes() { return pendientes; }
    public void setPendientes(int pendientes) { this.pendientes = pendientes; }
    public List<DetallePrestamo> getDetalles() { return detalles; }

    /** RN-17: vencido = fecha esperada pasada y todavía con equipos pendientes. */
    public boolean isVencido() {
        return estado == EstadoPrestamo.ACTIVO && pendientes > 0
                && fechaEsperada != null && fechaEsperada.isBefore(LocalDateTime.now());
    }
}