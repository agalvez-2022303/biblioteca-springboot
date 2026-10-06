package com.albertogalvez.biblioteca.autenticacion.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/libros").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/libros/*").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/libros/*").hasRole("ADMIN")
                .requestMatchers("/api/v1/libros/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/prestamos").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/prestamos/*/devolucion").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/prestamos/atrasados").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/prestamos/mis-prestamos").authenticated()
                .requestMatchers("/api/v1/prestamos/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
