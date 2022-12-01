package no.nav.dokdistfordeling.consumer.regoppslag;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.constants.Constants;
import no.nav.dokdistfordeling.consumer.NavHeaders;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseRequestTo;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;
import no.nav.dokdistfordeling.exception.functional.PersonErDoedUkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.UkjentAdresseException;
import no.nav.dokdistfordeling.exception.technical.StsTechnicalException;
import no.nav.dokdistfordeling.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;

@Slf4j
@Component
class RegoppslagRestConsumer {

	private final RestTemplate restTemplate;
	private final String regoppslagUrl;
	private final StsRestConsumer stsRestConsumer;

	public RegoppslagRestConsumer(RestTemplateBuilder restTemplateBuilder,
								  @Value("${regoppslag.url}") String regoppslagUrl,
								  final ServiceuserAlias serviceuserAlias,
								  StsRestConsumer stsRestConsumer) {
		this.regoppslagUrl = regoppslagUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
		this.stsRestConsumer = stsRestConsumer;
	}

	@Monitor(value = "dok_consumer", extraTags = {"process", "treg002HentAdresse"}, histogram = true)
	@Retryable(include = RegoppslagHentAdresseTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	HentMottakerOgAdresseResponseTo.AdresseTo hentAdresse(HentMottakerOgAdresseRequestTo request) {
		HttpEntity<HentMottakerOgAdresseRequestTo> entity = createRequestWithHeader(request, retrieveOidcTokenAndCreateHeader());
		try {
			return restTemplate.postForObject(this.regoppslagUrl + "/hentMottakerOgAdresse", entity, HentMottakerOgAdresseResponseTo.class)
					.getAdresse();
		} catch (HttpClientErrorException e) {
			if (HttpStatus.UNAUTHORIZED == e.getStatusCode()) {
				throw new RegoppslagHentAdresseSecurityException(format("Kall mot TREG002 feilet. Ingen tilgang. feilmelding=%s", e
						.getMessage()), e);
			} else if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
				throw new UkjentAdresseException("Mottaker har ukjent adresse.", e);
			} else if (HttpStatus.GONE == e.getStatusCode()) {
				throw new PersonErDoedUkjentAdresseException(format("Mottaker er død og har ukjent adresse. Status=%s", e.getStatusCode()), e);
			} else {
				throw new RegoppslagHentAdresseFunctionalException(format("Kunne ikke hente adresse for bruker. status=%s, feilmelding=%s", e
						.getStatusCode(), e.getMessage()), e);
			}
		} catch (HttpServerErrorException e) {
			throw new RegoppslagHentAdresseTechnicalException(format("Kall mot TREG002 feilet teknisk. status=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders retrieveOidcTokenAndCreateHeader() {
		try {
			String oidcToken = stsRestConsumer.getOidcToken();
			HttpHeaders httpHeaders = new HttpHeaders();
			final String callId = MDC.get(Constants.CALL_ID);
			httpHeaders.set(Constants.CALL_ID, callId);
			httpHeaders.set(NavHeaders.NAV_CALL_ID, callId);
			httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + oidcToken);
			return httpHeaders;
		} catch (StsTechnicalException e) {
			throw new RegoppslagHentAdresseTechnicalException(format("Henting av oidctoken fra STS feilet. Feilmelding=%s", e.getMessage()), e);
		}
	}

	private HttpEntity<HentMottakerOgAdresseRequestTo> createRequestWithHeader(HentMottakerOgAdresseRequestTo request, HttpHeaders httpHeaders) {
		return new HttpEntity<>(request, httpHeaders);
	}
}
