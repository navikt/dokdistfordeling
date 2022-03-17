package no.nav.dokdistfordeling.config.dokarkiv;

import lombok.Data;
import org.apache.http.HttpHeaders;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;

import javax.validation.constraints.NotEmpty;

@Data
@ConfigurationProperties("journalpost.api")
@Validated
public class JournalpostApiConfig {

	@NotEmpty
	private String baseUrl;

	@Bean("journalpostApiClient")
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder
				.clone()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
