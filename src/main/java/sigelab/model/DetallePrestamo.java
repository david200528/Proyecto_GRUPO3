package sigelab.model;

import java.time.LocalDateTime;

/** Un equipo dentro de un préstamo, con su devolución si ya ocurrió. */
public class DetallePrestamo {
    private int idDetalle;
    private int idPrestamo;
    private Equipo equipo;
    private boolean devuelto;
    private Devolucion devolucion;

    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public int getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(int idPrestamo) { this.idPrestamo = idPrestamo; }
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }
    public boolean isDevuelto() { return devuelto; }
    public void setDevuelto(boolean devuelto) { this.devuelto = devuelto; }
    public Devolucion getDevolucion() { return devolucion; }
    public void setDevolucion(Devolucion devolucion) { this.devolucion = devolucion; }
    public LocalDateTime getFechaDevolucion() { return devolucion == null ? null : devolucion.getFechaDevolucion(); }
}