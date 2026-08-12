package br.com.techchallenge.mecanica.auth.infrastructure.lambda;

import java.security.PrivateKey;
import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;

import br.com.techchallenge.mecanica.auth.application.gateway.ClientRepository;
import br.com.techchallenge.mecanica.auth.application.gateway.TokenGateway;
import br.com.techchallenge.mecanica.auth.application.usecase.AuthenticateClientUseCase;
import br.com.techchallenge.mecanica.auth.infrastructure.config.LambdaConfiguration;
import br.com.techchallenge.mecanica.auth.infrastructure.database.JdbcClientRepository;
import br.com.techchallenge.mecanica.auth.infrastructure.database.PostgresDataSourceFactory;
import br.com.techchallenge.mecanica.auth.infrastructure.jwt.JwtTokenGateway;
import br.com.techchallenge.mecanica.auth.infrastructure.jwt.RsaPrivateKeyLoader;
import br.com.techchallenge.mecanica.auth.presentation.handler.AuthenticationHandler;

public class LambdaBootstrap {

    public AuthenticationHandler createFromEnvironment() {
        return create(LambdaConfiguration.fromEnvironment());
    }

    AuthenticationHandler create(
            LambdaConfiguration configuration) {

        Clock clock = Clock.systemUTC();

        HikariDataSource dataSource =
                new PostgresDataSourceFactory().create(configuration);

        ClientRepository clientRepository =
                new JdbcClientRepository(dataSource);

        PrivateKey privateKey =
                new RsaPrivateKeyLoader()
                        .load(configuration.jwtPrivateKey());

        TokenGateway tokenGateway =
                new JwtTokenGateway(
                        privateKey,
                        configuration.jwtIssuer(),
                        configuration.jwtExpiration(),
                        clock);

        AuthenticateClientUseCase useCase =
                new AuthenticateClientUseCase(
                        clientRepository,
                        tokenGateway);

        ObjectMapper objectMapper =
                new ObjectMapper().findAndRegisterModules();

        return new AuthenticationHandler(
                useCase,
                objectMapper,
                AuthenticationHandler.uuidCorrelationIdSupplier(),
                System.out::println,
                clock);
    }
}