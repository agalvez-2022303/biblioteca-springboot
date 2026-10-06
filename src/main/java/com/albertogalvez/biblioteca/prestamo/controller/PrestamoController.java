package com.albertogalvez.biblioteca.prestamo.controller;

import com.albertogalvez.biblioteca.prestamo.dto.PrestamoRequest;
import com.albertogalvez.biblioteca.prestamo.dto.PrestamoResponse;
import com.albertogalvez.biblioteca.prestamo.service.PrestamoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @PostMapping
    public ResponseEntity<PrestamoResponse> crear(@RequestBody PrestamoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamoService.crear(request));
    }

    @GetMapping("/mis-prestamos")
    public ResponseEntity<java.util.List<PrestamoResponse>> misPrestamos() {
        org.springframework.security.core.Authentication autenticacion = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = autenticacion == null ? null : autenticacion.getName();
        return ResponseEntity.ok(prestamoService.misPrestamos(email));
    }

    @GetMapping("/atrasados")
    public ResponseEntity<java.util.List<PrestamoResponse>> atrasados() {
        return ResponseEntity.ok(prestamoService.atrasados());
    }

    @PatchMapping("/{id}/devolucion")
    public ResponseEntity<PrestamoResponse> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolver(id));
    }
}
