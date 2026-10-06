package com.albertogalvez.biblioteca.usuario.repository;

import com.albertogalvez.biblioteca.usuario.entity.EstadoUsuario;
import com.albertogalvez.biblioteca.usuario.entity.Rol;
import com.albertogalvez.biblioteca.usuario.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from Usuario u where (:rol is null or u.rol = :rol) and (:estado is null or u.estado = :estado)")
    Page<Usuario> buscar(@Param("rol") Rol rol, @Param("estado") EstadoUsuario estado, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.usuarioId = :id")
    Optional<Usuario> findByIdConBloqueo(@Param("id") Long id);
}
