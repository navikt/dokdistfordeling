package no.nav.dokdistfordeling.consumer.dokdistadmin;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.DokdistadminFunctionalException;
import no.nav.dokdistfordeling.exception.technical.DokdistadminTechnicalException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.http.codec.HttpCodecsProperties;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKDISTADMIN;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class DokdistadminConsumer {

	private static final String RESILIENCE4J_INSTANCE = "dokdistadmin";

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public DokdistadminConsumer(DokdistfordelingProperties dokdistfordelingProperties,
								WebClient webClient,
								HttpCodecsProperties codecProperties,
								CircuitBreakerRegistry circuitBreakerRegistry,
								RetryRegistry retryRegistry) {
		var clientHttpConnector = new ReactorClientHttpConnector(HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.ofSeconds(60)));

		this.webClient = webClient.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokdistadmin().getUrl())
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer ->
								configurer.defaultCodecs().maxInMemorySize((int) codecProperties.getMaxInMemorySize().toBytes()))
						.build())
				.clientConnector(clientHttpConnector)
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	public String opprettForsendelse(final OpprettForsendelseRequestTo opprettForsendelseRequestTo) {
		var bestillingsId = opprettForsendelseRequestTo.getBestillingsId();

		log.info("opprettForsendelse oppretter forsendelse med bestillingsId={}", bestillingsId);

		var forsendelseId = webClient.post()
				.headers(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(CALL_ID)))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(opprettForsendelseRequestTo)
				.retrieve()
				.bodyToMono(OpprettForsendelseResponseTo.class)
				.onErrorMap(this::mapError)
				.map(response -> new Forsendelse(response.forsendelseId()).getForsendelseId())
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();

		log.info("opprettForsendelse har opprettet forsendelse med forsendelseId={} og bestillingsId={}", forsendelseId, bestillingsId);

		return forsendelseId;
	}

	public void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelse) {
		log.info("oppdaterForsendelse oppdaterer forsendelse med forsendelseId={}", oppdaterForsendelse.forsendelseId());

		webClient.put()
				.uri("/oppdaterforsendelse")
				.headers(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(CALL_ID)))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterForsendelse)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();

		log.info("oppdaterForsendelse har oppdatert forsendelse med forsendelseId={} til forsendelseStatus={}",
				oppdaterForsendelse.forsendelseId(), oppdaterForsendelse.forsendelseStatus());
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			return new DokdistadminFunctionalException(
					format("Kall mot rdist001 feilet funksjonelt med status: %s, feilmelding: %s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			return new DokdistadminTechnicalException(
					format("Kall mot rdist001 feilet feilet teknisk med feilmelding: %s", error.getMessage()),
					error);
		}
	}

}