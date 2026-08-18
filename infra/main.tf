locals {
  name            = "mecanica-auth-${var.environment}"
  lambda_jar_path = abspath("${path.module}/${var.lambda_jar_path}")
}

resource "aws_lambda_function" "authentication" {
  function_name = local.name
  description   = "Autenticação de clientes por CPF ou CNPJ."

  role    = var.lambda_role_arn
  runtime = "java21"
  handler = "br.com.techchallenge.mecanica.auth.infrastructure.lambda.LambdaHandler::handleRequest"

  filename         = local.lambda_jar_path
  source_code_hash = filebase64sha256(local.lambda_jar_path)

  memory_size = 512
  timeout     = 15

  environment {
    variables = {
      DATABASE_URL           = var.database_url
      DATABASE_USERNAME      = var.database_username
      DATABASE_PASSWORD      = var.database_password
      JWT_PRIVATE_KEY        = var.jwt_private_key
      JWT_ISSUER             = var.jwt_issuer
      JWT_EXPIRATION_SECONDS = tostring(var.jwt_expiration_seconds)
    }
  }
}

resource "aws_apigatewayv2_api" "authentication" {
  name          = local.name
  protocol_type = "HTTP"

  cors_configuration {
    allow_headers = [
      "Content-Type",
      "X-Correlation-ID"
    ]

    allow_methods = ["POST", "OPTIONS"]
    allow_origins = ["*"]
  }
}

resource "aws_apigatewayv2_integration" "authentication" {
  api_id = aws_apigatewayv2_api.authentication.id

  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.authentication.invoke_arn
  payload_format_version = "2.0"
  timeout_milliseconds   = 15000
}

resource "aws_apigatewayv2_route" "authentication" {
  api_id = aws_apigatewayv2_api.authentication.id

  route_key = "POST /auth"
  target = join(
    "/",
    [
      "integrations",
      aws_apigatewayv2_integration.authentication.id
    ]
  )
}

resource "aws_apigatewayv2_stage" "authentication" {
  api_id = aws_apigatewayv2_api.authentication.id

  name        = "$default"
  auto_deploy = true

  default_route_settings {
    detailed_metrics_enabled = true
    throttling_burst_limit   = 10
    throttling_rate_limit    = 5
  }
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.authentication.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_apigatewayv2_api.authentication.execution_arn}/*/*"
}