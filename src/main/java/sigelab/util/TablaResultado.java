package sigelab.util;

import java.util.List;

/** Resultado tabular genérico para reportes y consultas. */
public record TablaResultado(String[] columnas, List<Object[]> filas) { }