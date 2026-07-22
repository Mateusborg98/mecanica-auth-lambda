package br.com.techchallenge.mecanica.auth.domain;

import br.com.techchallenge.mecanica.auth.domain.exception.InvalidCpfCnpjException;

public record CpfCnpj(String value) {

    private static final int CPF_LENGTH = 11;
    private static final int CNPJ_LENGTH = 14;

    private static final int[] CNPJ_FIRST_WEIGHTS = {
            5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2
    };

    private static final int[] CNPJ_SECOND_WEIGHTS = {
            6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2
    };

    public CpfCnpj {
        value = normalize(value);
        validate(value);
    }

    private static String normalize(String rawValue) {
        if (rawValue == null) {
            throw new InvalidCpfCnpjException();
        }

        String normalized = rawValue.replaceAll("[.\\-/\\s]", "");

        if (!normalized.matches("\\d+")) {
            throw new InvalidCpfCnpjException();
        }

        return normalized;
    }

    private static void validate(String document) {
        if (document.length() != CPF_LENGTH
                && document.length() != CNPJ_LENGTH) {
            throw new InvalidCpfCnpjException();
        }

        if (document.matches("(\\d)\\1+")) {
            throw new InvalidCpfCnpjException();
        }

        if (document.length() == CPF_LENGTH) {
            validateCpf(document);
            return;
        }

        validateCnpj(document);
    }

    private static void validateCpf(String cpf) {
        int firstDigit = calculateCpfDigit(cpf, 9, 10);

        if (firstDigit != numericValue(cpf, 9)) {
            throw new InvalidCpfCnpjException();
        }

        int secondDigit = calculateCpfDigit(cpf, 10, 11);

        if (secondDigit != numericValue(cpf, 10)) {
            throw new InvalidCpfCnpjException();
        }
    }

    private static int calculateCpfDigit(
            String cpf,
            int length,
            int initialWeight) {

        int sum = 0;

        for (int index = 0; index < length; index++) {
            sum += numericValue(cpf, index) * (initialWeight - index);
        }

        return calculateMod11Digit(sum);
    }

    private static void validateCnpj(String cnpj) {
        int firstDigit = calculateCnpjDigit(
                cnpj,
                CNPJ_FIRST_WEIGHTS);

        if (firstDigit != numericValue(cnpj, 12)) {
            throw new InvalidCpfCnpjException();
        }

        int secondDigit = calculateCnpjDigit(
                cnpj,
                CNPJ_SECOND_WEIGHTS);

        if (secondDigit != numericValue(cnpj, 13)) {
            throw new InvalidCpfCnpjException();
        }
    }

    private static int calculateCnpjDigit(
            String cnpj,
            int[] weights) {

        int sum = 0;

        for (int index = 0; index < weights.length; index++) {
            sum += numericValue(cnpj, index) * weights[index];
        }

        return calculateMod11Digit(sum);
    }

    private static int calculateMod11Digit(int sum) {
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int numericValue(String value, int index) {
        return Character.getNumericValue(value.charAt(index));
    }

    public boolean isCpf() {
        return value.length() == CPF_LENGTH;
    }

    public boolean isCnpj() {
        return value.length() == CNPJ_LENGTH;
    }

    public String masked() {
        if (isCpf()) {
            return "***."
                    + value.substring(3, 6)
                    + "."
                    + value.substring(6, 9)
                    + "-**";
        }

        return "**."
                + value.substring(2, 5)
                + "."
                + value.substring(5, 8)
                + "/"
                + value.substring(8, 12)
                + "-**";
    }
}