package no.nav.dokdistfordeling.consumer.aktoerregister;

import static no.nav.dokdistfordeling.constants.Constants.APP_NAME;
import static no.nav.dokdistfordeling.constants.Constants.BEARER_PREFIX;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.NAV_CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.NAV_CONSUMER_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;
import no.nav.dokdistfordeling.exception.functional.AktoerHentIdentForAktoerIdFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AktoerHentIdentForAktoerIdTechnicalException;
import no.nav.dokdistfordeling.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.Map;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

@Component
@Slf4j
public class AktoerregisterConsumer implements Aktoerregister {

	private static final String NAV_PERSONIDENTER = "Nav-Personidenter";

	private final RestTemplate restTemplate;
	private final String aktoerregisterUrl;
	private final StsRestConsumer stsRestConsumer;

	public AktoerregisterConsumer(RestTemplateBuilder restTemplateBuilder,
								  @Value("${aktoerregister.api.v1.url}") String aktoerregisterUrl,
								  StsRestConsumer stsRestConsumer) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.aktoerregisterUrl = aktoerregisterUrl;
		this.stsRestConsumer = stsRestConsumer;
	}

	@Monitor(value = "dok_metric", extraTags = {"process", "hentIdentForAktoerId"}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = AktoerHentIdentForAktoerIdTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT))
	public String hentIdentForAktoerId(String aktoerId) {
		try {
			final String aktoerIdTrimmed = aktoerId.trim();
			HttpHeaders headers = createHeaders();
			headers.add(NAV_PERSONIDENTER, aktoerIdTrimmed);
			Map<String, IdentInfoForAktoer> response = restTemplate.exchange(aktoerregisterUrl + "/identer?gjeldende=true&identgruppe=NorskIdent",
					HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<Map<String, IdentInfoForAktoer>>() {
					}).getBody();

			assertResponse(response, aktoerIdTrimmed);
			return response.get(aktoerIdTrimmed).getIdenter().get(0).getIdent();
		} catch (HttpClientErrorException e) {
			throw new AktoerHentIdentForAktoerIdFunctionalException(String.format("Funksjonell feil ved kall mot Aktoerregister:hentIdentForAktoerId for aktørId=%s: %s",
					aktoerId, e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new AktoerHentIdentForAktoerIdTechnicalException(String.format("Teknisk feil ved kall mot Aktoerregister:hentIdentForAktoerId for aktørId=%s: %s",
					aktoerId, e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}

	private void assertResponse(Map<String, IdentInfoForAktoer> response, String aktoerId) {
		assertResponseNotNull(response, aktoerId);
		IdentInfoForAktoer identInfoForAktoer = response.get(aktoerId);
		assertNoFeilmelding(identInfoForAktoer, aktoerId);
		assertIdenter(identInfoForAktoer, aktoerId);
	}

	private void assertResponseNotNull(Map<String, IdentInfoForAktoer> response, String aktoerId) {
		if (response == null || response.get(aktoerId) == null) {
			throw new AktoerHentIdentForAktoerIdFunctionalException(String.format("Fikk ingen resons fra Aktoerregister:hentIdentForAktoerId på aktørId=%s.", aktoerId));
		}
	}

	private void assertNoFeilmelding(IdentInfoForAktoer identInfoForAktoer, String aktoerId) {
		if (identInfoForAktoer.getFeilmelding() != null) {
			throw new AktoerHentIdentForAktoerIdFunctionalException(String.format("Feil ved respons fra Aktoerregister:hentIdentForAktoerId på aktørId=%s. Feilmelding=%s",
					aktoerId, identInfoForAktoer.getFeilmelding()));
		}
	}

	private void assertIdenter(IdentInfoForAktoer identInfoForAktoer, String aktoerId) {
		if (identInfoForAktoer.getIdenter() == null || identInfoForAktoer.getIdenter().size() != 1) {
			throw new AktoerHentIdentForAktoerIdFunctionalException(String.format("Feil ved respons fra Aktoerregister:hentIdentForAktoerId på aktørId=%s. Forventet å få tilbake identliste med ett innslag ved forespørsel om gjeldende norskIdent. " +
							"Fikk identliste med %s innslag. Sannsynligvis en feil i aktørregisteret. Aktørregisteret ryddes ved batchjobb hver natt kl 03.00.", aktoerId,
					identInfoForAktoer.getIdenter() == null ? "null" : identInfoForAktoer.getIdenter().size()));
		}
	}
}
