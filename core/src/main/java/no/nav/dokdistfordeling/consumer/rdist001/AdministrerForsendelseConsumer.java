package no.nav.dokdistfordeling.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.consumer.NavHeaders;
import no.nav.dokdistfordeling.consumer.rdist001.domain.Forsendelse;
import no.nav.dokdistfordeling.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistfordeling.exception.functional.OppdaterForsendelseFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.AdminstrerForsendelseTechnicalException;
import no.nav.dokdistfordeling.exception.technical.OppdaterForsendelseTechnicalException;
import no.nav.dokdistfordeling.metrics.ConsumerMonitor;
import no.nav.dokdistfordeling.security.AzureToken;
import no.nav.dokdistfordeling.security.WebClientAzureAuthentication;
import no.nav.dokdistfordeling.support.NavHeadersFilter;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final String administrerforsendelseV1Url;
	private final RestTemplate restTemplate;
	private final WebClient webClient;

	public AdministrerForsendelseConsumer(@Value("${administrerforsendelse.v1.url}") String administrerforsendelseV1Url,
										  RestTemplateBuilder restTemplateBuilder,
										  final DokdistfordelingProperties dokdistfordelingProperties,
										  WebClient webClient,
										  AzureToken azureToken) {
		this.administrerforsendelseV1Url = administrerforsendelseV1Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokdistfordelingProperties.getServiceuser().getUsername(), dokdistfordelingProperties.getServiceuser().getPassword())
				.build();

		this.webClient = webClient
				.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokdistadmin().getUrl())
				.filter(new WebClientAzureAuthentication(azureToken, dokdistfordelingProperties.getEndpoints().getDokdistadmin().getScope()))
				.filter(new NavHeadersFilter())
				.build();
	}

	@ConsumerMonitor(value = "dok_metric", extraTags = {"process", "opprettForsendelse"}, histogram = true)
	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
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

	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String bestillingsId) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders());
			String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
					.queryParam("forsendelseId", forsendelseId)
					.queryParam("forsendelseStatus", forsendelseStatus)
					.toUriString();
			restTemplate.exchange(uri, HttpMethod.PUT, entity, Object.class);
		} catch (HttpClientErrorException e) {
			throw new OppdaterForsendelseFunctionalException(String.format("Kall mot rdist001 feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new OppdaterForsendelseTechnicalException(String.format("Kall mot rdist001 feilet teknisk med statusKode=%s, feilmelding=%s", e.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new AdminstrerForsendelseFunctionalException(
					String.format("Kall mot rdist001 feilet funksjonelt med status: %s, feilmelding: %s",
							response.getRawStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new AdminstrerForsendelseTechnicalException(
					String.format("Kall mot rdist001 feilet feilet teknisk med feilmelding: %s", error.getMessage()),
					error) {
			};
		}
	}


	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		final String callId = MDC.get(CALL_ID);
		headers.set(CALL_ID, callId);
		headers.set(NavHeaders.NAV_CALL_ID, callId);
		return headers;
	}

}
