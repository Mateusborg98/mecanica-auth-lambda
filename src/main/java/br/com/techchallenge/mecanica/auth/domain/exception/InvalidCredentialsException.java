package br.com.techchallenge.mecanica.auth.domain.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credencial inválida");
    }
}