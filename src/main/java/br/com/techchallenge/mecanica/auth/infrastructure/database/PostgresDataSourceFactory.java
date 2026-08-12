package br.com.techchallenge.mecanica.auth.infrastructure.database;

import java.util.Objects;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import br.com.techchallenge.mecanica.auth.infrastructure.config.LambdaConfiguration;

public class PostgresDataSourceFactory {

    private static final int MAXIMUM_POOL_SIZE = 2;
    private static final int MINIMUM_IDLE = 0;

    private static final long CONNECTION_TIMEOUT_MILLISECONDS = 5_000;
    private static final long VALIDATION_TIMEOUT_MILLISECONDS = 3_000;
    private static final long IDLE_TIMEOUT_MILLISECONDS = 60_000;
    private static final long MAX_LIFETIME_MILLISECONDS = 300_000;

    public HikariDataSource create(
            LambdaConfiguration configuration) {

        Objects.requireNonNull(configuration);

        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(configuration.databaseUrl());
        hikariConfig.setUsername(configuration.databaseUsername());
        hikariConfig.setPassword(configuration.databasePassword());

        hikariConfig.setPoolName("mecanica-auth-pool");
        hikariConfig.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        hikariConfig.setMinimumIdle(MINIMUM_IDLE);

        hikariConfig.setConnectionTimeout(
                CONNECTION_TIMEOUT_MILLISECONDS);

        hikariConfig.setValidationTimeout(
                VALIDATION_TIMEOUT_MILLISECONDS);

        hikariConfig.setIdleTimeout(
                IDLE_TIMEOUT_MILLISECONDS);

        hikariConfig.setMaxLifetime(
                MAX_LIFETIME_MILLISECONDS);

        /*
         * Não tenta conectar durante a criação do pool.
         * A primeira conexão será aberta quando a autenticação consultar
         * o cliente.
         */
        hikariConfig.setInitializationFailTimeout(-1);

        return new HikariDataSource(hikariConfig);
    }
}