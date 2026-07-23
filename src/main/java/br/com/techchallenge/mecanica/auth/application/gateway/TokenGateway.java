package br.com.techchallenge.mecanica.auth.application.gateway;

import java.util.UUID;

import br.com.techchallenge.mecanica.auth.application.model.IssuedToken;
import br.com.techchallenge.mecanica.auth.domain.DocumentType;

@FunctionalInterface
public interface TokenGateway {

    IssuedToken issue(
            UUID clientId,
            DocumentType documentType);
}