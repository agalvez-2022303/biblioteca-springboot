package com.albertogalvez.biblioteca.usuario.service;

import com.albertogalvez.biblioteca.comun.exception.BusinessRuleException;
import com.albertogalvez.biblioteca.comun.exception.ResourceNotFoundException;
import com.albertogalvez.biblioteca.comun.validacion.Validaciones;
import com.albertogalvez.biblioteca.usuario.dto.EstadoUsuarioRequest;
import com.albertogalvez.biblioteca.usuario.dto.RolUsuarioRequest;
import com.albertogalvez.biblioteca.usuario.dto.UsuarioRequest;
import com.albertogalvez.biblioteca.usuario.dto.UsuarioResponse;
import com.albertogalvez.biblioteca.usuario.entity.EstadoUsuario;
import com.albertogalvez.biblioteca.usuario.entity.Rol;
import com.albertogalvez.biblioteca.usuario.entity.Usuario;
import com.albertogalvez.biblioteca.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Rol rol, EstadoUsuario estado, Pageable paginable) {
        return usuarioRepository.buscar(rol, estado, paginable).map(this::aRespuesta);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return aRespuesta(buscarPorId(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request == null) {
            throw new BusinessRuleException("La solicitud es obligatoria");
        }
        Validaciones.email(request.email());
        Validaciones.password(request.password());
        if (request.rol() == null) {
            throw new BusinessRuleException("El rol es obligatorio");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRol(request.rol());
        return aRespuesta(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, EstadoUsuarioRequest request, String emailAdminActual) {
        if (request == null || request.estado() == null) {
            throw new BusinessRuleException("El estado es obligatorio");
        }
        Usuario usuario = buscarPorId(id);
        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new BusinessRuleException("No puedes cambiar tu propio estado");
        }
        usuario.setEstado(request.estado());
        return aRespuesta(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, RolUsuarioRequest request, String emailAdminActual) {
        if (request == null || request.rol() == null) {
            throw new BusinessRuleException("El rol es obligatorio");
        }
        Usuario usuario = buscarPorId(id);
        if (usuario.getEmail().equalsIgnoreCase(emailAdminActual)) {
            throw new BusinessRuleException("No puedes cambiar tu propio rol");
        }
        usuario.setRol(request.rol());
        return aRespuesta(usuarioRepository.save(usuario));
    }

    private Usuario buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessRuleException("El ID del usuario es obligatorio");
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getUsuarioId(), usuario.getEmail(), usuario.getEstado(), usuario.getRol());
    }
}
