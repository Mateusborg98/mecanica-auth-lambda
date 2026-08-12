package br.com.techchallenge.mecanica.auth.infrastructure.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import br.com.techchallenge.mecanica.auth.infrastructure.config.LambdaConfiguration;
import br.com.techchallenge.mecanica.auth.presentation.handler.AuthenticationHandler;

class LambdaBootstrapTest {

    @Test
    void shouldCreateHandlerWithProductionDependencies()
            throws Exception {

        LambdaConfiguration configuration =
                new LambdaConfiguration(
                        "jdbc:h2:mem:bootstrap;MODE=PostgreSQL",
                        "sa",
                        "",
                        generatePrivateKeyPem(),
                        "mecanica-auth",
                        Duration.ofMinutes(15));

        AuthenticationHandler handler =
                new LambdaBootstrap().create(configuration);

        assertNotNull(handler);

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(null, null);

        assertEquals(400, response.getStatusCode());
    }

    private String generatePrivateKeyPem() throws Exception {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        String encodedKey = Base64.getMimeEncoder(
                        64,
                        "\n".getBytes())
                .encodeToString(
                        keyPair.getPrivate().getEncoded());

        return """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(encodedKey);
    }
}