package br.com.techchallenge.mecanica.auth.infrastructure.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.com.techchallenge.mecanica.auth.domain.DocumentType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

class JwtTokenGatewayTest {

    private static final UUID CLIENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final Instant NOW =
            Instant.parse("2026-07-30T12:00:00Z");

    private static final String ISSUER = "mecanica-auth";
    private static final Duration EXPIRATION = Duration.ofHours(1);

    private static KeyPair keyPair;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void shouldIssueValidRs256TokenWithExpectedClaims() {
        JwtTokenGateway gateway = createGateway();

        var issuedToken = gateway.issue(
                CLIENT_ID,
                DocumentType.CPF);

        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(issuedToken.accessToken());

        Claims claims = parsedToken.getPayload();

        assertEquals("RS256", parsedToken.getHeader().getAlgorithm());
        assertEquals(CLIENT_ID.toString(), claims.getSubject());
        assertEquals(ISSUER, claims.getIssuer());
        assertEquals("CLIENTE", claims.get("role", String.class));
        assertEquals("CPF", claims.get("documentType", String.class));
        assertEquals(Date.from(NOW), claims.getIssuedAt());
        assertEquals(
                Date.from(NOW.plus(EXPIRATION)),
                claims.getExpiration());

        assertNotNull(claims.getId());
        assertEquals(3600, issuedToken.expiresIn());
    }

    @Test
    void shouldIssueTokenForCnpjWithoutExposingDocument() {
        JwtTokenGateway gateway = createGateway();

        var issuedToken = gateway.issue(
                CLIENT_ID,
                DocumentType.CNPJ);

        Claims claims = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(issuedToken.accessToken())
                .getPayload();

        assertEquals("CNPJ", claims.get("documentType", String.class));
        assertEquals(null, claims.get("cpf"));
        assertEquals(null, claims.get("cpfCnpj"));
    }

    @Test
    void shouldGenerateUniqueTokenIdentifier() {
        JwtTokenGateway gateway = createGateway();

        String firstTokenId = parseTokenId(
                gateway.issue(CLIENT_ID, DocumentType.CPF).accessToken());

        String secondTokenId = parseTokenId(
                gateway.issue(CLIENT_ID, DocumentType.CPF).accessToken());

        assertNotEquals(firstTokenId, secondTokenId);
    }

    @Test
    void shouldRejectTokenValidationWithAnotherPublicKey()
            throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair anotherKeyPair = generator.generateKeyPair();

        String token = createGateway()
                .issue(CLIENT_ID, DocumentType.CPF)
                .accessToken();

        assertThrows(
                JwtException.class,
                () -> Jwts.parser()
                        .verifyWith(anotherKeyPair.getPublic())
                        .clock(() -> Date.from(NOW))
                        .build()
                        .parseSignedClaims(token));
    }

    @Test
    void shouldValidateConstructorArguments() {
        Clock clock = fixedClock();

        assertThrows(
                NullPointerException.class,
                () -> new JwtTokenGateway(
                        null,
                        ISSUER,
                        EXPIRATION,
                        clock));

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtTokenGateway(
                        keyPair.getPrivate(),
                        " ",
                        EXPIRATION,
                        clock));

        assertThrows(
                NullPointerException.class,
                () -> new JwtTokenGateway(
                        keyPair.getPrivate(),
                        ISSUER,
                        null,
                        clock));

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtTokenGateway(
                        keyPair.getPrivate(),
                        ISSUER,
                        Duration.ZERO,
                        clock));

        assertThrows(
                NullPointerException.class,
                () -> new JwtTokenGateway(
                        keyPair.getPrivate(),
                        ISSUER,
                        EXPIRATION,
                        null));
    }

    @Test
    void shouldValidateIssueArguments() {
        JwtTokenGateway gateway = createGateway();

        assertThrows(
                NullPointerException.class,
                () -> gateway.issue(null, DocumentType.CPF));

        assertThrows(
                NullPointerException.class,
                () -> gateway.issue(CLIENT_ID, null));
    }

    private JwtTokenGateway createGateway() {
        return new JwtTokenGateway(
                keyPair.getPrivate(),
                ISSUER,
                EXPIRATION,
                fixedClock());
    }

    private Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private String parseTokenId(String token) {
        return Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }
}