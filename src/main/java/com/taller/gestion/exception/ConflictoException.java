package com.taller.gestion.exception;

// Conflicto de negocio, p. ej. matricula duplicada. El manejador la traduce a 409.
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
