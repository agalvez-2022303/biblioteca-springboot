package com.albertogalvez.biblioteca.prestamo.dto;

import com.albertogalvez.biblioteca.prestamo.entity.EstadoPrestamo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrestamoResponse(Long idPrestamo, Long usuarioId, Long libroId, LocalDate fechaPrestamo, LocalDate fechaDevolucionEsperada, LocalDateTime fechaDevolucionReal, EstadoPrestamo estado) {
}
