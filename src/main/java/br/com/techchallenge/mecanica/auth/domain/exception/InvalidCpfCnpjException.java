package br.com.techchallenge.mecanica.auth.domain.exception;

public class InvalidCpfCnpjException extends IllegalArgumentException {

    public InvalidCpfCnpjException() {
        super("CPF ou CNPJ inválido");
    }
}