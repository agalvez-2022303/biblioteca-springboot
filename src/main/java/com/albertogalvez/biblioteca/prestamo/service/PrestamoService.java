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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrestamoService {

    private static final int MAXIMO_PRESTAMOS_PENDIENTES = 3;

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;

    public PrestamoService(PrestamoRepository prestamoRepository, UsuarioRepository usuarioRepository, LibroRepository libroRepository) {
        this.prestamoRepository = prestamoRepository;
        this.usuarioRepository = usuarioRepository;
        this.libroRepository = libroRepository;
    }

    // Orden de bloqueo: usuario -> libro. Bloquear al usuario serializa las solicitudes del mismo
    // lector y evita superar el limite de 3 prestamos con peticiones simultaneas.
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

        Usuario usuario = usuarioRepository.findByIdConBloqueo(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + request.usuarioId()));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessRuleException("El usuario no está activo");
        }

        long pendientes = prestamoRepository.countByUsuario_UsuarioIdAndEstadoNot(usuario.getUsuarioId(), EstadoPrestamo.DEVUELTO);
        if (pendientes >= MAXIMO_PRESTAMOS_PENDIENTES) {
            throw new BusinessRuleException("El usuario ya tiene " + MAXIMO_PRESTAMOS_PENDIENTES + " préstamos sin devolver");
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

    // Orden de bloqueo: prestamo -> libro. Bloquear el prestamo evita que dos devoluciones
    // simultaneas del mismo prestamo incrementen el stock dos veces.
    @Transactional
    public PrestamoResponse devolver(Long idPrestamo) {
        if (idPrestamo == null) {
            throw new BusinessRuleException("El ID del préstamo es obligatorio");
        }

        PrestamoLibro prestamo = prestamoRepository.findByIdConBloqueo(idPrestamo)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + idPrestamo));

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new BusinessRuleException("El préstamo ya fue devuelto");
        }

        Long libroId = prestamo.getLibro().getLibroId();
        Libro libro = libroRepository.findByIdConBloqueo(libroId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + libroId));

        int nuevoStock = libro.getStockDisponible() + 1;
        if (nuevoStock > libro.getStockTotal()) {
            throw new BusinessRuleException("Inconsistencia de stock: el stock disponible no puede superar al total");
        }

        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        libro.setStockDisponible(nuevoStock);

        libroRepository.save(libro);
        return aRespuesta(prestamoRepository.save(prestamo));
    }

    @Transactional(readOnly = true)
    public Page<PrestamoResponse> listar(Long usuarioId, EstadoPrestamo estado, Pageable paginable) {
        return prestamoRepository.buscar(usuarioId, estado, paginable).map(this::aRespuesta);
    }

    @Transactional(readOnly = true)
    public Page<PrestamoResponse> misPrestamos(String email, Pageable paginable, EstadoPrestamo estado) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("El email del usuario autenticado es obligatorio");
        }
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        if (estado != null) {
            return prestamoRepository.findByUsuario_UsuarioIdAndEstado(usuario.getUsuarioId(), estado, paginable)
                    .map(this::aRespuesta);
        }
        return prestamoRepository.findByUsuario_UsuarioId(usuario.getUsuarioId(), paginable)
                .map(this::aRespuesta);
    }

    @Transactional(readOnly = true)
    public Page<PrestamoResponse> atrasados(Pageable paginable) {
        List<EstadoPrestamo> estados = List.of(EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO);
        return prestamoRepository.findByFechaDevolucionEsperadaBeforeAndEstadoIn(LocalDate.now(), estados, paginable)
                .map(this::aRespuesta);
    }

    // El proceso programado persiste ATRASADO cada cierto tiempo; mientras tanto, un prestamo ACTIVO
    // con fecha vencida se informa como ATRASADO para que la respuesta sea siempre coherente.
    private PrestamoResponse aRespuesta(PrestamoLibro p) {
        EstadoPrestamo estado = p.getEstado();
        if (estado == EstadoPrestamo.ACTIVO && p.getFechaDevolucionEsperada().isBefore(LocalDate.now())) {
            estado = EstadoPrestamo.ATRASADO;
        }
        return new PrestamoResponse(p.getIdPrestamo(), p.getUsuario().getUsuarioId(), p.getLibro().getLibroId(),
                p.getFechaPrestamo(), p.getFechaDevolucionEsperada(), p.getFechaDevolucionReal(), estado);
    }
}
