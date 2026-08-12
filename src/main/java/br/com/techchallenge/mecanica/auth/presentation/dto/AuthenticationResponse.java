package br.com.techchallenge.mecanica.auth.presentation.dto;

public record AuthenticationResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {

    public static AuthenticationResponse bearer(
            String accessToken,
            long expiresIn) {

        return new AuthenticationResponse(
                accessToken,
                "Bearer",
                expiresIn);
    }
}