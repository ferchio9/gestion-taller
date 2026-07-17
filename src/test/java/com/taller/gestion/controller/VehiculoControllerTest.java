package com.taller.gestion.controller;

import com.taller.gestion.dto.VehiculoResponse;
import com.taller.gestion.security.SecurityConfig;
import com.taller.gestion.service.VehiculoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehiculoController.class)
@Import(SecurityConfig.class)
class VehiculoControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private VehiculoService vehiculoService;

    @BeforeEach
    void construirMockMvcConSeguridad() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static final String CUERPO_VALIDO =
            "{\"matricula\":\"1234ABC\",\"marca\":\"Seat\",\"modelo\":\"Ibiza\",\"tipo\":\"COCHE\",\"idCliente\":1}";

    @Test
    void sinAutenticarRecibeNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/vehiculos")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearConDatosValidosDevuelveCreado() throws Exception {
        when(vehiculoService.crear(any())).thenReturn(
                new VehiculoResponse(1L, "1234ABC", "Seat", "Ibiza", null, null, null, "COCHE", 1L, "Cliente Test"));

        mockMvc.perform(post("/api/vehiculos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matricula").value("1234ABC"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearConTipoInvalidoDevuelveDatosNoValidos() throws Exception {
        String cuerpoInvalido =
                "{\"matricula\":\"1234ABC\",\"marca\":\"Seat\",\"modelo\":\"Ibiza\",\"tipo\":\"AVION\",\"idCliente\":1}";

        mockMvc.perform(post("/api/vehiculos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void unMecanicoNoPuedeBorrarVehiculos() throws Exception {
        mockMvc.perform(delete("/api/vehiculos/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminSiPuedeBorrarVehiculos() throws Exception {
        mockMvc.perform(delete("/api/vehiculos/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(vehiculoService).eliminar(eq(1L));
    }
}
