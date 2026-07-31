package br.com.techchallenge.mecanica.auth.infrastructure.jwt;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class RsaPrivateKeyLoader {

    private static final String PRIVATE_KEY_HEADER =
            "-----BEGIN PRIVATE KEY-----";

    private static final String PRIVATE_KEY_FOOTER =
            "-----END PRIVATE KEY-----";

    public PrivateKey load(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new RsaKeyLoadingException(
                    "A chave privada RSA é obrigatória");
        }

        String normalizedPem = pem
                .replace("\\n", "\n")
                .trim();

        validateEnvelope(normalizedPem);

        String encodedKey = normalizedPem
                .replace(PRIVATE_KEY_HEADER, "")
                .replace(PRIVATE_KEY_FOOTER, "")
                .replaceAll("\\s", "");

        try {
            byte[] decodedKey = Base64.getDecoder()
                    .decode(encodedKey);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(decodedKey);

            PrivateKey privateKey = KeyFactory
                    .getInstance("RSA")
                    .generatePrivate(keySpec);

            if (!(privateKey instanceof RSAPrivateKey)) {
                throw new RsaKeyLoadingException(
                        "A chave informada não é uma chave privada RSA");
            }

            return privateKey;
        } catch (IllegalArgumentException exception) {
            throw new RsaKeyLoadingException(
                    "O conteúdo Base64 da chave privada é inválido",
                    exception);
        } catch (GeneralSecurityException exception) {
            throw new RsaKeyLoadingException(
                    "Não foi possível carregar a chave privada RSA",
                    exception);
        }
    }

    private void validateEnvelope(String pem) {
        if (!pem.startsWith(PRIVATE_KEY_HEADER)
                || !pem.endsWith(PRIVATE_KEY_FOOTER)) {

            throw new RsaKeyLoadingException(
                    "A chave deve estar no formato PEM PKCS#8");
        }
    }
}