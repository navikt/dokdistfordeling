package no.nav.dokdistfordeling.config.dokarkiv;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Data
@ConfigurationProperties("journalpost.api")
@Validated
public class JournalpostApiConfig {

	@NotEmpty
	private String baseUrl;

}
