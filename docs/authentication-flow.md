# Fluxo de autenticação

## Objetivo

O documento pode ser recebido com ou sem pontuação e deve ser
normalizado antes da validação.

A autenticação aceita:

- CPF com 11 dígitos;
- CNPJ com 14 dígitos.

## Entrada

`POST /auth`

```json
{
  "cpfCnpj": "52998224725"
}