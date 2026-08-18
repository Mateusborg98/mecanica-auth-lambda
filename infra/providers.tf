provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "mecanica"
      Component   = "auth-lambda"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}