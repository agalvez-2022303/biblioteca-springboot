package com.albertogalvez.biblioteca.prestamo.controller;

import com.albertogalvez.biblioteca.prestamo.dto.PrestamoRequest;
import com.albertogalvez.biblioteca.prestamo.dto.PrestamoResponse;
import com.albertogalvez.biblioteca.prestamo.entity.EstadoPrestamo;
import com.albertogalvez.biblioteca.prestamo.service.PrestamoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ResponseEntity<Page<PrestamoResponse>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) EstadoPrestamo estado,
            @PageableDefault(sort = "idPrestamo", direction = Sort.Direction.DESC) Pageable paginable) {
        return ResponseEntity.ok(prestamoService.listar(usuarioId, estado, paginable));
    }

    @GetMapping("/mis-prestamos")
    public ResponseEntity<Page<PrestamoResponse>> misPrestamos(
            @RequestParam(required = false) EstadoPrestamo estado,
            Pageable paginable) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String email = autenticacion == null ? null : autenticacion.getName();
        return ResponseEntity.ok(prestamoService.misPrestamos(email, paginable, estado));
    }

    @GetMapping("/atrasados")
    public ResponseEntity<Page<PrestamoResponse>> atrasados(Pageable paginable) {
        return ResponseEntity.ok(prestamoService.atrasados(paginable));
    }

    @PatchMapping("/{id}/devolucion")
    public ResponseEntity<PrestamoResponse> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolver(id));
    }
}
