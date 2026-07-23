package br.com.techchallenge.mecanica.auth.application.model;

public record IssuedToken(
        String accessToken,
        long expiresIn) {

    public IssuedToken {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("O token é obrigatório");
        }

        if (expiresIn <= 0) {
            throw new IllegalArgumentException(
                    "A expiração do token deve ser positiva");
        }
    }
}