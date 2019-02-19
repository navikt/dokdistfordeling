package no.nav.dokdistfordeling.consumer.dokdist.rdist001;

import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.exception.DokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.exception.DokdistfordelingTechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public class ForsendelseConsumer implements Forsendelse {

	private final String forsendelseV1Url;
	private final RestTemplate restTemplate;

	@Inject
	public ForsendelseConsumer(@Value("${forsendelse.v1.url}") String forsendelseV1Url,
							   RestTemplateBuilder restTemplateBuilder,
							   final ServiceuserAlias serviceuserAlias) {
		this.forsendelseV1Url = forsendelseV1Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	public ForsendelseResponseTo persisterForsendelse(final ForsendelseRequestTo forsendelseRequestTo) {
		try {
			return ForsendelseResponseTo.builder().forsendelseId("1234").build();
			//todo Kommenter inn når tjeneste er på plass
//			return restTemplate.getForObject(this.forsendelseV1Url, ForsendelseResponseTo.class);
		} catch (HttpClientErrorException e) {
			throw new DokdistfordelingFunctionalException(String.format("Kall mot rdist001 feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new DokdistfordelingTechnicalException(String.format("Kall mot rdist001 feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}
}
