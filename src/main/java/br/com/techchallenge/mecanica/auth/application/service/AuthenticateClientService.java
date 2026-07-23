package br.com.techchallenge.mecanica.auth.application.service;

import java.util.Objects;

import br.com.techchallenge.mecanica.auth.application.gateway.ClientRepository;
import br.com.techchallenge.mecanica.auth.application.gateway.TokenGateway;
import br.com.techchallenge.mecanica.auth.application.model.IssuedToken;
import br.com.techchallenge.mecanica.auth.domain.Client;
import br.com.techchallenge.mecanica.auth.domain.CpfCnpj;
import br.com.techchallenge.mecanica.auth.domain.exception.InvalidCredentialsException;

public class AuthenticateClientService {

    private final ClientRepository clientRepository;
    private final TokenGateway tokenGateway;

    public AuthenticateClientService(
            ClientRepository clientRepository,
            TokenGateway tokenGateway) {

        this.clientRepository = Objects.requireNonNull(clientRepository);
        this.tokenGateway = Objects.requireNonNull(tokenGateway);
    }

    public IssuedToken authenticate(String rawDocument) {
        CpfCnpj document = new CpfCnpj(rawDocument);

        Client client = clientRepository.findByCpfCnpj(document)
                .filter(Client::active)
                .orElseThrow(InvalidCredentialsException::new);

        return tokenGateway.issue(
                client.id(),
                document.documentType());
    }
}