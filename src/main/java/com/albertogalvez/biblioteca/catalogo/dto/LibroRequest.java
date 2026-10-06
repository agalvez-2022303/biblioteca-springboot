package com.albertogalvez.biblioteca.catalogo.dto;

public record LibroRequest(String titulo, String autor, String categoria, Integer stockTotal) {
}
