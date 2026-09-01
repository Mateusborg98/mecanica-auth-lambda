output "lambda_function_name" {
  description = "Nome da função de autenticação."
  value       = aws_lambda_function.authentication.function_name
}

output "api_gateway_url" {
  description = "URL base da API de autenticação."
  value       = aws_apigatewayv2_stage.authentication.invoke_url
}

output "authentication_endpoint" {
  description = "Endpoint completo para autenticação."
  value       = "${aws_apigatewayv2_stage.authentication.invoke_url}/auth"
}

output "lambda_security_group_id" {
  description = "Security group utilizado pela Lambda dentro da VPC."
  value       = aws_security_group.lambda.id
}
