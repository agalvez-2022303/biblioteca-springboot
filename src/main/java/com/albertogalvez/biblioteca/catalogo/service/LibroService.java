package com.albertogalvez.biblioteca.catalogo.service;

import com.albertogalvez.biblioteca.catalogo.dto.LibroRequest;
import com.albertogalvez.biblioteca.catalogo.dto.LibroResponse;
import com.albertogalvez.biblioteca.catalogo.entity.Libro;
import com.albertogalvez.biblioteca.catalogo.repository.LibroRepository;
import com.albertogalvez.biblioteca.comun.exception.BusinessRuleException;
import com.albertogalvez.biblioteca.comun.exception.ResourceNotFoundException;
import com.albertogalvez.biblioteca.comun.validacion.Validaciones;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibroService {

    private final LibroRepository libroRepository;

    public LibroService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    @Transactional(readOnly = true)
    public Page<LibroResponse> listar(String titulo, String categoria, Pageable paginable) {
        Page<Libro> pagina;
        boolean hayTitulo = titulo != null && !titulo.isBlank();
        boolean hayCategoria = categoria != null && !categoria.isBlank();

        if (hayTitulo && hayCategoria) {
            pagina = libroRepository.findByActivoTrueAndTituloContainingIgnoreCaseAndCategoriaIgnoreCase(titulo, categoria, paginable);
        } else if (hayTitulo) {
            pagina = libroRepository.findByActivoTrueAndTituloContainingIgnoreCase(titulo, paginable);
        } else if (hayCategoria) {
            pagina = libroRepository.findByActivoTrueAndCategoriaIgnoreCase(categoria, paginable);
        } else {
            pagina = libroRepository.findByActivoTrue(paginable);
        }
        return pagina.map(this::aRespuesta);
    }

    @Transactional(readOnly = true)
    public LibroResponse obtenerPorId(Long id) {
        if (id == null) {
            throw new BusinessRuleException("El ID del libro es obligatorio");
        }
        Libro libro = libroRepository.findById(id)
                .filter(l -> Boolean.TRUE.equals(l.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));
        return aRespuesta(libro);
    }

    @Transactional
    public LibroResponse crear(LibroRequest request) {
        validar(request);
        Libro libro = new Libro();
        libro.setTitulo(request.titulo());
        libro.setAutor(request.autor());
        libro.setCategoria(request.categoria());
        libro.setStockTotal(request.stockTotal());
        libro.setStockDisponible(request.stockTotal());
        libro.setActivo(true);
        return aRespuesta(libroRepository.save(libro));
    }

    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest request) {
        if (id == null) {
            throw new BusinessRuleException("El ID del libro es obligatorio");
        }
        validar(request);
        Libro libro = libroRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));

        int prestados = libro.getStockTotal() - libro.getStockDisponible();
        if (request.stockTotal() < prestados) {
            throw new BusinessRuleException("El stock total no puede ser menor a los ejemplares prestados (" + prestados + ")");
        }

        libro.setTitulo(request.titulo());
        libro.setAutor(request.autor());
        libro.setCategoria(request.categoria());
        libro.setStockTotal(request.stockTotal());
        libro.setStockDisponible(request.stockTotal() - prestados);
        return aRespuesta(libroRepository.save(libro));
    }

    @Transactional
    public void eliminar(Long id) {
        if (id == null) {
            throw new BusinessRuleException("El ID del libro es obligatorio");
        }
        Libro libro = libroRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));
        libro.setActivo(false);
        libroRepository.save(libro);
    }

    private void validar(LibroRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La solicitud es obligatoria");
        }
        Validaciones.texto(request.titulo(), "El título es obligatorio", "El título no puede superar", 200);
        Validaciones.texto(request.autor(), "El autor es obligatorio", "El autor no puede superar", 150);
        Validaciones.texto(request.categoria(), "La categoría es obligatoria", "La categoría no puede superar", 100);
        if (request.stockTotal() == null) {
            throw new BusinessRuleException("El stock total es obligatorio");
        }
        if (request.stockTotal() < 0) {
            throw new BusinessRuleException("El stock total no puede ser negativo");
        }
    }

    private LibroResponse aRespuesta(Libro libro) {
        return new LibroResponse(libro.getLibroId(), libro.getTitulo(), libro.getAutor(),
                libro.getCategoria(), libro.getStockTotal(), libro.getStockDisponible(), libro.getActivo());
    }
}
