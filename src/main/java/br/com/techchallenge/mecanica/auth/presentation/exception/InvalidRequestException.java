package br.com.techchallenge.mecanica.auth.presentation.exception;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException() {
        super("Requisição inválida");
    }

    public InvalidRequestException(Throwable cause) {
        super("Requisição inválida", cause);
    }
}