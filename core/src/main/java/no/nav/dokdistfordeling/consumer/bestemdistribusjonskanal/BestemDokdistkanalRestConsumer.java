package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;


import static no.nav.dokdistfordeling.constants.Constants.DITT_NAV;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.DITTNAV;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.constants.Constants;
import no.nav.dokdistfordeling.exception.functional.BestemDokdistKanalFunctionalException;
import no.nav.dokdistfordeling.exception.functional.BestemDokdistKanalMappingException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.BestemDokdistKanalTechnicalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
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

import javax.inject.Inject;
import java.time.Duration;

@Component
public class BestemDokdistkanalRestConsumer implements BestemDistribusjonskanal {

	private final RestTemplate restTemplate;

	private final String bestemDokdistKanalUrl;

	@Inject
	public BestemDokdistkanalRestConsumer(RestTemplateBuilder restTemplateBuilder,
										  final ServiceuserAlias serviceuserAlias,
										  @Value("${bestemDistribusjonKanal_url}") String bestemDistKanalUrl) {
		this.bestemDokdistKanalUrl = bestemDistKanalUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DistribusjonsKanalCode bestemKanal(DokDistKanalRequest dokDistKanalRequest) {
		try {
			HttpEntity<DokDistKanalRequest> httpEntity = new HttpEntity<>(dokDistKanalRequest, httpHeaders());
			DokDistKanalResponseTo dokDistKanalResponseTo = restTemplate.postForObject(bestemDokdistKanalUrl, httpEntity, DokDistKanalResponseTo.class);
			return mapToDistribusjonKanalCode(dokDistKanalResponseTo.getDistribusjonsKanal());

		} catch (HttpClientErrorException e) {
			throw new BestemDokdistKanalFunctionalException("BestemDokdistkanal feilet med statusCode=" + e.getRawStatusCode() + ", melding=" + e
					.getResponseBodyAsString(), e);
		} catch (HttpServerErrorException e) {
			throw new BestemDokdistKanalTechnicalException("BestemDokdistkanal feilet med statusCode=" + e.getRawStatusCode() + ", melding=" + e
					.getResponseBodyAsString(), e);
		}
	}

	private DistribusjonsKanalCode mapToDistribusjonKanalCode(String distribusjonKanalCodeTo) {
		try {
			if(DITT_NAV.equals(distribusjonKanalCodeTo)){
				return DITTNAV;
			}else{
				return DistribusjonsKanalCode.valueOf(distribusjonKanalCodeTo);
			}
		} catch (IllegalArgumentException e) {
			throw new BestemDokdistKanalMappingException("DistribusjonKanalCode i dokprod støtter ikke enum-verdien " + distribusjonKanalCodeTo);
		}
	}

	private HttpHeaders httpHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(Constants.CALL_ID, MDC.get(Constants.CALL_ID));
		httpHeaders.setContentType(APPLICATION_JSON_UTF8);
		return httpHeaders;
	}
}
