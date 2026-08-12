package br.com.techchallenge.mecanica.auth.presentation.log;

import java.time.Instant;

public record StructuredLogEvent(
        Instant timestamp,
        String level,
        String event,
        String correlationId) {
}