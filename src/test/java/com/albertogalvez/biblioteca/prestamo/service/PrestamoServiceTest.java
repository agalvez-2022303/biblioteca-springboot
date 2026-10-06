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
import com.albertogalvez.biblioteca.usuario.entity.Rol;
import com.albertogalvez.biblioteca.usuario.entity.Usuario;
import com.albertogalvez.biblioteca.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LibroRepository libroRepository;

    @InjectMocks
    private PrestamoService prestamoService;

    private Usuario usuarioActivo;
    private Libro libroConStock;

    @BeforeEach
    void setUp() {
        usuarioActivo = new Usuario();
        usuarioActivo.setUsuarioId(1L);
        usuarioActivo.setEmail("usuario@test.com");
        usuarioActivo.setPassword("hash");
        usuarioActivo.setEstado(EstadoUsuario.ACTIVO);
        usuarioActivo.setRol(Rol.LECTOR);

        libroConStock = new Libro();
        libroConStock.setLibroId(10L);
        libroConStock.setTitulo("Libro Test");
        libroConStock.setAutor("Autor");
        libroConStock.setCategoria("Categoria");
        libroConStock.setStockTotal(5);
        libroConStock.setStockDisponible(3);
        libroConStock.setActivo(true);
    }

    // --- CREAR PRÉSTAMO ---

    @Test
    void crear_deberiaCrearPrestamo_conDatosValidos() {
        PrestamoRequest request = new PrestamoRequest(1L, 10L, LocalDate.now().plusDays(7));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(prestamoRepository.countByUsuario_UsuarioIdAndEstado(1L, EstadoPrestamo.ACTIVO)).thenReturn(0L);
        when(libroRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(libroConStock));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(prestamoRepository.save(any(PrestamoLibro.class))).thenAnswer(inv -> {
            PrestamoLibro p = inv.getArgument(0);
            p.setIdPrestamo(1L);
            return p;
        });

        PrestamoResponse response = prestamoService.crear(request);

        assertThat(response.idPrestamo()).isEqualTo(1L);
        assertThat(response.usuarioId()).isEqualTo(1L);
        assertThat(response.libroId()).isEqualTo(10L);
        assertThat(response.estado()).isEqualTo(EstadoPrestamo.ACTIVO);

        ArgumentCaptor<Libro> libroCaptor = ArgumentCaptor.forClass(Libro.class);
        verify(libroRepository).save(libroCaptor.capture());
        assertThat(libroCaptor.getValue().getStockDisponible()).isEqualTo(2);
    }

    @Test
    void crear_deberiaLanzarExcepcion_siUsuarioIdNull() {
        PrestamoRequest request = new PrestamoRequest(null, 10L, LocalDate.now().plusDays(7));
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("usuarioId");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siLibroIdNull() {
        PrestamoRequest request = new PrestamoRequest(1L, null, LocalDate.now().plusDays(7));
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("libroId");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siFechaNull() {
        PrestamoRequest request = new PrestamoRequest(1L, 10L, null);
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fechaDevolucionEsperada");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siFechaPasada() {
        PrestamoRequest request = new PrestamoRequest(1L, 10L, LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("futura");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siUsuarioNoExiste() {
        PrestamoRequest request = new PrestamoRequest(99L, 10L, LocalDate.now().plusDays(7));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_deberiaLanzarExcepcion_siUsuarioSancionado() {
        usuarioActivo.setEstado(EstadoUsuario.SANCIONADO);
        PrestamoRequest request = new PrestamoRequest(1L, 10L, LocalDate.now().plusDays(7));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("activo");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siLibroNoExiste() {
        PrestamoRequest request = new PrestamoRequest(1L, 999L, LocalDate.now().plusDays(7));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(prestamoRepository.countByUsuario_UsuarioIdAndEstado(1L, EstadoPrestamo.ACTIVO)).thenReturn(0L);
        when(libroRepository.findByIdConBloqueo(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Libro no encontrado");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siLibroInactivo() {
        libroConStock.setActivo(false);
        PrestamoRequest request = new PrestamoRequest(1L, 10L, LocalDate.now().plusDays(7));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(prestamoRepository.countByUsuario_UsuarioIdAndEstado(1L, EstadoPrestamo.ACTIVO)).thenReturn(0L);
        when(libroRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(libroConStock));
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está activo");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siStockAgotado() {
        libroConStock.setStockDisponible(0);
        PrestamoRequest request = new PrestamoRequest(1L, 10L, LocalDate.now().plusDays(7));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(prestamoRepository.countByUsuario_UsuarioIdAndEstado(1L, EstadoPrestamo.ACTIVO)).thenReturn(0L);
        when(libroRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(libroConStock));
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("stock disponible");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siUsuarioTiene3PrestamosActivos() {
        PrestamoRequest request = new PrestamoRequest(1L, 10L, LocalDate.now().plusDays(7));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo));
        when(prestamoRepository.countByUsuario_UsuarioIdAndEstado(1L, EstadoPrestamo.ACTIVO)).thenReturn(3L);
        assertThatThrownBy(() -> prestamoService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 préstamos activos");
    }

    // --- DEVOLVER ---

    @Test
    void devolver_deberiaMarcarComoDevuelto_eIncrementarStock() {
        PrestamoLibro prestamo = new PrestamoLibro();
        prestamo.setIdPrestamo(1L);
        prestamo.setUsuario(usuarioActivo);
        prestamo.setLibro(libroConStock);
        prestamo.setFechaPrestamo(LocalDate.now().minusDays(5));
        prestamo.setFechaDevolucionEsperada(LocalDate.now().plusDays(2));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(libroRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(libroConStock));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(prestamoRepository.save(any(PrestamoLibro.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoResponse response = prestamoService.devolver(1L);

        assertThat(response.estado()).isEqualTo(EstadoPrestamo.DEVUELTO);
        assertThat(response.fechaDevolucionReal()).isEqualTo(LocalDate.now());

        ArgumentCaptor<Libro> libroCaptor = ArgumentCaptor.forClass(Libro.class);
        verify(libroRepository).save(libroCaptor.capture());
        assertThat(libroCaptor.getValue().getStockDisponible()).isEqualTo(4);
    }

    @Test
    void devolver_deberiaLanzarExcepcion_siIdNull() {
        assertThatThrownBy(() -> prestamoService.devolver(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void devolver_deberiaLanzarExcepcion_siPrestamoNoExiste() {
        when(prestamoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> prestamoService.devolver(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void devolver_deberiaLanzarExcepcion_siYaDevuelto() {
        PrestamoLibro prestamo = new PrestamoLibro();
        prestamo.setIdPrestamo(1L);
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setLibro(libroConStock);
        prestamo.setUsuario(usuarioActivo);

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        assertThatThrownBy(() -> prestamoService.devolver(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya fue devuelto");
    }

    @Test
    void devolver_deberiaPermitirDevolverAtrasado() {
        PrestamoLibro prestamo = new PrestamoLibro();
        prestamo.setIdPrestamo(1L);
        prestamo.setEstado(EstadoPrestamo.ATRASADO);
        prestamo.setLibro(libroConStock);
        prestamo.setUsuario(usuarioActivo);

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(libroRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(libroConStock));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(prestamoRepository.save(any(PrestamoLibro.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoResponse response = prestamoService.devolver(1L);
        assertThat(response.estado()).isEqualTo(EstadoPrestamo.DEVUELTO);
    }

    @Test
    void devolver_deberiaLanzarExcepcion_siStockSuperariaTotal() {
        PrestamoLibro prestamo = new PrestamoLibro();
        prestamo.setIdPrestamo(1L);
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo.setLibro(libroConStock);
        prestamo.setUsuario(usuarioActivo);
        libroConStock.setStockDisponible(5);
        libroConStock.setStockTotal(5);

        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(libroRepository.findByIdConBloqueo(10L)).thenReturn(Optional.of(libroConStock));
        assertThatThrownBy(() -> prestamoService.devolver(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no puede superar");
    }
}