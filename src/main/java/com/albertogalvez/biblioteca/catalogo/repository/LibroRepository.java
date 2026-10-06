package com.albertogalvez.biblioteca.catalogo.repository;

import com.albertogalvez.biblioteca.catalogo.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    Page<Libro> findByActivoTrue(Pageable pageable);

    Page<Libro> findByActivoTrueAndTituloContainingIgnoreCase(String titulo, Pageable pageable);

    Page<Libro> findByActivoTrueAndCategoriaIgnoreCase(String categoria, Pageable pageable);

    Page<Libro> findByActivoTrueAndTituloContainingIgnoreCaseAndCategoriaIgnoreCase(String titulo, String categoria, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.libroId = :id")
    Optional<Libro> findByIdConBloqueo(@Param("id") Long id);
}
