package no.nav.dokdistfordeling.consumer.sts;

import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;
import no.nav.dokdistfordeling.exception.technical.StsTechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.OIDC_TOKEN_CACHE;
import static no.nav.dokdistfordeling.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistfordeling.constants.RetryConstants.MULTIPLIER_SHORT;

@Component
public class StsRestConsumer {

	private final RestTemplate restTemplate;
	private final String stsUrl;

	public StsRestConsumer(@Value("${security-token-service-token.url}") String stsUrl,
						   RestTemplateBuilder restTemplateBuilder,
						   final DokdistfordelingProperties dokdistfordelingProperties) {
		this.stsUrl = stsUrl;
		this.restTemplate = restTemplateBuilder
				.readTimeout(Duration.ofSeconds(20))
				.connectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokdistfordelingProperties.getServiceuser().getUsername(), dokdistfordelingProperties.getServiceuser().getPassword())
				.build();
	}

	@Cacheable(OIDC_TOKEN_CACHE)
	@Retryable(retryFor = AbstractDokdistfordelingTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public String getOidcToken() {
		try {
			return restTemplate.getForObject(stsUrl + "/token?grant_type=client_credentials&scope=openid", StsResponseTo.class).getAccessToken();
		} catch (HttpStatusCodeException e) {
			throw new StsTechnicalException(String.format("Kall mot STS feilet med status=%s feilmelding=%s.", e.getStatusCode(), e.getMessage()), e);
		}
	}
}
