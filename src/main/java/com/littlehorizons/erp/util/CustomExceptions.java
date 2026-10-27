package com.littlehorizons.erp.util;

/** Excepciones propias del sistema. Ambas son no verificadas. */
public final class CustomExceptions {

    private CustomExceptions() {
    }

    /** Falla de acceso a datos (SQL, conexión). El usuario ve un mensaje genérico; el detalle va al log. */
    public static class DataAccessException extends RuntimeException {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Regla de negocio violada. El mensaje se muestra tal cual al usuario. */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
