package no.nav.dokdistfordeling.config.props;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("nais")
@Validated
public class NaisProperties {

	// https://doc.nais.io/auth/reference/#texas
	@NotEmpty
	private String tokenEndpoint;
}
