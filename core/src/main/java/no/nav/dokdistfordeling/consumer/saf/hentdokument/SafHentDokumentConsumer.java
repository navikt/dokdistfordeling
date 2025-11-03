package no.nav.dokdistfordeling.consumer.saf.hentdokument;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.functional.SafHentDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.technical.SafHentDokumentTechnicalException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.consumer.token.NaisTexasRequestInterceptor.TARGET_SCOPE;

@Component
public class SafHentDokumentConsumer {

	private static final String RESILIENCE4J_INSTANCE = "safhentdokument";

	private final RestClient restClientTexas;
	private final String safScope;
	private final ObjectMapper objectMapper;

	public SafHentDokumentConsumer(RestClient restClientTexas,
								   DokdistfordelingProperties dokdistfordelingProperties,
								   ObjectMapper objectMapper) {
		this.restClientTexas = restClientTexas.mutate()
				.baseUrl(dokdistfordelingProperties.getEndpoints().getSaf().getUrl())
				.build();
		this.safScope = dokdistfordelingProperties.getEndpoints().getSaf().getScope();
		this.objectMapper = objectMapper;
	}

	@Retry(name = RESILIENCE4J_INSTANCE)
	@CircuitBreaker(name = RESILIENCE4J_INSTANCE)
	public byte[] hentDokument(String journalpostId, String dokumentInfoId, String variantFormat) {
		return restClientTexas.get()
				.uri("/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}", journalpostId, dokumentInfoId, variantFormat)
				.attribute(TARGET_SCOPE, safScope)
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
					ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
					throw new SafHentDokumentFunctionalException(format("Kall mot saf hentdokument feilet funksjonelt med status=%s, feilmelding=%s",
							response.getStatusCode(), problemDetail));
				})
				.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
					ProblemDetail problemDetail = objectMapper.readValue(response.getBody(), ProblemDetail.class);
					throw new SafHentDokumentTechnicalException(format("Kall mot saf hentdokument feilet teknisk med status=%s, feilmelding=%s",
							response.getStatusCode(), problemDetail));
				})
				.body(byte[].class);
	}
}
