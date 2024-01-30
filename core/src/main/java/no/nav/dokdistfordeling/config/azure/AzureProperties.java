package no.nav.dokdistfordeling.config.azure;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@ConfigurationProperties("azure")
@Validated
public record AzureProperties(
		@NotEmpty
		String openidConfigTokenEndpoint,
		@NotEmpty
		String appClientId,
		@NotEmpty
		String appClientSecret) {
}
