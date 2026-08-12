package br.com.techchallenge.mecanica.auth.presentation.handler;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.techchallenge.mecanica.auth.application.model.IssuedToken;
import br.com.techchallenge.mecanica.auth.application.usecase.AuthenticateClientUseCase;
import br.com.techchallenge.mecanica.auth.domain.exception.InvalidCpfCnpjException;
import br.com.techchallenge.mecanica.auth.domain.exception.InvalidCredentialsException;
import br.com.techchallenge.mecanica.auth.presentation.dto.AuthenticationRequest;
import br.com.techchallenge.mecanica.auth.presentation.dto.AuthenticationResponse;
import br.com.techchallenge.mecanica.auth.presentation.dto.ErrorResponse;
import br.com.techchallenge.mecanica.auth.presentation.exception.InvalidRequestException;
import br.com.techchallenge.mecanica.auth.presentation.log.StructuredLogEvent;

public class AuthenticationHandler implements
        RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final AuthenticateClientUseCase authenticateClientUseCase;
    private final ObjectMapper objectMapper;
    private final Supplier<String> correlationIdSupplier;
    private final Consumer<String> logWriter;
    private final Clock clock;

    public AuthenticationHandler(
            AuthenticateClientUseCase authenticateClientUseCase,
            ObjectMapper objectMapper,
            Supplier<String> correlationIdSupplier,
            Consumer<String> logWriter,
            Clock clock) {

        this.authenticateClientUseCase =
                Objects.requireNonNull(authenticateClientUseCase);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.correlationIdSupplier =
                Objects.requireNonNull(correlationIdSupplier);
        this.logWriter = Objects.requireNonNull(logWriter);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(
            APIGatewayV2HTTPEvent event,
            Context context) {

        String correlationId = resolveCorrelationId(event);

        log("INFO", "authentication_started", correlationId);

        try {
            AuthenticationRequest request = parseRequest(event);

            IssuedToken issuedToken =
                    authenticateClientUseCase.authenticate(request.cpfCnpj());

            AuthenticationResponse response =
                    AuthenticationResponse.bearer(
                            issuedToken.accessToken(),
                            issuedToken.expiresIn());

            log("INFO", "authentication_succeeded", correlationId);

            return response(
                    200,
                    response,
                    correlationId);

        } catch (InvalidRequestException exception) {
            log("WARN", "authentication_invalid_request", correlationId);

            return errorResponse(
                    400,
                    "INVALID_REQUEST",
                    "Requisição inválida",
                    correlationId);

        } catch (InvalidCpfCnpjException exception) {
            log("WARN", "authentication_invalid_document", correlationId);

            return errorResponse(
                    400,
                    "INVALID_DOCUMENT",
                    "CPF ou CNPJ inválido",
                    correlationId);

        } catch (InvalidCredentialsException exception) {
            log("WARN", "authentication_invalid_credentials", correlationId);

            return errorResponse(
                    401,
                    "INVALID_CREDENTIALS",
                    "Cliente não encontrado ou inativo",
                    correlationId);

        } catch (RuntimeException exception) {
            log("ERROR", "authentication_failed", correlationId);

            return errorResponse(
                    500,
                    "INTERNAL_ERROR",
                    "Erro interno ao processar a autenticação",
                    correlationId);
        }
    }

    private AuthenticationRequest parseRequest(
            APIGatewayV2HTTPEvent event) {

        if (event == null
                || event.getBody() == null
                || event.getBody().isBlank()) {

            throw new InvalidRequestException();
        }

        try {
            AuthenticationRequest request = objectMapper.readValue(
                    event.getBody(),
                    AuthenticationRequest.class);

            if (request == null
                    || request.cpfCnpj() == null
                    || request.cpfCnpj().isBlank()) {

                throw new InvalidRequestException();
            }

            return request;

        } catch (JsonProcessingException exception) {
            throw new InvalidRequestException(exception);
        }
    }

    private String resolveCorrelationId(
            APIGatewayV2HTTPEvent event) {

        if (event != null && event.getHeaders() != null) {
            String correlationId = event.getHeaders().entrySet().stream()
                    .filter(entry -> CORRELATION_ID_HEADER.equalsIgnoreCase(
                            entry.getKey()))
                    .map(Map.Entry::getValue)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);

            if (correlationId != null) {
                return correlationId;
            }
        }

        return correlationIdSupplier.get();
    }

    private APIGatewayV2HTTPResponse errorResponse(
            int statusCode,
            String code,
            String message,
            String correlationId) {

        ErrorResponse error =
                new ErrorResponse(code, message, correlationId);

        return response(statusCode, error, correlationId);
    }

    private APIGatewayV2HTTPResponse response(
            int statusCode,
            Object body,
            String correlationId) {

        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        headers.put(CORRELATION_ID_HEADER, correlationId);

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(toJson(body))
                .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Não foi possível gerar o JSON da resposta",
                    exception);
        }
    }

    private void log(
            String level,
            String event,
            String correlationId) {

        StructuredLogEvent logEvent = new StructuredLogEvent(
                clock.instant(),
                level,
                event,
                correlationId);

        logWriter.accept(toJson(logEvent));
    }

    public static Supplier<String> uuidCorrelationIdSupplier() {
        return () -> UUID.randomUUID().toString();
    }
}