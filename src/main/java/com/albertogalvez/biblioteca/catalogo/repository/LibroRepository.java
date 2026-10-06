package com.albertogalvez.biblioteca.catalogo.repository;

import com.albertogalvez.biblioteca.catalogo.entity.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    Page<Libro> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    Page<Libro> findByCategoriaIgnoreCase(String categoria, Pageable pageable);

    Page<Libro> findByTituloContainingIgnoreCaseAndCategoriaIgnoreCase(String titulo, String categoria, Pageable pageable);
}
