package no.nav.dokdistfordeling.consumer.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.PdlHentFolkeregisteridentForAktoerIdFunctionalException;
import no.nav.dokdistfordeling.exception.functional.PdlPersonIkkeFunnetFunctionalException;
import no.nav.dokdistfordeling.exception.technical.PdlHentFolkeregisteridentForAktoerIdTechnicalException;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

import static no.nav.dokdistfordeling.config.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_PDL;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistfordeling.consumer.pdl.IdentType.FOLKEREGISTERIDENT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	private static final String PERSON_IKKE_FUNNET_CODE = "not_found";

	// https://pdldocs-navno.msappproxy.net/ekstern/index.html#_dokumenter_hjemmel
	private static final String HEADER_PDL_BEHANDLINGSNUMMER = "behandlingsnummer";

	// https://behandlingskatalog.nais.adeo.no/process/purpose/ARKIVPLEIE/756fd557-b95e-4b20-9de9-6179fb8317e6
	private static final String ARKIVPLEIE_BEHANDLINGSNUMMER = "B315";

	private final WebClient webClient;

	public PdlGraphQLConsumer(WebClient webClient,
							  DokdistfordelingProperties dokdistfordelingProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getPdl().getUrl())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.setContentType(APPLICATION_JSON);
					httpHeaders.set(HEADER_PDL_BEHANDLINGSNUMMER, ARKIVPLEIE_BEHANDLINGSNUMMER);
				})
				.build();
	}

	@Retryable(retryFor = HttpServerErrorException.class)
	public String hentFolkeregisteridentForAktoerId(final String aktorId) {

		var pdlHentIdenterResponse = webClient.post()
				.headers(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(NAV_CALL_ID)))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_PDL))
				.bodyValue(mapRequest(aktorId))
				.retrieve()
				.bodyToMono(PdlHentIdenterResponse.class)
				.doOnError(handlePdlErrors())
				.block();

		if (pdlHentIdenterResponse.getErrors() == null || pdlHentIdenterResponse.getErrors().isEmpty()) {
			return getFolkeregisteridentFromResponse(pdlHentIdenterResponse);
		} else {
			if (PERSON_IKKE_FUNNET_CODE.equals(pdlHentIdenterResponse.getErrors().get(0).getExtensions().getCode())) {
				throw new PdlPersonIkkeFunnetFunctionalException("Fant ikke folkeregisterident for person i PDL.");
			}
			throw new PdlHentFolkeregisteridentForAktoerIdFunctionalException("Kunne ikke hente folkeregisterident fra PDL." + pdlHentIdenterResponse.getErrors());
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
				.orElseThrow(() -> new PdlHentFolkeregisteridentForAktoerIdFunctionalException("Kunne ikke hente folkeregisterident fra PDL. Respons fra PDL inneholdt ikke gjeldende folkeregisterident"));
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

	private Consumer<Throwable> handlePdlErrors() {
		return error -> {
			if (error instanceof WebClientResponseException webException && webException.getStatusCode().is4xxClientError()) {
				ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);
				throw new PdlHentFolkeregisteridentForAktoerIdFunctionalException("Funksjonell feil ved kall mot PDL, feilmelding=" + problemDetail);
			} else {
				throw new PdlHentFolkeregisteridentForAktoerIdTechnicalException("Teknisk feil ved kall mot PDL, melding=" + error.getMessage(), error);
			}
		};
	}
}
