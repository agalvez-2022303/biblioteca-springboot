package com.albertogalvez.biblioteca.prestamo.service;

import com.albertogalvez.biblioteca.prestamo.entity.EstadoPrestamo;
import com.albertogalvez.biblioteca.prestamo.repository.PrestamoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class PrestamoAtrasadoProgramado {

    private final PrestamoRepository prestamoRepository;

    public PrestamoAtrasadoProgramado(PrestamoRepository prestamoRepository) {
        this.prestamoRepository = prestamoRepository;
    }

    // Idempotente: solo actualiza préstamos ACTIVO cuya fecha esperada ya pasó.
    @Scheduled(fixedRateString = "${prestamo.atrasados.cada-ms:60000}")
    @Transactional
    public void marcarAtrasados() {
        prestamoRepository.marcarAtrasados(EstadoPrestamo.ATRASADO, EstadoPrestamo.ACTIVO, LocalDate.now());
    }
}
