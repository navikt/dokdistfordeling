package no.nav.dokdistfordeling.consumer.dokmet;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.DokmetFunctionalException;
import no.nav.dokdistfordeling.exception.technical.DokmetTechnicalException;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DokmetConsumer {

	private final WebClient webclient;

	public DokmetConsumer(DokdistfordelingProperties dokdistfordelingProperties,
						  WebClient webclient) {
		this.webclient = webclient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Cacheable(DOKMET_CACHE)
	@Retryable(retryFor = DokmetTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DokumenttypeInfoTo getDokumenttypeInfo(final String dokumenttypeId) {
		return webclient.get()
				.uri(uriBuilder -> uriBuilder.path("/{dokumenttypeId}")
						.build(dokumenttypeId))
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.retrieve()
				.bodyToMono(DokumenttypeInfoTo.class)
				.doOnError(this::handleError)
				.block();
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new DokmetFunctionalException(
					format("Kall mot dokmet feilet funksjonelt med statuskode=%s, feilmelding=%s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			throw new DokmetTechnicalException(
					format("Kall mot dokmet feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}
}
