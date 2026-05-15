package no.nav.dokdistfordeling.consumer.regoppslag;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseRequestTo;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo.AdresseTo;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.functional.PersonErDoedUkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.UkjentAdresseException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.consumer.token.NaisTexasRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
class RegoppslagRestConsumer {

	private static final String RESILIENCE4J_INSTANCE = "regoppslag";

	private final RestClient restClientTexas;
	private final JsonMapper objectMapper;
	private final String regoppslagScope;

	RegoppslagRestConsumer(RestClient restClientTexas,
						   DokdistfordelingProperties dokdistfordelingProperties,
						   JsonMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.regoppslagScope = dokdistfordelingProperties.getEndpoints().getRegoppslag().getScope();
		this.restClientTexas = restClientTexas.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getRegoppslag().getUrl())
				.build();
	}

	@Retryable(includes = AbstractDokdistfordelingTechnicalException.class)
	@CircuitBreaker(name = RESILIENCE4J_INSTANCE)
	public AdresseTo hentAdresse(HentMottakerOgAdresseRequestTo hentMottakerOgAdresseRequest) {
		return restClientTexas.post()
				.uri("/hentMottakerOgAdresse")
				.body(hentMottakerOgAdresseRequest)
				.attribute(TARGET_SCOPE, regoppslagScope)
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
					switch (response.getStatusCode()) {
						case UNAUTHORIZED -> {
							ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
							throw new RegoppslagSecurityException(format("Kall mot TREG002 feilet. Ingen tilgang. problemDetail=%s", problemDetail));
						}
						case NOT_FOUND -> throw new UkjentAdresseException("Fant ikke adresseinformasjon for mottaker i PDL. Mottaker har ukjent adresse.");
						case GONE -> throw new PersonErDoedUkjentAdresseException("Mottaker er død og har ukjent adresse.");
						default -> {
							ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
							throw new RegoppslagFunctionalException(format("Henting av adresse for bruker feilet funksjonelt mot Regoppslag. status=%s, problemDetail=%s", response.getStatusCode(), problemDetail));
						}
					}
				})
				.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
					ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
					throw new RegoppslagTechnicalException(format("Kall mot TREG002 feilet teknisk. status=%s, problemDetail=%s", response.getStatusCode(), problemDetail));
				})
				.body(HentMottakerOgAdresseResponseTo.class).getAdresse();
	}
}
