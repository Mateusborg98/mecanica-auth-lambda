package br.com.techchallenge.mecanica.auth.presentation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.techchallenge.mecanica.auth.application.gateway.ClientRepository;
import br.com.techchallenge.mecanica.auth.application.gateway.TokenGateway;
import br.com.techchallenge.mecanica.auth.application.model.IssuedToken;
import br.com.techchallenge.mecanica.auth.application.usecase.AuthenticateClientUseCase;
import br.com.techchallenge.mecanica.auth.domain.Client;

class AuthenticationHandlerTest {

    private static final Instant NOW =
            Instant.parse("2026-07-30T12:00:00Z");

    private static final UUID CLIENT_ID =
            UUID.fromString("8bb27ff0-6ce8-45c7-bfe8-bfd341dbea12");

    private static final String TOKEN = "signed.jwt.token";
    private static final long EXPIRES_IN = 900L;
    private static final String GENERATED_CORRELATION_ID =
            "generated-correlation-id";

    private ObjectMapper objectMapper;
    private Clock clock;
    private List<String> logs;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        logs = new ArrayList<>();
    }

    @Test
    void shouldAuthenticateClientAndReturnToken() throws Exception {
        AuthenticationHandler handler = handler(
                document -> Optional.of(new Client(CLIENT_ID, true)),
                (clientId, documentType) ->
                        new IssuedToken(TOKEN, EXPIRES_IN));

        APIGatewayV2HTTPEvent event = event(
                """
                {
                  "cpfCnpj": "529.982.247-25"
                }
                """,
                Map.of("X-Correlation-ID", "correlation-123"));

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertEquals(
                "application/json",
                response.getHeaders().get("Content-Type"));
        assertEquals(
                "correlation-123",
                response.getHeaders().get("X-Correlation-ID"));

        JsonNode body = objectMapper.readTree(response.getBody());

        assertEquals(TOKEN, body.get("accessToken").asText());
        assertEquals("Bearer", body.get("tokenType").asText());
        assertEquals(EXPIRES_IN, body.get("expiresIn").asLong());

        assertTrue(logs.stream()
                .anyMatch(log -> log.contains(
                        "\"event\":\"authentication_started\"")));

        assertTrue(logs.stream()
                .anyMatch(log -> log.contains(
                        "\"event\":\"authentication_succeeded\"")));

        assertFalse(String.join("", logs).contains("52998224725"));
        assertFalse(String.join("", logs).contains(TOKEN));
    }

    @Test
    void shouldReadCorrelationIdIgnoringHeaderCase() {
        AuthenticationHandler handler = successfulHandler();

        APIGatewayV2HTTPEvent event = event(
                """
                {"cpfCnpj":"52998224725"}
                """,
                Map.of("x-correlation-id", "lowercase-header"));

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(event, null);

        assertEquals(
                "lowercase-header",
                response.getHeaders().get("X-Correlation-ID"));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {
        AuthenticationHandler handler = successfulHandler();

        APIGatewayV2HTTPEvent event = event(
                """
                {"cpfCnpj":"52998224725"}
                """,
                Map.of());

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(event, null);

        assertEquals(
                GENERATED_CORRELATION_ID,
                response.getHeaders().get("X-Correlation-ID"));
    }

    @Test
    void shouldReturnBadRequestWhenEventIsNull() throws Exception {
        AuthenticationHandler handler = successfulHandler();

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(null, null);

        assertError(
                response,
                400,
                "INVALID_REQUEST",
                GENERATED_CORRELATION_ID);
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsBlank() throws Exception {
        AuthenticationHandler handler = successfulHandler();

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(event("   ", Map.of()), null);

        assertError(
                response,
                400,
                "INVALID_REQUEST",
                GENERATED_CORRELATION_ID);
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        AuthenticationHandler handler = successfulHandler();

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        event("{invalid-json", Map.of()),
                        null);

        assertError(
                response,
                400,
                "INVALID_REQUEST",
                GENERATED_CORRELATION_ID);
    }

    @Test
    void shouldReturnBadRequestWhenDocumentIsMissing() throws Exception {
        AuthenticationHandler handler = successfulHandler();

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(event("{}", Map.of()), null);

        assertError(
                response,
                400,
                "INVALID_REQUEST",
                GENERATED_CORRELATION_ID);
    }

    @Test
    void shouldReturnBadRequestForInvalidCpfCnpj() throws Exception {
        AuthenticationHandler handler = successfulHandler();

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        event(
                                """
                                {"cpfCnpj":"123"}
                                """,
                                Map.of()),
                        null);

        assertError(
                response,
                400,
                "INVALID_DOCUMENT",
                GENERATED_CORRELATION_ID);

        assertTrue(logs.stream()
                .anyMatch(log -> log.contains(
                        "\"event\":\"authentication_invalid_document\"")));
    }

    @Test
    void shouldReturnUnauthorizedWhenClientDoesNotExist()
            throws Exception {

        AuthenticationHandler handler = handler(
                document -> Optional.empty(),
                (clientId, documentType) ->
                        new IssuedToken(TOKEN, EXPIRES_IN));

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        event(
                                """
                                {"cpfCnpj":"52998224725"}
                                """,
                                Map.of()),
                        null);

        assertError(
                response,
                401,
                "INVALID_CREDENTIALS",
                GENERATED_CORRELATION_ID);

        assertTrue(logs.stream()
                .anyMatch(log -> log.contains(
                        "\"event\":\"authentication_invalid_credentials\"")));
    }

    @Test
    void shouldReturnUnauthorizedWhenClientIsInactive()
            throws Exception {

        AuthenticationHandler handler = handler(
                document -> Optional.of(new Client(CLIENT_ID, false)),
                (clientId, documentType) ->
                        new IssuedToken(TOKEN, EXPIRES_IN));

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        event(
                                """
                                {"cpfCnpj":"52998224725"}
                                """,
                                Map.of()),
                        null);

        assertError(
                response,
                401,
                "INVALID_CREDENTIALS",
                GENERATED_CORRELATION_ID);
    }

    @Test
    void shouldReturnInternalErrorForUnexpectedFailure()
            throws Exception {

        AuthenticationHandler handler = handler(
                document -> {
                    throw new IllegalStateException(
                            "Database unavailable");
                },
                (clientId, documentType) ->
                        new IssuedToken(TOKEN, EXPIRES_IN));

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        event(
                                """
                                {"cpfCnpj":"52998224725"}
                                """,
                                Map.of()),
                        null);

        assertError(
                response,
                500,
                "INTERNAL_ERROR",
                GENERATED_CORRELATION_ID);

        assertTrue(logs.stream()
                .anyMatch(log -> log.contains(
                        "\"event\":\"authentication_failed\"")));

        assertFalse(String.join("", logs)
                .contains("Database unavailable"));
    }

    @Test
    void shouldCreateUuidCorrelationIdSupplier() {
        String correlationId = AuthenticationHandler
                .uuidCorrelationIdSupplier()
                .get();

        assertNotNull(correlationId);
        UUID.fromString(correlationId);
    }

    private AuthenticationHandler successfulHandler() {
        return handler(
                document -> Optional.of(new Client(CLIENT_ID, true)),
                (clientId, documentType) ->
                        new IssuedToken(TOKEN, EXPIRES_IN));
    }

    private AuthenticationHandler handler(
            ClientRepository clientRepository,
            TokenGateway tokenGateway) {

        AuthenticateClientUseCase useCase =
                new AuthenticateClientUseCase(
                        clientRepository,
                        tokenGateway);

        return new AuthenticationHandler(
                useCase,
                objectMapper,
                () -> GENERATED_CORRELATION_ID,
                logs::add,
                clock);
    }

    private APIGatewayV2HTTPEvent event(
            String body,
            Map<String, String> headers) {

        return APIGatewayV2HTTPEvent.builder()
                .withBody(body)
                .withHeaders(headers)
                .build();
    }

    private void assertError(
            APIGatewayV2HTTPResponse response,
            int expectedStatus,
            String expectedCode,
            String expectedCorrelationId) throws Exception {

        assertEquals(expectedStatus, response.getStatusCode());

        JsonNode body = objectMapper.readTree(response.getBody());

        assertEquals(expectedCode, body.get("code").asText());
        assertEquals(
                expectedCorrelationId,
                body.get("correlationId").asText());

        assertEquals(
                expectedCorrelationId,
                response.getHeaders().get("X-Correlation-ID"));
    }
}