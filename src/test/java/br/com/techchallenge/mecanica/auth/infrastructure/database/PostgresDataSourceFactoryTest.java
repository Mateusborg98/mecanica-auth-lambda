package br.com.techchallenge.mecanica.auth.infrastructure.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariDataSource;

import br.com.techchallenge.mecanica.auth.infrastructure.config.LambdaConfiguration;

class PostgresDataSourceFactoryTest {

    @Test
    void shouldCreateConfiguredDataSource() {
        LambdaConfiguration configuration =
                new LambdaConfiguration(
                        "jdbc:postgresql://localhost:5432/mecanica",
                        "mecanica",
                        "secret",
                        "private-key",
                        "mecanica-auth",
                        Duration.ofMinutes(15));

        PostgresDataSourceFactory factory =
                new PostgresDataSourceFactory();

        try (HikariDataSource dataSource =
                factory.create(configuration)) {

            assertEquals(
                    configuration.databaseUrl(),
                    dataSource.getJdbcUrl());

            assertEquals(
                    configuration.databaseUsername(),
                    dataSource.getUsername());

            assertEquals(
                    configuration.databasePassword(),
                    dataSource.getPassword());

            assertEquals(
                    "mecanica-auth-pool",
                    dataSource.getPoolName());

            assertEquals(2, dataSource.getMaximumPoolSize());
            assertEquals(0, dataSource.getMinimumIdle());
            assertEquals(5_000, dataSource.getConnectionTimeout());
            assertEquals(3_000, dataSource.getValidationTimeout());
            assertEquals(60_000, dataSource.getIdleTimeout());
            assertEquals(300_000, dataSource.getMaxLifetime());
            assertEquals(-1, dataSource.getInitializationFailTimeout());
        }
    }

    @Test
    void shouldRejectNullConfiguration() {
        PostgresDataSourceFactory factory =
                new PostgresDataSourceFactory();

        assertThrows(
                NullPointerException.class,
                () -> factory.create(null));
    }
}