package br.com.techchallenge.mecanica.auth.infrastructure.jwt;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import br.com.techchallenge.mecanica.auth.application.gateway.TokenGateway;
import br.com.techchallenge.mecanica.auth.application.model.IssuedToken;
import br.com.techchallenge.mecanica.auth.domain.DocumentType;
import io.jsonwebtoken.Jwts;

public class JwtTokenGateway implements TokenGateway {

    private static final String CLIENT_ROLE = "CLIENTE";

    private final PrivateKey privateKey;
    private final String issuer;
    private final Duration expiration;
    private final Clock clock;

    public JwtTokenGateway(
            PrivateKey privateKey,
            String issuer,
            Duration expiration,
            Clock clock) {

        this.privateKey = validatePrivateKey(privateKey);
        this.issuer = validateIssuer(issuer);
        this.expiration = validateExpiration(expiration);
        this.clock = Objects.requireNonNull(clock, "Clock é obrigatório");
    }

    @Override
    public IssuedToken issue(
            UUID clientId,
            DocumentType documentType) {

        Objects.requireNonNull(
                clientId,
                "Identificador do cliente é obrigatório");

        Objects.requireNonNull(
                documentType,
                "Tipo de documento é obrigatório");

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(expiration);

        String token = Jwts.builder()
                .subject(clientId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim("role", CLIENT_ROLE)
                .claim("documentType", documentType.name())
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        return new IssuedToken(
                token,
                expiration.toSeconds());
    }

    private static PrivateKey validatePrivateKey(PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "Chave privada é obrigatória");

        if (!"RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
            throw new IllegalArgumentException(
                    "A chave privada deve utilizar RSA");
        }

        return privateKey;
    }

    private static String validateIssuer(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(
                    "O emissor do token é obrigatório");
        }

        return issuer;
    }

    private static Duration validateExpiration(Duration expiration) {
        Objects.requireNonNull(
                expiration,
                "A expiração do token é obrigatória");

        if (expiration.isNegative()
                || expiration.isZero()
                || expiration.toSeconds() <= 0) {

            throw new IllegalArgumentException(
                    "A expiração deve ser maior que zero");
        }

        return expiration;
    }
}