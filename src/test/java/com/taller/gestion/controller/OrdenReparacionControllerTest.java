package com.taller.gestion.controller;

import com.taller.gestion.dto.OrdenResponse;
import com.taller.gestion.security.SecurityConfig;
import com.taller.gestion.service.OrdenReparacionService;
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

@WebMvcTest(OrdenReparacionController.class)
@Import(SecurityConfig.class)
class OrdenReparacionControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private OrdenReparacionService ordenService;

    @BeforeEach
    void construirMockMvcConSeguridad() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static final String CUERPO_VALIDO = "{\"idVehiculo\":1,\"descripcionProblema\":\"Ruido en el motor\"}";

    private OrdenResponse ordenDeEjemplo() {
        return new OrdenResponse(1L, 1L, "1234ABC", "COCHE", "RECEPCION", null, null,
                "Ruido en el motor", 50000, BigDecimal.ZERO, List.of(), "codigo-123", null);
    }

    @Test
    void sinAutenticarRecibeNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/ordenes")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearConDatosValidosDevuelveCreado() throws Exception {
        when(ordenService.crear(any())).thenReturn(ordenDeEjemplo());

        mockMvc.perform(post("/api/ordenes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void crearSinIdVehiculoDevuelveDatosNoValidos() throws Exception {
        mockMvc.perform(post("/api/ordenes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void cambiarEstadoConEstadoValidoDevuelveOk() throws Exception {
        when(ordenService.cambiarEstado(eq(1L), eq("DIAGNOSTICO"))).thenReturn(ordenDeEjemplo());

        mockMvc.perform(put("/api/ordenes/1/estado").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"DIAGNOSTICO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void cambiarEstadoConEstadoInvalidoDevuelveDatosNoValidos() throws Exception {
        mockMvc.perform(put("/api/ordenes/1/estado").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void unMecanicoNoPuedeBorrarOrdenes() throws Exception {
        mockMvc.perform(delete("/api/ordenes/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminSiPuedeBorrarOrdenes() throws Exception {
        mockMvc.perform(delete("/api/ordenes/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(ordenService).eliminar(eq(1L));
    }
}
