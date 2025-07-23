package no.nav.dokdistfordeling.consumer.saf.graphql;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJsonResponse;
import no.nav.dokdistfordeling.exception.functional.SafBadRequestException;
import no.nav.dokdistfordeling.exception.functional.SafJournalpostQueryUnauthorizedException;
import no.nav.dokdistfordeling.exception.technical.SafJournalpostIkkeFunnetTechnicalException;
import no.nav.dokdistfordeling.exception.technical.SafJournalpostQueryTechnicalException;
import no.nav.dokdistfordeling.exception.technical.SafUkjentErrorCodeException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.consumer.token.NaisTexasRequestInterceptor.TARGET_SCOPE;
import static no.nav.dokdistfordeling.util.MappingUtil.splitBearerToken;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j
public class SafGraphqlConsumer {

	private static final String NOT_FOUND = "not_found";
	private static final String FORBIDDEN = "forbidden";
	private static final String SERVER_ERROR = "server_error";
	private static final String BAD_REQUEST = "bad_request";
	private static final String CLASSIFICATION_VALIDATIONERROR = "ValidationError";

	private final RestClient restClientTexas;
	private final String safScope;

	public SafGraphqlConsumer(RestClient restClientTexas,
							  DokdistfordelingProperties dokdistfordelingProperties) {
		this.restClientTexas = restClientTexas.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getSaf().getUrl())
				.build();
		this.safScope = dokdistfordelingProperties.getEndpoints().getSaf().getScope();
	}

	@Retryable(retryFor = SafJournalpostQueryTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT))
	public SafJournalpostTo performQuery(GraphQLRequest graphQLRequest, Optional<String> authorizationHeader) {
		try {
			SafJsonResponse result = restClientTexas.post()
					.uri("/graphql")
					.body(graphQLRequest)
					.headers(httpHeaders -> {
						httpHeaders.setContentType(APPLICATION_JSON);
						authorizationHeader.ifPresent(bearerToken -> httpHeaders.setBearerAuth(splitBearerToken(bearerToken)));
					})
					.attributes(attributes -> {
						if (authorizationHeader.isEmpty()) {
							attributes.put(TARGET_SCOPE, safScope);
						}
					})
					.retrieve()
					.body(SafJsonResponse.class);

			if (result != null && result.getErrors() != null && !result.getErrors().isEmpty()) {
				SafJsonResponse.Error safError = result.getErrors().getFirst();
				if (safError.getExtensions().getClassification().contains(CLASSIFICATION_VALIDATIONERROR)) {
					throw new SafJournalpostQueryTechnicalException("Feil i saf query: " + safError.getMessage());
				}
				String safErrorCode = safError.getExtensions().getCode();

				switch (safErrorCode) {
					case NOT_FOUND ->
							throw new SafJournalpostIkkeFunnetTechnicalException("Fant ikke journalposten i fagarkivet");
					case FORBIDDEN ->
							throw new SafJournalpostQueryUnauthorizedException("Saksbehandler har ikke tilgang til journalposten. Feilmelding fra SAF: " + safError.getMessage());
					case SERVER_ERROR -> {
						log.warn("Teknisk feil mot SAF. Feilmelding: " + safError.getMessage());
						throw new SafJournalpostQueryTechnicalException(safError.getMessage());
					}
					case BAD_REQUEST ->
							throw new SafBadRequestException("Bad request mot SAF: " + safError.getMessage());
					default ->
							throw new SafUkjentErrorCodeException("Ukjent error code fra SAF. Håndtering av ny feilkode må legges inn her. Feilmelding: " + safError.getMessage());
				}
			}

			return result.getData().getJournalpost();

		} catch (HttpClientErrorException e) {
			throw new SafJournalpostQueryUnauthorizedException(
					format("Henting av journalpost feilet med status: %s. Skyldes sannsynligvis at appen som gjorde kallet ikke har tilgang til SAF. " +
						   "For å få tilgang må appen som kaller dokdistfordeling legges til i SAF sin <env-config.json>. Feilmelding: %s", e.getStatusCode(), e.getMessage()),
					e);
		} catch (HttpServerErrorException e) {
			throw new SafJournalpostQueryTechnicalException(format("Tjenesten SAF (graphQL) feilet med status: %s, feilmelding: %s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}
}
