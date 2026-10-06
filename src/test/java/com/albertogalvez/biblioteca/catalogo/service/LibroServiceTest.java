package com.albertogalvez.biblioteca.catalogo.service;

import com.albertogalvez.biblioteca.catalogo.dto.LibroRequest;
import com.albertogalvez.biblioteca.catalogo.dto.LibroResponse;
import com.albertogalvez.biblioteca.catalogo.entity.Libro;
import com.albertogalvez.biblioteca.catalogo.repository.LibroRepository;
import com.albertogalvez.biblioteca.comun.exception.BusinessRuleException;
import com.albertogalvez.biblioteca.comun.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock
    private LibroRepository libroRepository;

    @InjectMocks
    private LibroService libroService;

    private Libro libroExistente;

    @BeforeEach
    void setUp() {
        libroExistente = new Libro();
        libroExistente.setLibroId(1L);
        libroExistente.setTitulo("Test Libro");
        libroExistente.setAutor("Test Autor");
        libroExistente.setCategoria("Test Categoria");
        libroExistente.setStockTotal(5);
        libroExistente.setStockDisponible(3);
        libroExistente.setActivo(true);
    }

    // --- CREAR ---

    @Test
    void crear_deberiaGuardarLibro_conDatosValidos() {
        LibroRequest request = new LibroRequest("Nuevo Libro", "Nuevo Autor", "Nueva Categoria", 10);
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> {
            Libro l = inv.getArgument(0);
            l.setLibroId(1L);
            return l;
        });

        LibroResponse response = libroService.crear(request);

        assertThat(response.libroId()).isEqualTo(1L);
        assertThat(response.titulo()).isEqualTo("Nuevo Libro");
        assertThat(response.stockTotal()).isEqualTo(10);
        assertThat(response.stockDisponible()).isEqualTo(10);
        assertThat(response.activo()).isTrue();

        ArgumentCaptor<Libro> captor = ArgumentCaptor.forClass(Libro.class);
        verify(libroRepository).save(captor.capture());
        assertThat(captor.getValue().getStockDisponible()).isEqualTo(10);
    }

    @Test
    void crear_deberiaLanzarExcepcion_siTituloVacio() {
        LibroRequest request = new LibroRequest("", "Autor", "Categoria", 5);
        assertThatThrownBy(() -> libroService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("título");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siAutorVacio() {
        LibroRequest request = new LibroRequest("Titulo", "", "Categoria", 5);
        assertThatThrownBy(() -> libroService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("autor");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siCategoriaVacia() {
        LibroRequest request = new LibroRequest("Titulo", "Autor", "", 5);
        assertThatThrownBy(() -> libroService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("categoría");
    }

    @Test
    void crear_deberiaLanzarExcepcion_siStockTotalNegativo() {
        LibroRequest request = new LibroRequest("Titulo", "Autor", "Categoria", -1);
        assertThatThrownBy(() -> libroService.crear(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("stock total");
    }

    // --- ACTUALIZAR ---

    @Test
    void actualizar_deberiaModificarLibro_conDatosValidos() {
        LibroRequest request = new LibroRequest("Actualizado", "Autor Actualizado", "Categoria Actualizada", 20);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        LibroResponse response = libroService.actualizar(1L, request);

        assertThat(response.titulo()).isEqualTo("Actualizado");
        assertThat(response.stockTotal()).isEqualTo(20);
        assertThat(response.stockDisponible()).isEqualTo(3);
    }

    @Test
    void actualizar_deberiaLanzarExcepcion_siStockTotalMenorADisponible() {
        libroExistente.setStockDisponible(5);
        LibroRequest request = new LibroRequest("Titulo", "Autor", "Categoria", 3);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));

        assertThatThrownBy(() -> libroService.actualizar(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("stock total no puede ser menor");
    }

    @Test
    void actualizar_deberiaLanzarExcepcion_siLibroNoExiste() {
        LibroRequest request = new LibroRequest("Titulo", "Autor", "Categoria", 5);
        when(libroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.actualizar(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

    // --- ELIMINAR ---

    @Test
    void eliminar_deberiaDesactivarLibro() {
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        libroService.eliminar(1L);

        ArgumentCaptor<Libro> captor = ArgumentCaptor.forClass(Libro.class);
        verify(libroRepository).save(captor.capture());
        assertThat(captor.getValue().getActivo()).isFalse();
    }

    @Test
    void eliminar_deberiaLanzarExcepcion_siLibroNoExiste() {
        when(libroRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> libroService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- LISTAR / OBTENER ---

    @Test
    void obtenerPorId_deberiaRetornarLibro() {
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        LibroResponse response = libroService.obtenerPorId(1L);
        assertThat(response.titulo()).isEqualTo("Test Libro");
    }

    @Test
    void obtenerPorId_deberiaLanzarExcepcion_siNoExiste() {
        when(libroRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> libroService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listar_deberiaRetornarPagina() {
        Page<Libro> pagina = new PageImpl<>(List.of(libroExistente));
        when(libroRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        Page<LibroResponse> resultado = libroService.listar(null, null, Pageable.unpaged());

        assertThat(resultado.getContent()).hasSize(1);
    }
}