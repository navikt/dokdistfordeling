package no.nav.dokdistfordeling.consumer.regoppslag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseRequestTo;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistfordeling.exception.functional.PersonErDoedUkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.UkjentAdresseException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.token.NaisTexasRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
class RegoppslagRestConsumer {

	private final RestClient restClientTexas;
	private final ObjectMapper objectMapper;
	private final String regoppslagScope;

	RegoppslagRestConsumer(RestClient restClientTexas,
						   final DokdistfordelingProperties dokdistfordelingProperties,
						   ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.regoppslagScope = dokdistfordelingProperties.getEndpoints().getRegoppslag().getScope();
		this.restClientTexas = restClientTexas.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getRegoppslag().getUrl())
				.build();
	}

	@Retryable(retryFor = RegoppslagHentAdresseTechnicalException.class,
			backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public HentMottakerOgAdresseResponseTo.AdresseTo hentAdresse(HentMottakerOgAdresseRequestTo hentMottakerOgAdresseRequest) {
		return restClientTexas.post()
				.uri("/hentMottakerOgAdresse")
				.body(hentMottakerOgAdresseRequest)
				.attribute(TARGET_SCOPE, regoppslagScope)
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
					switch (response.getStatusCode()) {
						case UNAUTHORIZED -> {
							ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
							throw new RegoppslagHentAdresseSecurityException(format("Kall mot TREG002 feilet. Ingen tilgang. feilmelding=%s", problemDetail));
						}
						case NOT_FOUND ->
								throw new UkjentAdresseException("Fant ikke adresseinformasjon for mottaker i PDL. Mottaker har ukjent adresse.");
						case GONE ->
								throw new PersonErDoedUkjentAdresseException("Mottaker er død og har ukjent adresse.");
						default -> {
							ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
							throw new RegoppslagHentAdresseFunctionalException(format("Henting av adresse for bruker feilet funksjonelt mot Regoppslag. status=%s, problemDetail=%s", response.getStatusCode(), problemDetail));
						}
					}
				})
				.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
					ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
					throw new RegoppslagHentAdresseTechnicalException(format("Kall mot TREG002 feilet teknisk. status=%s, feilmelding=%s", response.getStatusCode(), problemDetail));
				})
				.body(HentMottakerOgAdresseResponseTo.class).getAdresse();
	}
}
