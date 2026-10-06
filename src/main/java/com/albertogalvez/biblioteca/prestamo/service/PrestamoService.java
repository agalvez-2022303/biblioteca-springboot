package com.albertogalvez.biblioteca.prestamo.service;

import com.albertogalvez.biblioteca.catalogo.entity.Libro;
import com.albertogalvez.biblioteca.catalogo.repository.LibroRepository;
import com.albertogalvez.biblioteca.comun.exception.BusinessRuleException;
import com.albertogalvez.biblioteca.comun.exception.ResourceNotFoundException;
import com.albertogalvez.biblioteca.prestamo.dto.PrestamoRequest;
import com.albertogalvez.biblioteca.prestamo.dto.PrestamoResponse;
import com.albertogalvez.biblioteca.prestamo.entity.EstadoPrestamo;
import com.albertogalvez.biblioteca.prestamo.entity.PrestamoLibro;
import com.albertogalvez.biblioteca.prestamo.repository.PrestamoRepository;
import com.albertogalvez.biblioteca.usuario.entity.EstadoUsuario;
import com.albertogalvez.biblioteca.usuario.entity.Usuario;
import com.albertogalvez.biblioteca.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;

    public PrestamoService(PrestamoRepository prestamoRepository, UsuarioRepository usuarioRepository, LibroRepository libroRepository) {
        this.prestamoRepository = prestamoRepository;
        this.usuarioRepository = usuarioRepository;
        this.libroRepository = libroRepository;
    }

    @Transactional
    public PrestamoResponse crear(PrestamoRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La solicitud es obligatoria");
        }
        if (request.usuarioId() == null) {
            throw new BusinessRuleException("El usuarioId es obligatorio");
        }
        if (request.libroId() == null) {
            throw new BusinessRuleException("El libroId es obligatorio");
        }
        if (request.fechaDevolucionEsperada() == null) {
            throw new BusinessRuleException("La fechaDevolucionEsperada es obligatoria");
        }
        if (!request.fechaDevolucionEsperada().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fechaDevolucionEsperada debe ser futura");
        }

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + request.usuarioId()));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessRuleException("El usuario no está activo");
        }

        long activos = prestamoRepository.countByUsuario_UsuarioIdAndEstado(usuario.getUsuarioId(), EstadoPrestamo.ACTIVO);
        if (activos >= 3) {
            throw new BusinessRuleException("El usuario ya tiene 3 préstamos activos");
        }

        Libro libro = libroRepository.findByIdConBloqueo(request.libroId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + request.libroId()));
        if (!Boolean.TRUE.equals(libro.getActivo())) {
            throw new BusinessRuleException("El libro no está activo");
        }
        if (libro.getStockDisponible() == null || libro.getStockDisponible() <= 0) {
            throw new BusinessRuleException("No hay stock disponible del libro");
        }

        PrestamoLibro prestamo = new PrestamoLibro();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setFechaDevolucionEsperada(request.fechaDevolucionEsperada());
        prestamo.setFechaDevolucionReal(null);
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        libro.setStockDisponible(libro.getStockDisponible() - 1);
        libroRepository.save(libro);
        PrestamoLibro guardado = prestamoRepository.save(prestamo);

        return aRespuesta(guardado);
    }

    private PrestamoResponse aRespuesta(PrestamoLibro p) {
        return new PrestamoResponse(p.getIdPrestamo(), p.getUsuario().getUsuarioId(), p.getLibro().getLibroId(),
                p.getFechaPrestamo(), p.getFechaDevolucionEsperada(), p.getFechaDevolucionReal(), p.getEstado());
    }
}
