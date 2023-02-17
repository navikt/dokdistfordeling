package no.nav.dokdistfordeling.consumer.dokarkiv;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.JournalpostApiFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.JournalpostApiTechnicalException;
import no.nav.dokdistfordeling.metrics.ConsumerMonitor;
import no.nav.dokdistfordeling.security.AzureToken;
import no.nav.dokdistfordeling.security.WebClientAzureAuthentication;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class JournalpostApi {

	private final WebClient webClient;

	public JournalpostApi(AzureToken azureToken,
						  DokdistfordelingProperties dokdistfordelingProperties,
						  WebClient webClient) {
		this.webClient = webClient
				.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokarkiv().getUrl())
				.filter(new WebClientAzureAuthentication(azureToken, dokdistfordelingProperties.getEndpoints().getDokarkiv().getScope()))
				.build();
	}

	@ConsumerMonitor(value = "dok_metric", extraTags = {"process", "oppdaterDistribusjonsinfo"}, histogram = true)
	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterDistribusjonsinfo(String journalpostId, OppdaterDistribusjonsinfoTo oppdaterDistibusjonsinfoTo) {

		webClient.patch()
				.uri("/{journalpostId}/oppdaterDistribusjonsinfo", validateJournalpostId(journalpostId))
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.body(Mono.just(oppdaterDistibusjonsinfoTo), OppdaterDistribusjonsinfoTo.class)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();
	}

	@ConsumerMonitor(value = "dok_metric", extraTags = {"process", "oppdaterJournalpost"}, histogram = true)
	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public OppdaterJournalpostResponse oppdaterJournalpost(String journalpostId, OppdaterJournalpostRequest oppdaterJournalpostRequest) {

		return webClient.put()
				.uri("/" + validateJournalpostId(journalpostId))
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.body(Mono.just(oppdaterJournalpostRequest), OppdaterJournalpostRequest.class)
				.retrieve()
				.bodyToMono(OppdaterJournalpostResponse.class)
				.doOnError(this::handleError)
				.block();
	}

	private void handleError(Throwable error) {
		if(error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new JournalpostApiFunctionalException(
					String.format("Kall mot JournalpostAPI feilet med status=%s, feilmelding=%s",
							response.getRawStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new JournalpostApiTechnicalException(
					String.format("Kall mot JournalpostAPI feilet med feilmelding=%s", error.getMessage()),
					error);
		}
	}

	private Long validateJournalpostId(String journalpostId) {
		try {
			return Long.valueOf(journalpostId);
		} catch (NumberFormatException e) {
			throw new JournalpostApiTechnicalException(
					String.format("%s er ikke en gyldig journalpostId. Kan ikke kalle journalpostApi.", journalpostId), e);
		}
	}
}
