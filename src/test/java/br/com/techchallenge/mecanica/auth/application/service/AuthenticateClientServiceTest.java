package br.com.techchallenge.mecanica.auth.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import br.com.techchallenge.mecanica.auth.application.gateway.ClientRepository;
import br.com.techchallenge.mecanica.auth.application.gateway.TokenGateway;
import br.com.techchallenge.mecanica.auth.application.model.IssuedToken;
import br.com.techchallenge.mecanica.auth.domain.Client;
import br.com.techchallenge.mecanica.auth.domain.CpfCnpj;
import br.com.techchallenge.mecanica.auth.domain.DocumentType;
import br.com.techchallenge.mecanica.auth.domain.exception.InvalidCpfCnpjException;
import br.com.techchallenge.mecanica.auth.domain.exception.InvalidCredentialsException;

class AuthenticateClientServiceTest {

    private static final UUID CLIENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void shouldAuthenticateActiveClientWithCpf() {
        AtomicReference<CpfCnpj> searchedDocument = new AtomicReference<>();
        AtomicReference<DocumentType> issuedType = new AtomicReference<>();

        ClientRepository repository = document -> {
            searchedDocument.set(document);
            return Optional.of(new Client(CLIENT_ID, true));
        };

        TokenGateway tokenGateway = (clientId, documentType) -> {
            assertEquals(CLIENT_ID, clientId);
            issuedType.set(documentType);
            return new IssuedToken("signed.jwt.token", 3600);
        };

        AuthenticateClientService service =
                new AuthenticateClientService(repository, tokenGateway);

        IssuedToken result = service.authenticate("529.982.247-25");

        assertEquals("52998224725", searchedDocument.get().value());
        assertEquals(DocumentType.CPF, issuedType.get());
        assertEquals("signed.jwt.token", result.accessToken());
        assertEquals(3600, result.expiresIn());
    }

    @Test
    void shouldAuthenticateActiveClientWithCnpj() {
        AtomicReference<DocumentType> issuedType = new AtomicReference<>();

        ClientRepository repository = document ->
                Optional.of(new Client(CLIENT_ID, true));

        TokenGateway tokenGateway = (clientId, documentType) -> {
            issuedType.set(documentType);
            return new IssuedToken("signed.jwt.token", 3600);
        };

        AuthenticateClientService service =
                new AuthenticateClientService(repository, tokenGateway);

        service.authenticate("11.222.333/0001-81");

        assertEquals(DocumentType.CNPJ, issuedType.get());
    }

    @Test
    void shouldRejectClientNotFoundWithoutIssuingToken() {
        AtomicBoolean tokenIssued = new AtomicBoolean(false);

        ClientRepository repository = document -> Optional.empty();

        TokenGateway tokenGateway = (clientId, documentType) -> {
            tokenIssued.set(true);
            return new IssuedToken("should-not-exist", 3600);
        };

        AuthenticateClientService service =
                new AuthenticateClientService(repository, tokenGateway);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate("52998224725"));

        assertFalse(tokenIssued.get());
    }

    @Test
    void shouldRejectInactiveClientWithoutIssuingToken() {
        AtomicBoolean tokenIssued = new AtomicBoolean(false);

        ClientRepository repository = document ->
                Optional.of(new Client(CLIENT_ID, false));

        TokenGateway tokenGateway = (clientId, documentType) -> {
            tokenIssued.set(true);
            return new IssuedToken("should-not-exist", 3600);
        };

        AuthenticateClientService service =
                new AuthenticateClientService(repository, tokenGateway);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate("52998224725"));

        assertFalse(tokenIssued.get());
    }

    @Test
    void shouldRejectInvalidDocumentBeforeConsultingRepository() {
        AtomicBoolean repositoryCalled = new AtomicBoolean(false);

        ClientRepository repository = document -> {
            repositoryCalled.set(true);
            return Optional.empty();
        };

        TokenGateway tokenGateway = (clientId, documentType) ->
                new IssuedToken("should-not-exist", 3600);

        AuthenticateClientService service =
                new AuthenticateClientService(repository, tokenGateway);

        assertThrows(
                InvalidCpfCnpjException.class,
                () -> service.authenticate("documento-invalido"));

        assertFalse(repositoryCalled.get());
    }

    @Test
    void shouldRequireDependencies() {
        ClientRepository repository = document -> Optional.empty();
        TokenGateway tokenGateway = (clientId, documentType) ->
                new IssuedToken("token", 3600);

        assertThrows(
                NullPointerException.class,
                () -> new AuthenticateClientService(null, tokenGateway));

        assertThrows(
                NullPointerException.class,
                () -> new AuthenticateClientService(repository, null));
    }

    @Test
    void shouldValidateIssuedTokenContract() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssuedToken(null, 3600));

        assertThrows(
                IllegalArgumentException.class,
                () -> new IssuedToken(" ", 3600));

        assertThrows(
                IllegalArgumentException.class,
                () -> new IssuedToken("token", 0));

        assertTrue(new IssuedToken("token", 3600).expiresIn() > 0);
    }
}