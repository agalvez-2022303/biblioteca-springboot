package com.albertogalvez.biblioteca.autenticacion.service;

import com.albertogalvez.biblioteca.autenticacion.dto.LoginRequest;
import com.albertogalvez.biblioteca.autenticacion.dto.LoginResponse;
import com.albertogalvez.biblioteca.autenticacion.dto.RegisterRequest;
import com.albertogalvez.biblioteca.autenticacion.dto.RegisterResponse;
import com.albertogalvez.biblioteca.autenticacion.security.JwtService;
import com.albertogalvez.biblioteca.comun.exception.BusinessRuleException;
import com.albertogalvez.biblioteca.comun.exception.ResourceNotFoundException;
import com.albertogalvez.biblioteca.comun.exception.UnauthorizedException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioValido;

    @BeforeEach
    void setUp() {
        usuarioValido = new Usuario();
        usuarioValido.setUsuarioId(1L);
        usuarioValido.setEmail("test@test.com");
        usuarioValido.setPassword("$2a$10$hashedPassword");
        usuarioValido.setEstado(EstadoUsuario.ACTIVO);
        usuarioValido.setRol(Rol.LECTOR);
    }

    // --- REGISTER ---

    @Test
    void registrar_deberiaCrearUsuario_conDatosValidos() {
        RegisterRequest request = new RegisterRequest("nuevo@test.com", "password123");
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setUsuarioId(1L);
            return u;
        });

        RegisterResponse response = authService.registrar(request);

        assertThat(response.usuarioId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("nuevo@test.com");
        assertThat(response.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(response.rol()).isEqualTo(Rol.LECTOR);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(captor.getValue().getRol()).isEqualTo(Rol.LECTOR);
    }

    @Test
    void registrar_deberiaLanzarExcepcion_siRequestEsNull() {
        assertThatThrownBy(() -> authService.registrar(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("obligatoria");
    }

    @Test
    void registrar_deberiaLanzarExcepcion_siEmailVacio() {
        RegisterRequest request = new RegisterRequest("", "password123");
        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email");
    }

    @Test
    void registrar_deberiaLanzarExcepcion_siPasswordVacia() {
        RegisterRequest request = new RegisterRequest("test@test.com", "");
        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("password");
    }

    @Test
    void registrar_deberiaLanzarExcepcion_siEmailDuplicado() {
        RegisterRequest request = new RegisterRequest("existente@test.com", "password123");
        when(usuarioRepository.existsByEmail("existente@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya está registrado");
    }

    // --- LOGIN ---

    @Test
    void login_deberiaRetornarToken_conCredencialesValidas() {
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuarioValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generarToken(usuarioValido)).thenReturn("jwt-token-123");

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token-123");
        assertThat(response.tipoToken()).isEqualTo("Bearer");
        assertThat(response.usuarioId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.rol()).isEqualTo(Rol.LECTOR);
    }

    @Test
    void login_deberiaLanzarExcepcion_siRequestEsNull() {
        assertThatThrownBy(() -> authService.login(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("obligatoria");
    }

    @Test
    void login_deberiaLanzarExcepcion_siEmailVacio() {
        LoginRequest request = new LoginRequest("", "password123");
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email");
    }

    @Test
    void login_deberiaLanzarExcepcion_siPasswordVacia() {
        LoginRequest request = new LoginRequest("test@test.com", "");
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("password");
    }

    @Test
    void login_deberiaLanzarExcepcion_siUsuarioNoExiste() {
        LoginRequest request = new LoginRequest("noexiste@test.com", "password123");
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    void login_deberiaLanzarExcepcion_siPasswordIncorrecta() {
        LoginRequest request = new LoginRequest("test@test.com", "wrongPassword");
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuarioValido));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    void login_deberiaLanzarExcepcion_siUsuarioSancionado() {
        usuarioValido.setEstado(EstadoUsuario.SANCIONADO);
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuarioValido));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("sancionado");
    }
}