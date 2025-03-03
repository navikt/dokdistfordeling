package no.nav.dokdistfordeling.consumer.dokarkiv;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.JournalpostApiFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.JournalpostApiTechnicalException;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKARKIV;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class JournalpostApi {

	private final WebClient webClient;

	public JournalpostApi(DokdistfordelingProperties dokdistfordelingProperties,
						  WebClient webClient) {
		this.webClient = webClient
				.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokarkiv().getUrl())
				.build();
	}

	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterDistribusjonsinfo(String journalpostId, OppdaterDistribusjonsinfoTo oppdaterDistibusjonsinfoTo) {

		webClient.patch()
				.uri("/{journalpostId}/oppdaterDistribusjonsinfo", validateJournalpostId(journalpostId))
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.body(Mono.just(oppdaterDistibusjonsinfoTo), OppdaterDistribusjonsinfoTo.class)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();
	}

	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public OppdaterJournalpostResponse oppdaterJournalpost(String journalpostId, OppdaterJournalpostRequest oppdaterJournalpostRequest) {

		return webClient.put()
				.uri("/{journalpostId}", validateJournalpostId(journalpostId))
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.body(Mono.just(oppdaterJournalpostRequest), OppdaterJournalpostRequest.class)
				.retrieve()
				.bodyToMono(OppdaterJournalpostResponse.class)
				.doOnError(this::handleError)
				.block();
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new JournalpostApiFunctionalException(
					format("Kall mot JournalpostAPI feilet med status=%s, feilmelding=%s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			throw new JournalpostApiTechnicalException(
					format("Kall mot JournalpostAPI feilet med feilmelding=%s", error.getMessage()),
					error);
		}
	}

	private Long validateJournalpostId(String journalpostId) {
		try {
			return Long.valueOf(journalpostId);
		} catch (NumberFormatException e) {
			throw new JournalpostApiTechnicalException(
					format("%s er ikke en gyldig journalpostId. Kan ikke kalle journalpostApi.", journalpostId), e);
		}
	}
}
