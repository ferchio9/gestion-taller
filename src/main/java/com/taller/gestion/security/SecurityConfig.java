package com.taller.gestion.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Autenticacion minima: un unico usuario admin, login por sesion. Protege toda la
// aplicacion (paginas HTML y API) salvo la propia pagina de login y los recursos estaticos.
//
// Dos cadenas de seguridad porque /api/** y las paginas necesitan reaccionar distinto
// cuando no hay sesion: la API debe devolver 401 (la lee fetch()), las paginas deben
// redirigir a /login.html. Con una unica cadena, formLogin() registra su propio
// AuthenticationEntryPoint con prioridad y "gana" siempre, ignorando el 401 para la API.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        // El portal de seguimiento es publico: el codigo UUID de la orden
                        // hace de "contraseña" (quien lo tiene, puede verlo y responder al presupuesto).
                        .requestMatchers("/api/seguimiento/**").permitAll()
                        // Solo ADMIN: borrados, gestion de usuarios y catalogo de precios.
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**", "/api/vehiculos/**",
                                "/api/ordenes/**", "/api/citas/**")
                            .hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/servicios/**").authenticated()
                        .requestMatchers("/api/servicios/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Handler "plano" (no XOR/BREACH): el frontend lee el valor tal cual de la
                // cookie XSRF-TOKEN y lo reenvia, asi que debe coincidir sin enmascarar.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(csrfCookieFilter(), BasicAuthenticationFilter.class)
                // setStatus() y no sendError(): sendError() provoca un forward interno a
                // /error, que vuelve a pasar por el filtro de seguridad (esta vez por la
                // cadena web) y termina redirigiendo a /login.html en lugar de dar 401/403.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> response.setStatus(HttpServletResponse.SC_FORBIDDEN)));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login.html", "/seguimiento.html", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/index.html", true)
                        .failureUrl("/login.html?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.html")
                        .permitAll())
                // Handler "plano" (no XOR/BREACH): el frontend lee el valor tal cual de la
                // cookie XSRF-TOKEN y lo reenvia, asi que debe coincidir sin enmascarar.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(csrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    // Fuerza a resolver el token CSRF diferido en cada peticion para que la cookie
    // XSRF-TOKEN se escriba siempre (si no, solo se escribe la primera vez que algo la lee).
    private OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                             FilterChain filterChain) throws ServletException, IOException {
                Object token = request.getAttribute(CsrfToken.class.getName());
                if (token instanceof CsrfToken csrfToken) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
