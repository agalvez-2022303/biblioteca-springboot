package com.albertogalvez.biblioteca.autenticacion.security;

import com.albertogalvez.biblioteca.usuario.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${jwt.secret:Zm9vYmFyLXN1cGVyLXNlY2V0by1jaGF2ZS1qd3QtdGltby13ZWRzZXQtZGVyYTIzMjM=}") String secreto,
                      @Value("${jwt.expiracion-ms:86400000}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim("usuarioId", usuario.getUsuarioId())
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiracionMs))
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public Long extraerUsuarioId(String token) {
        Object valor = extraerClaims(token).get("usuarioId");
        if (valor == null) {
            return null;
        }
        return Long.valueOf(valor.toString());
    }

    public String extraerRol(String token) {
        Object valor = extraerClaims(token).get("rol");
        return valor == null ? null : valor.toString();
    }

    public boolean esValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(clave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
