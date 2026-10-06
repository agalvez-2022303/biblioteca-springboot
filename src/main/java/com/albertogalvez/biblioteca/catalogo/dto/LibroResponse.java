package com.albertogalvez.biblioteca.catalogo.dto;

public record LibroResponse(Long libroId, String titulo, String autor, String categoria, Integer stockTotal, Integer stockDisponible, Boolean activo) {
}
