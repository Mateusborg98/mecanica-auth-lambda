package br.com.techchallenge.mecanica.auth.infrastructure.config;

public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}