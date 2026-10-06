package com.albertogalvez.biblioteca.usuario.controller;

import com.albertogalvez.biblioteca.usuario.dto.EstadoUsuarioRequest;
import com.albertogalvez.biblioteca.usuario.dto.RolUsuarioRequest;
import com.albertogalvez.biblioteca.usuario.dto.UsuarioRequest;
import com.albertogalvez.biblioteca.usuario.dto.UsuarioResponse;
import com.albertogalvez.biblioteca.usuario.entity.EstadoUsuario;
import com.albertogalvez.biblioteca.usuario.entity.Rol;
import com.albertogalvez.biblioteca.usuario.service.UsuarioService;
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
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) EstadoUsuario estado,
            @PageableDefault(sort = "usuarioId", direction = Sort.Direction.ASC) Pageable paginable) {
        return ResponseEntity.ok(usuarioService.listar(rol, estado, paginable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable Long id, @RequestBody EstadoUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request, emailAutenticado()));
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id, @RequestBody RolUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request, emailAutenticado()));
    }

    private String emailAutenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion == null ? null : autenticacion.getName();
    }
}
