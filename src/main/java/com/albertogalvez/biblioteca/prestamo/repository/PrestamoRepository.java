package com.albertogalvez.biblioteca.prestamo.repository;

import com.albertogalvez.biblioteca.prestamo.entity.EstadoPrestamo;
import com.albertogalvez.biblioteca.prestamo.entity.PrestamoLibro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<PrestamoLibro, Long> {

    @EntityGraph(attributePaths = {"libro", "usuario"})
    List<PrestamoLibro> findByUsuario_UsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    List<PrestamoLibro> findByUsuario_UsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    long countByUsuario_UsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    List<PrestamoLibro> findByFechaDevolucionEsperadaBeforeAndEstadoNot(LocalDate fecha, EstadoPrestamo estado);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    Page<PrestamoLibro> findByUsuario_UsuarioId(Long usuarioId, Pageable pageable);
}
