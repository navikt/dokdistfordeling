package no.nav.dokdistfordeling.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.consumer.rdist001.domain.Forsendelse;
import no.nav.dokdistfordeling.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistfordeling.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.AdminstrerForsendelseTechnicalException;
import no.nav.dokdistfordeling.metrics.ConsumerMonitor;
import no.nav.dokdistfordeling.security.AzureToken;
import no.nav.dokdistfordeling.security.WebClientAzureAuthentication;
import no.nav.dokdistfordeling.support.NavHeadersFilter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final WebClient webClient;

	public AdministrerForsendelseConsumer(final DokdistfordelingProperties dokdistfordelingProperties,
										  WebClient webClient,
										  AzureToken azureToken) {
		this.webClient = webClient
				.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokdistadmin().getUrl())
				.filter(new WebClientAzureAuthentication(azureToken, dokdistfordelingProperties.getEndpoints().getDokdistadmin().getScope()))
				.filter(new NavHeadersFilter())
				.build();
	}

	@ConsumerMonitor(value = "dok_metric", extraTags = {"process", "opprettForsendelse"}, histogram = true)
	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public String opprettForsendelse(final OpprettForsendelseRequestTo opprettForsendelseRequestTo) {
		var bestillingsId = opprettForsendelseRequestTo.getBestillingsId();

		log.info("opprettForsendelse oppretter forsendelse med bestillingsId={}", bestillingsId);

		var forsendelseId = webClient.post()
				.bodyValue(opprettForsendelseRequestTo)
				.retrieve()
				.bodyToMono(OpprettForsendelseResponseTo.class)
				.doOnError(this::handleError)
				.map(response -> new Forsendelse(response.forsendelseId()).getForsendelseId())
				.block();

		log.info("opprettForsendelse har opprettet forsendelse med forsendelseId={} og bestillingsId={}", forsendelseId, bestillingsId);

		return forsendelseId;
	}

	@ConsumerMonitor(value = "dok_metric", extraTags = {"process", "oppdaterForsendelse"}, histogram = true)
	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelse) {
		log.info("oppdaterForsendelse oppdaterer forsendelse med forsendelseId={}", oppdaterForsendelse.forsendelseId());

		webClient.put()
				.uri("/oppdaterforsendelse")
				.bodyValue(oppdaterForsendelse)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("oppdaterForsendelse har oppdatert forsendelse med forsendelseId={} til forsendelseStatus={}",
				oppdaterForsendelse.forsendelseId(), oppdaterForsendelse.forsendelseStatus());
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new AdminstrerForsendelseFunctionalException(
					format("Kall mot rdist001 feilet funksjonelt med status: %s, feilmelding: %s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			throw new AdminstrerForsendelseTechnicalException(
					format("Kall mot rdist001 feilet feilet teknisk med feilmelding: %s", error.getMessage()),
					error);
		}
	}

}
