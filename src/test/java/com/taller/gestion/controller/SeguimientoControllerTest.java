package com.taller.gestion.controller;

import com.taller.gestion.dto.SeguimientoResponse;
import com.taller.gestion.security.SecurityConfig;
import com.taller.gestion.service.SeguimientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El portal de seguimiento es publico (permitAll en SecurityConfig): sin @WithMockUser
// en ninguno de estos tests se comprueba precisamente que el acceso anonimo funciona.
@WebMvcTest(SeguimientoController.class)
@Import(SecurityConfig.class)
class SeguimientoControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private SeguimientoService seguimientoService;

    @BeforeEach
    void construirMockMvcConSeguridad() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void obtenerSinAutenticarDevuelveLosDatosDeLaOrden() throws Exception {
        when(seguimientoService.obtenerSeguimiento(eq("codigo-123"))).thenReturn(
                new SeguimientoResponse("1234ABC", "Seat", "Ibiza", "COCHE", "Cliente Test",
                        "RECEPCION", null, null, "Ruido en el motor", BigDecimal.ZERO,
                        List.of(), null, null, List.of()));

        mockMvc.perform(get("/api/seguimiento/codigo-123")).andExpect(status().isOk());
    }

    @Test
    void responderPresupuestoSinAutenticarFunciona() throws Exception {
        when(seguimientoService.obtenerSeguimiento(eq("codigo-123"))).thenReturn(
                new SeguimientoResponse("1234ABC", "Seat", "Ibiza", "COCHE", "Cliente Test",
                        "RECEPCION", null, null, "Ruido en el motor", BigDecimal.ZERO,
                        List.of(), true, null, List.of()));

        mockMvc.perform(put("/api/seguimiento/codigo-123/presupuesto").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprobado\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void responderPresupuestoSinCampoAprobadoDevuelveDatosNoValidos() throws Exception {
        mockMvc.perform(put("/api/seguimiento/codigo-123/presupuesto").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
