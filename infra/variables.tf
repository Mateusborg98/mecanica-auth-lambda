variable "aws_region" {
  description = "Região AWS utilizada pelos recursos."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Ambiente de implantação."
  type        = string

  validation {
    condition     = contains(["homolog", "prod"], var.environment)
    error_message = "O ambiente deve ser homolog ou prod."
  }
}

variable "lambda_role_arn" {
  description = "ARN da role de execução permitida pelo AWS Academy."
  type        = string
}

variable "lambda_jar_path" {
  description = "Caminho do fat JAR da Lambda, relativo ao diretório infra."
  type        = string
  default     = "../target/mecanica-auth-lambda-0.1.0-SNAPSHOT-aws.jar"
}

variable "database_url" {
  description = "URL JDBC do PostgreSQL."
  type        = string
  sensitive   = true
}

variable "database_username" {
  description = "Usuário do PostgreSQL."
  type        = string
  sensitive   = true
}

variable "database_password" {
  description = "Senha do PostgreSQL."
  type        = string
  sensitive   = true
}

variable "jwt_private_key" {
  description = "Chave privada RSA no formato PEM PKCS#8."
  type        = string
  sensitive   = true
}

variable "jwt_issuer" {
  description = "Emissor registrado nos tokens JWT."
  type        = string
  default     = "mecanica-auth"
}

variable "jwt_expiration_seconds" {
  description = "Tempo de validade do JWT em segundos."
  type        = number
  default     = 900

  validation {
    condition     = var.jwt_expiration_seconds > 0
    error_message = "A expiração do JWT deve ser maior que zero."
  }
}