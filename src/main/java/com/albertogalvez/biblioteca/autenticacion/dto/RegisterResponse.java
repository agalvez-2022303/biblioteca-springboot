package com.albertogalvez.biblioteca.autenticacion.dto;

import com.albertogalvez.biblioteca.usuario.entity.EstadoUsuario;
import com.albertogalvez.biblioteca.usuario.entity.Rol;

public record RegisterResponse(Long usuarioId, String email, EstadoUsuario estado, Rol rol) {
}
