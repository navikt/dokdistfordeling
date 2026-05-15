package no.nav.dokdistfordeling.consumer.pdl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.PdlFunctionalException;
import no.nav.dokdistfordeling.exception.functional.PdlPersonIkkeFunnetFunctionalException;
import no.nav.dokdistfordeling.exception.technical.PdlTechnicalException;
import org.slf4j.MDC;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Optional;

import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_PDL;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistfordeling.consumer.pdl.IdentType.FOLKEREGISTERIDENT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	private static final String RESILIENCE4J_INSTANCE = "pdl";

	private static final String PERSON_IKKE_FUNNET_CODE = "not_found";
	// https://pdldocs-navno.msappproxy.net/ekstern/index.html#_dokumenter_hjemmel
	private static final String HEADER_PDL_BEHANDLINGSNUMMER = "behandlingsnummer";
	// https://behandlingskatalog.nais.adeo.no/process/purpose/ARKIVPLEIE/756fd557-b95e-4b20-9de9-6179fb8317e6
	private static final String ARKIVPLEIE_BEHANDLINGSNUMMER = "B315";

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public PdlGraphQLConsumer(WebClient webClient,
							  DokdistfordelingProperties dokdistfordelingProperties,
							  CircuitBreakerRegistry circuitBreakerRegistry,
							  RetryRegistry retryRegistry) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getPdl().getUrl())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.setContentType(APPLICATION_JSON);
					httpHeaders.set(HEADER_PDL_BEHANDLINGSNUMMER, ARKIVPLEIE_BEHANDLINGSNUMMER);
				})
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	@Retryable(includes = PdlTechnicalException.class)
	public String hentFolkeregisteridentForAktoerId(final String aktorId) {

		var pdlHentIdenterResponse = webClient.post()
				.headers(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(CALL_ID)))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_PDL))
				.bodyValue(mapRequest(aktorId))
				.retrieve()
				.bodyToMono(PdlHentIdenterResponse.class)
				.onErrorMap(this::mapError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();

		if (isEmpty(pdlHentIdenterResponse.getErrors())) {
			return getFolkeregisteridentFromResponse(pdlHentIdenterResponse);
		} else {
			if (PERSON_IKKE_FUNNET_CODE.equals(pdlHentIdenterResponse.getErrors().get(0).getExtensions().getCode())) {
				throw new PdlPersonIkkeFunnetFunctionalException("Fant ikke folkeregisterident for person i PDL.");
			}
			throw new PdlFunctionalException("Kunne ikke hente folkeregisterident fra PDL pga følgende feilmeldinger:" + pdlHentIdenterResponse.getErrors());
		}
	}

	private String getFolkeregisteridentFromResponse(PdlHentIdenterResponse pdlHentIdenterResponse) {
		return Optional.ofNullable(pdlHentIdenterResponse.getData())
				.map(PdlHentIdenterResponse.PdlHentIdenterData::getHentIdenter)
				.map(PdlHentIdenterResponse.PdlIdenter::getIdenter)
				.flatMap(identer -> identer.stream()
						.filter(it -> it.getGruppe() == FOLKEREGISTERIDENT)
						.filter(it -> !it.isHistorisk())
						.map(PdlHentIdenterResponse.PdlIdentTo::getIdent)
						.findFirst())
				.orElseThrow(() -> new PdlFunctionalException("Kunne ikke hente folkeregisterident fra PDL. Respons fra PDL inneholdt ikke gjeldende folkeregisterident"));
	}

	private PdlRequest mapRequest(final String aktoerId) {
		final HashMap<String, Object> variables = new HashMap<>();
		variables.put("ident", aktoerId);
		return PdlRequest.builder()
				.query("""
						query($ident: ID!) {
						  hentIdenter(ident: $ident, historikk: false, grupper: FOLKEREGISTERIDENT) {
						    identer {
						      ident
						      historisk
						      gruppe
						    }
						  }
						}
						""")
				.variables(variables)
				.build();
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException webException && webException.getStatusCode().is4xxClientError()) {
			return new PdlFunctionalException("Funksjonell feil ved kall mot PDL, feilmelding=" + webException.getMessage());
		} else {
			return new PdlTechnicalException("Teknisk feil ved kall mot PDL, feilmelding=" + error.getMessage(), error);
		}
	}
}
