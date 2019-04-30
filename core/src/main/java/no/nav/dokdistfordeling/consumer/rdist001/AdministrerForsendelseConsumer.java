package no.nav.dokdistfordeling.consumer.rdist001;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;

import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.exception.functional.OppdaterForsendelseFunctionalException;
import no.nav.dokdistfordeling.exception.functional.PersisterForsendelseFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.OppdaterForsendelseTechnicalException;
import no.nav.dokdistfordeling.exception.technical.PersisterForsendelseTechnicalException;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final String administrerforsendelseV1Url;
	private final RestTemplate restTemplate;

	@Inject
	public AdministrerForsendelseConsumer(@Value("${administrerforsendelse.v1.url}") String administrerforsendelseV1Url,
										  RestTemplateBuilder restTemplateBuilder,
										  final ServiceuserAlias serviceuserAlias) {
		this.administrerforsendelseV1Url = administrerforsendelseV1Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public PersisterForsendelseResponseTo persisterForsendelse(final PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		try {
			HttpEntity entity = new HttpEntity<>(persisterForsendelseRequestTo, createHeaders(persisterForsendelseRequestTo.getBestillingsId()));
			return restTemplate.exchange(this.administrerforsendelseV1Url, HttpMethod.POST, entity, PersisterForsendelseResponseTo.class)
					.getBody();
		} catch (HttpClientErrorException e) {
			throw new PersisterForsendelseFunctionalException(String.format("Kall mot rdist001 feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new PersisterForsendelseTechnicalException(String.format("Kall mot rdist001 feilet teknisk med statusKode=%s, feilmelding=%s", e.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}

	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String bestillingsId) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders(bestillingsId));
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

	private HttpHeaders createHeaders(String bestillingsId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(CALL_ID, bestillingsId);
		return headers;
	}

}
