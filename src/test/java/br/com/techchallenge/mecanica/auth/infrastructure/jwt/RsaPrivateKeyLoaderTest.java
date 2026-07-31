package br.com.techchallenge.mecanica.auth.infrastructure.jwt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RsaPrivateKeyLoaderTest {

    private static KeyPair keyPair;
    private static String privateKeyPem;

    private final RsaPrivateKeyLoader loader =
            new RsaPrivateKeyLoader();

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        privateKeyPem = toPem(keyPair.getPrivate());
    }

    @Test
    void shouldLoadRsaPrivateKeyFromMultilinePem() {
        PrivateKey loadedKey = loader.load(privateKeyPem);

        assertEquals("RSA", loadedKey.getAlgorithm());
        assertEquals("PKCS#8", loadedKey.getFormat());
        assertArrayEquals(
                keyPair.getPrivate().getEncoded(),
                loadedKey.getEncoded());
    }

    @Test
    void shouldLoadPemWithEscapedLineBreaks() {
        String escapedPem = privateKeyPem
                .replace("\n", "\\n");

        PrivateKey loadedKey = loader.load(escapedPem);

        assertArrayEquals(
                keyPair.getPrivate().getEncoded(),
                loadedKey.getEncoded());
    }

    @Test
    void shouldRejectNullOrBlankKey() {
        assertThrows(
                RsaKeyLoadingException.class,
                () -> loader.load(null));

        assertThrows(
                RsaKeyLoadingException.class,
                () -> loader.load(" "));
    }

    @Test
    void shouldRejectPemWithoutPrivateKeyEnvelope() {
        String invalidPem = privateKeyPem
                .replace("PRIVATE KEY", "INVALID KEY");

        assertThrows(
                RsaKeyLoadingException.class,
                () -> loader.load(invalidPem));
    }

    @Test
    void shouldRejectMalformedBase64() {
        String malformedPem = """
                -----BEGIN PRIVATE KEY-----
                conteúdo-inválido-###
                -----END PRIVATE KEY-----
                """;

        assertThrows(
                RsaKeyLoadingException.class,
                () -> loader.load(malformedPem));
    }

    @Test
    void shouldRejectPublicKeyPem() {
        String publicKeyPem = """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(
                Base64.getMimeEncoder(
                                64,
                                new byte[] {'\n'})
                        .encodeToString(
                                keyPair.getPublic().getEncoded()));

        assertThrows(
                RsaKeyLoadingException.class,
                () -> loader.load(publicKeyPem));
    }

    private static String toPem(PrivateKey privateKey) {
        String encodedKey = Base64.getMimeEncoder(
                        64,
                        new byte[] {'\n'})
                .encodeToString(privateKey.getEncoded());

        return """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(encodedKey);
    }
}