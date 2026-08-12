package br.com.techchallenge.mecanica.auth.presentation.dto;

public record ErrorResponse(
        String code,
        String message,
        String correlationId) {
}