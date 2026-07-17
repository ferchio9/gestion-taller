package com.taller.gestion.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noEncontradoDevuelve404ConElMensajeDeLaExcepcion() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.noEncontrado(new RecursoNoEncontradoException("no existe"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "no existe");
        assertThat(respuesta.getBody()).containsEntry("estado", 404);
    }

    @Test
    void conflictoDevuelve409ConElMensajeDeLaExcepcion() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.conflicto(new ConflictoException("ya existe"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).containsEntry("mensaje", "ya existe");
    }

    @Test
    void integridadDevuelve409ConMensajeGenerico() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.integridad(new DataIntegrityViolationException("FK violation"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().get("mensaje")).asString().contains("datos relacionados");
    }

    @Test
    void validacionDevuelve400ConMapaDeErroresPorCampo() {
        FieldError error = new FieldError("objeto", "nombre", "no puede estar vacío");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> respuesta = handler.validacion(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) respuesta.getBody().get("errores");
        assertThat(errores).containsEntry("nombre", "no puede estar vacío");
    }

    @Test
    void inesperadoDevuelve500ConMensajeGenericoSinFugarDetalles() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.inesperado(new NullPointerException("detalle interno sensible"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().get("mensaje").toString()).doesNotContain("detalle interno sensible");
    }
}
