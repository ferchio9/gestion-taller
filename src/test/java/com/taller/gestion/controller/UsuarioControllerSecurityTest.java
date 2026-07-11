package com.taller.gestion.controller;

import com.taller.gestion.dto.UsuarioResponse;
import com.taller.gestion.security.SecurityConfig;
import com.taller.gestion.service.UsuarioService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Comprueba la regla de SecurityConfig que restringe /api/usuarios/** a ADMIN
// (el rol MECANICO puede hacer casi todo salvo esto: borrados, precios y usuarios).
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    // @WithMockUser solo llega al SecurityContextHolder si MockMvc se construye
    // con el configurer de spring-security-test: si no, el SecurityContextHolderFilter
    // de la cadena real lo pisa y toda peticion se ve como anonima (401 en vez de 403/201).
    @BeforeEach
    void construirMockMvcConSeguridad() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static final String CUERPO_VALIDO =
            "{\"username\":\"nuevo\",\"password\":\"password123\",\"rol\":\"MECANICO\"}";

    @Test
    void sinAutenticarRecibeNoAutorizado() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void unMecanicoNoPuedeCrearUsuarios() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminSiPuedeCrearUsuarios() throws Exception {
        when(usuarioService.crear(any())).thenReturn(new UsuarioResponse(1L, "nuevo", "MECANICO"));

        mockMvc.perform(post("/api/usuarios").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated());
    }
}
