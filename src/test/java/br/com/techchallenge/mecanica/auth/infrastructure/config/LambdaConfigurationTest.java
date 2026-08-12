package br.com.techchallenge.mecanica.auth.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LambdaConfigurationTest {

    @Test
    void shouldLoadConfigurationFromEnvironment() {
        Map<String, String> environment = validEnvironment();

        LambdaConfiguration configuration =
                LambdaConfiguration.from(environment::get);

        assertEquals(
                "jdbc:postgresql://localhost:5432/mecanica",
                configuration.databaseUrl());

        assertEquals(
                "mecanica",
                configuration.databaseUsername());

        assertEquals(
                "secret",
                configuration.databasePassword());

        assertEquals(
                "private-key",
                configuration.jwtPrivateKey());

        assertEquals(
                "mecanica-auth",
                configuration.jwtIssuer());

        assertEquals(
                Duration.ofSeconds(900),
                configuration.jwtExpiration());
    }

    @Test
    void shouldRejectMissingRequiredVariable() {
        Map<String, String> environment = validEnvironment();
        environment.remove("DATABASE_URL");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> LambdaConfiguration.from(environment::get));

        assertTrue(exception.getMessage().contains("DATABASE_URL"));
    }

    @Test
    void shouldRejectBlankRequiredVariable() {
        Map<String, String> environment = validEnvironment();
        environment.put("JWT_ISSUER", "   ");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> LambdaConfiguration.from(environment::get));

        assertTrue(exception.getMessage().contains("JWT_ISSUER"));
    }

    @Test
    void shouldRejectNonNumericExpiration() {
        Map<String, String> environment = validEnvironment();
        environment.put("JWT_EXPIRATION_SECONDS", "fifteen-minutes");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> LambdaConfiguration.from(environment::get));

        assertTrue(
                exception.getMessage()
                        .contains("JWT_EXPIRATION_SECONDS"));
    }

    @Test
    void shouldRejectZeroExpiration() {
        Map<String, String> environment = validEnvironment();
        environment.put("JWT_EXPIRATION_SECONDS", "0");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> LambdaConfiguration.from(environment::get));

        assertTrue(
                exception.getMessage()
                        .contains("maior que zero"));
    }

    @Test
    void shouldRejectNegativeExpiration() {
        Map<String, String> environment = validEnvironment();
        environment.put("JWT_EXPIRATION_SECONDS", "-1");

        assertThrows(
                ConfigurationException.class,
                () -> LambdaConfiguration.from(environment::get));
    }

    private Map<String, String> validEnvironment() {
        Map<String, String> environment = new HashMap<>();

        environment.put(
                "DATABASE_URL",
                "jdbc:postgresql://localhost:5432/mecanica");

        environment.put("DATABASE_USERNAME", "mecanica");
        environment.put("DATABASE_PASSWORD", "secret");
        environment.put("JWT_PRIVATE_KEY", "private-key");
        environment.put("JWT_ISSUER", "mecanica-auth");
        environment.put("JWT_EXPIRATION_SECONDS", "900");

        return environment;
    }
}