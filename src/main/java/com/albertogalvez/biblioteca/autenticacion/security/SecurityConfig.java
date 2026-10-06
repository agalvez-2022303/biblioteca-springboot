package com.albertogalvez.biblioteca.autenticacion.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    escribirError(response, HttpStatus.UNAUTHORIZED, "Autenticación requerida: token JWT ausente, inválido o expirado"))
                .accessDeniedHandler((request, response, e) ->
                    escribirError(response, HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/libros").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/libros/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/libros/*").hasRole("ADMIN")
                .requestMatchers("/api/v1/libros/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/prestamos").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/prestamos/*/devolucion").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(HttpMethod.GET, "/api/v1/prestamos").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/atrasados").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/mis-prestamos").hasRole("LECTOR")
                .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escribirError(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + estado.value()
                + ",\"error\":\"" + estado.getReasonPhrase() + "\",\"message\":\"" + mensaje + "\"}";
        response.getWriter().write(json);
    }
}
