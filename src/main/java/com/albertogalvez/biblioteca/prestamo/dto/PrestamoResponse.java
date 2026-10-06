package com.albertogalvez.biblioteca.prestamo.dto;

import com.albertogalvez.biblioteca.prestamo.entity.EstadoPrestamo;

import java.time.LocalDate;

public record PrestamoResponse(Long idPrestamo, Long usuarioId, Long libroId, LocalDate fechaPrestamo, LocalDate fechaDevolucionEsperada, LocalDate fechaDevolucionReal, EstadoPrestamo estado) {
}
