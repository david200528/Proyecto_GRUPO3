package sigelab.model;

public class TipoEquipo {
    private int idTipoEquipo;
    private String nombre;
    private String descripcion;

    public TipoEquipo() { }

    public TipoEquipo(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getIdTipoEquipo() { return idTipoEquipo; }
    public void setIdTipoEquipo(int idTipoEquipo) { this.idTipoEquipo = idTipoEquipo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() { return nombre; }
}