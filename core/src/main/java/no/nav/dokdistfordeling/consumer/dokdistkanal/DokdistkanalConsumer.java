package no.nav.dokdistfordeling.consumer.dokdistkanal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.DokdistkanalFunctionalException;
import no.nav.dokdistfordeling.exception.functional.DokdistkanalMappingException;
import no.nav.dokdistfordeling.exception.functional.DokdistkanalUnauthorizedException;
import no.nav.dokdistfordeling.exception.technical.DokdistkanalTechnicalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKDISTKANAL;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.DITT_NAV;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class DokdistkanalConsumer {

	private static final String RESILIENCE4J_INSTANCE = "dokdistkanal";

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public DokdistkanalConsumer(DokdistfordelingProperties dokdistfordelingProperties,
								WebClient webClient,
								CircuitBreakerRegistry circuitBreakerRegistry,
								RetryRegistry retryRegistry) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokdistkanal().getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.setContentType(APPLICATION_JSON))
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	public DistribusjonKanalCode bestemDistribusjonskanal(BestemDistribusjonskanalRequest request) {
		return webClient.post()
				.uri(uriBuilder -> uriBuilder.path("/rest/bestemDistribusjonskanal").build())
				.headers(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(CALL_ID)))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTKANAL))
				.bodyValue(request)
				.retrieve()
				.bodyToMono(BestemDistribusjonskanalResponse.class)
				.map(this::mapToDistribusjonKanalCode)
				.onErrorMap(this::mapError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();
	}

	private DistribusjonKanalCode mapToDistribusjonKanalCode(BestemDistribusjonskanalResponse response) {
		if (response == null) {
			throw new DokdistkanalTechnicalException("Endepunktet bestemDistribusjonskanal returnerte null som respons");
		}

		var distribusjonskanal = response.distribusjonskanal();
		try {
			if (DITT_NAV.equals(distribusjonskanal)) {
				return DITTNAV;
			} else {
				return DistribusjonKanalCode.valueOf(distribusjonskanal);
			}
		} catch (IllegalArgumentException e) {
			throw new DokdistkanalMappingException("DistribusjonKanalCode i dokdist støtter ikke enum-verdien " + distribusjonskanal);
		}
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException webException) {
			ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);

			if (webException.getStatusCode().is4xxClientError()) {
				if (UNAUTHORIZED.isSameCodeAs(webException.getStatusCode())) {
					return new DokdistkanalUnauthorizedException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + problemDetail, error);
				}
				return new DokdistkanalFunctionalException("Kall mot bestemDistribusjonskanal feilet funksjonelt med feilmelding=" + problemDetail, error);
			}

			return new DokdistkanalTechnicalException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + problemDetail, error);
		} else {
			return new DokdistkanalTechnicalException("Kall mot bestemDistribusjonskanal feilet teknisk med feilmelding=" + error.getMessage(), error);
		}
	}

}