package com.taller.gestion.exception;

// Se lanza cuando se pide un id que no existe. El manejador global la traduce a 404.
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
