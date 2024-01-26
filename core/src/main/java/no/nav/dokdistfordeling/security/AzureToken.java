package no.nav.dokdistfordeling.security;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.dokdistfordeling.exception.functional.AzureTokenException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.AZURE_TOKEN_CACHE;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@Slf4j
@Component
public class AzureToken {

	private final AzureProperties azureConfig;
	private final WebClient webClient;

	public AzureToken(AzureProperties azureConfig,
					  WebClient webClient) {
		this.azureConfig = azureConfig;
		this.webClient = webClient.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
				.baseUrl(azureConfig.getOpenidConfigTokenEndpoint())
				.build();
	}

	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	@Cacheable(AZURE_TOKEN_CACHE)
	public String accessToken(String scope) {
		return fetchAccessToken(scope);
	}

	private String fetchAccessToken(String scope) {

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("client_id", azureConfig.getAppClientId());
		formData.add("client_secret", azureConfig.getAppClientSecret());
		formData.add("grant_type", "client_credentials");
		formData.add("scope", scope);

		return webClient.post()
				.body(BodyInserters.fromFormData(formData))
				.retrieve()
				.bodyToMono(JsonNode.class)
				.map(jsonNode -> jsonNode.get("access_token").asText())
				.doOnError(this::handleError)
				.block();

	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			throw new AzureTokenException(
					format("Klarte ikke hente token fra Azure. Feilet med statuskode=%s Feilmelding=%s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			throw new AzureTokenException(
					format("Kall mot Azure feilet med feilmelding=%s", error.getMessage()),
					error);
		}
	}
}
