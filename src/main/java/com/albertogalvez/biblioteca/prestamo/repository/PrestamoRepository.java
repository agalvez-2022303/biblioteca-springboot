package com.albertogalvez.biblioteca.prestamo.repository;

import com.albertogalvez.biblioteca.prestamo.entity.EstadoPrestamo;
import com.albertogalvez.biblioteca.prestamo.entity.PrestamoLibro;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<PrestamoLibro, Long> {

    @EntityGraph(attributePaths = {"libro", "usuario"})
    List<PrestamoLibro> findByUsuario_UsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    List<PrestamoLibro> findByUsuario_UsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    long countByUsuario_UsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    long countByUsuario_UsuarioIdAndEstadoNot(Long usuarioId, EstadoPrestamo estado);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    List<PrestamoLibro> findByFechaDevolucionEsperadaBeforeAndEstadoNot(LocalDate fecha, EstadoPrestamo estado);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    Page<PrestamoLibro> findByFechaDevolucionEsperadaBeforeAndEstadoIn(LocalDate fecha, List<EstadoPrestamo> estados, Pageable pageable);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    Page<PrestamoLibro> findByUsuario_UsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado, Pageable pageable);

    @EntityGraph(attributePaths = {"libro", "usuario"})
    Page<PrestamoLibro> findByUsuario_UsuarioId(Long usuarioId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrestamoLibro p where p.idPrestamo = :id")
    Optional<PrestamoLibro> findByIdConBloqueo(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PrestamoLibro p SET p.estado = :atrasado WHERE p.estado = :activo AND p.fechaDevolucionEsperada < :hoy")
    int marcarAtrasados(@Param("atrasado") EstadoPrestamo atrasado,
                        @Param("activo") EstadoPrestamo activo,
                        @Param("hoy") LocalDate hoy);
}
