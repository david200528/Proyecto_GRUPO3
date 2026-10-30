package sigelab.model;

import sigelab.enums.CondicionDevolucion;

import java.time.LocalDateTime;

public class Devolucion {
    private int idDevolucion;
    private int idDetalle;
    private LocalDateTime fechaDevolucion;
    private CondicionDevolucion condicion;
    private String observaciones;
    private int idUsuarioRegistra;

    public int getIdDevolucion() { return idDevolucion; }
    public void setIdDevolucion(int idDevolucion) { this.idDevolucion = idDevolucion; }
    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public LocalDateTime getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(LocalDateTime fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }
    public CondicionDevolucion getCondicion() { return condicion; }
    public void setCondicion(CondicionDevolucion condicion) { this.condicion = condicion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public int getIdUsuarioRegistra() { return idUsuarioRegistra; }
    public void setIdUsuarioRegistra(int idUsuarioRegistra) { this.idUsuarioRegistra = idUsuarioRegistra; }
}