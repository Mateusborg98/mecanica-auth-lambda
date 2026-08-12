package br.com.techchallenge.mecanica.auth.infrastructure.lambda;

import java.util.Objects;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import br.com.techchallenge.mecanica.auth.presentation.handler.AuthenticationHandler;

public class LambdaHandler implements
        RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final AuthenticationHandler delegate;

    public LambdaHandler() {
        this(new LambdaBootstrap().createFromEnvironment());
    }

    LambdaHandler(AuthenticationHandler delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(
            APIGatewayV2HTTPEvent event,
            Context context) {

        return delegate.handleRequest(event, context);
    }
}