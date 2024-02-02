package no.nav.dokdistfordeling.consumer.tkat020;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.DokumenttypeInfoFunctionalException;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.DokumenttypeInfoTechnicalException;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKMET;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
class DokumentkatalogAdminConsumer implements DokumentkatalogAdmin {

	private final WebClient webclient;

	public DokumentkatalogAdminConsumer(DokdistfordelingProperties dokdistfordelingProperties,
										WebClient webclient) {
		this.webclient = webclient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Cacheable(TKAT020_CACHE)
	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DokumenttypeInfoTo getDokumenttypeInfo(final String dokumenttypeId) {
		DokumentTypeInfoToV4 dokumentTypeInfoToV4 = webclient.get()
				.uri("/" + dokumenttypeId)
				.header(NAV_CALL_ID, MDC.get(CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKMET))
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
					format("Kall mot tkat020 feilet funksjonelt med statuskode=%s Feilmelding=%s", response.getStatusCode(), response.getMessage()),
					error);
		} else {
			throw new DokumenttypeInfoTechnicalException(
					format("Kall mot tkat020 feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}
}
