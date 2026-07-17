package com.taller.gestion.controller;

import com.taller.gestion.dto.CitaResponse;
import com.taller.gestion.security.SecurityConfig;
import com.taller.gestion.service.CitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CitaController.class)
@Import(SecurityConfig.class)
class CitaControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private CitaService citaService;

    @BeforeEach
    void construirMockMvcConSeguridad() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static final String CUERPO_VALIDO =
            "{\"idVehiculo\":1,\"fechaHora\":\"2026-08-01T10:00:00\",\"motivo\":\"Revisión\"}";

    @Test
    void sinAutenticarRecibeNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/citas")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearConDatosValidosDevuelveCreado() throws Exception {
        when(citaService.crear(any())).thenReturn(
                new CitaResponse(1L, 1L, "1234ABC", "COCHE", "Cliente Test",
                        LocalDateTime.of(2026, 8, 1, 10, 0), "Revisión", "PENDIENTE", null));

        mockMvc.perform(post("/api/citas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearSinFechaDevuelveDatosNoValidos() throws Exception {
        String cuerpoInvalido = "{\"idVehiculo\":1}";

        mockMvc.perform(post("/api/citas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void convertirEnlazaLaCitaConLaOrden() throws Exception {
        when(citaService.convertir(1L, 42L)).thenReturn(
                new CitaResponse(1L, 1L, "1234ABC", "COCHE", "Cliente Test",
                        LocalDateTime.now(), "Revisión", "COMPLETADA", 42L));

        mockMvc.perform(put("/api/citas/1/convertir").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idOrden\":42}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void unMecanicoNoPuedeBorrarCitas() throws Exception {
        mockMvc.perform(delete("/api/citas/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminSiPuedeBorrarCitas() throws Exception {
        mockMvc.perform(delete("/api/citas/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(citaService).eliminar(eq(1L));
    }
}
