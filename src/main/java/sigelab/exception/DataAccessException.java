package sigelab.exception;

public class DataAccessException extends RuntimeException {
    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}