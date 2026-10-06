package com.albertogalvez.biblioteca.autenticacion.controller;

import com.albertogalvez.biblioteca.autenticacion.dto.LoginRequest;
import com.albertogalvez.biblioteca.autenticacion.dto.LoginResponse;
import com.albertogalvez.biblioteca.autenticacion.dto.RegisterRequest;
import com.albertogalvez.biblioteca.autenticacion.dto.RegisterResponse;
import com.albertogalvez.biblioteca.autenticacion.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse respuesta = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse respuesta = authService.login(request);
        return ResponseEntity.ok(respuesta);
    }
}
