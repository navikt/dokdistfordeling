package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.BestemDokdistKanalFunctionalException;
import no.nav.dokdistfordeling.exception.functional.BestemDokdistKanalMappingException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.BestemDokdistKanalTechnicalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.security.AzureToken;
import no.nav.dokdistfordeling.security.WebClientAzureAuthentication;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.DITT_NAV;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.DITTNAV;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class BestemDokdistkanalRestConsumer implements BestemDistribusjonskanal {
	private final WebClient webClient;

	public BestemDokdistkanalRestConsumer(final DokdistfordelingProperties dokdistfordelingProperties,
										  WebClient webClient,
										  AzureToken azureToken) {
		DokdistfordelingProperties.AzureEndpoint dokdistkanal = dokdistfordelingProperties.getEndpoints().getDokdistkanal();
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanal.getUrl())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.setContentType(APPLICATION_JSON);
				})
				.filter(new WebClientAzureAuthentication(azureToken, dokdistkanal.getScope()))
				.build();
	}

	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DistribusjonsKanalCode bestemKanal(DokDistKanalRequest dokDistKanalRequest) {
		final String callId = MDC.get(CALL_ID);
		return webClient.post()
				.headers(httpHeaders -> {
					httpHeaders.set(CALL_ID, callId);
					httpHeaders.set(NAV_CALL_ID, callId);
				})
				.bodyValue(dokDistKanalRequest)
				.retrieve()
				.bodyToMono(DokDistKanalResponseTo.class)
				.map(dokDistKanalResponse -> mapToDistribusjonKanalCode(dokDistKanalResponse.getDistribusjonsKanal()))
				.doOnError(handleDokdistKanalErrors())
				.block();

	}

	private DistribusjonsKanalCode mapToDistribusjonKanalCode(String distribusjonKanal) {
		try {
			if (DITT_NAV.equals(distribusjonKanal)) {
				return DITTNAV;
			} else {
				return DistribusjonsKanalCode.valueOf(distribusjonKanal);
			}
		} catch (IllegalArgumentException e) {
			throw new BestemDokdistKanalMappingException("DistribusjonKanalCode i dokdist støtter ikke enum-verdien " + distribusjonKanal);
		}
	}

	private Consumer<Throwable> handleDokdistKanalErrors() {
		return error -> {
			if (error instanceof WebClientResponseException webException && webException.getStatusCode().is4xxClientError()) {
				ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);
				throw new BestemDokdistKanalFunctionalException("BestemDokdistkanal feilet med statusCode=" + problemDetail.getStatus() + ", problem=" + problemDetail);
			} else {
				throw new BestemDokdistKanalTechnicalException("BestemDokdistkanal feilet med melding=" + error.getMessage(), error);
			}
		};
	}

}
