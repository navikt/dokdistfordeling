package no.nav.dokdistfordeling.consumer.tkat020;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.DokumenttypeInfoFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.DokumenttypeInfoTechnicalException;
import no.nav.dokdistfordeling.metrics.ConsumerMonitor;
import no.nav.dokdistfordeling.security.AzureToken;
import no.nav.dokdistfordeling.security.WebClientAzureAuthentication;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.util.Objects.isNull;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
class DokumentkatalogAdminConsumer implements DokumentkatalogAdmin {

	private final WebClient webclient;

	public DokumentkatalogAdminConsumer(DokdistfordelingProperties dokdistfordelingProperties,
										WebClient webclient, AzureToken azureToken) {
		this.webclient = webclient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.filter(new WebClientAzureAuthentication(azureToken, dokdistfordelingProperties.getEndpoints().getDokmet().getScope()))
				.build();
	}

	@Cacheable(TKAT020_CACHE)
	@ConsumerMonitor(value = "dok_metric", extraTags = {"process", "getDokumenttypeInfo"}, histogram = true)
	@Retryable(include = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DokumenttypeInfoTo getDokumenttypeInfo(final String dokumenttypeId) {
		DokumentTypeInfoToV4 dokumentTypeInfoToV4 = webclient.get()
				.uri("/" + dokumenttypeId)
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.retrieve()
				.bodyToMono(DokumentTypeInfoToV4.class)
				.doOnError(this::handleError)
				.block();
		return isNull(dokumentTypeInfoToV4) ? null : mapResponse(dokumentTypeInfoToV4);
	}

	private DokumenttypeInfoTo mapResponse(final DokumentTypeInfoToV4 response) {
		return DokumenttypeInfoTo.builder()
				.dokumentTittel(response.getDokumentTittel())
				.build();
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new DokumenttypeInfoFunctionalException(
					String.format("Kall mot tkat020 feilet funksjonelt med statuskode=%s Feilmelding=%s",
							response.getRawStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new DokumenttypeInfoTechnicalException(
					String.format("Kall mot tkat020 feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}
}
