package com.albertogalvez.biblioteca.autenticacion.dto;

import com.albertogalvez.biblioteca.usuario.entity.Rol;

public record LoginResponse(String token, String tipoToken, Long usuarioId, String email, Rol rol) {
}
