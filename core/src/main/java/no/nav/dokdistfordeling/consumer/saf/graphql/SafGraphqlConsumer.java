package no.nav.dokdistfordeling.consumer.saf.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.constants.Constants;
import no.nav.dokdistfordeling.consumer.NavHeaders;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJsonResponse;
import no.nav.dokdistfordeling.exception.functional.SafBadRequestException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostQueryUnauthorizedException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.exception.technical.MarshalGraphqlRequestToJsonTechnicalException;
import no.nav.dokdistfordeling.exception.technical.SafJournalpostIkkeFunnetTechnicalException;
import no.nav.dokdistfordeling.exception.technical.SafJournalpostQueryTechnicalException;
import no.nav.dokdistfordeling.metrics.ConsumerMonitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MAX_ATTEMPTS_SHORT;

@Component
@Slf4j
public class SafGraphqlConsumer {

	private static final String OIDC_TOKEN_PREFIX = "Bearer";
	private static final String NOT_FOUND = "not_found";
	private static final String FORBIDDEN = "forbidden";
	private static final String SERVER_ERROR = "server_error";
	private static final String BAD_REQUEST = "bad_request";
	private final RestTemplate restTemplate;
	private final String graphQLurl;

	public SafGraphqlConsumer(RestTemplateBuilder restTemplateBuilder,
							  @Value("${saf.graphql.url}") String graphQLurl) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.graphQLurl = graphQLurl;
	}

	@ConsumerMonitor(value = "dok_metric", extraTags = {"process", "safJournalpostquery"}, histogram = true)
	@Retryable(include = SafJournalpostQueryTechnicalException.class, maxAttempts = MAX_ATTEMPTS_SHORT, backoff = @Backoff(delay = DELAY_SHORT))
	public SafJournalpostTo performQuery(GraphQLRequest graphQLRequest, String authorizationHeader) {

		try {
			HttpHeaders httpHeaders = createAuthHeaderFromToken(authorizationHeader);

			SafJsonResponse result = restTemplate.exchange(graphQLurl, HttpMethod.POST, new HttpEntity<>(requestToJson(graphQLRequest), httpHeaders), SafJsonResponse.class).getBody();

			if (result.getErrors() != null && result.getErrors().size() > 0) {
				no.nav.dokdistfordeling.consumer.saf.journalpost.SafJsonResponse.Error safError = result.getErrors().get(0);
				String safErrorCode = safError.getExtensions().getCode();

				switch (safErrorCode) {
					case NOT_FOUND ->
							throw new SafJournalpostIkkeFunnetTechnicalException("Fant ikke journalposten i fagarkivet");
					case FORBIDDEN -> throw new SafJournalpostQueryUnauthorizedException(
							"Saksbehandler har ikke tilgang til journalposten. Feilmelding fra SAF: " + safError.getMessage());
					case SERVER_ERROR -> {
						log.warn("Teknisk feil mot SAF. Feilmelding: " + safError.getMessage());
						throw new SafJournalpostQueryTechnicalException(safError.getMessage());
					}
					case BAD_REQUEST ->
							throw new SafBadRequestException("Bad request mot SAF: " + safError.getMessage());
					default ->
							throw new SafBadRequestException("Ukjent funksjonell feil mot SAF: " + safError.getMessage());
				}
			}

			return result.getData().getJournalpost();

		} catch (HttpClientErrorException e) {
			throw new SafJournalpostQueryUnauthorizedException(String.format("Henting av journalpost feilet med status: %s. Skyldes sannsynligvis at appen som gjorde kallet ikke har tilgang til SAF. " +
					"For å få tilgang må appen som kaller dokdistfordeling legges til i SAF sin <env-config.json>. Feilmelding: %s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new SafJournalpostQueryTechnicalException(String.format("Tjenesten SAF (graphQL) feilet med status: %s, feilmelding: %s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createAuthHeaderFromToken(String authorizationHeader) {
		HttpHeaders headers = new HttpHeaders();
		if (authorizationHeader == null || !OIDC_TOKEN_PREFIX.equalsIgnoreCase(authorizationHeader.split(" ")[0])) {
			throw new ValidationException("Authorization header må være på formen Bearer {token}");
		}

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(NavHeaders.NAV_CALL_ID, MDC.get(Constants.CALL_ID));
		headers.set(HttpHeaders.AUTHORIZATION, OIDC_TOKEN_PREFIX + " " + authorizationHeader.split(" ")[1]);
		return headers;
	}

	private String requestToJson(GraphQLRequest graphQLRequest) {
		try {
			return new ObjectMapper().writeValueAsString(graphQLRequest);
		} catch (JsonProcessingException e) {
			throw new MarshalGraphqlRequestToJsonTechnicalException(String.format("Kunne ikke konvertere graphQlRequest til json, feilmelding=%s", e
					.getMessage()), e);
		}
	}
}
