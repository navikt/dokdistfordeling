package no.nav.dokdistfordeling.consumer.regoppslag;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseRequestTo;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;
import no.nav.dokdistfordeling.exception.functional.PersonErDoedUkjentAdresseException;
import no.nav.dokdistfordeling.exception.functional.UkjentAdresseException;
import no.nav.dokdistfordeling.exception.technical.StsTechnicalException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
class RegoppslagRestConsumer {

	private final RestTemplate restTemplate;
	private final String regoppslagUrl;
	private final StsRestConsumer stsRestConsumer;

	public RegoppslagRestConsumer(RestTemplateBuilder restTemplateBuilder,
								  @Value("${regoppslag.url}") String regoppslagUrl,
								  final DokdistfordelingProperties dokdistfordelingProperties,
								  StsRestConsumer stsRestConsumer) {
		this.regoppslagUrl = regoppslagUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokdistfordelingProperties.getServiceuser().getUsername(), dokdistfordelingProperties.getServiceuser().getPassword())
				.build();
		this.stsRestConsumer = stsRestConsumer;
	}

	@Retryable(retryFor = RegoppslagHentAdresseTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	HentMottakerOgAdresseResponseTo.AdresseTo hentAdresse(HentMottakerOgAdresseRequestTo request) {
		HttpEntity<HentMottakerOgAdresseRequestTo> entity = createRequestWithHeader(request, retrieveOidcTokenAndCreateHeader());
		try {
			return restTemplate.postForObject(this.regoppslagUrl + "/hentMottakerOgAdresse", entity, HentMottakerOgAdresseResponseTo.class).getAdresse();
		} catch (HttpClientErrorException e) {
			if (UNAUTHORIZED == e.getStatusCode()) {
				throw new RegoppslagHentAdresseSecurityException(format("Kall mot TREG002 feilet. Ingen tilgang. feilmelding=%s", e.getMessage()), e);
			} else if (NOT_FOUND == e.getStatusCode()) {
				throw new UkjentAdresseException("Fant ikke adresseinformasjon for mottaker i PDL. Mottaker har ukjent adresse.", e);
			} else if (GONE == e.getStatusCode()) {
				throw new PersonErDoedUkjentAdresseException("Mottaker er død og har ukjent adresse.", e);
			} else {
				throw new RegoppslagHentAdresseFunctionalException(format("Henting av adresse for bruker feilet funksjonelt mot Regoppslag / PDL. Dette skyldes mest sannsynlig at bruker ikke har en gyldig adresse. status=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
			}
		} catch (HttpServerErrorException e) {
			throw new RegoppslagHentAdresseTechnicalException(format("Kall mot TREG002 feilet teknisk. status=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders retrieveOidcTokenAndCreateHeader() {
		try {
			String oidcToken = stsRestConsumer.getOidcToken();
			HttpHeaders httpHeaders = new HttpHeaders();
			final String callId = MDC.get(CALL_ID);
			httpHeaders.set(CALL_ID, callId);
			httpHeaders.set(NAV_CALL_ID, callId);
			httpHeaders.setBearerAuth(oidcToken);
			return httpHeaders;
		} catch (StsTechnicalException e) {
			throw new RegoppslagHentAdresseTechnicalException(format("Henting av oidctoken fra STS feilet. Feilmelding=%s", e.getMessage()), e);
		}
	}

	private HttpEntity<HentMottakerOgAdresseRequestTo> createRequestWithHeader(HentMottakerOgAdresseRequestTo request, HttpHeaders httpHeaders) {
		return new HttpEntity<>(request, httpHeaders);
	}
}
