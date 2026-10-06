package com.albertogalvez.biblioteca.usuario.dto;

import com.albertogalvez.biblioteca.usuario.entity.Rol;

public record UsuarioRequest(String email, String password, Rol rol) {
}
