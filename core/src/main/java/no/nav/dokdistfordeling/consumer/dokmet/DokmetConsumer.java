package no.nav.dokdistfordeling.consumer.dokmet;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.DokmetFunctionalException;
import no.nav.dokdistfordeling.exception.technical.DokmetTechnicalException;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DokmetConsumer {

	private static final String RESILIENCE4J_INSTANCE = "dokmet";

	private final WebClient webclient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public DokmetConsumer(DokdistfordelingProperties dokdistfordelingProperties,
						  WebClient webclient,
						  CircuitBreakerRegistry circuitBreakerRegistry,
						  RetryRegistry retryRegistry) {
		this.webclient = webclient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	@Cacheable(DOKMET_CACHE)
	public DokumenttypeInfoTo getDokumenttypeInfo(final String dokumenttypeId) {
		return webclient.get()
				.uri(uriBuilder -> uriBuilder.path("/{dokumenttypeId}")
						.build(dokumenttypeId))
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.retrieve()
				.bodyToMono(DokumenttypeInfoTo.class)
				.onErrorMap(this::mapError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			return new DokmetFunctionalException(
					format("Kall mot dokmet feilet funksjonelt med statuskode=%s, feilmelding=%s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			return new DokmetTechnicalException(
					format("Kall mot dokmet feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}
}
