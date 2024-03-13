package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.BestemDistribusjonskanalFunctionalException;
import no.nav.dokdistfordeling.exception.functional.BestemDistribusjonskanalUnauthorizedException;
import no.nav.dokdistfordeling.exception.functional.BestemDistribusjonskanalMappingException;
import no.nav.dokdistfordeling.exception.technical.BestemDistribusjonskanalTechnicalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKDISTKANAL;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.DITT_NAV;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class BestemDistribusjonskanalConsumer {

	private final WebClient webClient;

	public BestemDistribusjonskanalConsumer(final DokdistfordelingProperties dokdistfordelingProperties,
											WebClient webClient) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokdistkanal().getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.setContentType(APPLICATION_JSON))
				.build();
	}

	@Retryable(retryFor = BestemDistribusjonskanalTechnicalException.class,
			noRetryFor = BestemDistribusjonskanalMappingException.class,
			backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DistribusjonKanalCode bestemDistribusjonskanal(BestemDistribusjonskanalRequest request) {
		return webClient.post()
				.uri(uriBuilder -> uriBuilder.path("/rest/bestemDistribusjonskanal").build())
				.headers(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(CALL_ID)))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTKANAL))
				.bodyValue(request)
				.retrieve()
				.bodyToMono(BestemDistribusjonskanalResponse.class)
				.map(this::mapToDistribusjonKanalCode)
				.doOnError(this::handleErrors)
				.block();
	}

	private DistribusjonKanalCode mapToDistribusjonKanalCode(BestemDistribusjonskanalResponse response) {
		if (response == null) {
			throw new BestemDistribusjonskanalTechnicalException("Endepunktet bestemDistribusjonskanal returnerte null som respons");
		}

		var distribusjonskanal = response.distribusjonskanal();
		try {
			if (DITT_NAV.equals(distribusjonskanal)) {
				return DITTNAV;
			} else {
				return DistribusjonKanalCode.valueOf(distribusjonskanal);
			}
		} catch (IllegalArgumentException e) {
			throw new BestemDistribusjonskanalMappingException("DistribusjonKanalCode i dokdist støtter ikke enum-verdien " + distribusjonskanal);
		}
	}

	private void handleErrors(Throwable error) {
		if (error instanceof WebClientResponseException webException) {
			ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);

			if (webException.getStatusCode().is4xxClientError()) {
				if (UNAUTHORIZED.isSameCodeAs(webException.getStatusCode())) {
					throw new BestemDistribusjonskanalUnauthorizedException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + problemDetail, error);
				}
				throw new BestemDistribusjonskanalFunctionalException("Kall mot bestemDistribusjonskanal feilet funksjonelt med feilmelding=" + problemDetail, error);
			}

			throw new BestemDistribusjonskanalTechnicalException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + problemDetail, error);
		} else {
			throw new BestemDistribusjonskanalTechnicalException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + error.getMessage(), error);
		}
	}

}
