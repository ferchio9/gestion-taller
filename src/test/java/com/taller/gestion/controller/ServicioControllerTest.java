package com.taller.gestion.controller;

import com.taller.gestion.dto.ServicioResponse;
import com.taller.gestion.security.SecurityConfig;
import com.taller.gestion.service.ServicioService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServicioController.class)
@Import(SecurityConfig.class)
class ServicioControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ServicioService servicioService;

    @BeforeEach
    void construirMockMvcConSeguridad() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static final String CUERPO_VALIDO =
            "{\"nombre\":\"Cambio de aceite\",\"tipo\":\"MANO_OBRA\",\"precioBase\":30.00}";

    @Test
    void sinAutenticarRecibeNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/servicios")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void unMecanicoPuedeListarElCatalogo() throws Exception {
        when(servicioService.listar()).thenReturn(List.of(
                new ServicioResponse(1L, "Cambio de aceite", "MANO_OBRA", new BigDecimal("30.00"))));

        mockMvc.perform(get("/api/servicios")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void unMecanicoNoPuedeCrearServicios() throws Exception {
        mockMvc.perform(post("/api/servicios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminSiPuedeCrearServicios() throws Exception {
        when(servicioService.crear(any())).thenReturn(
                new ServicioResponse(1L, "Cambio de aceite", "MANO_OBRA", new BigDecimal("30.00")));

        mockMvc.perform(post("/api/servicios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearConTipoInvalidoDevuelveDatosNoValidos() throws Exception {
        String cuerpoInvalido = "{\"nombre\":\"X\",\"tipo\":\"OTRO\",\"precioBase\":10.00}";

        mockMvc.perform(post("/api/servicios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest());
    }
}
