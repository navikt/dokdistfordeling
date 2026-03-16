package no.nav.dokdistfordeling.consumer.dokarkiv;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.JournalpostApiFunctionalException;
import no.nav.dokdistfordeling.exception.technical.JournalpostApiTechnicalException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKARKIV;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class JournalpostApi {

	private static final String RESILIENCE4J_INSTANCE = "dokarkiv";

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public JournalpostApi(DokdistfordelingProperties dokdistfordelingProperties,
						  WebClient webClient,
						  CircuitBreakerRegistry circuitBreakerRegistry,
						  RetryRegistry retryRegistry) {
		this.webClient = webClient.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokarkiv().getUrl())
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	public void oppdaterDistribusjonsinfo(String journalpostId, OppdaterDistribusjonsinfoTo oppdaterDistibusjonsinfoTo) {
		webClient.patch()
				.uri("/{journalpostId}/oppdaterDistribusjonsinfo", validateJournalpostId(journalpostId))
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.body(Mono.just(oppdaterDistibusjonsinfoTo), OppdaterDistribusjonsinfoTo.class)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();
	}

	public OppdaterJournalpostResponse oppdaterJournalpost(long journalpostId, OppdaterJournalpostRequest oppdaterJournalpostRequest) {
		return webClient.put()
				.uri("/{journalpostId}", journalpostId)
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.body(Mono.just(oppdaterJournalpostRequest), OppdaterJournalpostRequest.class)
				.retrieve()
				.bodyToMono(OppdaterJournalpostResponse.class)
				.onErrorMap(this::mapError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()
			&& !TOO_MANY_REQUESTS.isSameCodeAs(response.getStatusCode())) {
			return new JournalpostApiFunctionalException(
					format("Kall mot JournalpostAPI feilet med status=%s, feilmelding=%s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			return new JournalpostApiTechnicalException(
					format("Kall mot JournalpostAPI feilet med feilmelding=%s", error.getMessage()),
					error);
		}
	}

	private Long validateJournalpostId(String journalpostId) {
		try {
			return Long.valueOf(journalpostId);
		} catch (NumberFormatException e) {
			throw new JournalpostApiTechnicalException(
					format("%s er ikke en gyldig journalpostId. Kan ikke kalle journalpostApi.", journalpostId), e);
		}
	}
}
