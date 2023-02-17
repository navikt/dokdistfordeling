package no.nav.dokdistfordeling.security;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public record WebClientAzureAuthentication(AzureToken azureToken, String scope) implements ExchangeFilterFunction {
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(ClientRequest.from(request).headers((headers) ->
            headers.setBearerAuth(azureToken.accessToken(scope))
        ).build());
    }
}
