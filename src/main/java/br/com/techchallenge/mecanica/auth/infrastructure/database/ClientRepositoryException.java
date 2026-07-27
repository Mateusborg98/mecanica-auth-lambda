package br.com.techchallenge.mecanica.auth.infrastructure.database;

public class ClientRepositoryException extends RuntimeException {

    public ClientRepositoryException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}