package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.BestemDokdistKanalFunctionalException;
import no.nav.dokdistfordeling.exception.functional.BestemDokdistKanalMappingException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.BestemDokdistKanalTechnicalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKDISTKANAL;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.DITT_NAV;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Component
public class BestemDokdistkanalRestConsumer implements BestemDistribusjonskanal {
	private final WebClient webClient;

	public BestemDokdistkanalRestConsumer(final DokdistfordelingProperties dokdistfordelingProperties,
										  WebClient webClient) {
		DokdistfordelingProperties.AzureEndpoint dokdistkanal = dokdistfordelingProperties.getEndpoints().getDokdistkanal();
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanal.getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.setContentType(APPLICATION_JSON))
				.build();
	}

	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DistribusjonKanalCode bestemKanal(DokDistKanalRequest dokDistKanalRequest) {
		return webClient.post()
				.uri(uriBuilder -> uriBuilder.path("/rest/bestemDistribusjonskanal")
						.build())
				.headers(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(CALL_ID)))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTKANAL))
				.bodyValue(dokDistKanalRequest)
				.retrieve()
				.bodyToMono(BestemDistribusjonskanalResponse.class)
				.map(dokDistKanalResponse -> mapToDistribusjonKanalCode(dokDistKanalResponse.distribusjonskanal()))
				.doOnError(this::handleErrors)
				.block();
	}

	private DistribusjonKanalCode mapToDistribusjonKanalCode(String distribusjonKanal) {
		try {
			if (DITT_NAV.equals(distribusjonKanal)) {
				return DITTNAV;
			} else {
				return DistribusjonKanalCode.valueOf(distribusjonKanal);
			}
		} catch (IllegalArgumentException e) {
			throw new BestemDokdistKanalMappingException("DistribusjonKanalCode i dokdist støtter ikke enum-verdien " + distribusjonKanal);
		}
	}

	private void handleErrors(Throwable error) {
		if (error instanceof WebClientResponseException webException) {
			ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);

			if (webException.getStatusCode().is4xxClientError()) {
				throw new BestemDokdistKanalFunctionalException("Kall mot bestemDistribusjonskanal feilet funksjonelt med feilmelding=" + problemDetail, error);
			}

			throw new BestemDokdistKanalTechnicalException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + problemDetail, error);
		} else {
			throw new BestemDokdistKanalTechnicalException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + error.getMessage(), error);
		}
	}

}
