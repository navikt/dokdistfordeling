package no.nav.dokdistfordeling.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.azure.AzureConfig;
import no.nav.dokdistfordeling.exception.functional.AzureTokenException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.net.URI;
import java.util.Map;

import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.AZURE_TOKEN_CACHE;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class AzureToken {

    private final AzureConfig azureConfig;
    private final ObjectMapper objectMapper;
    private final String proxyHost;

    public AzureToken(AzureConfig azureConfig,
                      ObjectMapper objectMapper,
                      @Value("${proxy.host:#{null}}") String proxyHost) {
        this.azureConfig = azureConfig;
        this.objectMapper = objectMapper;
        this.proxyHost = proxyHost;
    }

    @Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
    @Cacheable(AZURE_TOKEN_CACHE)
    public String accessToken() {
		return fetchAccessToken();
    }

    private String fetchAccessToken() {

        HttpClient httpClient = HttpClient.create();
        if (!isBlank(proxyHost)) {
            var proxyUri = URI.create(proxyHost);
            httpClient = httpClient
                    .proxy(proxy -> proxy
                            .type(ProxyProvider.Proxy.HTTP)
                            .host(proxyUri.getHost())
                            .port(proxyUri.getPort()));
        }
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        WebClient webClient = WebClient.builder()
                .clientConnector(connector)
                .build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", azureConfig.getClientId());
        formData.add("client_secret", azureConfig.getClientSecret());
        formData.add("grant_type", "client_credentials");
        formData.add("scope", azureConfig.getScope());

        String responseJson = webClient.post()
                .uri(azureConfig.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(this::handleError)
                .block();

        try {
            Map<String, Object> tokenData = objectMapper.readValue(responseJson, Map.class);
            return (String) tokenData.get("access_token");
        } catch (JsonProcessingException | ClassCastException e) {
            throw new AzureTokenException(String.format("Klarte ikke parse token fra Azure. Feilmelding=%s", e.getMessage()), e);
        }
    }

    private void handleError(Throwable error) {
        if(error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
            throw new AzureTokenException(
                    String.format("Klarte ikke hente token fra Azure. Feilet med statuskode=%s Feilmelding=%s",
                            response.getRawStatusCode(),
                            response.getMessage()),
                    error);
        } else {
            throw new AzureTokenException(
                    String.format("Kall mot Azure feilet med feilmelding=%s", error.getMessage()),
                    error);
        }

    }

}
