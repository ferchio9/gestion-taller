package com.taller.gestion.controller;

import com.taller.gestion.dto.ClienteResponse;
import com.taller.gestion.security.SecurityConfig;
import com.taller.gestion.service.ClienteService;
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

import java.time.LocalDate;

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

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @BeforeEach
    void construirMockMvcConSeguridad() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static final String CUERPO_VALIDO =
            "{\"nombre\":\"Juan Pérez\",\"telefono\":\"600111222\",\"email\":\"juan@test.com\",\"nifCif\":\"12345678A\"}";

    @Test
    void sinAutenticarRecibeNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearConDatosValidosDevuelveCreado() throws Exception {
        when(clienteService.crear(any())).thenReturn(
                new ClienteResponse(1L, "Juan Pérez", "600111222", "juan@test.com", "12345678A", LocalDate.now()));

        mockMvc.perform(post("/api/clientes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"));
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearConNombreVacioDevuelveDatosNoValidos() throws Exception {
        String cuerpoInvalido = "{\"nombre\":\"\"}";

        mockMvc.perform(post("/api/clientes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void unMecanicoNoPuedeBorrarClientes() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminSiPuedeBorrarClientes() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(clienteService).eliminar(eq(1L));
    }
}
