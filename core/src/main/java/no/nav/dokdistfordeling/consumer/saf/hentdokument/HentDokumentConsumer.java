package no.nav.dokdistfordeling.consumer.saf.hentdokument;

import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;

import no.nav.dokdistfordeling.consumer.sts.StsConsumer;
import no.nav.dokdistfordeling.exception.functional.SafHentDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.SafHentDokumentTechnicalException;
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

import javax.inject.Inject;
import java.time.Duration;
import java.util.Base64;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class HentDokumentConsumer implements HentDokument {

	private final String hentDokumentUrl;
	private final RestTemplate restTemplate;
	private final StsConsumer stsConsumer;

	@Inject
	public HentDokumentConsumer(@Value("${hentdokument.url}") String hentDokumentUrl,
								RestTemplateBuilder restTemplateBuilder,
								StsConsumer stsConsumer) {
		this.hentDokumentUrl = hentDokumentUrl;
		this.stsConsumer = stsConsumer;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public HentDokumentResponseTo hentDokument(String journalpostId, String dokumentInfoId, String variantFormat) {
		try {
			HttpEntity entity = new HttpEntity<>(createAuthorizationHeader());
			String dokumentBase64String = restTemplate.exchange(this.hentDokumentUrl + "/{journalpostId}/{dokumentInfoId}/{variantFormat}", HttpMethod.GET, entity, String.class)
					.getBody();
			return decodeAndMapResponse(dokumentBase64String, journalpostId, dokumentInfoId, variantFormat);
		} catch (HttpClientErrorException e) {
			throw new SafHentDokumentFunctionalException(String.format("Kall mot saf:hentdokument feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new SafHentDokumentTechnicalException(String.format("Kall mot saf:hentdokument feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}

	private HentDokumentResponseTo decodeAndMapResponse(String dokumentBase64String, String journalpostId, String dokumentInfoId, String variantFormat) {
		try {
			return HentDokumentResponseTo.builder()
					.dokument(Base64.getDecoder().decode(dokumentBase64String))
					.build();
		} catch (Exception e) {
			throw new SafHentDokumentTechnicalException(String.format("Kunne ikke dekode dokument. journalpostId=%s, dokumentInfoId=%s, variantFormat=%s. Feilmelding=%s", journalpostId, dokumentInfoId, variantFormat, e
					.getMessage()), e);
		}
	}

	private HttpHeaders createAuthorizationHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, "bearer " + stsConsumer.getOidcToken());
		return headers;
	}

}
