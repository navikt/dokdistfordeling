package no.nav.dokdistfordeling.consumer.saf.graphql;

import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertParameterIsAsExpected;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJsonJournalpost;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostIkkeFunnetFunctionalException;
import no.nav.dokdistfordeling.exception.technical.DokdistFordelingConvertToJsonTechnicalException;
import no.nav.dokdistfordeling.exception.technical.DokdistfordelingJournalpostQueryTechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

@Component
@Slf4j
public class SafGraphqlConsumer {

	private static final String OIDC_TOKEN_PREFIX = "Bearer";
	private final RestTemplate restTemplate;
	private final String graphQLurl;

	@Inject
	public SafGraphqlConsumer(RestTemplateBuilder restTemplateBuilder,
							  @Value("${saf.graphql.url}") String graphQLurl) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.graphQLurl = graphQLurl;
	}

	@Retryable(include = DokdistfordelingJournalpostQueryTechnicalException.class, maxAttempts = MAX_ATTEMPTS_SHORT, backoff = @Backoff(delay = DELAY_SHORT))
	public Journalpost performQuery(GraphQLRequest graphQLRequest, String authorizationHeader) {

		try {
			HttpHeaders httpHeaders = createAuthHeaderFromToken(authorizationHeader);

			ResponseEntity<SafJsonJournalpost> responseEntity = restTemplate.exchange(graphQLurl, HttpMethod.POST, new HttpEntity<>(requestToJson(graphQLRequest), httpHeaders), SafJsonJournalpost.class);

			if (responseEntity.getStatusCodeValue() > HttpStatus.OK.value()) {
				throw new DokdistfordelingJournalpostQueryTechnicalException(String.format("Saf respons var ikke OK. Statuskode: %s", responseEntity.getStatusCode()));
			}
			if (responseEntity.getBody().getData().getJournalpost() == null) {
				throw new SafJournalpostIkkeFunnetFunctionalException("Datafeltet mottatt fra saf var null.");
			}

			return responseEntity.getBody().getJournalpost();

		} catch (HttpClientErrorException e) {
			log.warn("Kallet til SAF (graphQL) feilet: " + e.getMessage());
			throw new DokdistfordelingJournalpostQueryTechnicalException(String.format("Kallet til SAF (graphQL) feilet med status=%s feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.warn("Tjenesten SAF (graphQL) feilet: " + e.getMessage());
			throw new DokdistfordelingJournalpostQueryTechnicalException(String.format("Tjenesten SAF (graphQL) feilet med status=%s feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createAuthHeaderFromToken(String authorizationHeader) {
		HttpHeaders headers = new HttpHeaders();
		assertParameterIsAsExpected("authorization header prefix", authorizationHeader.split(" ")[0], OIDC_TOKEN_PREFIX);

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(HttpHeaders.AUTHORIZATION, authorizationHeader);
		return headers;
	}

	private String requestToJson(GraphQLRequest graphQLRequest) {
		try {
			return new ObjectMapper().writeValueAsString(graphQLRequest);
		} catch (JsonProcessingException e) {
			throw new DokdistFordelingConvertToJsonTechnicalException(String.format("Kunne ikke konvertere graphqlrequest objekt til json, feilmelding=%s", e.getMessage()), e);
		}
	}
}
