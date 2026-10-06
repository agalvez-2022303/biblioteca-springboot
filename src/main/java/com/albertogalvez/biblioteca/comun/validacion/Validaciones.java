package com.albertogalvez.biblioteca.comun.validacion;

import com.albertogalvez.biblioteca.comun.exception.BusinessRuleException;

import java.nio.charset.StandardCharsets;

public final class Validaciones {

    public static final int MAX_EMAIL = 150;
    public static final int MAX_PASSWORD_BYTES = 72;

    private Validaciones() {
    }

    public static void email(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("El email es obligatorio");
        }
        if (email.length() > MAX_EMAIL) {
            throw new BusinessRuleException("El email no puede superar " + MAX_EMAIL + " caracteres");
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0 || arroba != email.lastIndexOf('@') || arroba == email.length() - 1) {
            throw new BusinessRuleException("El email no tiene un formato válido");
        }
        String dominio = email.substring(arroba + 1);
        if (email.contains(" ") || !dominio.contains(".") || dominio.startsWith(".") || dominio.endsWith(".")) {
            throw new BusinessRuleException("El email no tiene un formato válido");
        }
    }

    public static void password(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("La password es obligatoria");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessRuleException("La password no puede superar " + MAX_PASSWORD_BYTES + " bytes");
        }
    }

    public static void texto(String valor, String mensajeObligatorio, String mensajeLargo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessRuleException(mensajeObligatorio);
        }
        if (valor.length() > maximo) {
            throw new BusinessRuleException(mensajeLargo + " " + maximo + " caracteres");
        }
    }
}
