package com.taller.gestion.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Centraliza el tratamiento de errores: convierte excepciones en respuestas JSON
// con el codigo HTTP correcto. Asi los controladores quedan limpios.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException ex) {
        log.warn("Conflicto: {}", ex.getMessage());
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Salta al borrar un registro con hijos (FK RESTRICT de Oracle), entre otros.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMessage());
        return construir(HttpStatus.CONFLICT,
                "No se puede completar: hay datos relacionados que lo impiden "
                        + "(por ejemplo, borrar un registro que aún tiene elementos asociados).");
    }

    // Errores de @Valid en los DTO: devuelve un mapa campo -> mensaje.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));

        Map<String, Object> cuerpo = base(HttpStatus.BAD_REQUEST, "Datos no válidos");
        cuerpo.put("errores", errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    // Red de seguridad: cualquier excepción no contemplada arriba (p. ej. un NullPointerException)
    // no debe fugar detalles internos al cliente ni escapar sin control hacia el manejador
    // por defecto de Spring. Se registra completa a ERROR para poder depurarla en los logs.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error inesperado. Inténtalo de nuevo más tarde.");
    }

    private ResponseEntity<Map<String, Object>> construir(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(base(estado, mensaje));
    }

    private Map<String, Object> base(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("fecha", LocalDateTime.now());
        cuerpo.put("estado", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("mensaje", mensaje);
        return cuerpo;
    }
}
