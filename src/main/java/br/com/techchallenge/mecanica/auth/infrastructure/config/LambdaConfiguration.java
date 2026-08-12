package br.com.techchallenge.mecanica.auth.infrastructure.config;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

public record LambdaConfiguration(
        String databaseUrl,
        String databaseUsername,
        String databasePassword,
        String jwtPrivateKey,
        String jwtIssuer,
        Duration jwtExpiration) {

    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String DATABASE_USERNAME = "DATABASE_USERNAME";
    private static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
    private static final String JWT_PRIVATE_KEY = "JWT_PRIVATE_KEY";
    private static final String JWT_ISSUER = "JWT_ISSUER";
    private static final String JWT_EXPIRATION_SECONDS =
            "JWT_EXPIRATION_SECONDS";

    public static LambdaConfiguration fromEnvironment() {
        return from(System::getenv);
    }

    static LambdaConfiguration from(
            Function<String, String> environment) {

        Objects.requireNonNull(environment);

        String expirationValue = required(
                environment,
                JWT_EXPIRATION_SECONDS);

        long expirationSeconds;

        try {
            expirationSeconds = Long.parseLong(expirationValue);
        } catch (NumberFormatException exception) {
            throw new ConfigurationException(
                    JWT_EXPIRATION_SECONDS
                            + " deve ser um número inteiro positivo",
                    exception);
        }

        if (expirationSeconds <= 0) {
            throw new ConfigurationException(
                    JWT_EXPIRATION_SECONDS
                            + " deve ser maior que zero");
        }

        return new LambdaConfiguration(
                required(environment, DATABASE_URL),
                required(environment, DATABASE_USERNAME),
                required(environment, DATABASE_PASSWORD),
                required(environment, JWT_PRIVATE_KEY),
                required(environment, JWT_ISSUER),
                Duration.ofSeconds(expirationSeconds));
    }

    private static String required(
            Function<String, String> environment,
            String variableName) {

        String value = environment.apply(variableName);

        if (value == null || value.isBlank()) {
            throw new ConfigurationException(
                    "Variável de ambiente obrigatória ausente: "
                            + variableName);
        }

        return value;
    }
}