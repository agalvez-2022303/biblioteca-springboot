package com.albertogalvez.biblioteca.comun.advisor;

import java.time.LocalDateTime;

public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message) {
}
