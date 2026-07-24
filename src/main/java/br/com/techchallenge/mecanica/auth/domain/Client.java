package br.com.techchallenge.mecanica.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record Client(
        UUID id,
        boolean active) {

    public Client {
        Objects.requireNonNull(id, "O identificador do cliente é obrigatório");
    }
}