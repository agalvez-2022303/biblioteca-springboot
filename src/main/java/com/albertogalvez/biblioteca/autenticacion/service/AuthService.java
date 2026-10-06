package com.albertogalvez.biblioteca.autenticacion.service;

import com.albertogalvez.biblioteca.autenticacion.dto.LoginRequest;
import com.albertogalvez.biblioteca.autenticacion.dto.LoginResponse;
import com.albertogalvez.biblioteca.autenticacion.dto.RegisterRequest;
import com.albertogalvez.biblioteca.autenticacion.dto.RegisterResponse;
import com.albertogalvez.biblioteca.autenticacion.security.JwtService;
import com.albertogalvez.biblioteca.comun.exception.BusinessRuleException;
import com.albertogalvez.biblioteca.comun.exception.UnauthorizedException;
import com.albertogalvez.biblioteca.usuario.entity.EstadoUsuario;
import com.albertogalvez.biblioteca.usuario.entity.Rol;
import com.albertogalvez.biblioteca.usuario.entity.Usuario;
import com.albertogalvez.biblioteca.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResponse registrar(RegisterRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La solicitud es obligatoria");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new BusinessRuleException("El email es obligatorio");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessRuleException("La password es obligatoria");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRol(Rol.LECTOR);

        Usuario guardado = usuarioRepository.save(usuario);
        return new RegisterResponse(guardado.getUsuarioId(), guardado.getEmail(), guardado.getEstado(), guardado.getRol());
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La solicitud es obligatoria");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new BusinessRuleException("El email es obligatorio");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessRuleException("La password es obligatoria");
        }

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new UnauthorizedException("El usuario está sancionado");
        }

        String token = jwtService.generarToken(usuario);
        return new LoginResponse(token, "Bearer", usuario.getUsuarioId(), usuario.getEmail(), usuario.getRol());
    }
}
