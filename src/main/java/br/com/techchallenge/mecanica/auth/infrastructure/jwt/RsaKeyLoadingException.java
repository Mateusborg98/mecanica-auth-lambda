package br.com.techchallenge.mecanica.auth.infrastructure.jwt;

public class RsaKeyLoadingException extends RuntimeException {

    public RsaKeyLoadingException(String message) {
        super(message);
    }

    public RsaKeyLoadingException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}