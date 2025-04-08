package no.nav.dokdistfordeling.consumer.saf.hentdokument;

import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;
import no.nav.dokdistfordeling.exception.functional.SafHentDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.SafHentDokumentTechnicalException;
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
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class HentDokumentConsumer implements HentDokument {

	private final String hentDokumentUrl;
	private final RestTemplate restTemplate;
	private final StsRestConsumer stsRestConsumer;

	public HentDokumentConsumer(@Value("${hentdokument.url}") String hentDokumentUrl,
								RestTemplateBuilder restTemplateBuilder,
								StsRestConsumer stsRestConsumer) {
		this.hentDokumentUrl = hentDokumentUrl;
		this.stsRestConsumer = stsRestConsumer;
		this.restTemplate = restTemplateBuilder
				.readTimeout(Duration.ofSeconds(20))
				.connectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public HentDokumentResponseTo hentDokument(String journalpostId, String dokumentInfoId, String variantFormat) {
		try {
			HttpEntity entity = new HttpEntity<>(createAuthorizationHeader());
			byte[] dokument = restTemplate.exchange(this.hentDokumentUrl + "/{journalpostId}/{dokumentInfoId}/{variantFormat}", GET, entity, byte[].class, journalpostId, dokumentInfoId, variantFormat).getBody();

			return mapResponse(dokument, journalpostId, dokumentInfoId, variantFormat);

		} catch (HttpClientErrorException e) {
			throw new SafHentDokumentFunctionalException(
					format("Kall mot saf:hentdokument feilet funksjonelt med statusKode=%s, feilmelding=%s", e.getStatusCode(), e.getResponseBodyAsString()),
					e);
		} catch (HttpServerErrorException e) {
			throw new SafHentDokumentTechnicalException(
					format("Kall mot saf:hentdokument feilet teknisk med statusKode=%s, feilmelding=%s", e.getStatusCode(), e.getResponseBodyAsString()),
					e);
		}
	}

	private HentDokumentResponseTo mapResponse(byte[] dokument, String journalpostId, String dokumentInfoId, String variantFormat) {
		try {
			return HentDokumentResponseTo.builder()
					.dokument(dokument)
					.build();
		} catch (Exception e) {
			throw new SafHentDokumentFunctionalException(
					format("Kunne ikke dekode dokument, da dokumentet ikke er base64-encodet journalpostId=%s, dokumentInfoId=%s, variantFormat=%s. Feilmelding=%s", journalpostId, dokumentInfoId, variantFormat, e.getMessage()),
					e);
		}
	}

	private HttpHeaders createAuthorizationHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.set(NAV_CALL_ID, MDC.get(CALL_ID));
		headers.setBearerAuth(stsRestConsumer.getOidcToken());
		return headers;
	}

}
