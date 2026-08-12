package br.com.techchallenge.mecanica.auth.infrastructure.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.techchallenge.mecanica.auth.application.gateway.ClientRepository;
import br.com.techchallenge.mecanica.auth.application.gateway.TokenGateway;
import br.com.techchallenge.mecanica.auth.application.usecase.AuthenticateClientUseCase;
import br.com.techchallenge.mecanica.auth.presentation.handler.AuthenticationHandler;

class LambdaHandlerTest {

    @Test
    void shouldDelegateRequestToAuthenticationHandler() {
        ClientRepository clientRepository =
                document -> java.util.Optional.empty();

        TokenGateway tokenGateway =
                (clientId, documentType) -> {
                    throw new AssertionError(
                            "Token não deveria ser gerado");
                };

        AuthenticateClientUseCase useCase =
                new AuthenticateClientUseCase(
                        clientRepository,
                        tokenGateway);

        AuthenticationHandler authenticationHandler =
                new AuthenticationHandler(
                        useCase,
                        new ObjectMapper().findAndRegisterModules(),
                        () -> "correlation-test",
                        new ArrayList<String>()::add,
                        Clock.systemUTC());

        LambdaHandler lambdaHandler =
                new LambdaHandler(authenticationHandler);

        APIGatewayV2HTTPResponse response =
                lambdaHandler.handleRequest(null, null);

        assertEquals(400, response.getStatusCode());
    }

    @Test
    void shouldRejectNullDelegate() {
        assertThrows(
                NullPointerException.class,
                () -> new LambdaHandler(null));
    }
}