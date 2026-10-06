package com.albertogalvez.biblioteca.prestamo.dto;

import java.time.LocalDate;

public record PrestamoRequest(Long usuarioId, Long libroId, LocalDate fechaDevolucionEsperada) {
}
