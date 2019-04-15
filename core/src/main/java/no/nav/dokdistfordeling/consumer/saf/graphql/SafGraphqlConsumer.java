package no.nav.dokdistfordeling.consumer.saf.graphql;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJsonJournalpost;
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
public class SafGraphqlConsumer {

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

	@Retryable(include = DokdistfordelingJournalpostQueryTechnicalException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
	public Journalpost performQuery(GraphQLRequest graphQLRequest, String authorizationHeader) {
		try {
			HttpHeaders httpHeaders = createAuthHeaderFromToken(authorizationHeader);
			ResponseEntity<SafJsonJournalpost> result = restTemplate.exchange(graphQLurl, HttpMethod.POST, new HttpEntity<>(graphQLRequest, httpHeaders), SafJsonJournalpost.class);

			if (result.getStatusCodeValue() > HttpStatus.OK.value()) {
				throw new DokdistfordelingJournalpostQueryTechnicalException(String.format("Saf respons var ikke OK. Statuskode: %s", result.getStatusCode()));
			}
			if (result.getBody() == null) { // todo specify exception
				throw new DokdistfordelingJournalpostQueryTechnicalException("Tom responsebody fra motatt fra saf.");
			}

			return result.getBody().getJournalpost();

		} catch (HttpClientErrorException e) { // todo two technical exceptions, correct?
			throw new DokdistfordelingJournalpostQueryTechnicalException(String.format("Kallet til SAF (graphQL) feilet med status=%s feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DokdistfordelingJournalpostQueryTechnicalException(String.format("Tjenesten SAF (graphQL) feilet med status=%s feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createAuthHeaderFromToken(String authorizationHeader) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(HttpHeaders.AUTHORIZATION, authorizationHeader);
		return headers;
	}
}
